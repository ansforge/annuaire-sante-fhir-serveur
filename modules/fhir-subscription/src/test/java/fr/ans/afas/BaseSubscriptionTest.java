package fr.ans.afas;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public abstract class BaseSubscriptionTest {

    /**
     * The Fhir context
     */
    protected static final FhirContext ctx = FhirContext.forR4();
    /**
     * The Fhir client
     */
    protected static IGenericClient client;


    /**
     * Service to access fhir data
     */
    @Autowired
    FhirStoreService<?> fhirStoreService;
    /**
     * The secure key
     */
    @Value("${afas.fhir.write-mode-secure-key:}")
    String writeModeSecureKey;


    /**
     * Create the client with the good port and a Hapi interceptor to add the token in the headers.
     * Note that the token is only used for write operations
     */
    protected void setupClient() {
        client = ctx.newRestfulGenericClient("http://localhost:" + getServerPort() + "/fhir/v1");
        client.registerInterceptor(new LoggingInterceptor(false));
    }


    protected void insertSampleData() {
        var s1 = new Subscription();
        s1.setId("S1");
        s1.setCriteria("Device?_format=json");
        s1.setChannel(buildChannel(null, "http:localhost:1000/hook", List.of()));
        s1.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        var s2 = new Subscription();
        s2.setId("S2");
        s2.setCriteria("Device?identifier=123456");
        s2.setChannel(buildChannel("application/fhir+json", "http:localhost:2000/hook", List.of()));
        s2.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        var s3 = new Subscription();
        s3.setId("S3");
        s3.setCriteria("Device?name:contains=Some");
        s3.setChannel(buildChannel("application/fhir+json", "http:localhost:3000/hook", List.of()));
        s3.setStatus(Subscription.SubscriptionStatus.ERROR);

        this.fhirStoreService.store(List.of(s1, s2, s3), false);
    }


    private Subscription.SubscriptionChannelComponent buildChannel(String payload, String endpoint, List<StringType> headers) {
        var channel = new Subscription.SubscriptionChannelComponent();
        channel.setEndpoint(endpoint);
        channel.setPayload(payload);
        channel.setHeader(headers);
        channel.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        return channel;
    }


    /**
     * Get the port of the server
     *
     * @return the port of the server
     */
    protected abstract int getServerPort();
}
