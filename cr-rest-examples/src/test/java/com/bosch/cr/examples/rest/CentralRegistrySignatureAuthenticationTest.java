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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Test;
import com.bosch.cr.examples.rest.solution.Solution;
import com.bosch.cr.examples.rest.solution.SolutionsClient;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

/**
 * Unit test to show CRS Authentication.
 *
 * @since 1.0.0
 */
public class CentralRegistrySignatureAuthenticationTest
{

   private static final String HOST = "cr.apps.bosch-iot-cloud.com";
   private static final String BASE_URL = "https://" + HOST;
   private static final String SOLUTIONS_URL = BASE_URL + "/cr/1/solutions";

   private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
   private static final String HTTP_HEADER_X_CR_API_TOKEN = "x-cr-api-token";

   private static final int HTTP_STATUS_CREATED = 201;
   private static final int HTTP_STATUS_NO_CONTENT = 204;

   private static final String CONTENT_TYPE_JSON = "application-json";

   private static final String CUSTOMER_NAME = "example";
   private static final String CUSTOMER_EMAIL = "solution@example.com";
   private static final String CUSTOMER_INFO = "example solution";

   private static String thingId;
   private static Solution solution;
   private static AsyncHttpClient asyncHttpClient;

   /** */
   @BeforeClass
   public static void setUp() throws KeyManagementException, NoSuchAlgorithmException
   {
      thingId = "com.bosch.cr.example:myThing-" + UUID.randomUUID().toString();
      final SignatureFactory signatureFactory = SignatureFactory.newInstance();

      final AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
      builder.setSSLContext(setupAcceptingSelfSignedCertificates());

      asyncHttpClient = new AsyncHttpClient(builder.build());

      final SolutionsClient solutionsClient = SolutionsClient.newInstance(asyncHttpClient, SOLUTIONS_URL);
      solution = solutionsClient.createSolution(CUSTOMER_NAME, CUSTOMER_EMAIL, CUSTOMER_INFO,
         signatureFactory.getPublicKeyString());
      final String clientId = solution.getSolutionId() + ":test";

      asyncHttpClient.setSignatureCalculator(new CrAsymmetricalSignatureCalculator(signatureFactory, clientId));
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

      final ListenableFuture<Response> future = asyncHttpClient.preparePut(BASE_URL + path) //
         .addHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON) //
         .addHeader(HTTP_HEADER_X_CR_API_TOKEN, solution.getApiToken()) //
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

      final ListenableFuture<Response> future = asyncHttpClient.prepareDelete(BASE_URL + path) //
         .addHeader(HTTP_HEADER_X_CR_API_TOKEN, solution.getApiToken()) //
         .execute();

      final Response response = future.get();

      assertEquals(HTTP_STATUS_NO_CONTENT, response.getStatusCode());
   }

}
