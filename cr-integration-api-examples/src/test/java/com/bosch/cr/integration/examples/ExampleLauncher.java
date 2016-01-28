/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Very simple main class which can launch the examples to try them out.
 */
public class ExampleLauncher
{
   public static void main(final String... args) throws InterruptedException, ExecutionException, TimeoutException
   {
      // final ManageThings manageThings = new ManageThings();
      // try
      // {
      // manageThings.createReadUpdateDelete();
      // manageThings.createAComplexThing();
      // manageThings.retrieveThings();
      // }
      // catch (InterruptedException | ExecutionException | TimeoutException e)
      // {
      // e.printStackTrace();
      // }

      // final RegisterForChangesAndLifecycleEvents registerForChangesAndLifecycleEvents =
      // new RegisterForChangesAndLifecycleEvents();
      // registerForChangesAndLifecycleEvents.registerForAttributeChanges();
      // registerForChangesAndLifecycleEvents.registerForLifecycleChanges();

      // final RegisterForAndSendMessages registerForAndSendMessages = new RegisterForAndSendMessages();
      // registerForAndSendMessages.registerForMessages();
      // registerForAndSendMessages.sendMessages();

      final ManageFeatures manageFeatures = new ManageFeatures();
//      manageFeatures.crudFeature();
//      manageFeatures.crudFeatureProperty();
      manageFeatures.crudFeatureProperties();
   }
}
