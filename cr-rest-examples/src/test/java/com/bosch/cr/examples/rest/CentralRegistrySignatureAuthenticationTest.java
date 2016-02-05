/*
 * Copyright (c) 2015, Bosch Software Innovations GmbH, Germany All rights reserved.
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
 * 3. Neither the name of the Bosch Software Innovations GmbH, Germany nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.bosch.cr.examples.rest;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Response;
import org.asynchttpclient.proxy.ProxyServer;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test to show CRS Authentication.
 *
 * @since 1.0.0
 */
public class CentralRegistrySignatureAuthenticationTest
{

   private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
   private static final String CONTENT_TYPE_JSON = "application-json";

   private static final int HTTP_STATUS_CREATED = 201;
   private static final int HTTP_STATUS_NO_CONTENT = 204;

   private static String centralRegistryEndpointUrl;
   private static AsyncHttpClient asyncHttpClient;
   private static String thingId;

   /** */
   @BeforeClass
   public static void setUp() throws KeyManagementException, NoSuchAlgorithmException, IOException
   {
      final Properties props = new Properties(System.getProperties());
      final FileReader r;
      if (Files.exists(Paths.get("config.properties")))
      {
         r = new FileReader(Paths.get("config.properties").toFile());
      }
      else
      {
         r = new FileReader(
            CentralRegistrySignatureAuthenticationTest.class.getClassLoader().getResource("config.properties").getFile());
      }
      props.load(r);
      r.close();

      centralRegistryEndpointUrl = props.getProperty("centralRegistryEndpointUrl");

      final String clientId = props.getProperty("clientId");
      final String apiToken = props.getProperty("apiToken");

      final URI keystoreUri = new File(props.getProperty("keystoreLocation")).toURI();
      final String keyStorePassword = props.getProperty("keyStorePassword");
      final String keyAlias = props.getProperty("keyAlias");
      final String keyAliasPassword = props.getProperty("keyAliasPassword");

      final SignatureFactory signatureFactory =
         SignatureFactory.newInstance(keystoreUri, keyStorePassword, keyAlias, keyAliasPassword);

      final DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
      builder.setAcceptAnyCertificate(true); // WORKAROUND: Trust self-signed certificate of BICS until there is a trusted one.

      final String proxyHost = props.getProperty("http.proxyHost");
      final String proxyPort = props.getProperty("http.proxyPort");
      final String proxyPrincipal = props.getProperty("http.proxyPrincipal");
      final String proxyPassword = props.getProperty("http.proxyPassword");
      if (proxyHost != null && proxyPort != null)
      {
         final ProxyServer.Builder proxyBuilder = new ProxyServer.Builder(proxyHost, Integer.valueOf(proxyPort));
         if (proxyPrincipal != null && proxyPassword != null)
         {
            // proxy with authentication
            proxyBuilder.setRealm(new Realm.Builder(proxyPrincipal, proxyPassword).setScheme(Realm.AuthScheme.BASIC).setUsePreemptiveAuth(true));
         }
         builder.setProxyServer(proxyBuilder);
      }

      asyncHttpClient = new DefaultAsyncHttpClient(builder.build());
      asyncHttpClient.setSignatureCalculator(new CrAsymmetricalSignatureCalculator(signatureFactory, clientId, apiToken));

      thingId = "com.bosch.cr.example:myThing-" + UUID.randomUUID().toString();
   }

   /**
    * PUT a Thing with CRS Authentication.
    *
    * @throws ExecutionException
    * @throws InterruptedException
    */
   @Test
   public void putThingWithCRS() throws ExecutionException, InterruptedException
   {
      final String thingJsonString = "{}";
      final String path = "/cr/1/things/" + thingId;

      final ListenableFuture<Response> future = asyncHttpClient.preparePut(centralRegistryEndpointUrl + path) //
         .addHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON) //
         .setBody(thingJsonString) //
         .execute();

      final Response response = future.get();
      assertEquals(HTTP_STATUS_CREATED, response.getStatusCode());
   }

   /**
    * Delete a Thing with CRS Authentication.
    *
    * @throws ExecutionException
    * @throws InterruptedException
    */
   @Test
   public void deleteThingWithCRS() throws ExecutionException, InterruptedException
   {
      final String path = "/cr/1/things/" + thingId;

      final ListenableFuture<Response> future = asyncHttpClient.prepareDelete(centralRegistryEndpointUrl + path) //
         .execute();

      final Response response = future.get();
      assertEquals(HTTP_STATUS_NO_CONTENT, response.getStatusCode());
   }

}
