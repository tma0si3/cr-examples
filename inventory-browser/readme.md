# Bosch IoT Central Registry - Example Inventory Browser

This example shows how to create a simple user interface to list things and their details and show the things on a map.

![Screenshot](screenshot.png)

# Build

Use the following maven command to build the server:
```
mvn clean install
```

# Configure your API Key and other settings

Create or adjust file "proxy.properties"

```
centralRegistryTargetHost=https://craas-api-dev.apps.bosch-iot-cloud.com
centralRegistryApiToken=### your CR Solution API Token ###
http.proxyHost=### your http proxy host, if you need one ###
http.proxyPort=### your http proxy port, if you need one ###
```

# Run Server

Use the following command to run the server. Adapt your proxy settings in advance:
```
java -jar target/inventory-browser.jar
```

# Usage

## Show Dashboard

Browse to the Bosch IoT Central Registry Dashboard: <https://cr.apps.bosch-iot-cloud.com/>

## Create Demo User

Use the dashboard to create a demo user.

## Show Inventory Browser

Browse to the example web app: <http://localhost:8080/inventory-browser/>

## Create Empty Thing

In REST Documentation (Swagger): <https://cr.apps.bosch-iot-cloud.com/doc/>
use "Things - POST /things"

thing:
```
{}
```

## Create Thing for Herbie

Use "Things - PUT /things"
thingId: demo:car-53

thing:
```
{
  "attributes": {
      "name": "Herbie 53",
      "manufacturer": "VW",
      "VIN": "5313879"
  },
    "features": {
      "geolocation": {
        "properties": {
          "_definition": "org.eclipse.vorto.Geolocation:1.0.0",
          "geoposition": {
            "latitude": 47.68,
            "longitude": 9.3865
          },
          "accuracy": 15
        }
      }
   }
}
```

# Refresh Things in Inventory Browser

# Update Position of Herbie

Use "Features - PUT /things/{thingId}/features/{featureId}/properties/{propertyPath}"

thingId: demo:car-53

featureId: geolocation

propertyPath: geoposition/latitude
```
47.665
```

# Refresh Things in Inventory Browser

# More example Things

See [testdata.json](testdata.json) for more example things to create.
