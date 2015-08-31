package com.bosch.cr.integration.examples;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.ClientConfiguration;
import com.bosch.cr.integration.ThingHandle;
import com.bosch.cr.integration.ThingIntegration;
import com.bosch.cr.integration.ThingIntegrationClient;
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
      final ProviderConfiguration providerConfig = StompProviderConfiguration.newBuilder()
         .proxyHost("cache.innovations.de")    // Configure proxy (if needed)
         .proxyPort(3128)                      // Configure proxy (if needed)
         .sslKeyStoreLocation(Examples.class.getResource("/bosch-iot-cloud.jks"))
         .sslKeyStorePassword("jks")
         .build();

      final ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
         .clientId("examples_client")
         .centralRegistryEndpointUri(BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URI)
         .providerConfig(providerConfig)
         .build();

      final ThingIntegration thingIntegration = ThingIntegrationClient.newInstance(clientConfiguration);

      /* Register for *all* lifecycle events of *all* things */
      final String registration_1 = "registration_1";
      thingIntegration.registerForLifecycleEvent(registration_1, lifecycle ->
            LOGGER.info("lifecycle received: {}", lifecycle)
      );

      /* Register for *all* attribute changes of *all* things */
      final String registration_2 = "registration_2";
      thingIntegration.registerForAttributeChange(registration_2,
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of all things */
      final String registration_3 = "registration_2";
      thingIntegration.registerForAttributeChange(registration_3, "address/city",
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Create a new thing and define handlers for success and failure */
      thingIntegration.createThing("myThing")
         .onSuccess( thing -> LOGGER.info("Thing created: {}", thing))
         .onFailure(throwable -> LOGGER.error("Create Thing Failed: {}", throwable))
         .apply();

      /* Terminate a registration using the client */
      thingIntegration.deregister(registration_1);

      ------------------------------------------------------------------------------------------------------------------

      /* Create a handle for an existing thing */
      final ThingHandle myThing = thingIntegration.forThing("myThing");

      /* Register for *all* attribute changes of a *specific* thing */
      final String registration_4 = "registration_4";
      myThing.registerForAttributeChange(registration_4, change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of a *specific* thing */
      final String registration_5 = "registration_5";
      myThing.registerForAttributeChange(registration_5, "address/city",
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *all* lifecycle events of a *specific* thing */
      final String registration_6 = "registration_6";
      myThing.registerForLifecycleEvent(registration_6,
         lifecycleEvent -> LOGGER.info("lifecycle received: {}", lifecycleEvent));

      /* Create new attribute for a thing and define handlers for success and failure */
      myThing.changeAttribute("address/city", "Berlin")
         .onSuccess( _void -> LOGGER.info("New attribute created successfully."))
         .onFailure( throwable -> LOGGER.error("Failed to create new attribute: {}", throwable))
         .apply();

      /* Terminate a registration using a thingHandle */
      myThing.deregister(registration_4);

      /* Delete a thing */
      myThing.deleteThing();

      /* Destroy the client and wait 30 seconds for its graceful shutdown */
      thingIntegration.destroy(30, TimeUnit.SECONDS);
   }
}
