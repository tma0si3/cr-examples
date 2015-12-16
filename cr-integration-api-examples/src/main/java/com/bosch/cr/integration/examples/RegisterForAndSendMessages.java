/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bosch.cr.integration.ThingHandle;
import com.bosch.cr.json.JsonFactory;
import com.bosch.cr.json.JsonValue;


/**
 * This examples shows the various possibilities that the {@code IntegrationClient} offers to register handlers for
 * {@link com.bosch.cr.model.messages.Message}s being sent to/from your {@code Thing}s, and shows how you can send
 * such {@code Message}s using the {@code IntegrationClient}.
 * <p>
 * NOTE: Make sure to invoke {@code IntegrationClient.subscriptions().consume()} once after all message handlers are
 * registered to start receiving events from Central Registry.
 *
 * @since 2.0.0
 */
public final class RegisterForAndSendMessages extends ExamplesBase
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegisterForAndSendMessages.class);

   /**
    * Shows various possibilities to register handlers for {@code Message}s of interest.
    */
   public void registerForMessages()
   {
      /* Register for *all* messages of *all* things and provide payload as JsonValue */
      final String allThings_jsonMessageRegistration = "allThings_jsonMessageRegistration";
      thingIntegration.registerForMessage(allThings_jsonMessageRegistration, "*", JsonValue.class, message -> {
         final String topic = message.getTopic();
         final JsonValue payload = message.getPayload().get();
         LOGGER.info("message for topic {} with payload {} received", topic, payload);
      });

      /* Register for messages with topic *topicOfInterest* of *all* things and provide payload as byte array */
      final String allThings_rawMessageRegistration = "allThings_rawMessageRegistration";
      thingIntegration.registerForMessage(allThings_rawMessageRegistration, "topicOfInterest", message -> {
         final String topic = message.getTopic();
         final ByteBuffer payload = message.getPayload().get();
         LOGGER.info("message for topic {} with payload {} received", topic, StandardCharsets.UTF_8.decode(payload).toString());
      });

      /* Register for messages with topic *some.topic* of *all* things and provide payload as String */
      final String allThings_stringMessageRegistration = "allThings_stringMessageRegistration";
      thingIntegration.registerForMessage(allThings_stringMessageRegistration, "some.topic", String.class, message -> {
         final String topic = message.getTopic();
         final String payload = message.getPayload().get();
         LOGGER.info("message for topic {} with payload {} received", topic, payload);
      });

      /* Register for *all* messages of a *specific* thing of and provide payload as JsonValue */
      final String myThing_jsonMessageRegistration = "myThing_jsonMessageRegistration";
      myThing.registerForMessage(myThing_jsonMessageRegistration, "*", JsonValue.class, message -> {
         final String topic = message.getTopic();
         final JsonValue payload = message.getPayload().get();
         LOGGER.info("message for topic {} with payload {} received", topic, payload);
      });

      /* Register for *all* messages with topic *some_message_topic* of a *specific* thing and provide payload as byte array */
      final String myThing_rawMessageRegistration = "myThing_rawMessageRegistration";
      myThing.registerForMessage(myThing_rawMessageRegistration, "some_message_topic", message -> {
         final String topic = message.getTopic();
         final ByteBuffer payload = message.getPayload().get();
         LOGGER.info("message for topic {} with payload {} received", topic, StandardCharsets.UTF_8.decode(payload).toString());
      });

      /* Register for *all* messages of a *specific* thing and provide payload as String */
      final String myThing_stringMessageRegistration = "myThing_stringMessageRegistration";
      myThing.registerForMessage(myThing_stringMessageRegistration, "*", String.class, message -> {
         final String topic = message.getTopic();
         final String payload = message.getPayload().get();
         LOGGER.info("message for topic {} with payload {} received", topic, payload);
      });
   }

   /**
    * Shows how to send a {@code Message} to/from a {@code Thing} using the {@code IntegrationClient}.
    */
   public void sendMessages()
   {
      /* Send a message *from* a thing with the given topic but without any payload */
      thingIntegration.message() //
         .from(":sendFromThisThing") //
         .topic("some.arbitrary.topic") //
         .send();

      /* Send a message *from* a feature with the given topic but without any payload */
      thingIntegration.message() //
         .from(":thingId") //
         .featureId("sendFromThisFeature") //
         .topic("justWantToLetYouKnow") //
         .send();

      /* Send a message *to* a thing with the given topic and text payload */
      thingIntegration.message() //
         .to("com.bosch.building:sprinklerSystem") //
         .topic("monitoring/building/fireAlert") //
         .payload("Roof is on fire") //
         .contentType("text/plain") //
         .send();

      /* Send a message *from* a feature with the given topic and json payload */
      thingIntegration.message() //
         .from("com.bosch.building.monitoring:fireDetectionDevice") //
         .featureId("smokeDetector") //
         .topic("fireAlert") //
         .payload(JsonFactory.readFrom("{\"action\" : \"call fire department\"}")) //
         .contentType("application/json") //
         .send();

      final ThingHandle thingHandle = thingIntegration.forId(":thingId");
      /* Send a message *to* a thing (id already defined by the ThingHandle) with the given topic but without any payload */
      thingHandle.message() //
         .to() //
         .topic("someTopic") //
         .send();

      /* Send a message *from* a feature (thing id already defined by the ThingHandle) with the given topic and text payload */
      thingHandle.message() //
         .from() //
         .featureId("sendFromThisFeature") //
         .topic("someTopic") //
         .payload("someContent") //
         .contentType("application/text") //
         .send();
   }
}
