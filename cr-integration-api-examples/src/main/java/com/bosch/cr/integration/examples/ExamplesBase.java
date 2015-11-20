/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.integration.examples;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.bosch.cr.integration.IntegrationClient;
import com.bosch.cr.integration.IntegrationClientConfiguration;
import com.bosch.cr.integration.IntegrationClientImpl;
import com.bosch.cr.integration.ThingHandle;
import com.bosch.cr.integration.ThingIntegration;
import com.bosch.cr.integration.authentication.AuthenticationConfiguration;
import com.bosch.cr.integration.authentication.PublicKeyAuthenticationConfiguration;
import com.bosch.cr.integration.configuration.ProxyConfiguration;
import com.bosch.cr.integration.configuration.TrustStoreConfiguration;

/**
 * Instantiates an {@link IntegrationClient} and connects to the Bosch IoT Central Registry. It also initializes
 * {@link ThingIntegration} and {@link ThingHandle} instances for reuse in tests that extend this base class.
 *
 * @since 2.0.0
 */
public abstract class ExamplesBase
{
   public static final String KEYSTORE_PASSWORD = "solutionPass";
   public static final String ALIAS = "CR";
   public static final String ALIAS_PASSWORD = "crPass";

   public static final String BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URL = "wss://events.apps.bosch-iot-cloud.com:443/";
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
      AuthenticationConfiguration authenticationConfiguration = PublicKeyAuthenticationConfiguration.newBuilder()
         .clientId("example-client")
         .keyStoreLocation(KEYSTORE_LOCATION)
         .keyStorePassword(KEYSTORE_PASSWORD)
         .alias(ALIAS)
         .aliasPassword(ALIAS_PASSWORD)
         .build();

      /* optionally configure a proxy server or a truststore */
      ProxyConfiguration proxy = ProxyConfiguration.newBuilder().proxyHost("some.proxy.server").proxyPort(1234).build();
      TrustStoreConfiguration trustStore =
         TrustStoreConfiguration.newBuilder().location(TRUSTSTORE_LOCATION).password(TRUSTSTORE_PASSWORD).build();

      /* provide required configuration (authentication configuration and CR URI),
         optional configuration (proxy, truststore etc.) can be added when needed */
      final IntegrationClientConfiguration integrationClientConfiguration = IntegrationClientConfiguration.newBuilder()
         .authenticationConfiguration(authenticationConfiguration).centralRegistryEndpointUrl(
            BOSCH_IOT_CENTRAL_REGISTRY_ENDPOINT_URL)
            // .proxyConfiguration(proxy)
            .trustStoreConfiguration(trustStore)
         .build();

      this.integrationClient = IntegrationClientImpl.newInstance(integrationClientConfiguration);
      this.thingIntegration = integrationClient.things();
      this.myThingId = ":myThing";
      this.myThing = thingIntegration.forId(myThingId);
   }

   /**
    * Destroys the client and waits for its graceful shutdown.
    */
   public void terminate()
   {
      integrationClient.destroy(30, TimeUnit.SECONDS);
   }
}
