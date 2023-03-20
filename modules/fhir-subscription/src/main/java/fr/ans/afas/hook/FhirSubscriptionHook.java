/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.hook;

import fr.ans.afas.fhirserver.hook.annotations.AfasSubscribe;
import fr.ans.afas.fhirserver.hook.annotations.AfasSubscriber;
import fr.ans.afas.fhirserver.hook.event.AfterCreateResourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AfasSubscriber
public class FhirSubscriptionHook {

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @AfasSubscribe
    public void on(AfterCreateResourceEvent e) {
        // noop
    }

}
