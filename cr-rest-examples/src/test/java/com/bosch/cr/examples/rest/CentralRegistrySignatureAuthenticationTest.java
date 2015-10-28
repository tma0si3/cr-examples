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

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.bosch.cr.examples.rest.solution.Solution;
import com.bosch.cr.examples.rest.solution.SolutionsClient;
import com.ning.http.client.AsyncHttpClient;
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

   private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
   private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
   private static final String HTTP_HEADER_HOST = "Host";
   private static final String HTTP_HEADER_X_CR_DATE = "x-cr-date";
   private static final String HTTP_HEADER_X_CR_SOLUTION_API_TOKEN = "x-craas-solution-api-token";
   private static final String HTTP_METHOD_PUT = "PUT";
   private static final String HTTP_METHOD_DELETE = "DELETE";
   private static final int HTTP_STATUS_CREATED = 201;
   private static final int HTTP_STATUS_NO_CONTENT = 204;

   private static final String CONTENT_TYPE_JSON = "application-json";

   private static final String CUSTOMER_NAME = "example";
   private static final String CUSTOMER_EMAIL = "solution@example.com";
   private static final String CUSTOMER_INFO = "example solution";

   private static String thingId;
   private static SignatureFactory signatureFactory;
   private static Solution solution;
   private static String clientId;
   private static AsyncHttpClient asyncHttpClient;

   /** */
   @BeforeClass
   public static void setUp()
   {
      thingId = "com.bosch.cr.example:myThing-" + UUID.randomUUID().toString();
      signatureFactory = SignatureFactory.newInstance();
      final SolutionsClient solutionsClient = SolutionsClient.newInstance(SOLUTIONS_URL);
      solution = solutionsClient.createSolution(CUSTOMER_NAME, CUSTOMER_EMAIL, CUSTOMER_INFO, signatureFactory.getPublicKey());
      clientId = solution.getSolutionId() + ":test";
      asyncHttpClient = new AsyncHttpClient();
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
      final String date = OffsetDateTime.now().toString();
      final String path = "/cr/1/things/" + thingId;
      final String signatureData = String.join(";", HTTP_METHOD_PUT, HOST, path, thingJsonString, date);
      final String signature = signatureFactory.sign(signatureData);

      final ListenableFuture<Response> future = asyncHttpClient.preparePut(BASE_URL + path) //
         .addHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON) //
         .addHeader(HTTP_HEADER_AUTHORIZATION, crsFor(signature)) //
         .addHeader(HTTP_HEADER_HOST, HOST) //
         .addHeader(HTTP_HEADER_X_CR_DATE, date) //
         .addHeader(HTTP_HEADER_X_CR_SOLUTION_API_TOKEN, solution.getApiToken()) //
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
      final String date = OffsetDateTime.now().toString();
      final String path = "/cr/1/things/" + thingId;
      final String signatureData = String.join(";", HTTP_METHOD_DELETE, HOST, path, date);
      final String signature = signatureFactory.sign(signatureData);

      final ListenableFuture<Response> future = asyncHttpClient.prepareDelete(BASE_URL + path) //
         .addHeader(HTTP_HEADER_AUTHORIZATION, crsFor(signature)) //
         .addHeader(HTTP_HEADER_HOST, HOST) //
         .addHeader(HTTP_HEADER_X_CR_DATE, date) //
         .addHeader(HTTP_HEADER_X_CR_SOLUTION_API_TOKEN, solution.getApiToken()) //
         .execute();

      final Response response = future.get();

      assertEquals(HTTP_STATUS_NO_CONTENT, response.getStatusCode());
   }

   private static String crsFor(final String signature)
   {
      return "CRS " + clientId + ";" + SignatureFactory.SIGNATURE_ALGORITHM + ";" + signature;
   }

}
