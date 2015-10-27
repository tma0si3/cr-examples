# Central Registry REST examples

This example shows in a simple test how to connect to the CR with the Central Registry Signature (CRS) Authentication.
The CRS Authentication allows clients to connect with a signature instead of basic auth.
You can find more information on the different authentication processes at our [wiki](http://m2m.bosch-si.com/dokuwiki/doku.php?id=005_dev_guide:004_rest_api:004_rest_api).

## Configure

The test is configured to run against `cr.apps.bosch-iot-cloud.com` by default.
To test against a local or docker instance of the CR, simply change the `HOST` at the top of the test.

## Build

Run the Test with the following command:
```
mvn test
```

## License

The examples are made available under the terms of BSD 3-Clause License. See individual files for details.