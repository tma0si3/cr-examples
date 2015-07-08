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

var services = angular.module('crClientServices', ['ngResource']);

/**
 * The Things service implements basic CRUD functionality for the things REST endpoint.
 *
 */
services.factory('Thing', function ($resource) {
    var url = '/cr/1/things/:thingId';
    var actions = {
        get: {
            method: 'GET',
            params: { thingId: '@thingId', fields: '@fields' },
            isArray: false
        }
    };
    return $resource(url, null, actions);
});

services.factory('Things', function ($resource) {
    var url = '/cr/1/things';
    var actions = {
        get: {
            method: 'GET',
            params: { fields: '@fields' },
            isArray: true
        },
        queryThingIds: {
            method: 'GET',
            params: { ids: '@ids', fields: '@fields' },
            isArray: true
        }
    };
    return $resource(url, null, actions);
});

services.factory('ThingAttribute', function ($resource) {
    var url = '/cr/1/things/:thingId/attributes/:path';
    var actions = {
        get: {
            method: 'GET',
            params: { thingId: '@thingId', path: '@path' },
            isArray: false
        },
        put: {
            method: 'PUT',
            params: { thingId: '@thingId', path: '@path' },
            isArray: false
        },
        delete: {
            method: 'DELETE',
            params: { thingId: '@thingId', path: '@path' },
            isArray: false
        }
    };
    return $resource(url, null, actions);
});

services.factory('ThingOwner', function ($resource) {
    var url = '/cr/1/things/:thingId/owner';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId'},
            transformResponse: function (data) {
                return {result: angular.fromJson(data)}
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId'},
            isArray: false
        },
        delete: {
            method: 'DELETE',
            params: {thingId: '@thingId'},
            isArray: false
        }
    };
    return $resource(url, null, actions);
});