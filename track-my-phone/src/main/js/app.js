/**
 * Copyright (c) 2015, Bosch Software Innovations GmbH, Germany
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the Bosch Software Innovations GmbH, Germany nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
'use strict';
(function ($, Promise, window, geolocation) {
    var THROTTLE_RATE_MS = 1000;

    var ui = {};
    ui.window = $(window);
    ui.geolocationLabel = $('#geolocation-label');
    ui.orientationLabel = $('#orientation-label');
    ui.permissionCheckbox = $('#permissions').find('input[type=checkbox]');
    ui.loginModal = $('#login-modal');
    ui.loginForm = ui.loginModal.find('#login-form');
    ui.registerForm = ui.loginModal.find('#login-register-form');
    ui.registerFormResult = ui.loginModal.find('#login-register-form-result');

    var authContext;
    var device;

    ui.loginModal.modal('toggle');

    ui.loginForm.on('submit', function () {
        var userName = this.username.value;
        var password = this.password.value;
        authContext = {
            userName: userName,
            authData: encodeBase64(userName + ':' + password)
        };

        registerThing()
            .then(function onSuccess(thing) {
                device = thing;
                logger.info("Thing '" + device.thingId + "' registered successfully.");

                // listen for geolocation changes
                if (geolocation) {
                    geolocation.watchPosition(throttle(geolocationChanged, THROTTLE_RATE_MS));
                } else {
                    ui.geolocationLabel.text('Not available.');
                }

                // listen for orientation changes and transmit data every 100ms
                ui.window.on('deviceorientation', throttle(orientationChanged, THROTTLE_RATE_MS));

                // listen for permission changes
                ui.permissionCheckbox.on('change', permissionChanged);
                ui.permissionCheckbox.each(function () {
                    var el = this;
                    var aclEntry = device.acl[el.dataset.sid];
                    if (aclEntry) {
                        el.checked = aclEntry.READ;
                    }
                });

                // close the login dialog
                ui.loginModal.modal('toggle');
            }, function onError(error) {
                logger.error(error);
            });

        return false;
    });

    ui.registerForm.on('submit', function () {
        $.ajax({
            type: 'POST',
            url: '/registerUser',
            data: ui.registerForm.serialize()
        }).then(function onSuccess(data) {
            ui.registerFormResult.empty().append(data);
            ui.registerFormResult.show();
        }, function onError(data) {
            ui.registerFormResult.empty().append(data.responseText || data.statusText);
            ui.registerFormResult.show();
        });

        return false;
    });

    function geolocationChanged(geoData) {
        var position = {
            latitude: geoData.coords.latitude,
            longitude: geoData.coords.longitude
        };

        ui.geolocationLabel.text(JSON.stringify(position, null, 3));
        updateGeolocation(position)
            .then(function onSuccess(data) {
                logger.info('Geolocation updated successfully.');
            }, function onError(error) {
                logger.error(error);
            });
    }

    function orientationChanged(event) {
        var eventData = (event.alpha && event.beta && event.gamma) ? event : event.originalEvent;
        var orientation = {
            x: eventData.beta,
            y: eventData.gamma,
            z: eventData.alpha
        };

        ui.orientationLabel.text(JSON.stringify(orientation, null, 3));

        updateOrientation(orientation)
            .then(function onSuccess(data) {
                logger.info('Orientation updated successfully.');
            }, function onError(error) {
                logger.error(error);
            });
    }

    function permissionChanged() {
        var sid = this.dataset.sid;

        if (this.checked) {
            var aclEntry = {
                READ: true,
                WRITE: false,
                ADMINISTRATE: false
            };
            grantPermissions(sid, aclEntry);
            logger.info("Permissions granted for '" + sid + "'.");
        } else {
            revokePermissions(sid);
            logger.info("Permissions revoked for '" + sid + "'.");
        }
    }


    function registerThing() {
        return new Promise(function (resolve, reject) {
            var thingId = "track.my.phone:device-of-" + authContext.userName;

            $.ajax({
                type: 'GET',
                url: '/cr/1/things/' + thingId,
                beforeSend: setAuthorizationHeader
            }).then(resolve, function onError(data) {
                var errorJson = JSON.parse(data.responseText);
                if (errorJson.status === 404) {
                    createThing(thingId)
                        .then(resolve, function onError(data) {
                            reject(data.responseText || data.statusText);
                        });
                } else {
                    reject(data.responseText || data.statusText);
                }
            });
        });
    }

    function createThing(thingId) {
        return new Promise(function (resolve, reject) {
            var newThing = {
                features: {
                    geolocation: {
                        properties: {
                            _definition: 'org.eclipse.vorto.Geolocation:1.0.0',
                            geoposition: {
                                latitude: null,
                                longitude: null
                            },
                            accuracy: null
                        }
                    },
                    orientation: {
                        properties: {
                            x: null,
                            y: null,
                            z: null
                        }
                    }
                }
            };

            $.ajax({
                type: 'PUT',
                url: '/cr/1/things/' + thingId,
                data: JSON.stringify(newThing),
                contentType: 'application/json; charset=UTF-8',
                beforeSend: setAuthorizationHeader
            }).then(resolve, function onError(data) {
                reject(data.responseText || data.statusText)
            });
        });
    }

    function updateGeolocation(position) {
        return updateFeatureProperty(device, 'geolocation', 'geoposition', position);
    }

    function updateOrientation(orientation) {
        return updateFeatureProperties(device, 'orientation', orientation);
    }

    function updateFeatureProperty(thing, feature, jsonPointer, jsonValue) {
        return new Promise(function (resolve, reject) {
            if (thing) {
                $.ajax({
                    type: 'PUT',
                    url: '/cr/1/things/' + thing.thingId + '/features/' + feature + '/properties/' + jsonPointer,
                    data: JSON.stringify(jsonValue),
                    contentType: 'application/json; charset=UTF-8',
                    beforeSend: setAuthorizationHeader
                }).then(resolve, function onError(data) {
                    reject(data.responseText || data.statusText)
                });
            } else {
                reject('no thing registered');
            }
        });
    }

    function updateFeatureProperties(thing, feature, jsonValue) {
        return new Promise(function (resolve, reject) {
            if (thing) {
                $.ajax({
                    type: 'PUT',
                    url: '/cr/1/things/' + thing.thingId + '/features/' + feature + '/properties',
                    data: JSON.stringify(jsonValue),
                    contentType: 'application/json; charset=UTF-8',
                    beforeSend: setAuthorizationHeader
                }).then(resolve, function onError(data) {
                    reject(data.responseText || data.statusText)
                });
            } else {
                reject('no thing registered');
            }
        });
    }

    function grantPermissions(sid, aclEntry) {
        return new Promise(function (resolve, reject) {
            $.ajax({
                type: 'PUT',
                url: '/cr/1/things/' + device.thingId + '/acl/' + sid,
                data: JSON.stringify(aclEntry),
                contentType: 'application/json; charset=UTF-8',
                beforeSend: setAuthorizationHeader
            }).then(resolve, function onError(data) {
                reject(data.responseText || data.statusText)
            });
        });
    }

    function revokePermissions(sid) {
        return new Promise(function (resolve, reject) {
            $.ajax({
                type: 'DELETE',
                url: '/cr/1/things/' + device.thingId + '/acl/' + sid,
                beforeSend: setAuthorizationHeader
            }).then(resolve, function onError(data) {
                reject(data.responseText || data.statusText)
            });
        });
    }


    function setAuthorizationHeader(xhr) {
        xhr.setRequestHeader("Authorization", "Basic " + authContext.authData);
    }

    function encodeBase64(value) {
        return window.btoa(value);
    }

    function throttle(fn, threshhold, scope) {
        threshhold || (threshhold = 250);
        var last,
            deferTimer;
        return function () {
            var context = scope || this;

            var now = +new Date,
                args = arguments;
            if (last && now < last + threshhold) {
                // hold on to it
                clearTimeout(deferTimer);
                deferTimer = setTimeout(function () {
                    last = now;
                    fn.apply(context, args);
                }, threshhold);
            } else {
                last = now;
                fn.apply(context, args);
            }
        };
    }

    var logger = {
        info: function (message) {
            console.log(new Date().toISOString() + ": INFO - " + message);
        },
        warn: function (message) {
            console.log(new Date().toISOString() + ": WARN - " + message);
        },
        error: function (message) {
            console.log(new Date().toISOString() + ": ERROR - " + message);
        }
    }
})(jQuery, Promise, window, navigator.geolocation);
