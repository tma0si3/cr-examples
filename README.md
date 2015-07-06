# bosch-iot-central-registry-examples

This repository contains examples for using the Bosch IOT Central Registry in the cloud.

## Application things-rest-angular

Implements a simple web application with angular.js and bootstrap to show how to use the ThingsService Rest Api with JavaScript.

### Configuration

The application will be hosted in Cloud Foundry using staticfile as buildpack. Deployment is preconfigured in the manifest.yml

```
---
applications:
- name: craas-things-example
  memory: 64M
  buildpack: https://github.com/cloudfoundry/staticfile-buildpack.git
```

To avoid CORS(Cross-Origin resource sharing) issues, the nginx configuration will use an internal proxy to redirect the rest calls to the ThingsService. Inside the application, the api could therefore be called from /cr.

```
location /cr {
	proxy_pass https://craas-things.apps.cf.bosch-poc.de/cr/1;
}
```

### Deployment

To deploy the application to Cloud Foundry, we use staticfile as buildpack. Execute the following commands from a shell in the application root folder.

* ```cf login``` to configure your cloud foundry login
* ```cf push``` to deploy the application

## License

The examples are made available under the terms of BSD 3-Clause License. See individual files for details.

As an exception the file iframeResizer.contentWindow.min.js is made available under the terms of the MIT License.