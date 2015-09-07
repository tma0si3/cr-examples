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

services.statusInterceptor = function (response) {
    var resource = response.resource;
    resource.$status = response.status;
    resource.$data = response.data || null;
    return resource;
};

/**
 * The Things service implements basic CRUD functionality for the things REST endpoint.
 *
 */
services.factory('Things', function ($resource) {
    var url = '/cr/1/things';
    var actions = {
        get: {
            method: 'GET',
            params: { fields: '@fields' },
            interceptor: {
                response: services.statusInterceptor
            }
        },
        getArray: {
            method: 'GET',
            params: { ids: '@ids', fields: '@fields' },
            isArray: true,
            interceptor: {
                response: services.statusInterceptor
            }
        },
        post: {
            method: 'POST',
            interceptor: {
                response: services.statusInterceptor
            }
        },
        delete: {
            method: 'DELETE',
            params: { thingId: '@thingId' },
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Thing', function ($resource) {
    var url = '/cr/1/things/:thingId';
    var actions = {
        get: {
            method: 'GET',
            params: { thingId: '@thingId', fields: '@fields' },
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: { thingId: '@thingId' },
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Acl', function ($resource) {
    var url = '/cr/1/things/:thingId/acl';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId'},
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('AclEntry', function ($resource) {
    var url = '/cr/1/things/:thingId/acl/:subject';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId', subject: '@subject'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId', subject: '@subject'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        delete: {
            method: 'DELETE',
            params: {thingId: '@thingId', subject: '@subject'},
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Attributes', function ($resource) {
    var url = '/cr/1/things/:thingId/attributes';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId'},
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Attribute', function ($resource) {
    var url = '/cr/1/things/:thingId/attributes/:jsonPointer';
    var actions = {
        get: {
            method: 'GET',
            params: { thingId: '@thingId', jsonPointer: '@jsonPointer' },
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: { thingId: '@thingId', jsonPointer: '@jsonPointer' },
            interceptor: {
                response: services.statusInterceptor
            }
        },
        delete: {
            method: 'DELETE',
            params: { thingId: '@thingId', jsonPointer: '@jsonPointer' },
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Features', function ($resource) {
    var url = '/cr/1/things/:thingId/features';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId'},
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Feature', function ($resource) {
    var url = '/cr/1/things/:thingId/features/:featureId';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId', featureId: '@featureId'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId', featureId: '@featureId'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        delete: {
            method: 'DELETE',
            params: {thingId: '@thingId', feature: '@featureId'},
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Properties', function ($resource) {
    var url = '/cr/1/things/:thingId/features/:featureId/properties';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId', featureId: '@featureId'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId', featureId: '@featureId'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        delete: {
            method: 'DELETE',
            params: {thingId: '@thingId', property: '@featureId'},
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});

services.factory('Property', function ($resource) {
    var url = '/cr/1/things/:thingId/features/:featureId/properties/:jsonPointer';
    var actions = {
        get: {
            method: 'GET',
            params: {thingId: '@thingId', property: '@featureId', jsonPointer: '@jsonPointer'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        put: {
            method: 'PUT',
            params: {thingId: '@thingId', property: '@featureId', jsonPointer: '@jsonPointer'},
            interceptor: {
                response: services.statusInterceptor
            }
        },
        delete: {
            method: 'DELETE',
            params: {thingId: '@thingId', property: '@featureId', jsonPointer: '@jsonPointer'},
            interceptor: {
                response: services.statusInterceptor
            }
        }
    };
    return $resource(url, null, actions);
});