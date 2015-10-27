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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to represent a solution.
 *
 * @since 1.0.0
 */
public final class Solution
{

   private final String solutionId;
   private final String apiToken;
   private final String plan;
   private final Customer customer;
   private final Key key;

   private Solution()
   {
      // only used for jackson deserialization.
      this(null, null, null, null);
   }

   /**
    * Constructs a new {@code Solution} for a {@code Customer} with a {@code Key}.
    *
    * @param name the customer's name.
    * @param email the customer's email.
    * @param info the customer's info.
    * @param publicKey the customer's public key.
    */
   public Solution(final String name, final String email, final String info, final String publicKey)
   {
      this.solutionId = "";
      this.apiToken = "";
      this.plan = "Free";
      this.customer = new Customer(name, email, info);
      this.key = new Key(publicKey);
   }

   /**
    * Creates a {@code Solution} from a JSON string representation.
    *
    * @param jsonString the JSON string.
    * @return the Solution.
    */
   public static Solution fromJson(final String jsonString)
   {
      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
      try
      {
         return objectMapper.readValue(jsonString, Solution.class);
      }
      catch (final Exception e)
      {
         throw new RuntimeException("Could not read Solution JSON.", e.getCause());
      }
   }

   /**
    * Creates a JSON string representation of this {@code Solution}.
    *
    * @return the JSON string.
    */
   public String toJson()
   {
      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
      try
      {
         return objectMapper.writeValueAsString(this);
      }
      catch (final JsonProcessingException e)
      {
         throw new RuntimeException("Could not write Solution JSON.", e.getCause());
      }
   }

   public String getSolutionId()
   {
      return solutionId;
   }

   public String getApiToken()
   {
      return apiToken;
   }

   /**
    * Utility class to represent a {@code Solution}'s customer.
    */
   static class Customer
   {
      private final String name;
      private final String email;
      private final String info;

      Customer(final String name, final String email, final String info)
      {
         this.name = name;
         this.email = email;
         this.info = info;
      }
   }

   /**
    * Utility class to represent a {@code Solution}'s public key.
    */
   static class Key
   {
      private final String type;
      private final String data;

      Key(final String data)
      {
         this.type = "EC";
         this.data = data;
      }
   }
}
