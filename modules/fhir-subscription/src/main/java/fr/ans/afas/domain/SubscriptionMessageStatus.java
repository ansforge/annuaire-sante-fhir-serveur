/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.domain;

/**
 * Status of the subscription messages
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public enum SubscriptionMessageStatus {
    PENDING,
    SUCCESS,
    PENDING_RETRY,
    IN_ERROR

}
