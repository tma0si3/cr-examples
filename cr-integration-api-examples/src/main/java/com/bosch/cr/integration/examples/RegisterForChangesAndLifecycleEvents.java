/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.json.JsonFactory;

/**
 * This example shows the various possibilities that the {@code IntegrationClient} offers for registering
 * handlers to be informed about {@link com.bosch.cr.integration.things.ThingLifecycleEvent}s and
 * {@link com.bosch.cr.integration.things.ThingAttributeChange}s of your {@code Thing}s.
 * <p>
 * NOTE: Make sure to invoke {@code IntegrationClient.subscriptions().consume()} once after all handlers are
 * registered to start receiving events from Central Registry.
 */
public final class RegisterForChangesAndLifecycleEvents extends ExamplesBase
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegisterForChangesAndLifecycleEvents.class);

   private static final String ALL_THINGS_LIFECYCLE = "allThings_lifecycle";
   private static final String MY_THING_LIFECYCLE = "myThing_lifecycle";
   private static final String ALL_THINGS_ATTRIBUTE_CHANGE = "allThings_attributeChange";
   private static final String ALL_THINGS_SPECIFIC_ATTRIBUTE_CHANGE = "allThings_specificAttributeChange";
   private static final String MY_THING_ATTRIBUTE_CHANGE = "myThing_attributeChange";
   private static final String MY_THING_SPECIFIC_ATTRIBUTE_CHANGE = "myThing_specificAttributeChange";

   /**
    * Register for {@code LifecycleEvent}s.
    */
   public void registerForLifecycleChanges()
   {
      /* Register for lifecycle events of *all* things */
      thingIntegration.registerForLifecycleEvent(ALL_THINGS_LIFECYCLE, lifecycle -> LOGGER.info("lifecycle received: {}", lifecycle));

      /* Register for *all* lifecycle events of a *specific* thing */
      myThing.registerForLifecycleEvent(MY_THING_LIFECYCLE, lifecycleEvent -> LOGGER.info("lifecycle received: {}", lifecycleEvent));
   }

   /**
    * Register for {@code ImmutableThingAttributeChange}s.
    */
   public void registerForAttributeChanges()
   {
      /* Register for *all* attribute changes of *all* things */
      thingIntegration.registerForAttributeChange(ALL_THINGS_ATTRIBUTE_CHANGE, change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of *all* things */
      thingIntegration.registerForAttributeChange(ALL_THINGS_SPECIFIC_ATTRIBUTE_CHANGE,
         JsonFactory.newPointer("address/city"), change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *all* attribute changes of a *specific* thing */
      myThing.registerForAttributeChange(MY_THING_ATTRIBUTE_CHANGE, change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of a *specific* thing */
      myThing.registerForAttributeChange(MY_THING_SPECIFIC_ATTRIBUTE_CHANGE, JsonFactory.newPointer("address/city"),
         change -> LOGGER.info("attributeChange received: {}", change));
   }
}
