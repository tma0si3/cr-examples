/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This example shows the various possibilities that the {@code IntegrationClient} offers for registering
 * handlers to be informed about {@link com.bosch.cr.integration.registration.ThingLifecycleEvent}s and
 * {@link com.bosch.cr.integration.registration.ThingAttributeChange}s of your {@code Thing}s.
 *
 * @since 2.0.0
 */
public class RegisterForChangesAndLifecycleEvents extends ExamplesBase
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegisterForChangesAndLifecycleEvents.class);

   /**
    * Register for {@code LifecycleEvent}s.
    */
   public void registerForLifecycleChanges()
   {
      /* Register for lifecycle events of *all* things */
      final String allThings_lifecycleRegistration = "allThings_lifecycleRegistration";
      thingIntegration.registerForLifecycleEvent(allThings_lifecycleRegistration,
         lifecycle -> LOGGER.info("lifecycle received: {}", lifecycle));

      /* Register for *all* lifecycle events of a *specific* thing */
      final String myThing_lifecycleRegistration = "myThing_lifecycleRegistration";
      myThing.registerForLifecycleEvent(myThing_lifecycleRegistration,
         lifecycleEvent -> LOGGER.info("lifecycle received: {}", lifecycleEvent));
   }

   /**
    * Register for {@code ThingAttributeChange}s.
    */
   public void registerForAttributeChanges()
   {
      /* Register for *all* attribute changes of *all* things */
      final String allThings_attributeChangeRegistration = "allThings_attributeChangeRegistration";
      thingIntegration.registerForAttributeChange(allThings_attributeChangeRegistration,
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of *all* things */
      final String allThings_specificAttributeChangeRegistration = "allThings_specificAttributeChangeRegistration";
      thingIntegration.registerForAttributeChange(allThings_specificAttributeChangeRegistration, "address/city",
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *all* attribute changes of a *specific* thing */
      final String myThing_attributeChangeRegistration = "myThing_attributeChangeRegistration";
      myThing.registerForAttributeChange(myThing_attributeChangeRegistration,
         change -> LOGGER.info("attributeChange received: {}", change));

      /* Register for *specific* attribute changes of a *specific* thing */
      final String myThing_specificAttributeChangeRegistration = "myThing_specificAttributeChangeRegistration";
      myThing.registerForAttributeChange(myThing_specificAttributeChangeRegistration, "address/city",
         change -> LOGGER.info("attributeChange received: {}", change));
   }
}
