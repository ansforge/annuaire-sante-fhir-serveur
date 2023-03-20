/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service.impl;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.domain.SubscriptionMessage;
import fr.ans.afas.domain.SubscriptionMessageStatus;
import fr.ans.afas.repository.SubscriptionMessageRepository;
import fr.ans.afas.service.SignatureService;
import fr.ans.afas.service.SubscriptionManager;
import org.hl7.fhir.r4.model.DomainResource;

import java.util.Date;
import java.util.UUID;

/**
 * Implementation of the subscription manager in mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class DefaultSubscriptionManager implements SubscriptionManager {

    final FhirContext context;
    final SignatureService signService;
    final SubscriptionMessageRepository subscriptionMessageRepository;


    public DefaultSubscriptionManager(FhirContext context, SubscriptionMessageRepository subscriptionMessageRepository,
                                      SignatureService signService) {
        this.context = context;
        this.subscriptionMessageRepository = subscriptionMessageRepository;
        this.signService = signService;

    }


    public void sendMessage(String subscriptionId, DomainResource payload) {
        var parser = this.context.newJsonParser();

        var subscriptionMessage = new SubscriptionMessage();
        subscriptionMessage.setUuid(UUID.randomUUID().toString());
        if (payload.getMeta().getLastUpdated() == null) {
            subscriptionMessage.setCreationDate(new Date());
        } else {
            subscriptionMessage.setCreationDate(payload.getMeta().getLastUpdated());
        }
        subscriptionMessage.setLastUpdated(new Date());
        subscriptionMessage.setNextTryDate(new Date());
        subscriptionMessage.setStatus(SubscriptionMessageStatus.PENDING);
        subscriptionMessage.setType(payload.getResourceType().getPath());
        subscriptionMessage.setPayload(parser.encodeResourceToString(payload));
        subscriptionMessage.setSubscriptionId(subscriptionId);

        // sign the message:
        signService.sign(subscriptionMessage);

        this.subscriptionMessageRepository.save(subscriptionMessage);
    }


}
