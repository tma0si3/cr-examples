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

function RestController($scope, $log, Thing, Things, ThingAttribute) {
    $scope.responses = [];
    $scope.thing = new Thing();
    $scope.thing.attributes = {};
    $scope.thing.features = {};

    $scope.getThing = function (thingId, fields) {
        if (!thingId || thingId === '') {
            throw new Error('The thingId must not be undefined or empty!');
        }

        if (fields === '') {
            // sending an empty string selects no fields at all
            fields = undefined;
        }

        try {
            Thing.get({thingId: thingId, fields: fields})
                .$promise.then(function success(thing) {
                    logResponse(RESPONSE_TYPE.SUCCESS, "getThing", 200, thing);
                },
                function error(error) {
                    $log.error(error);
                    logResponse(RESPONSE_TYPE.ERROR, "getThing", error.status, error.statusText);
                });
        } catch (e) {
            $log.error(e);
        }
    };
    $scope.getThings = function (thingIds, fields) {
        if (thingIds === '') {
            thingIds = undefined;
        }

        if (fields === '') {
            fields = undefined;
        }

        try {
            Things.queryThingIds({ids: thingIds, fields: fields}, function success(things) {
                logResponse(RESPONSE_TYPE.SUCCESS, "getThings", 200, things);
            }, function error(error) {
                $log.error(error);
                logResponse(RESPONSE_TYPE.ERROR, "getThings", error.status, error.statusText);
            });
        } catch (e) {
            $log.error(e);
        }
    };
    $scope.saveThing = function () {
        try {
            if ($scope.thing.owner === '')
            {
                delete $scope.thing.owner;
            }
            $scope.thing.$save()
                .then(function success(thing) {
                    logResponse(RESPONSE_TYPE.SUCCESS, "saveThing", 201, thing);
                }, function error(error) {
                    $log.error(error);
                    logResponse(RESPONSE_TYPE.ERROR, "saveThing", error.status, error.statusText);
                });
        } catch (e) {
            $log.error(e);
        }
    };
    $scope.removeThing = function (thingId) {
        try {
            Thing.remove({thingId: thingId})
                .$promise.then(function success() {
                    logResponse(RESPONSE_TYPE.SUCCESS, "removeThing", 204, "Thing deleted successfully.");
                }, function error(error) {
                    $log.error(error);
                    logResponse(RESPONSE_TYPE.ERROR, "removeThing", error.status, error.statusText);
                });
        } catch (e) {
            $log.error(e);
        }
    };
    $scope.modifyThingAttribute = function (thingAttribute) {
        ThingAttribute.put({ thingId: thingAttribute.thingId, path: thingAttribute.path }, thingAttribute.value)
            .$promise.then(function success(response) {
                logResponse(RESPONSE_TYPE.SUCCESS, "modifyThingAttribute", 204, "Attribute modified successfully.");
            }, function error(error) {
                $log.error(error);
                logResponse(RESPONSE_TYPE.ERROR, "modifyThingAttribute", error.status, error.statusText);
            });
    };
    $scope.deleteThingAttribute = function (thingAttribute) {
        ThingAttribute.delete({ thingId: thingAttribute.thingId, path: thingAttribute.path })
            .$promise.then(function success() {
                logResponse(RESPONSE_TYPE.SUCCESS, "deleteThingAttribute", 204, "Attribute deleted successfully.");
            }, function error(error) {
                $log.error(error);
                logResponse(RESPONSE_TYPE.ERROR, "deleteThingAttribute", error.status, error.statusText);
            });
    };
    $scope.clearResponses = function () {
        $scope.responses.length = 0;
    };

    var RESPONSE_TYPE = {SUCCESS: 'success', ERROR: 'error', WARNING: 'warning'};

    function logResponse(responseType, method, status, message) {
        var ts = new Date().toISOString();
        var response = {type: responseType, method: method, timestamp: ts, status: status, message: message};

        $scope.responses.unshift(response); // add at first index in array
    }

}
