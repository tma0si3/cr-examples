# Central Registry REST examples

This example shows in a simple test how to connect to the CR with the Central Registry Signature (CRS) Authentication.
The CRS Authentication allows clients to connect with a signature instead of basic auth.
You can find more information on the different authentication processes at our [wiki](http://m2m.bosch-si.com/dokuwiki/doku.php?id=005_dev_guide:004_rest_api:011_authenticate_as_a_client).

## Configure

Create or adjust file "config.properties"

```
centralRegistryEndpointUrl=https\://cr.apps.bosch-iot-cloud.com
clientId=### user solution id ###\:restcli
apiToken=### user solution API Token ###
keystoreLocation=CRClient.jks
keyAlias=CR
keyStorePassword=### your key password ###
keyAliasPassword=### your key alias password ###
http.proxyHost=### your http proxy host, if you need one ###
http.proxyPort=### your http proxy host, if you need one ###
```

The `keystoreLocation` is a path relative to the path the test is run from.

In order to adjust to your settings, e.g. copy the `src/test/resources/config.properties` into this directory (containing `README.md`), 
adjust the properties accordingly and put your `.jks` (`CRClient.jks`) file here as well.

## Build

Run the Test with the following command:
```
mvn test
```

## License

The examples are made available under the terms of BSD 3-Clause License. See individual files for details.
