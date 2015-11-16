# Track my Phone

Implements a simple web application to integrate a device with the CR's REST API in JavaScript.
The application listenes for geolocation and orientation events of the device and pushes the received data into the CR.

## Configuration

The application will be hosted in Cloud Foundry using the staticfile buildpack. Deployment is preconfigured in the manifest.yml.

```
---
applications:
- name: track-my-phone
  memory: 16M
  disk_quota: 16M
  buildpack: https://github.com/cloudfoundry/staticfile-buildpack.git
  host: track-my-phone
  path: /dist
```

To avoid CORS(Cross-Origin resource sharing) issues, the nginx configuration will use a proxy to redirect the REST calls to the CR.

```
location /cr {
	proxy_pass https://cr.apps.bosch-iot-cloud.com/cr/1;
}
```

## Deployment

To deploy the application to Cloud Foundry, execute the following commands:

* ```mvn clean package -Dapi.token=<api-token>``` to build the project with your <api-token> for the reverse proxy.
* ```cf login``` to configure your cloud foundry login
* ```cf push target\manifest.yml``` to deploy the application

## License

The examples are made available under the terms of BSD 3-Clause License. See individual files for details.

As an exception the file iframeResizer.contentWindow.min.js is made available under the terms of the MIT License.