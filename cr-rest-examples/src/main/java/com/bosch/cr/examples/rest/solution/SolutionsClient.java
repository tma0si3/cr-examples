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
package com.bosch.cr.examples.rest.solution;

import static java.util.Objects.requireNonNull;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

/**
 * A client to create {@link Solution}s at the Solution Store.
 *
 * @since 1.0.0
 */
public final class SolutionsClient
{

   private final AsyncHttpClient asyncHttpClient;
   private final String url;

   /**
    * Constructs a {@code SolutionsClient}.
    */
   private SolutionsClient(final AsyncHttpClient asyncHttpClient, final String url)
   {
      this.asyncHttpClient = asyncHttpClient;
      this.url = url;
   }

   /**
    * Returns a {@code SolutionsClient} instance.
    *
    *
    * @param asyncHttpClient
    * @param url the URL of the Solutions Service.
    * @return the SolutionsClient.
    */
   public static SolutionsClient newInstance(final AsyncHttpClient asyncHttpClient, final String url)
   {
      requireNonNull(asyncHttpClient, "The asyncHttpClient must not be null!");
      requireNonNull(url, "The URL must not be null!");
      return new SolutionsClient(asyncHttpClient, url);
   }

   /**
    * Creates a {@code Solution} for the specified customer at the Solutions Service.
    *
    * @param name the customer's name.
    * @param email the customer's email.
    * @param info the customer's info.
    * @param publicKey the customer's public key.
    * @return the Solution.
    */
   public Solution createSolution(final String name, final String email, final String info, final String publicKey)
   {
      final Solution solution = new Solution(name, email, info, publicKey);
      final String solutionJsonString = solution.toJson();

      final ListenableFuture<Response> future = asyncHttpClient.preparePost(url) //
         .addHeader("Content-Type", "application/json") //
         .setBody(solutionJsonString) //
         .execute();

      try
      {
         final String responseBody = future.get().getResponseBody();
         return Solution.fromJson(responseBody);
      }
      catch (final Exception e)
      {
         throw new RuntimeException("Could not create Solution at the Solution Service.", e.getCause());
      }
   }
}
