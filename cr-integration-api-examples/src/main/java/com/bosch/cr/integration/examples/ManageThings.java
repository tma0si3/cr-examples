/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.json.Json;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.model.Permission;
import com.bosch.cr.integration.model.Thing;
import com.bosch.cr.integration.util.FieldSelector;
import com.bosch.cr.integration.util.ThingBuilder;
import com.bosch.cr.integration.util.ThingBuilderImpl;

/**
 * This example shows how {@link com.bosch.cr.integration.ThingIntegration} and {@link
 * com.bosch.cr.integration.ThingHandle}s can be used to perform CRUD (Create, Read, Update, and Delete) operations
 * on {@link Thing}(s).
 *
 * @since 2.0.0
 */
public class ManageThings extends ExamplesBase
{
   private static final Logger LOGGER = LoggerFactory.getLogger(ManageThings.class);

   /**
    * Creates a new {@code Thing} object, updates the thing by adding a new attribute to the thing, retrieves the
    * modified thing, and finally deletes it.
    *
    * @throws ExecutionException if a failure response is received for any of the requests, or if an exception occurs
    * inside the provided result handlers. This root cause can be retrieved using {@link
    * ExecutionException#getCause()}.
    * @throws TimeoutException if not all operations are terminated with a result (success or failure) within the
    * given timeout.
    * @throws InterruptedException if the executing thread is interrupted while waiting for a response.
    */
   public void createReadUpdateDelete() throws InterruptedException, ExecutionException, TimeoutException
   {
      thingIntegration.create(myThingId).thenApply(createdThing -> myThing.changeAttribute("address/city", "Berlin"))
         .thenApply(changedSuccessfully -> myThing.retrieve()).thenApply(retrievedThing -> {
         LOGGER.info("My thing as persisted on the Bosch IoT Central Registry: {}", retrievedThing);
         return myThing.delete();
      }).get(10, TimeUnit.SECONDS);
   }

   /**
    * Creates a complex {@code Thing} object with {@code ACL}s, {@code Feature}s, and {@code Attribute}s, and waits for
    * a success or failure result.
    *
    * @throws ExecutionException if a failure response is received for the requests, or if an exception occurs
    * inside the provided result handler. This root cause can be retrieved using {@link
    * ExecutionException#getCause()}.
    * @throws TimeoutException if the operation is not terminated with a result (success or failure) within the
    * given timeout.
    * @throws InterruptedException if the executing thread is interrupted while waiting for a response.
    */
   public void createAComplexThing() throws InterruptedException, ExecutionException, TimeoutException
   {
      /* Create a new thing with acls, features, attributes and define handlers for success and failure */
      ThingBuilder builder = ThingBuilderImpl.newInstance(":complexThing");
      builder.aclEntryBuilder("user").permission(Permission.READ, Permission.WRITE, Permission.ADMINISTRATE).end();
      builder.featureBuilder("featureId").properties(Json.createObjectBuilder().add("property", "value").build()).end();
      builder.attributes("{\"attr\":true}");
      Thing complexThing = builder.build();

      thingIntegration.create(complexThing).whenComplete((thing, throwable) -> {
         if (throwable == null)
         {
            LOGGER.info("Thing created: {}", thing);
         }
         else
         {
            LOGGER.error("Create Thing Failed: {}", throwable);
         }
      }).get(1, TimeUnit.SECONDS);
   }

   /**
    * Shows different possibilities to retrieve a {@code Thing} or list of {@code Thing}s using their ids, with or
    * without {@link FieldSelector}s. {@code FieldSelector}s allow you to gain performance and save bandwidth by
    * only retrieving those fields of a that you are interested in.
    *
    * @throws ExecutionException if a failure response is received for the requests, or if an exception occurs
    * inside the provided result handler. This root cause can be retrieved using {@link
    * ExecutionException#getCause()}.
    * @throws TimeoutException if the operation is not terminated with a result (success or failure) within the
    * given timeout.
    * @throws InterruptedException if the executing thread is interrupted while waiting for a response.
    */
   public void retrieveThings() throws InterruptedException, ExecutionException, TimeoutException
   {
      /* Retrieve a Single Thing*/
      thingIntegration.forId(":complexThing").retrieve().thenAccept(thing -> LOGGER.info("Retrieved thing: {}", thing))
         .get(1, TimeUnit.SECONDS);

      /* Retrieve a List of Things */
      thingIntegration.retrieve(":myThing", ":complexThing").thenAccept(things -> {
         if (things.size() == 0)
         {
            LOGGER.info("The requested things were not found, or you don't have sufficient permission to read them.");
         }
         else
         {
            LOGGER.info("Retrieved things: {}", Arrays.toString(things.toArray()));
         }
      }).get(1, TimeUnit.SECONDS);

      /* Retrieve a List of Things with field selectors */
      thingIntegration.retrieve(FieldSelector.of("attributes"), ":myThing", ":complexThing").thenAccept(things -> {
         if (things.size() == 0)
         {
            LOGGER.info("The requested things were not found, or you don't have sufficient permission to read them.");
         }
         else
         {
            things.stream().forEach(thing -> LOGGER.info("Thing {} has attributes {}.", thing, thing.getAttributes()));
         }
      }).get(1, TimeUnit.SECONDS);
   }
}
