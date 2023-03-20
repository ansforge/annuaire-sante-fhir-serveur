/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;

import fr.ans.afas.domain.SubscriptionMessage;
import fr.ans.afas.domain.SubscriptionMessageStatus;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.repository.SubscriptionMessageRepository;
import fr.ans.afas.service.SubscriptionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Test the storage of subscriptions messages
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FullSpringAppWithMongo.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
@ActiveProfiles("full")
public class PersistenceTestIT {


    @Autowired
    SubscriptionManager subscriptionManager;

    @Autowired
    SubscriptionMessageRepository subscriptionMessageRepository;


    @Before
    public void init() {
        subscriptionMessageRepository.deleteAll();
    }

    @Test
    public void storeSubscriptionToSend() {

        var now = new Date();

        subscriptionManager.sendMessage(TestData.SUBSCRIPTION_ID_1, TestData.SAMPLE_DEVICE);

        Assert.assertEquals(1, subscriptionMessageRepository.count());
        var saved = subscriptionMessageRepository.findAll().iterator().next();
        Assert.assertEquals(TestData.SAMPLE_DEVICE_SERIALIZED, saved.getPayload());
        Assert.assertTrue("Bad creation date", Math.abs(now.getTime() - saved.getCreationDate().getTime()) < 1000);
        Assert.assertNull(saved.getLastLogs());
        Assert.assertEquals(saved.getCreationDate(), saved.getLastUpdated());
        Assert.assertEquals(0, saved.getNbrTry());
        Assert.assertEquals(SubscriptionMessageStatus.PENDING, saved.getStatus());
        Assert.assertEquals("device", saved.getType());
        Assert.assertEquals(TestData.SUBSCRIPTION_ID_1, saved.getSubscriptionId());
        Assert.assertEquals(saved.getCreationDate(), saved.getNextTryDate());
        Assert.assertEquals(36, saved.getUuid().length());

        // FIXME check the hmac or check that it is not empty and test it in another service
        // Assert.assertEquals("", saved.getSignature());

    }

    @Test
    public void findToProcessMessage() {

        // must be sent:
        subscriptionMessageRepository.save(TestData.createMessage(SubscriptionMessageStatus.PENDING, 0));
        subscriptionMessageRepository.save(TestData.createMessage(SubscriptionMessageStatus.PENDING_RETRY, 0));
        subscriptionMessageRepository.save(TestData.createMessage(SubscriptionMessageStatus.PENDING_RETRY, -10));

        // must not be sent:
        subscriptionMessageRepository.save(TestData.createMessage(SubscriptionMessageStatus.PENDING, 10));
        subscriptionMessageRepository.save(TestData.createMessage(SubscriptionMessageStatus.PENDING_RETRY, 10));
        subscriptionMessageRepository.save(TestData.createMessage(SubscriptionMessageStatus.IN_ERROR, 0));
        subscriptionMessageRepository.save(TestData.createMessage(SubscriptionMessageStatus.SUCCESS, 0));

        var found = subscriptionMessageRepository.findAllByStatusInAndNextTryDateBefore(List.of(SubscriptionMessageStatus.PENDING, SubscriptionMessageStatus.PENDING_RETRY), new Date());

        var elements = new ArrayList<SubscriptionMessage>();
        found.iterator().forEachRemaining(elements::add);
        Assert.assertEquals(3, elements.size());

    }


}
