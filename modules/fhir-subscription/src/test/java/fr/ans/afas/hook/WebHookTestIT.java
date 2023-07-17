/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.hook;


import fr.ans.afas.FullSpringAppWithMongo;
import fr.ans.afas.domain.SubscriptionMessageStatus;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.repository.SubscriptionMessageRepository;
import fr.ans.afas.task.SubscriptionCron;
import org.assertj.core.util.Lists;
import org.awaitility.Awaitility;
import org.hl7.fhir.r4.model.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

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
public class WebHookTestIT {

    /**
     * The Fhir context
     */
    private static final String DEVICE_ID_1 = "device1";
    private static final String DEVICE_ID_2 = "device2";

    private static final String SUBSCRIPTION_ID_1 = "sub1";

    /**
     * The port of the server used in tests
     */
    @LocalServerPort
    int port;
    /**
     * Service to access fhir data
     */
    @Inject
    FhirStoreService<?> fhirStoreService;

    /**
     * The Select Expression Factory
     */
    @Inject
    SubscriptionMessageRepository subscriptionMessageRepository;
    /**
     * The secure key
     */
    @Value("${afas.fhir.write-mode-secure-key:}")
    String writeModeSecureKey;
    @Inject
    private SubscriptionCron<?> subscriptionCron;
    @Inject
    private FhirHookManager<?> fhirHookManager;
    @Inject
    private TestHookController testHookController;

    @AfterClass
    public static void shutdown() {
        WithMongoTest.clean();
    }

    @After
    public void clean() {
        fhirStoreService.deleteAll();
        subscriptionMessageRepository.deleteAll();
        this.testHookController.resetData();
    }

    /**
     * Setup test context
     */
    @Before
    public void init() {
        insertSampleData();
    }

    @Test
    public void testSingleHookCallWithPayload() {
        // create subscription
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1, "http://localhost:" + this.port + "/hooks", "application/fhir+json", false);
        this.fhirStoreService.store(List.of(subscription), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 2");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        this.fhirHookManager.process();

        assertEquals(1, this.testHookController.getNbCalled());
        assertTrue(this.testHookController.hasBody());

        this.fhirHookManager.process();

        assertEquals(1, this.testHookController.getNbCalled());
        assertEquals(1, this.subscriptionMessageRepository.count());

        var message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.SUCCESS, message.getStatus());
        assertEquals(0, message.getNbrTry());
    }

    @Test
    public void testSingleHookCallWithoutPayload() {
        // create subscription
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1, "http://localhost:" + this.port + "/hooks", null, false);
        this.fhirStoreService.store(List.of(subscription), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 3");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        this.fhirHookManager.process();

        assertEquals(1, this.testHookController.getNbCalled());
        assertFalse(this.testHookController.hasBody());

        this.fhirHookManager.process();

        assertEquals(1, this.testHookController.getNbCalled());
        assertEquals(1, this.subscriptionMessageRepository.count());

        var message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.SUCCESS, message.getStatus());
        assertEquals(0, message.getNbrTry());
    }

    @Test
    public void testSingleHookErrorCall() {

        // create subscription
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1, "http://localhost:" + this.port + "/hooks/error", "application/fhir+json", false);
        this.fhirStoreService.store(List.of(subscription), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 2");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        this.fhirHookManager.process();


        Awaitility.await().atMost(5000, TimeUnit.MILLISECONDS).until(() -> this.testHookController.getNbErrorCalled() >= 1);

        assertEquals(0, this.testHookController.getNbCalled());
        assertTrue(1 <= this.testHookController.getNbErrorCalled());

        this.fhirHookManager.process();

        var message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.PENDING_RETRY, message.getStatus());
        assertEquals(1, message.getNbrTry());
        assertNotEquals("", message.getLastLogs());

        this.fhirHookManager.process();

        assertEquals(0, this.testHookController.getNbCalled());

        message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.PENDING_RETRY, message.getStatus());
        assertEquals(1, message.getNbrTry());
    }

    @Test
    public void testSingleHookHeaderErrorCall() {
        // create subscription
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1, "http://localhost:" + this.port + "/hooks/error", "application/fhir+json", true);
        this.fhirStoreService.store(List.of(subscription), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 2");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        this.fhirHookManager.process();

        assertEquals(0, this.testHookController.getNbCalled());

        this.fhirHookManager.process();

        var message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.IN_ERROR, message.getStatus());
        assertEquals(1, message.getNbrTry());
        assertNotEquals("", message.getLastLogs());

        var errorSubscription = (Subscription) fhirStoreService.findById("Subscription", new IdType(message.getSubscriptionId()));
        assertEquals(Subscription.SubscriptionStatus.ERROR, errorSubscription.getStatus());
        assertNotNull(errorSubscription.getError());
    }

    @Test
    public void testHookErrorCallRetry() {
        // create subscription
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1, "http://localhost:" + this.port + "/hooks/error", "application/fhir+json", false);
        this.fhirStoreService.store(List.of(subscription), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 2");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        this.fhirHookManager.process();

        assertEquals(0, this.testHookController.getNbCalled());
        assertEquals(1, this.testHookController.getNbErrorCalled());

        var message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.PENDING_RETRY, message.getStatus());
        assertEquals(1, message.getNbrTry());
        assertNotEquals("", message.getLastLogs());

        await().atMost(3, TimeUnit.SECONDS).until(() -> {
            this.fhirHookManager.process();
            System.out.println("NB ERROR CALLED : " + this.testHookController.getNbErrorCalled());
            return this.testHookController.getNbErrorCalled() == 2;
        });

        message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.PENDING_RETRY, message.getStatus());
        assertEquals(2, message.getNbrTry());
        assertNotEquals("", message.getLastLogs());

        await().atMost(6, TimeUnit.SECONDS).until(() -> {
            this.fhirHookManager.process();
            return this.testHookController.getNbErrorCalled() == 3;
        });

        message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.PENDING_RETRY, message.getStatus());
        assertEquals(3, message.getNbrTry());
        assertNotEquals("", message.getLastLogs());

        await().atMost(8, TimeUnit.SECONDS).until(() -> {
            this.fhirHookManager.process();
            return this.testHookController.getNbErrorCalled() == 4;
        });

        // test that the subscription message has been put in error
        message = this.subscriptionMessageRepository.findAll().iterator().next();
        assertEquals(SubscriptionMessageStatus.IN_ERROR, message.getStatus());
        assertEquals(4, message.getNbrTry());
        assertNotEquals("", message.getLastLogs());

        // test that the subscription has been put in error
        var errorSubscription = (Subscription) fhirStoreService.findById("Subscription", new IdType(message.getSubscriptionId()));
        assertEquals(Subscription.SubscriptionStatus.ERROR, errorSubscription.getStatus());
        assertNotNull(errorSubscription.getError());
    }

    private void updateDevice(String id, String deviceName) {
        var rassDevice1 = new Device();
        rassDevice1.setId(id);
        rassDevice1.addIdentifier().setSystem("http://samplesysyem").setValue("1");
        rassDevice1.addDeviceName().setName(deviceName);

        this.fhirStoreService.store(List.of(rassDevice1), true);
    }


    private void insertSampleData() {
        var lastUpdated = new Date(new Date().getTime() - 2000);

        var extArhgos = "https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Device-NumberAuthorizationARHGOS";
        var rassDevice1 = new Device();
        rassDevice1.setId(DEVICE_ID_1);
        rassDevice1.addIdentifier().setSystem("http://samplesysyem").setValue("1");
        rassDevice1.addDeviceName().setName("Some dName");
        rassDevice1.getMeta().setLastUpdated(lastUpdated);
        var location1 = new Reference();
        location1.setReference("Location/loc1");
        rassDevice1.setLocation(location1);
        rassDevice1.setManufacturer("man1");
        rassDevice1.setModelNumber("model1");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        rassDevice1.setOwner(owner1);
        var type = new CodeableConcept();
        type.addCoding().setSystem("http://types/").setCode("type1");
        type.addCoding().setSystem("http://part1/").setCode("other1");
        rassDevice1.setType(type);
        rassDevice1.setStatus(Device.FHIRDeviceStatus.ACTIVE);
        rassDevice1.addExtension().setUrl(extArhgos).setValue(new StringType("56565.6456.45789531230001"));

//        var s1 = new Subscription();
//        s1.setId("S1");
//        s1.setCriteria("Device?_format=json");
//        s1.setChannel(buildChannel(null, "http:localhost:1000/hook", List.of()));
//        s1.setStatus(Subscription.SubscriptionStatus.ACTIVE);
//        var s2 = new Subscription();
//        s2.setId("S2");
//        s2.setCriteria("Device?identifier=123456");
//        s2.setChannel(buildChannel("application/fhir+json", "http:localhost:2000/hook", List.of()));
//        s2.setStatus(Subscription.SubscriptionStatus.ACTIVE);
//        var s3 = new Subscription();
//        s3.setId("S3");
//        s3.setCriteria("Device?name:contains=Some");
//        s3.setChannel(buildChannel("application/fhir+json", "http:localhost:3000/hook", List.of()));
//        s3.setStatus(Subscription.SubscriptionStatus.ERROR);

        this.fhirStoreService.store(List.of(rassDevice1), false);
//
        var rassDevice2 = new Device();
        rassDevice2.setId(DEVICE_ID_2);
        rassDevice2.addIdentifier().setSystem("http://samplesysyem").setValue("1");
        rassDevice2.addDeviceName().setName("Name");
        rassDevice2.getMeta().setLastUpdated(lastUpdated);

        this.fhirStoreService.store(List.of(rassDevice2), false);
    }

    private Subscription buildSubscription(String subscriptionId, String deviceId, String endpoint, String payload, boolean wrongHeader) {
        var subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setCriteria("Device?_id=" + deviceId);

        subscription.setChannel(wrongHeader ? buildChannelWrongHeader(payload, endpoint) : this.buildChannel(payload, endpoint));

        return subscription;
    }

    private Subscription.SubscriptionChannelComponent buildChannel(String payload, String endpoint) {
        var channel = new Subscription.SubscriptionChannelComponent();
        channel.setEndpoint(endpoint);
        channel.setPayload(payload);
        channel.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        channel.setHeader(Lists.newArrayList(new StringType("my-header:test-value")));
        return channel;
    }

    private Subscription.SubscriptionChannelComponent buildChannelWrongHeader(String payload, String endpoint) {
        var channel = new Subscription.SubscriptionChannelComponent();
        channel.setEndpoint(endpoint);
        channel.setPayload(payload);
        channel.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        channel.setHeader(Lists.newArrayList(new StringType("a-wrong-header")));
        return channel;
    }

}
