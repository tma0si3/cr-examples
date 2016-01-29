# Bosch IoT Central Registry - Example Vehicle Simulator

This example shows how to integrate devices information using Java with the CR.

**Notice:** Currently this demo uses the REST API for the updating the properties of Features of Things.
As soon as the CR supports updating feature properties using the CR-Integration Client for Java this can be changed.

# Build

Use the following maven command to build the server:
```
mvn clean install
```

# Configure your Client Id and other settings

Create or adjust file "config.properties"

```
centralRegistryEndpointUrl=https://cr.apps.bosch-iot-cloud.com
centralRegistryMessagingUrl=wss\://events.apps.bosch-iot-cloud.com
clientId=###your solution id ###\:gateway
apiToken=###your api token ###
keyAlias=CR
keyStorePassword=### your key password ###
keyAliasPassword=### your key alias password ###
#http.proxyHost=### your http proxy host, if you need one ###
#http.proxyPort=### your http proxy host, if you need one ###
```

# Run it

Use the following command to run the example.
```
mvn exec:java -Dexec.mainClass="com.bosch.cr.examples.carintegrator.VehicleSimulator"
```

# Usage

Look in the Inventory Browser and see your vehicle(s) move.
