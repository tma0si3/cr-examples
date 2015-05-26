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

function AccountController($scope, $log, Account) {
	$scope.isAuthenticated = false;

	$scope.login = function(credentials) {
		if (credentials) {
			Account.login(credentials.user, credentials.password)
				.then(function success(user) {
					$scope.credentials.user = user;
					$scope.isAuthenticated = true;					
				}, function error(error) {
					$log.error(error);
				});
		}
	};	
	$scope.logout = function() {
		Account.logout();
		$scope.credentials.password = undefined;
		$scope.isAuthenticated = false;
	};
}

function RestController($scope, $log, Thing) {
	$scope.responses = [];	
	$scope.thing = new Thing();
	$scope.thing.attributes = {};
	$scope.thing.features = {};

	$scope.getThing = function(thingId) {
		if (!thingId || thingId === '') {
			throw new Error('The thingId must not be undefined or empty!');
		}

		try {
			Thing.get({ thingId: thingId })
				.$promise.then(function success(thing) {
					$scope.responses.push({ message: 200 + ': ' + JSON.stringify(thing) });
				},
				function error(error) {
					$log.error(error);
					$scope.responses.push({ message: error.status + ': ' + error.statusText });
				});
		} catch(e) {
			$log.error(e);
		}
	};
	$scope.saveThing = function() {
		try {			
			$scope.thing.$save()
				.then(function success(thing) {
					$scope.responses.push({ message: 201 + ': ' + JSON.stringify(thing) });
				}, function error(error) {
					$log.error(error);
					$scope.responses.push({ message: error.status + ': ' + error.statusText });
				});
		} catch (e) {
			$log.error(e);
		}
	};
	$scope.removeThing = function(thingId) {
		try {
            Thing.remove({ thingId: thingId })
                .$promise.then(function success(response) {
                    $scope.responses.push({ message: 200 + ': ' + thingId });
                }, function error(error) {
                    $log.error(error);
                    $scope.responses.push({ message: error.status + ': ' + error.statusText });
                });
		} catch (e) {
			$log.error(e);
		}
	};
}