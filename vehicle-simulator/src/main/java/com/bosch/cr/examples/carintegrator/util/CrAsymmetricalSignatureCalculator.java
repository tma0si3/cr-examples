/*
 * Copyright (c) 2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
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

package com.bosch.cr.examples.carintegrator.util;

import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilderBase;
import com.ning.http.client.SignatureCalculator;

import java.time.OffsetDateTime;

/**
 * Apache Ning SignatureCalculator which calculates the "CRS" asymmetrical signature for authenticating technical clients
 * at the RESTful interface of CR.
 *
 * @since 1.0.0
 */
public class CrAsymmetricalSignatureCalculator implements SignatureCalculator {
    private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    private static final String CRS_AUTH_PREFIX = "CRS ";
    private static final String DELIMITER = ";";

    private static final String HTTP_HEADER_HOST = "Host";
    private static final String HTTP_HEADER_X_CR_DATE = "x-cr-date";
    private static final String HTTP_HEADER_X_CR_API_TOKEN = "x-cr-api-token";

    private static final String HTTP_METHOD_PUT = "PUT";
    private static final String HTTP_METHOD_POST = "POST";

    private final SignatureFactory signatureFactory;
    private final String clientId;
    private final String apiToken;

    public CrAsymmetricalSignatureCalculator(final SignatureFactory signatureFactory, final String clientId,
                                             final String apiToken) {
        this.signatureFactory = signatureFactory;
        this.clientId = clientId;
        this.apiToken = apiToken;
    }

    @Override
    public void calculateAndAddSignature(final Request request, final RequestBuilderBase<?> requestBuilderBase) {
        final String method = request.getMethod();
        final String path = request.getUri().toRelativeUrl();
        final String date = OffsetDateTime.now().toString();
        final String host = request.getUri().getHost();
        final String signatureData;
        if (method.equals(HTTP_METHOD_POST) || method.equals(HTTP_METHOD_PUT)) {
            final String body = request.getStringData();
            signatureData = String.join(DELIMITER, method, host, path, body, date);
        } else {
            signatureData = String.join(DELIMITER, method, host, path, date);
        }
        final String signature = signatureFactory.sign(signatureData);
        requestBuilderBase.addHeader(HTTP_HEADER_HOST, host);
        requestBuilderBase.addHeader(HTTP_HEADER_X_CR_DATE, date);
        requestBuilderBase.addHeader(HTTP_HEADER_X_CR_API_TOKEN, apiToken);
        requestBuilderBase.addHeader(HTTP_HEADER_AUTHORIZATION,
                CRS_AUTH_PREFIX + clientId + DELIMITER + SignatureFactory.SIGNATURE_ALGORITHM + DELIMITER + signature);
    }
}