/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import fr.ans.afas.domain.SubscriptionMessage;

/**
 * Sign messages
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface SignatureService {

    /**
     * Generate a signature (hash, key..) of a subscription message and store it in the message passed in arg
     *
     * @param message the message
     */
    void sign(SubscriptionMessage message);

}
