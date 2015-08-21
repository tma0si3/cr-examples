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

function RestController($scope, $log, Thing, Things, ThingAttribute, ThingAcl, ThingAclEntry) {
    var RESPONSE_TYPE = {SUCCESS: 'success', ERROR: 'error', WARNING: 'warning'};
    var PERMISSIONS = ["READ", "WRITE", "ADMINISTRATE"];

    $scope.responses = [];
    $scope.thingToCreate = new Thing();
    $scope.thingToModify = new Thing();

    $scope.getThing = function (thingId, fields) {
        if (isNullOrEmpty(thingId)) {
            throw new Error('The Thing ID must not be undefined or empty!');
        }

        if (fields === '') {
            // sending an empty string selects no fields at all
            fields = undefined;
        }

        Thing.get({thingId: thingId, fields: fields},
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "getThing", value.$status, value);
            },
            function error(httpResponse) {
                logError("getThing", httpResponse);
            });
    };

    $scope.getThings = function (thingIds, fields) {
        if (thingIds === '') {
            thingIds = undefined;
        }

        if (fields === '') {
            fields = undefined;
        }

        Things.getArray({ids: thingIds, fields: fields},
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "getThings", value.$status, value);
            },
            function error(httpResponse) {
                logError("getThings", httpResponse);
            });
    };

    $scope.postThing = function (thing) {
        var t = new Thing();

        if (thing.authSubjectId === '') {
            delete t.acl;
        } else if (thing.authSubjectId !== undefined) {
            var permissions = {};
            for (var i = 0; i < PERMISSIONS.length; i++) {
                permissions[PERMISSIONS[i]] = true;
            }

            var acl = {};
            acl[thing.authSubjectId] = permissions;

            t.acl = acl;
        }

        if (thing.attributes === '') {
            delete t.attributes;
        } else if (thing.attributes) {
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
    };

    $scope.putThing = function (thing) {
        var t = new Thing();
        t.thingId = thing.thingId;

        if (thing.authSubjectId === '') {
            delete t.acl;
        } else if (thing.authSubjectId !== undefined) {
            var permissions = {};
            for (var i = 0; i < PERMISSIONS.length; i++) {
                permissions[PERMISSIONS[i]] = true;
            }

            var acl = {};
            acl[thing.authSubjectId] = permissions;

            t.acl = acl;
        }

        if (thing.attributes === '') {
            delete t.attributes;
        } else if (thing.attributes) {
            t.attributes = JSON.parse(thing.attributes);
        }

        Thing.put({ thingId: t.thingId }, t,
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS,
                    "putThing", value.$status, "Thing modified successfully.");
            },
            function error(httpResponse) {
                logError("putThing", httpResponse);
            });
    };

    $scope.deleteThing = function (thingId) {
        Thing.remove({thingId: thingId},
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "deleteThing", value.$status, "Thing deleted successfully.");
            }, function error(httpResponse) {
                logError("deleteThing", httpResponse);
            });
    };

    $scope.putThingAttribute = function (thingAttribute) {
        ThingAttribute.put({ thingId: thingAttribute.thingId, path: thingAttribute.path }, thingAttribute.value,
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "putThingAttribute", value.$status, "Attribute modified successfully.");
            },
            function error(httpResponse) {
                logError("putThingAttribute", httpResponse);
            });
    };

    $scope.deleteThingAttribute = function (thingAttribute) {
        ThingAttribute.delete({ thingId: thingAttribute.thingId, path: thingAttribute.path },
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "deleteThingAttribute", value.$status, "Attribute deleted successfully.");
            },
            function error(httpResponse) {
                logError("deleteThingAttribute", httpResponse);
            });
    };

    $scope.getThingAcl = function (thingId) {
        if (isNullOrEmpty(thingId)) {
            throw new Error('The Thing ID must not be undefined or empty!');
        }

        ThingAcl.get({thingId: thingId},
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "getThingAcl", value.$status, value);
            },
            function error(httpResponse) {
                logError("getThingAcl", httpResponse);
            });
    };
    
    $scope.getThingAclEntry = function (thingId, subject) {
        if (isNullOrEmpty(thingId)) {
            throw new Error('The Authorization Subject ID must not be undefined or empty!');
        }
        if (isNullOrEmpty(subject)) {
            throw new Error('The Authorization Subject ID must not be undefined or empty!');
        }

        ThingAclEntry.get({thingId: thingId, subject: subject},
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "getThingAclEntry", value.$status, value);
            },
            function error(httpResponse) {
                logError("getThingAclEntry", httpResponse);
            });
    };

    $scope.putThingAcl = function (thingId, aclEntries) {
        if (isNullOrEmpty(thingId)) {
            throw new Error('The Thing ID must not be undefined or empty!');
        }

        var acl = JSON.parse(aclEntries);

        ThingAcl.put({thingId: thingId}, acl,
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "putThingAcl", value.$status, "ACL modified successfully.");
            },
            function error(httpResponse) {
                logError("putThingAcl", httpResponse);
            });
    };

    $scope.putThingAclEntry = function (thingId, subject, permissions) {
        if (isNullOrEmpty(thingId)) {
            throw new Error('The Thing ID must not be undefined or empty!');
        }
        if (isNullOrEmpty(subject)) {
            throw new Error('The Authorization Subject ID must not be undefined or empty!');
        }

        permissions = permissions.split(',');
        var aclEntryPermissions = {};
        for (var index in PERMISSIONS) {
            var permission = PERMISSIONS[index];
            aclEntryPermissions[permission] = arrayContainsPermission(permissions, permission);
        }

        ThingAclEntry.put({thingId: thingId, subject: subject}, aclEntryPermissions,
            function success(value) {
                var message = value.$status === 201 ? value : "ACL entry modified successfully.";
                logResponse(RESPONSE_TYPE.SUCCESS, "putThingAclEntry", value.$status, message);
            },
            function error(httpResponse) {
                logError("putThingAclEntry", httpResponse);
            });
    };

    $scope.deleteThingAclEntry = function (thingId, subject) {
        if (isNullOrEmpty(thingId)) {
            throw new Error('The Thing ID must not be undefined or empty!');
        }
        if (isNullOrEmpty(subject)) {
            throw new Error('The Authorization Subject ID must not be undefined or empty!');
        }

        ThingAclEntry.delete({thingId: thingId, subject: subject},
            function success(value) {
                logResponse(RESPONSE_TYPE.SUCCESS, "deleteThingAclEntry", value.$status, "ACL entry deleted successfully.");
            },
            function error(httpResponse) {
                logError("deleteThingAclEntry", httpResponse);
            });
    };

    $scope.clearResponses = function () {
        $scope.responses.length = 0;
    };

    function logError(functionName, httpResponse)
    {
        logResponse(RESPONSE_TYPE.ERROR, functionName, httpResponse.status, httpResponse.statusText);
    }

    function isNullOrEmpty(string) {
        return (!string || string === '');
    }

    function arrayContainsPermission(array, permission) {
        var result = false;

        if (array instanceof Array && permission) {
            result = array.indexOf(permission) !== -1;
        }

        return result;
    }

    function logResponse(responseType, method, status, message) {
        var ts = new Date().toISOString();
        var response = {type: responseType, method: method, timestamp: ts, status: status, message: message};

        if (typeof message === 'object' && message.$status) {
            // delete the status property since it doesn't belong to the resource itself
            delete message.$status;
        }

        $scope.responses.unshift(response); // add at first index in array
    }
}
