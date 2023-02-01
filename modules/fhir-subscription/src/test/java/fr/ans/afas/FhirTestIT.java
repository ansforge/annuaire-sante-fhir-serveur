/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.codesystems.SubscriptionStatus;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.util.List;

/**
 * Test the fhir server for the resource: {@link org.hl7.fhir.r4.model.Subscription}
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FullSpringAppWithMongo.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
@ActiveProfiles("full")
public class FhirTestIT {

    /**
     * The Fhir context
     */
    protected static final FhirContext ctx = FhirContext.forR4();
    /**
     * The Fhir client
     */
    protected static IGenericClient client;

    /**
     * The port of the server used in tests
     */
    @LocalServerPort
    int port;
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

    @AfterClass
    public static void shutdown() {
        WithMongoTest.clean();
    }

    @After
    public void clean() {
        fhirStoreService.deleteAll();
    }

    /**
     * Setup test context
     */
    @Before
    public void init() throws ParseException {
        setupClient();
        insertSampleData();
    }

    /**
     * Create the client with the good port and a Hapi interceptor to add the token in the headers.
     * Note that the token is only used for write operations
     */
    protected void setupClient() {
        client = ctx.newRestfulGenericClient("http://localhost:" + getServerPort() + "/fhir/v1");
        client.registerInterceptor(new LoggingInterceptor(false));
    }


    @Test
    public void testReadById() {
        var device = client.read().resource(Subscription.class).withId("S1").encodedJson().prettyPrint().execute();
        Assert.assertEquals("S1", device.getIdElement().getIdPart());
    }


    @Test
    public void testSearchByStatus() {
        var result = (Bundle) client.search().forResource(Subscription.class).encodedJson().prettyPrint().where(Subscription.STATUS.exactly().code(SubscriptionStatus.ACTIVE.toCode())).count(10).execute();
        Assert.assertEquals(2, result.getTotal());
    }

    @Test
    public void testSearchByCriteria() {
        var result = (Bundle) client.search().forResource(Subscription.class).encodedJson().prettyPrint().where(Subscription.CRITERIA.matches().values("Device?identifier=123456")).count(10).execute();
        Assert.assertEquals(1, result.getTotal());
    }


    @Test
    public void testSearchByPayload() {
        var result = (Bundle) client.search().forResource(Subscription.class).encodedJson().prettyPrint().where(Subscription.PAYLOAD.exactly().code("application/fhir+json")).count(10).execute();
        Assert.assertEquals(2, result.getTotal());
    }

    @Test
    public void testSearchByUrl() {
        var result = (Bundle) client.search().forResource(Subscription.class).encodedJson().prettyPrint().where(Subscription.URL.matches().value("http:localhost:2000/hook")).count(10).execute();
        Assert.assertEquals(1, result.getTotal());
    }

    @Test
    public void testSearchByType() {
        var result = (Bundle) client.search().forResource(Subscription.class).encodedJson().prettyPrint().where(Subscription.TYPE.exactly().code(Subscription.SubscriptionChannelType.RESTHOOK.toCode())).count(10).execute();
        Assert.assertEquals(3, result.getTotal());
    }


    /**
     * Get the port of the server
     *
     * @return the port of the server
     */
    protected int getServerPort() {
        return this.port;
    }


    private void insertSampleData() {
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

}