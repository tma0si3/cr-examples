# bosch-iot-central-registry-examples

This repository contains examples for using the bosch iot central registry in the cloud.

## things-rest-angular

Implements a simple web application with angular.js and bootstrap to show how to use the things service rest api with javascript.

### configuration

The application will be hosted in cloud foundry using the staticfile buildpack. Deployment is preconfigured in the manifest.yml

```
---
applications:
- name: craas-things-example
  memory: 64M
  buildpack: https://github.com/cloudfoundry/staticfile-buildpack.git
```

To avoid cors issues, the nginx configuration will use an internal proxy to redirect the rest calls to the things service. Inside the application, the api could therefore be called from /cr.

```
location /cr {
	proxy_pass https://craas-things.apps.cf.bosch-poc.de/cr/1;
}
```

### deployment

To deploy the application to cloud foundry, we use the staticfile buildpack. Execute the following commands from a shell in the application root folder.

* ```cf login``` to configure your cloud foundry login
* ```cf push``` to deploy the application