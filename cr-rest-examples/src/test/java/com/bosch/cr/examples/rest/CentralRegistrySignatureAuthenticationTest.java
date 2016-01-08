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
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Test;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Response;

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

      final AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
      builder.setSSLContext(setupAcceptingSelfSignedCertificates());

      final String proxyHost = props.getProperty("http.proxyHost");
      final String proxyPort = props.getProperty("http.proxyPort");
      final String proxyPrincipal = props.getProperty("http.proxyPrincipal");
      final String proxyPassword = props.getProperty("http.proxyPassword");
      if (proxyHost != null && proxyPort != null)
      {
         if (proxyPrincipal != null && proxyPassword != null)
         {
            // proxy with authentication
            builder.setProxyServer(new ProxyServer(ProxyServer.Protocol.HTTPS, proxyHost, Integer.valueOf(proxyPort),
               proxyPrincipal, proxyPassword));
         }
         else
         {
            // proxy w/o authentication
            builder.setProxyServer(new ProxyServer(ProxyServer.Protocol.HTTPS, proxyHost, Integer.valueOf(proxyPort)));
         }
      }

      asyncHttpClient = new AsyncHttpClient(builder.build());
      asyncHttpClient.setSignatureCalculator(new CrAsymmetricalSignatureCalculator(signatureFactory, clientId, apiToken));

      thingId = "com.bosch.cr.example:myThing-" + UUID.randomUUID().toString();
   }

   /**
    * WORKAROUND: Trust self-signed certificate of BICS until there is a trusted one.
    */
   private static SSLContext setupAcceptingSelfSignedCertificates() throws NoSuchAlgorithmException, KeyManagementException
   {
      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
      {
         @Override
         public X509Certificate[] getAcceptedIssuers()
         {
            return new X509Certificate[0];
         }

         @Override
         public void checkClientTrusted(final X509Certificate[] certs, final String authType) {}

         @Override
         public void checkServerTrusted(final X509Certificate[] certs, final String authType) {}
      } };
      final SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, null);
      return sc;
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
