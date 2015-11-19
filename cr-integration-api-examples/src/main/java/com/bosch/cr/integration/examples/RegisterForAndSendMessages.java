/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import java.util.Arrays;

import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.ThingHandle;


/**
 * This examples shows the various possibilities that the {@code IntegrationClient} offers to register handlers for
 * {@link com.bosch.cr.integration.model.Message}s being sent to/from your {@code Thing}s, and shows how you can send
 * such {@code Message}s using the {@code IntegrationClient}.
 *
 * @since 2.0.0
 */
public class RegisterForAndSendMessages extends ExamplesBase
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegisterForAndSendMessages.class);

   /**
    * Shows various possibilities to register handlers for {@code Message}s of interest.
    */
   public void registerForMessages()
   {
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
   }

   /**
    * Shows how to send a {@code Message} to/from a {@code Thing} using the {@code IntegrationClient}.
    */
   public void sendMessages()
   {
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
   }
}
