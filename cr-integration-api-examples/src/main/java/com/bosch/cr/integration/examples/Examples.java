package com.bosch.cr.integration.examples;

import java.util.concurrent.CountDownLatch;
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
   public static final String BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URI =
      "wss://events-stomper.apps.bosch-iot-cloud.com:443/";

   private static final Logger LOGGER = LoggerFactory.getLogger(Examples.class);

   public static ThingIntegration instantiateClient()
   {

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

      return ThingIntegrationClient.newInstance(clientConfiguration);
   }

   public static CountDownLatch createNewThing(final ThingIntegration thingIntegration, final String thingId)
   {
      final CountDownLatch createdFlag = new CountDownLatch(1);
      thingIntegration.create("myThing").handleResult( (thing, throwable) ->
      {
         if( throwable != null )
         {
            LOGGER.error("Failed: {}", throwable);
         }
         else
         {
            LOGGER.info("Successfully created thing: {}", thing);
            createdFlag.countDown();
         }
      }).apply();
      return createdFlag;
   }

   public static void createNewAttributes(final ThingHandle thingHandle, final String path, final String value)
   {
      thingHandle.changeAttribute(path, value).handleResult( (_void, throwable) ->
      {
         if( throwable != null )
         {
            LOGGER.error("Failed: {}", throwable);
         }
         else
         {
            LOGGER.info("Successfully changed attribute");
         }
      }).apply();
   }

   final static String registerForAttributeChanges(final ThingHandle thingHandle, final String path)
   {
      final String registrationId = "myAttributeChangeRegistration";
      thingHandle.registerForAttributeChange(registrationId, path, change -> {
         LOGGER.info("attributeChange received: {}", change);
      });
      return registrationId;
   }

   final static String registerForLifecycleEvents(final ThingHandle thingHandle)
   {
      final String registrationId = "myLifecycleRegistration";
      thingHandle.registerForLifecycleEvent(registrationId, lifecycleEvent -> {
         LOGGER.info("lifecycle received: {}", lifecycleEvent);
      });
      return registrationId;
   }

   final static void deleteThing(final ThingHandle thingHandle)
   {
      thingHandle.delete();
   }

   final static void destroyClient(final ThingIntegration thingIntegration)
   {
      thingIntegration.destroy(30, TimeUnit.SECONDS);
   }

   public static void main(final String[] args) throws InterruptedException
   {
      final ThingIntegration thingIntegration = instantiateClient();

      final CountDownLatch createdFlag = createNewThing(thingIntegration, "myThing");
      createdFlag.await(1, TimeUnit.SECONDS);
      final ThingHandle myThing = thingIntegration.forThing("myThing");


      registerForAttributeChanges(myThing, "address/city");
      registerForLifecycleEvents(myThing);

      createNewAttributes(myThing, "address/city", "Berlin");
      deleteThing(myThing);

      destroyClient(thingIntegration);
   }
}
