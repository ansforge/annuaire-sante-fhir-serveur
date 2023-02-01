/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Date;

/**
 * A message sent/to send
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class SubscriptionMessage {

    /**
     * The uuid of the message to send
     */
    @Id
    @Setter
    String uuid;

    /**
     * Date of creation of the message
     */
    @Setter
    Date creationDate;

    /**
     * Date of last update of the message
     */
    @Setter
    Date lastUpdated;

    /**
     * Date when to send the message
     */
    @Setter
    Date nextTryDate;

    /**
     * The id (fhir) of the subscription
     */
    @Setter
    String subscriptionId;

    /**
     * Number of try for this message
     */
    int nbrTry;

    /**
     * Logs of the last call
     */
    @Setter
    String lastLogs;

    /**
     * Status of the message
     */
    @Setter
    SubscriptionMessageStatus status;

    /**
     * Signature of the message to authenticate the sender
     */
    @Setter
    String signature;

    /**
     * The payload to send
     */
    @Setter
    String payload;

    /**
     * The fhir type of the payload (lowercase)
     */
    @Setter
    String type;

    public SubscriptionMessage() {
        this.nbrTry = 0;
    }

    public void incrementNbTry() {
        this.nbrTry++;
    }

}
