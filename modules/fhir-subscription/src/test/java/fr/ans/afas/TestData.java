/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas;

import fr.ans.afas.domain.SubscriptionMessage;
import fr.ans.afas.domain.SubscriptionMessageStatus;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Identifier;

import java.util.Date;
import java.util.UUID;

/**
 * Some data used to test subscriptions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class TestData {

    public static String SUBSCRIPTION_ID_1 = "006-1234";

    public static Device SAMPLE_DEVICE = new Device().addIdentifier(new Identifier().setSystem("http://a").setValue("324"));
    public static String SAMPLE_DEVICE_SERIALIZED = "{\"resourceType\":\"Device\",\"identifier\":[{\"system\":\"http://a\",\"value\":\"324\"}]}";


    public static SubscriptionMessage createMessage(SubscriptionMessageStatus status, int retryDateDeltaInSecondFromNow) {

        var now = new Date();

        var subscriptionMessage = new SubscriptionMessage();

        subscriptionMessage.setSubscriptionId("001-123456");
        subscriptionMessage.setSignature("Some");
        subscriptionMessage.setUuid(UUID.randomUUID().toString());
        subscriptionMessage.setType("device");
        subscriptionMessage.setStatus(status);
        subscriptionMessage.setCreationDate(now);
        subscriptionMessage.setLastUpdated(now);
        subscriptionMessage.setNextTryDate(new Date(now.getTime() + (retryDateDeltaInSecondFromNow * 1000L)));
        subscriptionMessage.setLastLogs("Some Logs");

        return subscriptionMessage;
    }

}
