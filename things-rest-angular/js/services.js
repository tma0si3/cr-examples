/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
/**
 * This material contains sample programming source code ("Sample Code") of Bosch Software Innovations GmbH (hereinafter
 * called "Bosch-SI"). Bosch-SI grants you a nonexclusive license to compile, link, run, display, reproduce, distribute
 * and prepare derivative works of this Sample Code. The Sample Code has not been thoroughly tested under all
 * conditions. Bosch-SI, therefore, does not guarantee or imply its reliability, serviceability, or function. Bosch-SI
 * provides no program services for the Sample Code. All Sample Code contained herein is provided to you "AS IS" without
 * any warranties of any kind. THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE EXPRESSLY DISCLAIMED. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OF IMPLIED WARRANTIES, SO
 * THE ABOVE EXCLUSIONS MAY NOT APPLY TO YOU. IN NO EVENT WILL BOSCH_SI BE LIABLE TO ANY PARTY FOR ANY DIRECT, INDIRECT,
 * SPECIAL OR OTHER CONSEQUENTIAL DAMAGES FOR ANY USE OF THE SAMPLE CODE INCLUDING, WITHOUT LIMITATION, ANY LOST
 * PROFITS, BUSINESS INTERRUPTION, LOSS OF PROGRAMS OR OTHER DATA ON YOUR INFORMATION HANDLING SYSTEM OR OTHERWISE, EVEN
 * IF WE ARE EXPRESSLY ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
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
