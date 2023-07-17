/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;


import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
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
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FullSpringAppWithMongo.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
@ActiveProfiles("full")
public class EnableDisableSubscriptionTest extends BaseSubscriptionTest {


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

    /**
     * Create the client with the good port and a Hapi interceptor to add the token in the headers.
     * Note that the token is only used for write operations
     */
    protected void setupClient() {
        client = ctx.newRestfulGenericClient("http://localhost:" + getServerPort() + "/fhir/v1");
        client.registerInterceptor(new LoggingInterceptor(false));
    }


    /**
     * Get the port of the server
     *
     * @return the port of the server
     */
    protected int getServerPort() {
        return this.port;
    }


    @Test
    public void disableTest() {
        // given 3 subscriptions (2 in active, 1 in success):
        // when we disable subscriptions:
        var r = client.operation().onServer()
                .named("$admin-patch-server-configuration")
                .withParameters(getActivationParam(false))
                .execute();
        // then all subscriptions are OFF:
        var result = (Bundle) client.search().forResource(Subscription.class).count(10).execute();
        Assert.assertEquals(3, result.getTotal());
        for (var s : result.getEntry()) {
            var sub = (Subscription) s.getResource();
            Assert.assertEquals(Subscription.SubscriptionStatus.OFF, sub.getStatus());
        }
    }


    @Test
    public void reactivateTest() {
        // given 3 subscriptions deactivated:
        client.operation().onServer()
                .named("$admin-patch-server-configuration")
                .withParameters(getActivationParam(false))
                .execute();
        // when we enable again subscriptions:
        var r = client.operation().onServer()
                .named("$admin-patch-server-configuration")
                .withParameters(getActivationParam(true))
                .execute();
        // then subscriptions come back in the initial state:
        var result = (Bundle) client.search().forResource(Subscription.class).encodedJson().prettyPrint().where(Subscription.STATUS.exactly().code(SubscriptionStatus.ACTIVE.toCode())).count(10).execute();
        Assert.assertEquals(2, result.getTotal());

        result = (Bundle) client.search().forResource(Subscription.class).encodedJson().prettyPrint().where(Subscription.STATUS.exactly().code(SubscriptionStatus.OFF.toCode())).count(10).execute();
        Assert.assertEquals(1, result.getTotal());
    }

    protected Parameters getActivationParam(boolean b) {
        var inParams = new Parameters();
        inParams.addParameter().setName("subscriptionsActivated").setValue(new BooleanType(b));
        return inParams;
    }

}
