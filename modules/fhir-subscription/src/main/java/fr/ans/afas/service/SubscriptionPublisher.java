/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import fr.ans.afas.domain.SubscriptionMessage;

import java.util.concurrent.CompletableFuture;

/**
 * Publish a {@link SubscriptionMessage}
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface SubscriptionPublisher {
    CompletableFuture<SubscriptionMessage> publish(SubscriptionMessage message);
}
