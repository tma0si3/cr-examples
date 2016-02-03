/* Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved. */
package com.bosch.cr.examples.rest;

import java.time.OffsetDateTime;

import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilderBase;
import com.ning.http.client.SignatureCalculator;

/**
 * Apache Ning SignatureCalculator which calculates the "CRS" asymmetrical signature for authenticating technical clients
 * at the RESTful interface of CR.
 *
 * @since 1.0.0
 */
public class CrAsymmetricalSignatureCalculator implements SignatureCalculator
{
   private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
   private static final String CRS_AUTH_PREFIX = "CRS ";
   private static final String DELIMITER = ";";

   private static final String HTTP_HEADER_HOST = "Host";
   private static final String HTTP_HEADER_X_CR_DATE = "x-cr-date";
   private static final String HTTP_HEADER_X_CR_API_TOKEN = "x-cr-api-token";

   private final SignatureFactory signatureFactory;
   private final String clientId;
   private final String apiToken;

   public CrAsymmetricalSignatureCalculator(final SignatureFactory signatureFactory, final String clientId,
      final String apiToken)
   {
      this.signatureFactory = signatureFactory;
      this.clientId = clientId;
      this.apiToken = apiToken;
   }

   @Override
   public void calculateAndAddSignature(final Request request, final RequestBuilderBase<?> requestBuilderBase)
   {
      final String method = request.getMethod();
      final String path = request.getUri().toRelativeUrl();
      final String date = OffsetDateTime.now().toString();
      final String host = request.getUri().getHost();

      final String signatureData = String.join(DELIMITER, method, host, path, date);
      final String signature = signatureFactory.sign(signatureData);

      requestBuilderBase.addHeader(HTTP_HEADER_HOST, host);
      requestBuilderBase.addHeader(HTTP_HEADER_X_CR_DATE, date);
      requestBuilderBase.addHeader(HTTP_HEADER_X_CR_API_TOKEN, apiToken);
      requestBuilderBase.addHeader(HTTP_HEADER_AUTHORIZATION,
         CRS_AUTH_PREFIX + clientId + DELIMITER + SignatureFactory.SIGNATURE_ALGORITHM + DELIMITER + signature);
   }
}
