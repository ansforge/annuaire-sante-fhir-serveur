/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;


import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.codesystems.SubscriptionStatus;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

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
public class FhirTestIT extends BaseSubscriptionTest {


    /**
     * The port of the server used in tests
     */
    @LocalServerPort
    int port;


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
    public void init() {
        setupClient();
        insertSampleData();
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


}
