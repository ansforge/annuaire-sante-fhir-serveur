/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;


import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test the fhir server for the resource: {@link Subscription}
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FullSpringAppWithMongo.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
@ActiveProfiles("full")
public class FhirTestWriteIT extends BaseSubscriptionTest {


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
    public void testCreateSubscription() {
        var subscriptions = this.createSamples();
        var methodOutcome = client.create().resource(subscriptions.get(0)).execute();
        Assert.assertTrue(methodOutcome.getCreated());
        var found = (Subscription) client.read().resource("Subscription").withId(methodOutcome.getId().getIdPart()).execute();
        Assert.assertEquals("http:localhost:1000/hook", found.getChannel().getEndpoint());
    }

    /**
     * Test the update of device
     */
    @Test
    public void testUpdateSubscription() {
        var subscriptions = this.createSamples();
        subscriptions.get(0).getChannel().setEndpoint("updated");
        client.update().resource(subscriptions.get(0)).execute();
        var found = (Subscription) client.read().resource("Subscription").withId(subscriptions.get(0).getId()).execute();
        Assert.assertEquals("updated", found.getChannel().getEndpoint());
    }

    /**
     * Test the update of device
     */
    @Test
    public void testDeleteSubscription() {
        var subscriptions = this.createSamples();
        client.delete().resourceById("Subscription", subscriptions.get(0).getIdElement().getIdPart()).execute();
        var notFoundReq = client.read().resource("Subscription").withId(subscriptions.get(0).getId());
        Assert.assertThrows(ResourceNotFoundException.class, notFoundReq::execute);
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
