/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import com.bosch.cr.json.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example shows the various possibilities that the {@code IntegrationClient} offers for registering
 * handlers to be informed about {@link com.bosch.cr.integration.things.ThingChange}s and
 * {@link com.bosch.cr.integration.things.Change}s of your {@code Thing}s.
 * <p>
 * NOTE: Make sure to invoke {@code IntegrationClient.subscriptions().consume()} once after all handlers are
 * registered to start receiving events from Central Registry.
 */
public final class RegisterForChanges extends ExamplesBase
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegisterForChanges.class);

   private static final String ALL_THINGS = "allThings";
   private static final String MY_THING = "myThing";
   private static final String ALL_THINGS_ATTRIBUTE_CHANGE = "allThings_attributeChanges";
   private static final String ALL_THINGS_SPECIFIC_ATTRIBUTE_CHANGE = "allThings_specificAttributeChanges";
   private static final String MY_THING_ATTRIBUTE_CHANGE = "myThing_attributeChanges";
   private static final String MY_THING_SPECIFIC_ATTRIBUTE_CHANGE = "myThing_specificAttributeChanges";

   /**
    * Register for {@code ThingLifecycleChange}s.
    */
   public void registerForLifecycleChanges()
   {
      /* Register for lifecycle events of *all* things */
      thingIntegration.registerForThingChanges(ALL_THINGS, change -> LOGGER.info("ThingChange received: {}", change));

      /* Register for *all* lifecycle events of a *specific* thing */
      myThing.registerForThingChanges(MY_THING, change -> LOGGER.info("ThingChange received: {}", change));
   }

   /**
    * Register for {@code ImmutableThingAttributeChange}s.
    */
   public void registerForAttributeChanges()
   {
      /* Register for *all* attribute changes of *all* things */
      thingIntegration.registerForAttributeChanges(ALL_THINGS_ATTRIBUTE_CHANGE, change -> LOGGER.info("Change received: {}", change));

      /* Register for *specific* attribute changes of *all* things */
      thingIntegration.registerForAttributeChanges(ALL_THINGS_SPECIFIC_ATTRIBUTE_CHANGE,
         JsonFactory.newPointer("address/city"), change -> LOGGER.info("Change received: {}", change));

      /* Register for *all* attribute changes of a *specific* thing */
      myThing.registerForAttributeChanges(MY_THING_ATTRIBUTE_CHANGE, change -> LOGGER.info("Change received: {}", change));

      /* Register for *specific* attribute changes of a *specific* thing */
      myThing.registerForAttributeChanges(MY_THING_SPECIFIC_ATTRIBUTE_CHANGE, JsonFactory.newPointer("address/city"),
         change -> LOGGER.info("attributeChange received: {}", change));
   }
}
