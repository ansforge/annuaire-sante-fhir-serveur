/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service.audit;

import fr.ans.afas.fhirserver.hook.annotations.AfasSubscribe;
import fr.ans.afas.fhirserver.hook.annotations.AfasSubscriber;
import fr.ans.afas.fhirserver.hook.event.*;


@AfasSubscriber
public class DefaultReadAuditService extends BaseAuditService {


    @AfasSubscribe
    public void on(BeforeCountEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getSelectExpression(), null);
        }
    }

    @AfasSubscribe
    public void on(AfterCountEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getSelectExpression(), event.getCountResult());
        }
    }

    @AfasSubscribe
    public void on(BeforeFindByIdEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getTheId(), null);
        }
    }

    @AfasSubscribe
    public void on(AfterFindByIdEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getTheId(), event.getTheId());
        }
    }

    @AfasSubscribe
    public void on(BeforeSearchEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getSelectExpression(), null);
        }
    }

    @AfasSubscribe
    public void on(AfterSearchEvent event) {
        if (logger.isInfoEnabled()) {
            logRequest(event, event.getSelectExpression(), null);
        }
    }


}
