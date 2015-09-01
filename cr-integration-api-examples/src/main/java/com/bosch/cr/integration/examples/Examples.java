package com.bosch.cr.integration.examples;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.IntegrationClientConfiguration;
import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.IntegrationClientImpl;
import com.bosch.cr.integration.ThingHandle;
import com.bosch.cr.integration.ThingIntegration;
import com.bosch.cr.integration.messaging.ProviderConfiguration;
import com.bosch.cr.integration.messaging.stomp.StompProviderConfiguration;

public class Examples
{
   private static final Logger LOGGER = LoggerFactory.getLogger(Examples.class);
   public static final String BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URI =
      "wss://events-stomper.apps.bosch-iot-cloud.com:443/";

   public static void main(final String[] args) throws InterruptedException
   {
      /* Create a new integration client */
      final ProviderConfiguration providerConfiguration = StompProviderConfiguration.newBuilder()
         .proxyHost("cache.innovations.de")    // Configure proxy (if needed)
         .proxyPort(3128)                      // Configure proxy (if needed)
         .sslKeyStoreLocation(Examples.class.getResource("/bosch-iot-cloud.jks"))
         .sslKeyStorePassword("jks")
         .build();

      final IntegrationClientConfiguration integrationClientConfiguration = IntegrationClientConfiguration.newBuilder()
         .clientId("examples_client")
         .centralRegistryEndpointUri(BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URI)
         .providerConfiguration(providerConfiguration)
         .build();

      final IntegrationClient integrationClient = IntegrationClientImpl.newInstance(integrationClientConfiguration);

      /* Create a new thing integration */
      final ThingIntegration thingIntegration = integrationClient.things();

      /* Create a new thing and define handlers for success and failure */
      thingIntegration.create("myThing")
         .onSuccess( thing -> LOGGER.info("Thing created: {}", thing))
         .onFailure(throwable -> LOGGER.error("Create Thing Failed: {}", throwable)).apply();

      /* Register for lifecycle events of *all* things */
      final String allThings_lifecycleRegistration = "allThings_lifecycleRegistration";
      thingIntegration.registerForLifecycleEvent(allThings_lifecycleRegistration,
         lifecycle -> LOGGER.info("lifecycle received: {}", lifecycle));

      /* Register for *all* attribute changes of *all* things */
      final String allThings_attributeChangeRegistration = "allThings_attributeChangeRegistration";
      thingIntegration.registerForAttributeChange(allThings_attributeChangeRegistration,
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of *all* things */
      final String allThings_specificAttributeChangeRegistration = "allThings_specificAttributeChangeRegistration";
      thingIntegration.registerForAttributeChange(allThings_specificAttributeChangeRegistration, "address/city",
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Terminate a registration using the client */
      thingIntegration.deregister(allThings_lifecycleRegistration);

      /*--------------------------------------------------------------------------------------------------------------*/

      /* Create a handle for an existing thing */
      final ThingHandle myThing = thingIntegration.forId("myThing");

      /* Register for *all* lifecycle events of a *specific* thing */
      final String myThing_lifecycleRegistration = "myThing_lifecycleRegistration";
      myThing.registerForLifecycleEvent(myThing_lifecycleRegistration,
         lifecycleEvent -> LOGGER.info("lifecycle received: {}", lifecycleEvent));

      /* Register for *all* attribute changes of a *specific* thing */
      final String myThing_attributeChangeRegistration = "myThing_attributeChangeRegistration";
      myThing.registerForAttributeChange(myThing_attributeChangeRegistration,
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of a *specific* thing */
      final String myThing_specificAttributeChangeRegistration = "myThing_specificAttributeChangeRegistration";
      myThing.registerForAttributeChange(myThing_specificAttributeChangeRegistration, "address/city",
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Create new attribute for a thing and define handlers for success and failure */
      myThing.changeAttribute("address/city", "Berlin")
         .onSuccess( _void -> LOGGER.info("New attribute created successfully."))
         .onFailure( throwable -> LOGGER.error("Failed to create new attribute: {}", throwable))
         .apply();

      /* Terminate a registration using a thingHandle */
      myThing.deregister(myThing_lifecycleRegistration);

      /* Delete a thing */
      myThing.delete();

      /* Destroy the client and wait 30 seconds for its graceful shutdown */
      integrationClient.destroy(30, TimeUnit.SECONDS);
   }
}
