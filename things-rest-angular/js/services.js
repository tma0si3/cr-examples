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
        getCollection: {
            action: 'getCollection',
            method: 'GET',
            params: { thingId: '', ids: '@ids'},
            isArray: true
        }
    };
    return $resource(url, null, actions);
});

/**
 * The Account service implements basic authentication injection to the http requests.
 * No validation with the server is provided.
 *
 */
services.factory('Account', function ($q, $http) {
    function encodeBase64(input) {
        var keyStr = 'ABCDEFGHIJKLMNOP' +
            'QRSTUVWXYZabcdef' +
            'ghijklmnopqrstuv' +
            'wxyz0123456789+/' +
            '=';
        var output = '';
        var chr1, chr2, chr3 = '';
        var enc1, enc2, enc3, enc4 = '';
        var i = 0;
        do {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);
            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;
            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }
            output = output +
                keyStr.charAt(enc1) +
                keyStr.charAt(enc2) +
                keyStr.charAt(enc3) +
                keyStr.charAt(enc4);
            chr1 = chr2 = chr3 = '';
            enc1 = enc2 = enc3 = enc4 = '';
        } while (i < input.length);
        return output;
    }

    var m_user = undefined;

    return {
        getUser: function () {
            return m_user;
        },
        login: function (user, password) {
            return $q(function (resolve, reject) {
                if (user && password) {
                    m_user = user;
                    $http.defaults.headers.common['Authorization'] = 'Basic ' + encodeBase64(user + ':' + password);
                    resolve(m_user);
                } else {
                    reject('Invalid user or password.');
                }
            });
        },
        logout: function () {
            $http.defaults.headers.common['Authorization'] = undefined;
            delete $http.defaults.headers.common['Authorization'];
        }
    };
});