/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.hook;

import fr.ans.afas.domain.SubscriptionMessage;
import fr.ans.afas.exception.WebHookConfigurationException;
import fr.ans.afas.service.SignatureService;
import fr.ans.afas.utils.AesEncrypter;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FhirHookPublisher {

    private final ExecutorService executorService;
    private final SignatureService signatureService;

    private final HttpClient client;

    private final int timeout;

    private final AesEncrypter aesEncrypter;

    /**
     * To format the date in the header
     */
    DateFormat dfHeader = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public FhirHookPublisher(int timeout, SignatureService signatureService, AesEncrypter aesEncrypter) {
        this.signatureService = signatureService;
        this.timeout = timeout;
        this.executorService = Executors.newFixedThreadPool(5);
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(timeout))
                .executor(executorService)
                .build();
        this.aesEncrypter = aesEncrypter;
    }

    /**
     * Method called to create & send the http webhook request
     *
     * @param message      the subscription message to handle
     * @param subscription the subscription resource to send
     * @return the futur webhook response of the created request
     */
    public CompletableFuture<WebHookResponse> publish(SubscriptionMessage message, Subscription subscription) {
        HttpRequest request = null;
        try {
            request = this.createHttpRequest(message, subscription);
        } catch (WebHookConfigurationException e) {
            return CompletableFuture.completedFuture(new WebHookResponse(false, 400, new Date(), e.getMessage()));
        }

        var resp = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        return resp.thenApply(r -> {
            // this is ok
            if (r.statusCode() >= 200 && r.statusCode() < 300) {
                return new WebHookResponse(true, r.statusCode(), new Date(), "");
            }

            var log = "Bad return status. Wanted 2XX. The server respond with: " + r.statusCode() + ". Server response body: " + r.body();
            return new WebHookResponse(false, r.statusCode(), new Date(), log);
        }).exceptionally(e -> {
            var log = "Exception during the http call. Error: " + e.getMessage();
            return new WebHookResponse(false, 500, new Date(), log);
        });
    }

    /**
     * Method that creates the HTTP request to send
     *
     * @param message      the subscription message to handle
     * @param subscription the subscription resource to send
     * @return the created request
     */
    private HttpRequest createHttpRequest(SubscriptionMessage message, Subscription subscription) throws WebHookConfigurationException {
        var endpoint = subscription.getChannel().getEndpoint();
        var payload = subscription.getChannel().getPayload();
        this.signatureService.sign(message);

        HashMap<String, String> headers = extractSubscriptionHeaders(subscription);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(this.timeout))
                .header("Content-Type", "application/fhir+json");

        if (Strings.isNotBlank(payload)) {
            request = request.header("X-Esante-Api-Hmac-SHA256", message.getSignature());
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request = request.header(entry.getKey(), entry.getValue());
        }

        // set the date header:
        request = request.header("X-Esante-Api-Update-Date", dfHeader.format(message.getCreationDate()));


        request.POST(Strings.isNotBlank(payload) ? HttpRequest.BodyPublishers.ofString(message.getPayload()) : HttpRequest.BodyPublishers.noBody());

        return request.build();
    }

    private HashMap<String, String> extractSubscriptionHeaders(Subscription subscription) throws WebHookConfigurationException {
        HashMap<String, String> headers = new LinkedHashMap<>();

        for (StringType stringType : subscription.getChannel().getHeader()) {
            var header = decryptHeader(stringType.getValue());
            if (!header.contains(":")) {
                throw new WebHookConfigurationException(WebHookConstants.HEADER_MALFORMED_ERROR);
            }

            var parts = header.split(":");

            if (parts.length != 2) {
                throw new WebHookConfigurationException(WebHookConstants.HEADER_MALFORMED_ERROR);
            }

            headers.put(parts[0], parts[1]);
        }
        return headers;
    }


    protected String decryptHeader(String header) {
        if (header.startsWith("0$")) {
            return aesEncrypter.decrypt(header.substring(2));

        } else {
            return header;
        }
    }

}
