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

function RestController($scope, $log, Thing, Things, ThingAttribute, ThingOwner) {
    var RESPONSE_TYPE = {SUCCESS: 'success', ERROR: 'error', WARNING: 'warning'};

    $scope.responses = [];
    $scope.thingToCreate = new Thing();
    $scope.thingToModify = new Thing();

    $scope.getThing = function (thingId, fields) {
        if (!thingId || thingId === '') {
            throw new Error('The thingId must not be undefined or empty!');
        }

        if (fields === '') {
            // sending an empty string selects no fields at all
            fields = undefined;
        }

        try {
            Thing.get({thingId: thingId, fields: fields},
                function success(value, responseHeaders) {
                    logResponse(RESPONSE_TYPE.SUCCESS, "getThing", value.$status, value);
                },
                function error(httpResponse) {
                    logError("getThing", httpResponse);
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
            Things.getArray({ids: thingIds, fields: fields},
                function success(value, responseHeaders) {
                    logResponse(RESPONSE_TYPE.SUCCESS, "getThings", value.$status, value);
                },
                function error(httpResponse) {
                    logError("getThings", httpResponse);
                });
        } catch (e) {
            $log.error(e);
        }
    };

    $scope.postThing = function (thing) {
        try {
            var t = new Thing();

            if (thing.owner === '')
            {
                delete t.owner;
            }
            else if (thing.owner !== undefined)
            {
                t.owner = thing.owner;
            }

            if (thing.attributes === '')
            {
                delete t.attributes;
            }
            else if (thing.attributes)
            {
                t.attributes = JSON.parse(thing.attributes);
            }

            Things.post({}, t,
                function success(value, responseHeaders) {
                    logResponse(RESPONSE_TYPE.SUCCESS,
                        "postThing", value.$status, "Thing created successfully at " + responseHeaders("location"));
                },
                function error(httpResponse) {
                    logError("postThing", httpResponse);
                });
        } catch (e) {
            $log.error(e);
        }
    };

    $scope.putThing = function (thing) {
        try {
            var t = new Thing();
            t.thingId = thing.thingId;

            if (thing.owner === '')
            {
                delete t.owner;
            }
            else if (thing.owner !== undefined)
            {
                t.owner = thing.owner;
            }

            if (thing.attributes === '')
            {
                delete t.attributes;
            }
            else if (thing.attributes)
            {
                t.attributes = JSON.parse(thing.attributes);
            }

            Thing.put({ thingId: t.thingId }, t,
                function success(value, responseHeaders) {
                    logResponse(RESPONSE_TYPE.SUCCESS,
                        "putThing", value.$status, "Thing modified successfully");
                },
                function error(httpResponse) {
                    logError("putThing", httpResponse);
                });
        } catch (e) {
            $log.error(e);
        }
    };

    $scope.deleteThing = function (thingId) {
        try {
            Thing.remove({thingId: thingId},
                function success(value, responseHeaders) {
                    logResponse(RESPONSE_TYPE.SUCCESS, "deleteThing", value.$status, "Thing deleted successfully.");
                }, function error(httpResponse) {
                    logError("deleteThing", httpResponse);
                });
        } catch (e) {
            $log.error(e);
        }
    };

    $scope.putThingAttribute = function (thingAttribute) {
        ThingAttribute.put({ thingId: thingAttribute.thingId, path: thingAttribute.path }, thingAttribute.value,
            function success(value, responseHeaders) {
                logResponse(RESPONSE_TYPE.SUCCESS, "putThingAttribute", value.$status, "Attribute modified successfully.");
            },
            function error(httpResponse) {
                logError("putThingAttribute", httpResponse);
            });
    };

    $scope.deleteThingAttribute = function (thingAttribute) {
        ThingAttribute.delete({ thingId: thingAttribute.thingId, path: thingAttribute.path },
            function success(value, responseHeaders) {
                logResponse(RESPONSE_TYPE.SUCCESS, "deleteThingAttribute", value.$status, "Attribute deleted successfully.");
            },
            function error(httpResponse) {
                logError("deleteThingAttribute", httpResponse);
            });
    };

    $scope.getThingOwner = function (thingOwner) {
        ThingOwner.get({thingId: thingOwner.thingId},
            function success(value, responseHeaders) {
                logResponse(RESPONSE_TYPE.SUCCESS, "getThingOwner", value.$status, value);
            },
            function error(httpResponse) {
                logError("getThingOwner", httpResponse);
            });
    };

    $scope.updateThingOwner = function (thingOwner) {
        ThingOwner.put({thingId: thingOwner.thingId}, thingOwner.ownerId,
            function success(value, responseHeaders) {
                logResponse(RESPONSE_TYPE.SUCCESS, "updateThingOwner", value.$status, "Owner updated successfully.");
            },
            function error(httpResponse) {
                logError("updateThingOwner", httpResponse);
            });
    };

    $scope.deleteThingOwner = function (thingOwner) {
        ThingOwner.delete({thingId: thingOwner.thingId},
            function success(value, responseHeaders) {
                logResponse(RESPONSE_TYPE.SUCCESS, "deleteThingOwner", value.$status, "Owner deleted successfully.");
            },
            function error(httpResponse) {
                logError("deleteThingOwner", httpResponse);
            });
    };

    $scope.clearResponses = function () {
        $scope.responses.length = 0;
    };

    function logError(functionName, httpResponse)
    {
        logResponse(RESPONSE_TYPE.ERROR, functionName, httpResponse.status, httpResponse.statusText);
    }

    function logResponse(responseType, method, status, message) {
        var ts = new Date().toISOString();
        var response = {type: responseType, method: method, timestamp: ts, status: status, message: message};

        $scope.responses.unshift(response); // add at first index in array
    }
}
