/*
 * Bosch SI Example Code License Version 1.0, January 2016
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
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE
 * QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
 * NECESSARY SERVICING, REPAIR OR CORRECTION. THIS SHALL NOT APPLY TO MATERIAL DEFECTS AND DEFECTS OF TITLE WHICH BOSCH
 * SI HAS FRAUDULENTLY CONCEALED. APART FROM THE CASES STIPULATED ABOVE, BOSCH SI SHALL BE LIABLE WITHOUT LIMITATION FOR
 * INTENT OR GROSS NEGLIGENCE, FOR INJURIES TO LIFE, BODY OR HEALTH AND ACCORDING TO THE PROVISIONS OF THE GERMAN
 * PRODUCT LIABILITY ACT (PRODUKTHAFTUNGSGESETZ). THE SCOPE OF A GUARANTEE GRANTED BY BOSCH SI SHALL REMAIN UNAFFECTED
 * BY LIMITATIONS OF LIABILITY. IN ALL OTHER CASES, LIABILITY OF BOSCH SI IS EXCLUDED. THESE LIMITATIONS OF LIABILITY
 * ALSO APPLY IN REGARD TO THE FAULT OF VICARIOUS AGENTS OF BOSCH SI AND THE PERSONAL LIABILITY OF BOSCH SI'S EMPLOYEES,
 * REPRESENTATIVES AND ORGANS.
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
import com.bosch.cr.integration.things.ThingIntegration;
import com.bosch.cr.json.JsonFactory;
import com.bosch.cr.model.acl.AclEntry;
import com.bosch.cr.model.acl.Permission;
import com.bosch.cr.model.authorization.AuthorizationSubject;
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
   private static final int TIMEOUT = 2;

   public static int i = 0;

   final IntegrationClient integrationClient;
   final ThingIntegration thingIntegration;

   /**
    * Create and update a thing with the java client.
    */
   public static void main(final String... args) throws InterruptedException, ExecutionException, TimeoutException
   {
      /**
       * Instantiate the Java Client
       */
      final HelloWorld helloWorld = new HelloWorld();

      /**
       * Create an empty Thing and get Thing ID
       */
      final String thingId = helloWorld.createEmptyThing();

      /**
       * Update the ACL with your User ID to see your thing in the Demo Web UI
       */
      helloWorld.updateACL(thingId);

      /**
       * Loop to update the attributes of the Thing
       */
      for (int i = 0; i <= 100; i++)
      {
         helloWorld.updateThing(thingId);
         Thread.sleep(2000);
      }

      /**
       * This step must always be concluded to terminate the Java client.
       */
      helloWorld.terminate();
   }

   /**
    * Client instantiation
    */
   public HelloWorld()
   {
      /* Build an authentication configuration */
      final AuthenticationConfiguration authenticationConfiguration =
         PublicKeyAuthenticationConfiguration.newBuilder().clientId(CLIENT_ID) //
            .keyStoreLocation(KEYSTORE_LOCATION) //
            .keyStorePassword(KEYSTORE_PASSWORD) //
            .alias(ALIAS) //
            .aliasPassword(ALIAS_PASSWORD) //
            .build();

      /* optionally configure a proxy server */
      // final ProxyConfiguration proxy = ProxyConfiguration.newBuilder()
      // .proxyHost("some.proxy.server")
      // .proxyPort(1234)
      // .proxyUsername("some.proxy.username")
      // .proxyPassword("some.proxy.password")
      // .build();

      /* Configure a truststore that contains trusted certificates */
      final TrustStoreConfiguration trustStore =
         TrustStoreConfiguration.newBuilder().location(TRUSTSTORE_LOCATION).password(TRUSTSTORE_PASSWORD).build();

      /**
       * Provide required configuration (authentication configuration and CR URI), optional proxy configuration can be
       * added when needed
       */
         final IntegrationClientConfiguration integrationClientConfiguration =
            IntegrationClientConfiguration.newBuilder() //
               .authenticationConfiguration(authenticationConfiguration)
               .centralRegistryEndpointUrl(BOSCH_IOT_CENTRAL_REGISTRY_WS_ENDPOINT_URL) //
               .trustStoreConfiguration(trustStore) //
               // .proxyConfiguration(proxy) //
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
         thingId = thingIntegration.create() //
            .thenApply(thing -> {
               final String id = thing.getId().get();
               LOGGER.info("Thing with ID '{}' created.", id);
               return id;
            }).get(TIMEOUT, TimeUnit.SECONDS);
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         LOGGER.error(e.getMessage());
      }
      return thingId;
   }

   /**
    * Update Attributes of a specified Thing
    */
   public void updateThing(final String thingId)
   {
      thingIntegration.forId(thingId) //
         .retrieve() //
         .thenCompose(thing -> {
            final Thing updated = thing.setAttribute(JsonFactory.newPointer("Counter"), JsonFactory.newValue(i++));
            return thingIntegration.update(updated);
         }) //
         .whenComplete((aVoid, throwable) -> {
            if (null == throwable)
            {
               LOGGER.info("Thing with ID '{}' updated with Counter={}!", thingId, i);
            }
            else
            {
               LOGGER.error(throwable.getMessage());
            }
         });
   }

   /**
    * Update the ACL of a specified Thing
    */
   public void updateACL(final String thingId)
   {
      thingIntegration.forId(thingId) //
         .retrieve() //
         .thenCompose(thing -> {
            final AclEntry aclEntry = AclEntry.newInstance(AuthorizationSubject.newInstance(USER_ID), //
               Permission.READ, //
               Permission.WRITE, //
               Permission.ADMINISTRATE);

            final Thing updated = thing.setAclEntry(aclEntry);
            return thingIntegration.update(updated);
         }) //
         .whenComplete((aVoid, throwable) -> {
            if (null == throwable)
            {
               LOGGER.info("Thing with ID '{}' updated (ACL entry)!", thingId);
            }
            else
            {
               LOGGER.error(throwable.getMessage());
            }
         });
   }

   /**
    * Create a Thing with given ThingId
    */
   public void createThing(final String thingId)
   {
      thingIntegration.create(thingId) //
         .whenComplete((aVoid, throwable) -> {
            if (null == throwable)
            {
               LOGGER.info("Thing with ID '{}' created.", thingId);
            }
            else
            {
               LOGGER.error(throwable.getMessage());
            }
         });
   }

   /**
    * Find a Thing with given ThingId
    */
   public void getThingByID(final String thingId)
   {
      thingIntegration.forId(thingId) //
         .retrieve() //
         .whenComplete((thing, throwable) -> {
            if (null == throwable)
            {
               LOGGER.info("Thing with ID found: {}", thingId);
               LOGGER.info("Thing Attributes: {}", thing.getAttributes());
            }
            else
            {
               LOGGER.error(throwable.getMessage());
            }
         });
   }

   /**
    * Delete a specified Thing
    */
   public void deleteThing(final String thingId)
   {
      thingIntegration.delete(thingId) //
         .whenComplete((aVoid, throwable) -> {
            if (null == throwable)
            {
               LOGGER.info("Thing with ID deleted: {}", thingId);
            }
            else
            {
               LOGGER.error(throwable.getMessage());
            }
         });
   }

   /**
    * Destroys the client and waits for its graceful shutdown.
    */
   public void terminate()
   {
      /* Gracefully shutdown the integrationClient */
      integrationClient.destroy();
   }

}
