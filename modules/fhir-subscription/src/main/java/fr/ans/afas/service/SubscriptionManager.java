/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import org.hl7.fhir.r4.model.DomainResource;

/**
 * Manage subscription system (webhook...)
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface SubscriptionManager {
    /**
     * Send a notification. Notifications are not send directly. They are first stored in mongodb as {@link fr.ans.afas.domain.SubscriptionMessage} and then sent in a batch mode.
     *
     * @param subscriptionId the id of the fhir subscription that triggered the message
     * @param payload        the payload to send
     */
    void sendMessage(String subscriptionId, DomainResource payload);


}
