/*
 *                                            Bosch SI Example Code License
 *                                              Version 1.0, January 2016
 *
 * Copyright 2016 Bosch Software Innovations GmbH ("Bosch SI"). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * BOSCH SI PROVIDES THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF
 * ALL NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH
 * BOSCH SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT
 * LIMITATION FOR INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF
 * THE GERMAN PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN
 * UNAFFECTED BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF
 * LIABILITY ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S
 * EMPLOYEES, REPRESENTATIVES AND ORGANS.
 */
package com.bosch.cr.integration.helloworld;

import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.client.IntegrationClientImpl;
import com.bosch.cr.integration.client.configuration.AuthenticationConfiguration;
import com.bosch.cr.integration.client.configuration.IntegrationClientConfiguration;
import com.bosch.cr.integration.client.configuration.PublicKeyAuthenticationConfiguration;
import com.bosch.cr.integration.client.configuration.TrustStoreConfiguration;
import com.bosch.cr.integration.things.ThingHandle;
import com.bosch.cr.integration.things.ThingIntegration;
import com.bosch.cr.model.acl.AclEntry;
import com.bosch.cr.model.acl.Permission;
import com.bosch.cr.model.attributes.Attributes;
import com.bosch.cr.model.attributes.AttributesModelFactory;
import com.bosch.cr.model.authorization.AuthorizationModelFactory;
import com.bosch.cr.model.things.Thing;

public class HelloWorld
{
   // Things Service in Cloud
   public static final String BOSCH_IOT_CENTRAL_REGISTRY_WS_ENDPOINT_URL = "wss://events.apps.bosch-iot-cloud.com";
   // Insert your Solution ID here
   public static final String SOLUTION_ID = "<your-solution-id>";
   public static final String CLIENT_ID = SOLUTION_ID;
   // Insert your User ID here
   public static final String USER_ID = "<your-user-id>";
   // Insert your keystore passwords here
   public static final URL KEYSTORE_LOCATION = HelloWorld.class.getResource("/CRClient.jks");
   public static final String KEYSTORE_PASSWORD = "<your-keystore-password>";
   public static final String ALIAS = "CR";
   public static final String ALIAS_PASSWORD = "<your-alias-password>";
   // At the moment necessary for accepting bosch self signed certificates
   public static final URL TRUSTSTORE_LOCATION = HelloWorld.class.getResource("/bosch-iot-cloud.jks");
   public static final String TRUSTSTORE_PASSWORD = "jks";
   // Logger
   private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorld.class);

   public static int i = 0;

   final IntegrationClient integrationClient;
   final ThingIntegration thingIntegration;


   /**
    * See tutorial here for step by step instructions.
    */
   public static void main(final String... args) throws InterruptedException, ExecutionException, TimeoutException
   {
      // Instantiate the Java Client
      HelloWorld helloWorld = new HelloWorld();

      // Step 1: Create an empty Thing and get Thing ID
      String thingId = helloWorld.createEmptyThing();

      // Step 2: Update the ACL with your User ID
      // Before this Step you have to add your User ID in the HelloWorld Class
      helloWorld.updateACL(thingId);

      // Step 3: Loop to update the attributes of the Thing
      for (int i = 0; i <= 200; i++)
      {
         helloWorld.updateThing(thingId);
         Thread.sleep(2000);
      }

      // This step must always be concluded to terminate the Java client.
      helloWorld.terminate();
   }

   /**
    * Client instantiation
    */
   public HelloWorld()
   {
      /* build an authentication configuration */
      final AuthenticationConfiguration authenticationConfiguration = PublicKeyAuthenticationConfiguration.newBuilder().clientId(CLIENT_ID) //
         .keyStoreLocation(KEYSTORE_LOCATION) //
         .keyStorePassword(KEYSTORE_PASSWORD) //
         .alias(ALIAS) //
         .aliasPassword(ALIAS_PASSWORD) //
         .build();

      /* configure a truststore that contains trusted certificates */
      final TrustStoreConfiguration trustStore =
         TrustStoreConfiguration.newBuilder().location(TRUSTSTORE_LOCATION).password(TRUSTSTORE_PASSWORD).build();

      /**
       * provide required configuration (authentication configuration and CR URI),
       * optional configuration (proxy, truststore etc.) can be added when needed
       */
      final IntegrationClientConfiguration integrationClientConfiguration = IntegrationClientConfiguration.newBuilder()
         .authenticationConfiguration(authenticationConfiguration)
         .centralRegistryEndpointUrl(BOSCH_IOT_CENTRAL_REGISTRY_WS_ENDPOINT_URL)
         // .proxyConfiguration(proxy)
         .trustStoreConfiguration(trustStore)
         .build();

      LOGGER.info("Creating CR Integration Client for ClientID: {}", CLIENT_ID);

      /* Create a new integration client object to start interacting with the Central Registry */
      integrationClient = IntegrationClientImpl.newInstance(integrationClientConfiguration);

      /* Create a new thing integration object to start interacting with the Central Registry */
      thingIntegration = integrationClient.things();
   }

   /**
    * Create an empty Thing
    *
    * @return thing id
    */
   public String createEmptyThing()
   {
      String thingId = null;
      try
      {
         thingId = thingIntegration.create().thenCompose(thing -> {
            LOGGER.info("Thing with ID '{}' created.", thing);
            return integrationClient.things().forId(thing.getId().get()).retrieve();
         }).get(2, TimeUnit.SECONDS).getId().get();
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         e.printStackTrace();
      }
      return thingId;
   }

   /**
    * Update Attributes of a specified Thing
    */
   public void updateThing(String thingID)
   {
      Thing thing;
      ThingHandle thingHandle = thingIntegration.forId(thingID);
      try
      {
         thing = thingHandle.retrieve().get(2, TimeUnit.SECONDS);
         Attributes attributes = AttributesModelFactory.newAttributesBuilder().set("Counter", i++).build();
         thing = thing.setAttributes(attributes);
         thingIntegration.update(thing).get(2, TimeUnit.SECONDS);
         LOGGER.info("Thing with ID '{}' updated!", thingHandle.getThingId());
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Update the ACL of a specified Thing
    */
   public void updateACL(String thingID)
   {
      Thing thing;
      final AclEntry acl;
      ThingHandle thingHandle = thingIntegration.forId(thingID);
      try
      {
         thing = thingHandle.retrieve().get(2, TimeUnit.SECONDS);
         acl = AclEntry.newInstance(AuthorizationModelFactory.newAuthSubject(USER_ID), Permission.READ, Permission.WRITE, Permission.ADMINISTRATE);
         thing = thing.setAclEntry(acl);
         thingIntegration.update(thing).get(2, TimeUnit.SECONDS);
         LOGGER.info("Thing with ID '{}' updated (ACL entry)!", thingHandle.getThingId());
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Destroys the client and waits for its graceful shutdown.
    */
   public void terminate()
   {
      /* Gracefully shutdown the integrationClient */
      integrationClient.destroy();
   }

   /**
    * Create a Thing with given ThingId
    */
   public void createThing(String thingID)
   {
      try
      {
         thingIntegration.create(thingID).thenAccept(thing -> LOGGER.info("Thing created: {}", thing)).get(2, TimeUnit.SECONDS);
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Find a Thing with given ThingId
    */
   public void getThingByID(String thingID)
   {
      Thing thing;
      ThingHandle thingHandle = thingIntegration.forId(thingID);
      try
      {
         thing = thingHandle.retrieve().get(2, TimeUnit.SECONDS);
         LOGGER.info("Thing with ID found: {}", thingHandle.getThingId());
         LOGGER.info("Thing Attributes: {}", thing.getAttributes());
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Delete a specified Thing
    */
   public void deleteThing(String thingID)
   {
      try
      {
         thingIntegration.delete(thingID).get(2, TimeUnit.SECONDS);
         LOGGER.info("Thing with ID deleted: {}", thingID);
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         e.printStackTrace();
      }
   }
}
