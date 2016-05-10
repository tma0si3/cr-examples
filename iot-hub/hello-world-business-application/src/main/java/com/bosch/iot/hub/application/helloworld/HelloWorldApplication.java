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
package com.bosch.iot.hub.application.helloworld;

import static com.bosch.iot.hub.client.configuration.ConfigOptions.General.CLIENT_ID;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bosch.iot.hub.client.ConsumerRegistration;
import com.bosch.iot.hub.client.DefaultIotHubClient;
import com.bosch.iot.hub.client.IotHubClient;
import com.bosch.iot.hub.client.IotHubClientBuilder;

public class HelloWorldApplication
{
   // Hub Service in the cloud
   public static final String BOSCH_IOT_CENTRAL_REGISTRY_WS_ENDPOINT_URL = "wss://hub.apps.bosch-iot-cloud.com";

   // Insert your Solution ID here
   public static final String SOLUTION_ID = "<your-solution-id>";
   public static final String CONSUMER_CLIENT_ID = SOLUTION_ID+":consumer";

   // Insert your keystore passwords here
   public static final URL KEYSTORE_LOCATION = HelloWorldApplication.class.getResource("/CRClient.jks");
   public static final String KEYSTORE_PASSWORD = "<your-keystore-password>";
   public static final String ALIAS = "CR";
   public static final String ALIAS_PASSWORD = "<your-alias-password>";

   // At the moment necessary for accepting bosch self signed certificates
   public static final URL TRUSTSTORE_LOCATION = HelloWorldApplication.class.getResource("/bosch-iot-cloud.jks");
   public static final String TRUSTSTORE_PASSWORD = "jks";

   // Logger
   private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldApplication.class);

   /**
    * Create a Topic and send messages the java client.
    */
   public static void main(final String... args) throws InterruptedException, ExecutionException, TimeoutException, URISyntaxException
   {
      /**
       * Instantiate the Java Client
       */
      final HelloWorldApplication helloWorld = new HelloWorldApplication();

   }

   /**
    * Client instantiation
    */
   public HelloWorldApplication() throws URISyntaxException, InterruptedException, ExecutionException, TimeoutException
   {

      /* Create a new integration client object to start interacting with the Hub Service */
      IotHubClient iotHubClient = iniIotHubClient();
      /**
       * Connect the iot hub client
       */
      iotHubClient.connect();
      LOGGER.info("Creating Hub Integration Client for ClientID: {}", CLIENT_ID);

      /* Create a new topic with consumer and connector ACL*/
      ConsumerRegistration consumerRegistration = iotHubClient.consume(inboundMessage -> {
         LOGGER.info("Receive message: {} from client {}", new String(inboundMessage.getPayload().get().getContentAsByteArray()),inboundMessage.getSender().getIdentifier());
      });

      CompletableFuture completableFuture = new CompletableFuture();
      completableFuture.wait();

      /**
       * Deregister the consumer
       */
      consumerRegistration.unregister();
      /**
       * This step must always be concluded to terminate the Java client.
       */
      iotHubClient.destroy();

   }


   public IotHubClient iniIotHubClient() throws URISyntaxException
   {
      /**
       * Provide required configuration (authentication configuration and HUB URI), optional proxy configuration can be
       * added when needed
       */
      IotHubClientBuilder.OptionalPropertiesSettable builder = DefaultIotHubClient.newBuilder() //
         .endPoint(URI.create(BOSCH_IOT_CENTRAL_REGISTRY_WS_ENDPOINT_URL)) //
         .keyStore(KEYSTORE_LOCATION.toURI(),KEYSTORE_PASSWORD) //
         .alias(ALIAS, ALIAS_PASSWORD)
         .clientId(SOLUTION_ID).sslTrustStore(TRUSTSTORE_LOCATION.toURI(), TRUSTSTORE_PASSWORD); //

      return builder.build();
   }

}
