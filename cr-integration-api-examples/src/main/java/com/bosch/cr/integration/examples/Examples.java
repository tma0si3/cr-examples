package com.bosch.cr.integration.examples;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.IntegrationClientConfiguration;
import com.bosch.cr.integration.IntegrationClientImpl;
import com.bosch.cr.integration.ThingHandle;
import com.bosch.cr.integration.ThingIntegration;
import com.bosch.cr.integration.authentication.AuthenticationConfiguration;
import com.bosch.cr.integration.authentication.PublicKeyAuthenticationConfiguration;
import com.bosch.cr.integration.configuration.ProxyConfiguration;
import com.bosch.cr.integration.configuration.TrustStoreConfiguration;
import com.bosch.cr.integration.model.Feature;
import com.bosch.cr.integration.model.Permission;
import com.bosch.cr.integration.model.Thing;
import com.bosch.cr.integration.util.ThingBuilder;
import com.bosch.cr.integration.util.ThingBuilderImpl;

public class Examples
{
   private static final Logger LOGGER = LoggerFactory.getLogger(Examples.class);

   public static final String KEYSTORE_PASSWORD = "solutionPass";
   public static final String ALIAS = "CR";
   public static final String ALIAS_PASSWORD = "crPass";

   public static final String BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URL = "wss://events.apps.bosch-iot-cloud.com:443/";
   public static final URL KEYSTORE_LOCATION = Examples.class.getResource("/CRClient.jks");
   public static final URL TRUSTSTORE_LOCATION = Examples.class.getResource("/bosch-iot-cloud.jks");
   public static final String TRUSTSTORE_PASSWORD = "jks";

   public static void main(final String[] args) throws InterruptedException
   {
      AuthenticationConfiguration authenticationConfiguration =
         PublicKeyAuthenticationConfiguration.newBuilder()
            .clientId("example-client")
            .keyStoreLocation(KEYSTORE_LOCATION)
            .keyStorePassword(KEYSTORE_PASSWORD)
            .alias(ALIAS)
            .aliasPassword(ALIAS_PASSWORD)
            .build();

      /* optionally configure a proxy server or a truststore */
      ProxyConfiguration proxy = ProxyConfiguration.newBuilder().proxyHost("some.proxy.server").proxyPort(1234).build();
      TrustStoreConfiguration trustStore = TrustStoreConfiguration.newBuilder()
         .location(TRUSTSTORE_LOCATION).password(TRUSTSTORE_PASSWORD).build();

      /* provide required configuration (authentication configuration and CR URI),
         optional configuration (proxy, truststore etc.) can be added when needed */
      final IntegrationClientConfiguration integrationClientConfiguration =
         IntegrationClientConfiguration.newBuilder()
            .authenticationConfiguration(authenticationConfiguration)
            .centralRegistryEndpointUrl(BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URL)
            // .proxyConfiguration(proxy)
            // .trustStoreConfiguration(trustStore)
            .build();

      final IntegrationClient integrationClient = IntegrationClientImpl.newInstance(integrationClientConfiguration);

      /* Create a new thing integration */
      final ThingIntegration thingIntegration = integrationClient.things();

      /* Create a new thing and define handlers for success and failure */
      thingIntegration.create("myThing").onSuccess(thing -> LOGGER.info("Thing created: {}", thing))
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
         .onSuccess(_void -> LOGGER.info("New attribute created successfully."))
         .onFailure(throwable -> LOGGER.error("Failed to create new attribute: {}", throwable)).apply();
      /*--------------------------------------------------------------------------------------------------------------*/

      /* Register for *all* messages of *all* things of content type application/json and provide it as JsonMessage */
      final String allThings_jsonMessageRegistration = "allThings_jsonMessageRegistration";
      thingIntegration.registerForJsonMessage(allThings_jsonMessageRegistration, "application/json", (message) -> {
         final String topic = message.getTopic();
         final JsonValue payload = message.getPayload().get();
         LOGGER.info("json message for topic {} with payload {} received", topic, payload);
      });

      /* Register for *all* messages of *all* things of content type application/raw+image and provide it as RawMessage */
      final String allThings_rawMessageRegistration = "allThings_rawMessageRegistration";
      thingIntegration.registerForRawMessage(allThings_rawMessageRegistration, "application/raw+image", (message) -> {
         final String topic = message.getTopic();
         final byte[] payload = message.getPayload().get();
         LOGGER.info("raw message for topic {} with payload {} received", topic, Arrays.toString(payload));
      });

      /* Register for *all* messages of *all* things of content type application/xml and provide it as StringMessage */
      final String allThings_stringMessageRegistration = "allThings_stringMessageRegistration";
      thingIntegration.registerForStringMessage(allThings_stringMessageRegistration, "application/xml", (message) -> {
         final String topic = message.getTopic();
         final String payload = message.getPayload().get();
         LOGGER.info("string message for topic {} with payload {} received", topic, payload);
      });

      /* Register for *all* messages of a *specific* thing of content type application/json and provide it as JsonMessage */
      final String myThing_jsonMessageRegistration = "myThing_jsonMessageRegistration";
      myThing.registerForJsonMessage(myThing_jsonMessageRegistration, "application/json", (message) -> {
         final String topic = message.getTopic();
         final JsonValue payload = message.getPayload().get();
         LOGGER.info("json message for topic {} with payload {} received", topic, payload);
      });

      /* Register for *all* messages of a *specific* thing of content type application/raw+image and provide it as RawMessage */
      final String myThing_rawMessageRegistration = "myThing_rawMessageRegistration";
      myThing.registerForRawMessage(myThing_rawMessageRegistration, "application/raw+image", (message) -> {
         final String topic = message.getTopic();
         final byte[] payload = message.getPayload().get();
         LOGGER.info("raw message for topic {} with payload {} received", topic, Arrays.toString(payload));
      });

      /* Register for *all* messages of a *specific* thing of content type application/xml and provide it as StringMessage */
      final String myThing_stringMessageRegistration = "myThing_stringMessageRegistration";
      myThing.registerForStringMessage(myThing_stringMessageRegistration, "application/xml", (message) -> {
         final String topic = message.getTopic();
         final String payload = message.getPayload().get();
         LOGGER.info("string message for topic {} with payload {} received", topic, payload);
      });

      /* Send a message *from* a thing with the given topic but without any payload */
      thingIntegration.message().from(":sendFromThisThing").topic("some.arbitrary.topic").send();

      /* Send a message *from* a feature with the given topic but without any payload */
      thingIntegration.message().from(":thingId").featureId("sendFromThisFeature").topic("justWantToLetYouKnow").send();

      /* Send a message *to* a thing with the given topic and text payload */
      thingIntegration.message().to("com.bosch.building:sprinklerSystem").topic("monitoring/building/fireAlert")
         .payload("Roof is on fire").contentType("application/text").send();

      /* Send a message *from* a feature with the given topic and json payload */
      thingIntegration.message().from("com.bosch.building.monitoring:fireDetectionDevice").featureId("smokeDetector")
         .topic("fireAlert").payload("{\"action\" : \"call fire department\"}").contentType("application/json").send();

      final ThingHandle thingHandle = thingIntegration.forId(":thingId");
      /* Send a message *to* a thing (id already defined by the ThingHandle) with the given topic but without any payload */
      thingHandle.message().to().topic("someTopic").send();

      /* Send a message *from* a feature (thing id already defined by the ThingHandle) with the given topic and text payload */
      thingHandle.message().from().featureId("sendFromThisFeature").topic("someTopic").payload("someContent")
         .contentType("application/text").send();

      /* Delete a thing */
      myThing.delete().apply();

      /*--------------------------------------------------------------------------------------------------------------*/

      /* Create a new thing with acls, features, attributes and define handlers for success and failure */
      ThingBuilder builder = ThingBuilderImpl.newInstance(":complexThing");
      builder.aclEntryBuilder("user").permission(Permission.READ, Permission.WRITE, Permission.ADMINISTRATE).end();
      builder.featureBuilder("featureId").properties(Json.createObjectBuilder().add("property", "value").build()).end();
      builder.attributes("{\"attr\":true}");
      Thing complexThing = builder.build();

      thingIntegration.create(complexThing).onSuccess(thing -> LOGGER.info("Thing created: {}", thing))
         .onFailure(throwable -> LOGGER.error("Create Thing Failed: {}", throwable)).apply();

      /* Retrieve a List of Things */
      thingIntegration.retrieve(":complexThing1", ":complexThing2").fields("attributes", "acl", "features")
         .onSuccess(list -> inspectThingListFunction(list))
         .onFailure(throwable -> LOGGER.error("The List of Things couldn't be retrieved: {}" + throwable)).apply();

      /* Retrieve a Single Thing*/
      thingIntegration.forId(":complexThing1").retrieve().fields("attributes", "acl", "features")
         .onSuccess(thing -> inspectThingFunction(thing))
         .onFailure(throwable -> LOGGER.error("The Thing couldn't be retrieved: {}" + throwable)).apply();

      /* Destroy the client and wait 30 seconds for its graceful shutdown */
      integrationClient.destroy(30, TimeUnit.SECONDS);
   }

   private static void inspectThingFunction(Thing thing)
   {
      /* Acl Checks for Things */
      boolean ownerOfThing;
      ownerOfThing = thing.getAcl().getEntry("user")
         .map(entry -> entry.hasPermission(Permission.READ, Permission.WRITE, Permission.ADMINISTRATE)).orElse(false);
      if (ownerOfThing)
      {
         LOGGER.info("is Owner");
      }
      ownerOfThing = thing.getAcl().hasPermission("user1", Permission.READ, Permission.WRITE, Permission.ADMINISTRATE);
      if (!ownerOfThing)
      {
         LOGGER.info("is not Owner");
      }

      /* Read Features of a Thing */
      JsonObject retrievedProperties =
         thing.getFeatures().getFeature("featureId").map(Feature::getProperties).orElse(null);
      JsonObject expectedProperties = Json.createObjectBuilder().add("property", "value").build();
      if (expectedProperties.toString().equals(retrievedProperties.toString()))
      {
         LOGGER.info("Got the expected Properties");
      }
   }

   private static void inspectThingListFunction(List<Thing> thingList)
   {
      /* Read simple boolean attribute of attributes */
      thingList.forEach(thing -> {
         if (thing.getAttributes().getBoolean("attr"))
         {
            LOGGER.info("attr ist true");
         }
      });
   }
}
