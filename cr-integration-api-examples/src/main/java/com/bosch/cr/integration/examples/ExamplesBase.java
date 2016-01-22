/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.client.IntegrationClientImpl;
import com.bosch.cr.integration.client.configuration.AuthenticationConfiguration;
import com.bosch.cr.integration.client.configuration.IntegrationClientConfiguration;
import com.bosch.cr.integration.client.configuration.MessageSerializerConfiguration;
import com.bosch.cr.integration.client.configuration.ProxyConfiguration;
import com.bosch.cr.integration.client.configuration.PublicKeyAuthenticationConfiguration;
import com.bosch.cr.integration.client.configuration.TrustStoreConfiguration;
import com.bosch.cr.integration.client.messages.MessageSerializerRegistry;
import com.bosch.cr.integration.client.messages.MessageSerializers;
import com.bosch.cr.integration.examples.model.ExampleUser;
import com.bosch.cr.integration.things.ThingHandle;
import com.bosch.cr.integration.things.ThingIntegration;

/**
 * Instantiates an {@link IntegrationClient} and connects to the Bosch IoT Central Registry. It also initializes
 * {@link ThingIntegration} and {@link ThingHandle} instances for reuse in tests that extend this base class.
 */
public abstract class ExamplesBase
{
   public static final String KEYSTORE_PASSWORD = "solutionPass";
   public static final String ALIAS = "CR";
   public static final String ALIAS_PASSWORD = "crPass";

   public static final String SOLUTION_ID = "<your-solution-id>";

   public static final String BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URL = "wss://events.apps.bosch-iot-cloud.com/";
   public static final URL KEYSTORE_LOCATION = ExamplesBase.class.getResource("/CRClient.jks");
   public static final URL TRUSTSTORE_LOCATION = ExamplesBase.class.getResource("/bosch-iot-cloud.jks");
   public static final String TRUSTSTORE_PASSWORD = "jks";

   protected final IntegrationClient integrationClient;
   protected final ThingIntegration thingIntegration;
   protected final String myThingId;
   protected final ThingHandle myThing;

   /**
    * Constructor.
    */
   public ExamplesBase()
   {
      final AuthenticationConfiguration authenticationConfiguration = PublicKeyAuthenticationConfiguration.newBuilder()
         .clientId(SOLUTION_ID + ":example-client")
         .keyStoreLocation(KEYSTORE_LOCATION)
         .keyStorePassword(KEYSTORE_PASSWORD)
         .alias(ALIAS)
         .aliasPassword(ALIAS_PASSWORD)
         .build();

      /* optionally configure a proxy server or a truststore */
      final ProxyConfiguration proxy = ProxyConfiguration.newBuilder()
         .proxyHost("some.proxy.server")
         .proxyPort(1234)
         .proxyUsername("some.proxy.username")
         .proxyPassword("some.proxy.password")
         .build();

      final TrustStoreConfiguration trustStore = TrustStoreConfiguration.newBuilder()
         .location(TRUSTSTORE_LOCATION)
         .password(TRUSTSTORE_PASSWORD)
         .build();

      final MessageSerializerConfiguration serializerConfiguration = MessageSerializerConfiguration.newInstance();
      setupCustomMessageSerializer(serializerConfiguration);

      /* provide required configuration (authentication configuration and CR URI),
         optional configuration (proxy, truststore etc.) can be added when needed */
      final IntegrationClientConfiguration integrationClientConfiguration = IntegrationClientConfiguration.newBuilder()
         .authenticationConfiguration(authenticationConfiguration)
         .centralRegistryEndpointUrl(BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URL)
         //.proxyConfiguration(proxy)
         .trustStoreConfiguration(trustStore)
         .serializerConfiguration(serializerConfiguration)
         .build();

      this.integrationClient = IntegrationClientImpl.newInstance(integrationClientConfiguration);

      try
      {
         // create a subscription for this client, this step can be skipped if a subscription was created via REST
         this.integrationClient.subscriptions()
            .create()
            // and start consuming events that were triggered by the subscription
            .thenRun(() -> this.integrationClient.subscriptions().consume()).get(10, TimeUnit.SECONDS);

         this.integrationClient.subscriptions().consume().get(10, TimeUnit.SECONDS);
      }
      catch (InterruptedException | ExecutionException | TimeoutException e)
      {
         throw new IllegalStateException("Error creating CR Client.", e);
      }

      this.thingIntegration = integrationClient.things();
      this.myThingId = ":myThing";
      this.myThing = thingIntegration.forId(myThingId);
   }

   /**
    * Sets up a serializer/deserializer for the {@link ExampleUser} model class which uses JAXB in order to serialize
    * and deserialize messages which should directly be mapped to this type.
    *
    * @param serializerConfiguration the initial MessageSerializerConfiguration to adjust.
    */
   private void setupCustomMessageSerializer(final MessageSerializerConfiguration serializerConfiguration)
   {
      final JAXBContext jaxbContext;
      try
      {
         jaxbContext = JAXBContext.newInstance(ExampleUser.class);
      }
      catch (JAXBException e)
      {
         throw new RuntimeException("Could not setup JAXBContext", e);
      }

      final MessageSerializerRegistry serializerRegistry = serializerConfiguration.getMessageSerializerRegistry();

      serializerRegistry.registerMessageSerializer(
         MessageSerializers.of(ExampleUser.USER_CUSTOM_CONTENT_TYPE, ExampleUser.class, "*", //
            (exampleUser, charset) -> {
               try
               {
                  Marshaller marshaller = jaxbContext.createMarshaller();
                  marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                  ByteArrayOutputStream os = new ByteArrayOutputStream();
                  marshaller.marshal(exampleUser, os);
                  return ByteBuffer.wrap(os.toByteArray());
               }
               catch (JAXBException e)
               {
                  throw new RuntimeException("Could not serialize", e);
               }
            },
            (byteBuffer, charset) -> {
               try
               {
                  Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                  ByteArrayInputStream is = new ByteArrayInputStream(byteBuffer.array());
                  return (ExampleUser) jaxbUnmarshaller.unmarshal(is);
               }
               catch (JAXBException e)
               {
                  throw new RuntimeException("Could not deserialize", e);
               }
            }));
   }

   /**
    * Destroys the client and waits for its graceful shutdown.
    */
   public void terminate()
   {
      integrationClient.destroy();
   }
}
