/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service.audit;

import fr.ans.afas.fhirserver.hook.annotations.AfasSubscribe;
import fr.ans.afas.fhirserver.hook.annotations.AfasSubscriber;
import fr.ans.afas.fhirserver.hook.event.*;

@AfasSubscriber
public class DefaultWriteAuditService extends BaseAuditService {

    @AfasSubscribe
    public void on(BeforeCreateResourceEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getResource(), null);
        }
    }

    @AfasSubscribe
    public void on(AfterCreateResourceEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getResource(), null);
        }
    }

    @AfasSubscribe
    public void on(BeforeDeleteEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getResourceId(), null);
        }
    }

    @AfasSubscribe
    public void on(AfterDeleteEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getResourceId(), null);
        }
    }

    @AfasSubscribe
    public void on(BeforeDeleteAllEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, null, null);
        }
    }

    @AfasSubscribe
    public void on(AfterDeleteAllEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, null, null);
        }
    }


}
