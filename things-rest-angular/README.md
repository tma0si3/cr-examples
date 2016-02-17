# Things REST Angular

Implements a simple web application with angular.js and bootstrap to show how to use the ThingsService Rest Api with JavaScript.

## Configuration

The application will be hosted in Cloud Foundry using staticfile as buildpack. Deployment is preconfigured in the manifest.yml

```
---
applications:
- name: craas-things-example
  memory: 16M
  disk_quota: 16M
  buildpack: https://github.com/cloudfoundry/staticfile-buildpack.git
  host: craas-things-example${cf.space}
  path: /dist
```

To avoid CORS(Cross-Origin resource sharing) issues, the nginx configuration will use an internal proxy to redirect the rest calls to the ThingsService. Inside the application, the api could therefore be called from /cr.

```
location /cr {
	proxy_pass https://craas-things${cf.space}.apps.bosch-iot-cloud.com/cr/1;
}
```

## Deployment

To deploy the application to Cloud Foundry, do the following.

* Build the project with maven to set the properties ```cf.space``` and ```api.token```.
    
* Execute the following commands from a shell in the target folder.
    * ```cf login``` to configure your cloud foundry login
    * ```cf push``` to deploy the application

## License

See the cr-examples top level README.md file for license details.