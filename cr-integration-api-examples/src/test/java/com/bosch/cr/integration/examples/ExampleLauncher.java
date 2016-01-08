/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

/**
 * Very simple main class which can launch the examples to try them out.
 */
public class ExampleLauncher
{
   public static void main(String... args)
   {
      //      final ManageThings manageThings = new ManageThings();
      //      try
      //      {
      //         manageThings.createReadUpdateDelete();
      //         manageThings.createAComplexThing();
      //         manageThings.retrieveThings();
      //      }
      //      catch (InterruptedException | ExecutionException | TimeoutException e)
      //      {
      //         e.printStackTrace();
      //      }

      //      final RegisterForChangesAndLifecycleEvents registerForChangesAndLifecycleEvents =
      //         new RegisterForChangesAndLifecycleEvents();
      //      registerForChangesAndLifecycleEvents.registerForAttributeChanges();
      //      registerForChangesAndLifecycleEvents.registerForLifecycleChanges();

      final RegisterForAndSendMessages registerForAndSendMessages = new RegisterForAndSendMessages();
      registerForAndSendMessages.registerForMessages();
      registerForAndSendMessages.sendMessages();
   }
}
