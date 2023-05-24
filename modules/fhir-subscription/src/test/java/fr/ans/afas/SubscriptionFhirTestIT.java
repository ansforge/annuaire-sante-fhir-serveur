/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import fr.ans.afas.fhir.SubscriptionProvider;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.repository.SubscriptionMessageRepository;
import fr.ans.afas.service.SubscriptionManager;
import fr.ans.afas.service.impl.DefaultSubscriptionOperationService;
import fr.ans.afas.task.SubscriptionCron;
import org.hl7.fhir.r4.model.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

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
public class SubscriptionFhirTestIT<T> {

    /**
     * The Fhir context
     */
    protected static final FhirContext ctx = FhirContext.forR4();
    private static final String DEVICE_ID_1 = "device1";
    private static final String DEVICE_ID_2 = "device2";

    private static final String SUBSCRIPTION_ID_1 = "sub1";
    private static final String SUBSCRIPTION_ID_2 = "sub2";
    private static final String SUBSCRIPTION_ID_3 = "sub3";
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
    @Inject
    FhirStoreService<T> fhirStoreService;
    /**
     * The Select Expression Factory
     */
    @Inject
    ExpressionFactory<T> expressionFactory;
    /**
     * The Select Expression Factory
     */
    @Inject
    SubscriptionMessageRepository subscriptionMessageRepository;
    /**
     * The subscription manager
     */
    @Inject
    DefaultSubscriptionOperationService defaultSubscriptionOperationService;

    /**
     * The secure key
     */
    @Value("${afas.fhir.write-mode-secure-key:}")
    String writeModeSecureKey;
    private SubscriptionCron<T> subscriptionCron;
    @Autowired
    @Qualifier("subscriptionManagerMocked")
    private SubscriptionManager subscriptionManager;

    @AfterClass
    public static void shutdown() {
        WithMongoTest.clean();
    }

    /**
     * Scheduled CRON service to handle FHIR Subscription
     */
    @Inject
    @Bean
    SubscriptionCron<T> subscriptionCron(ExpressionFactory<T> expressionFactory, FhirStoreService<T> fhirStoreService, SearchConfig searchConfig) {
        this.subscriptionCron = new SubscriptionCron<>(expressionFactory, fhirStoreService, searchConfig, this.subscriptionManager, 1);
        return this.subscriptionCron;
    }


    @Inject
    @Bean
    SubscriptionProvider<T> subscriptionProvider(ExpressionFactory<T> expressionFactory, FhirStoreService<T> fhirStoreService, FhirContext fhirContext, NextUrlManager<T> nextUrlManager,
                                                 @Value("afas.fhir.next-url-encryption-key") String secretKey) {
        return new SubscriptionProvider<>(fhirStoreService, fhirContext, expressionFactory, nextUrlManager, secretKey);
    }

    @After
    public void clean() {
        fhirStoreService.deleteAll();
        subscriptionMessageRepository.deleteAll();
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

    @Test
    public void testSingleSubscription() {
        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.never()).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // create subscription
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1);
        this.fhirStoreService.store(List.of(subscription), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 2");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(1)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 3");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(2)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // update not wired device
        this.updateDevice(DEVICE_ID_2, "Some other name");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(2)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testMultipleSubscriptionsOnDifferentDevices() {

        Mockito.reset(this.subscriptionManager);
        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.never()).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // update devices
        this.updateDevice(DEVICE_ID_1, "Changed name");
        this.updateDevice(DEVICE_ID_2, "Changed name");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.never()).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // create subscription
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1);
        var subscription2 = this.buildSubscription(SUBSCRIPTION_ID_2, DEVICE_ID_2);
        this.fhirStoreService.store(List.of(subscription, subscription2), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 2");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(1)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 3");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(2)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // update device 2
        this.updateDevice(DEVICE_ID_2, "Some other name");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(3)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testMultipleSubscriptionOnSingleDevice() {

        Mockito.reset(this.subscriptionManager);

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.never()).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // create subscriptions
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1);
        var subscription2 = this.buildSubscription(SUBSCRIPTION_ID_2, DEVICE_ID_1);
        this.fhirStoreService.store(List.of(subscription, subscription2), true);

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 2");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(2)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // update device 1
        this.updateDevice(DEVICE_ID_1, "Changed name 3");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(4)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());

        // update device 2
        this.updateDevice(DEVICE_ID_2, "Some other name");

        // call scheduled method
        this.subscriptionCron.findUpdated();

        // search subscription messages
        Mockito.verify(this.subscriptionManager, Mockito.times(4)).sendMessage(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void testDeactivateAll() {
        // create subscriptions
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1);
        var subscription2 = this.buildSubscription(SUBSCRIPTION_ID_2, DEVICE_ID_1);
        this.fhirStoreService.store(List.of(subscription, subscription2), true);

        var pageResult = this.fhirStoreService.search(null, new SelectExpression<>("Subscription", expressionFactory));

        Assert.assertEquals(2, pageResult.getPage().size());

        for (var resource : pageResult.getPage()) {
            var sub = (Subscription) resource;
            Assert.assertEquals(Subscription.SubscriptionStatus.ACTIVE, sub.getStatus());
        }

        this.defaultSubscriptionOperationService.deactivateAllSubscription();

        pageResult = this.fhirStoreService.search(null, new SelectExpression<>("Subscription", expressionFactory));

        Assert.assertEquals(2, pageResult.getPage().size());

        for (var resource : pageResult.getPage()) {
            var sub = (Subscription) resource;
            Assert.assertEquals(Subscription.SubscriptionStatus.OFF, sub.getStatus());
        }
    }

    @Test
    public void testActivateAll() {
        // create subscriptions
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1);
        var subscription2 = this.buildSubscription(SUBSCRIPTION_ID_2, DEVICE_ID_1);
        this.fhirStoreService.store(List.of(subscription, subscription2), true);

        this.defaultSubscriptionOperationService.deactivateAllSubscription();

        var pageResult = this.fhirStoreService.search(null, new SelectExpression<>("Subscription", expressionFactory));

        Assert.assertEquals(2, pageResult.getPage().size());

        for (var resource : pageResult.getPage()) {
            var sub = (Subscription) resource;
            Assert.assertEquals(Subscription.SubscriptionStatus.OFF, sub.getStatus());
        }

        this.defaultSubscriptionOperationService.activateAllSubscription();

        pageResult = this.fhirStoreService.search(null, new SelectExpression<>("Subscription", expressionFactory));

        Assert.assertEquals(2, pageResult.getPage().size());

        for (var resource : pageResult.getPage()) {
            var sub = (Subscription) resource;
            Assert.assertEquals(Subscription.SubscriptionStatus.ACTIVE, sub.getStatus());
        }
    }

    @Test
    public void testActivateAllWithError() {
        // create subscriptions
        var subscription = this.buildSubscription(SUBSCRIPTION_ID_1, DEVICE_ID_1);
        var subscription2 = this.buildSubscription(SUBSCRIPTION_ID_2, DEVICE_ID_1);
        this.buildErrorSubscription(SUBSCRIPTION_ID_3, DEVICE_ID_1);

        this.fhirStoreService.store(List.of(subscription, subscription2), true);

        this.defaultSubscriptionOperationService.deactivateAllSubscription();

        var pageResult = this.fhirStoreService.search(null, new SelectExpression<>("Subscription", expressionFactory));

        Assert.assertEquals(2, pageResult.getPage().size());

        this.defaultSubscriptionOperationService.activateAllSubscription();

        pageResult = this.fhirStoreService.search(null, new SelectExpression<>("Subscription", expressionFactory));

        Assert.assertEquals(2, pageResult.getPage().size());

        for (var resource : pageResult.getPage()) {
            var sub = (Subscription) resource;

            Assert.assertEquals(Subscription.SubscriptionStatus.ACTIVE, sub.getStatus());
        }
    }

    private void updateDevice(String id, String deviceName) {
        var rassDevice1 = new Device();
        rassDevice1.setId(id);
        rassDevice1.addIdentifier().setSystem("http://samplesysyem").setValue("1");
        rassDevice1.addDeviceName().setName(deviceName);

        this.fhirStoreService.store(List.of(rassDevice1), true);
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


        this.fhirStoreService.store(List.of(rassDevice1), false);

        var rassDevice2 = new Device();
        rassDevice2.setId(DEVICE_ID_2);
        rassDevice2.addIdentifier().setSystem("http://samplesysyem").setValue("1");
        rassDevice2.addDeviceName().setName("Name");
        rassDevice2.getMeta().setLastUpdated(lastUpdated);

        this.fhirStoreService.store(List.of(rassDevice2), false);
    }

    private Subscription buildSubscription(String subscriptionId, String deviceId) {
        var subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setCriteria("Device?_id=" + deviceId);
        var sc = this.buildChannel("application/fhir+json", "http://localhost:8090/hooks");
        subscription.setChannel(sc);

        return subscription;
    }

    private Subscription buildErrorSubscription(String subscriptionId, String deviceId) {
        var subscription = new Subscription();
        subscription.setId(subscriptionId);
        subscription.setStatus(Subscription.SubscriptionStatus.ERROR);
        subscription.setCriteria("Device?_id=" + deviceId);
        var sc = this.buildChannel("application/fhir+json", "http://localhost:8090/hooks");
        subscription.setChannel(sc);
        return subscription;
    }

    private Subscription.SubscriptionChannelComponent buildChannel(String payload, String endpoint) {
        var channel = new Subscription.SubscriptionChannelComponent();
        channel.setEndpoint(endpoint);
        channel.setPayload(payload);
        channel.setType(Subscription.SubscriptionChannelType.RESTHOOK);
        return channel;
    }

}
