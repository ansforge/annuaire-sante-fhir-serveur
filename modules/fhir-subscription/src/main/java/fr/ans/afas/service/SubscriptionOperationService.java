/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

public interface SubscriptionOperationService {


    /**
     * Enable or disable all subscriptions
     *
     * @param enabled true to enable, false to disable
     */
    void setSubscriptionsEnabled(boolean enabled);
}
