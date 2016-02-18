/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.examples.rest.SimpleCrRestClient;
import com.bosch.cr.integration.messages.RepliableMessage;
import com.bosch.cr.integration.things.ThingHandle;
import com.bosch.cr.model.acl.AccessControlListModelFactory;
import com.bosch.cr.model.authorization.AuthorizationContext;
import com.bosch.cr.model.authorization.AuthorizationModelFactory;
import com.bosch.cr.model.things.Thing;
import com.bosch.cr.model.things.ThingsModelFactory;

/**
 * This example shows how to register for- and reply to claim messages with the CR Integration Client.
 *
 * @since 3.1.0
 */
public final class RegisterForClaimMessages extends ExamplesBase
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegisterForAndSendMessages.class);
   private static final String NAMESPACE = "com.bosch.cr.example:";

   private final String registrationIdAllClaimMessages;
   private final String registrationIdClaimMessagesForThing;

   private SimpleCrRestClient simpleCrRestClient;

   private RegisterForClaimMessages()
   {
      registrationIdAllClaimMessages = UUID.randomUUID().toString();
      registrationIdClaimMessagesForThing = UUID.randomUUID().toString();

      try
      {
         simpleCrRestClient = SimpleCrRestClient.of(BOSCH_IOT_CENTRAL_REGISTRY_HTTP_ENDPOINT_URL, CLIENT_ID, API_TOKEN,
            KEYSTORE_LOCATION.toURI(), KEYSTORE_PASSWORD, ALIAS, ALIAS_PASSWORD);
      }
      catch (final URISyntaxException e)
      {
         LOGGER.error("Error creating REST Client: {}", e);
      }
   }

   public static RegisterForClaimMessages newInstance()
   {
      return new RegisterForClaimMessages();
   }

   /**
    * Registers for claim messages sent to all things.
    * <p>
    * To claim the prepared Thing, you can use our swagger documentation provided at
    * https://cr.apps.bosch-iot-cloud.com/ or any other REST client.
    */
   public void registerForClaimMessagesToAllThings()
   {
      prepareClaimableThing() //
         .thenAccept(thingHandle -> {
            thingIntegration.registerForClaimMessage(registrationIdAllClaimMessages, this::handleMessage);
            LOGGER.info("Thing '{}' ready to be claimed", thingHandle.getThingId());
         });
   }

   /**
    * Registers for claim messages sent to a single Thing.
    * <p>
    * To claim the prepared Thing, you can use our swagger documentation provided at
    * https://cr.apps.bosch-iot-cloud.com/ or any other REST client.
    */
   public void registerForClaimMessagesToSingleThing()
   {
      prepareClaimableThing() //
         .thenAccept(thingHandle -> {
            thingHandle.registerForClaimMessage(registrationIdClaimMessagesForThing, this::handleMessage);
            LOGGER.info("Thing '{}' ready to be claimed!", thingHandle.getThingId());
         });
   }


   private CompletableFuture<ThingHandle> prepareClaimableThing()
   {
      final String thingId = NAMESPACE + UUID.randomUUID().toString();
      final Thing thing = ThingsModelFactory.newThingBuilder() //
         .setId(thingId) //
         .setPermissions(AuthorizationModelFactory.newAuthSubject(CLIENT_ID),
            AccessControlListModelFactory.allPermissions()) //
         .build();

      return thingIntegration.create(thing).thenApply(created -> thingIntegration.forId(thingId));
   }

   private void handleMessage(final RepliableMessage<ByteBuffer, Object> message)
   {
      final Optional<AuthorizationContext> optionalAuthorizationContext = message.getAuthorizationContext();
      if (optionalAuthorizationContext.isPresent())
      {
         // Workaround, as it is currently not possible to manage ACL with the CR Integration Client
         final String thingId = message.getThingId();
         final AuthorizationContext authorizationContext = optionalAuthorizationContext.get();
         simpleCrRestClient.grantPermissionsFor(thingId, authorizationContext).thenAccept(response -> {
            message.reply() //
               .timestamp(OffsetDateTime.now()) //
               .payload("Success!") //
               .contentType("text/plain") //
               .send();
            LOGGER.info("Thing '{}' claimed from authorization subject '{}'", thingId,
               authorizationContext.getFirstAuthorizationSubject().get());
         });
      }
      else
      {
         message.reply() //
            .timestamp(OffsetDateTime.now()) //
            .payload("Error: no authorization context present!") //
            .contentType("text/plain") //
            .send();
      }
   }
}
