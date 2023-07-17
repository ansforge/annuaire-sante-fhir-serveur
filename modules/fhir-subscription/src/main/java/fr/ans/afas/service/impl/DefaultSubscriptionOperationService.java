/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
package fr.ans.afas.service.impl;

import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPage;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.service.SubscriptionOperationService;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;

import java.util.LinkedList;
import java.util.List;

public class DefaultSubscriptionOperationService<T> implements SubscriptionOperationService {


    static final String STATUS_BEFORE_DEACTIVATION = "statusBeforeDeactivation";
    final FhirStoreService<T> storeService;
    final ExpressionFactory<T> expressionFactory;

    public DefaultSubscriptionOperationService(FhirStoreService<T> storeService, ExpressionFactory<T> expressionFactory) {
        this.storeService = storeService;
        this.expressionFactory = expressionFactory;
    }


    @Override
    public void setSubscriptionsEnabled(boolean enabled) {
        if (enabled) {
            activateAllSubscription();
        } else {
            deactivateAllSubscription();
        }
    }


    public void deactivateAllSubscription() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME, expressionFactory);

        FhirPage subscriptionPage = storeService.search(null, selectExpression);
        var resources = subscriptionPage.getPage();
        var subscriptions = new LinkedList<Subscription>();

        for (var resource : resources) {
            var subscription = (Subscription) resource;

            subscription.addExtension(STATUS_BEFORE_DEACTIVATION, new StringType(subscription.getStatus().toCode()));
            subscription.setStatus(Subscription.SubscriptionStatus.OFF);
            subscriptions.add(subscription);
        }

        storeService.store(subscriptions, true, false);
    }

    public void activateAllSubscription() {
        var resourceName = FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME;
        var selectExpression = new SelectExpression<>(resourceName, expressionFactory);

        FhirPage subscriptionPage = storeService.search(null, selectExpression);
        var resources = subscriptionPage.getPage();
        var subscriptions = new LinkedList<Subscription>();

        for (var resource : resources) {
            var subscription = (Subscription) resource;

            if (shouldActivate(subscription)) {
                subscription.setExtension(removeBeforeDeactivation(subscription.getExtension()));

                subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                subscriptions.add(subscription);
            }
        }

        storeService.store(subscriptions, true, false);
    }

    private boolean shouldActivate(Subscription subscription) {
        var extensions = subscription.getExtension();

        for (Extension extension : extensions) {

            if (extension.getUrl().equals(STATUS_BEFORE_DEACTIVATION)) {
                var value = (StringType) extension.getValue();
                var status = Subscription.SubscriptionStatus.fromCode(value.getValue());

                if (status == Subscription.SubscriptionStatus.ACTIVE || status == Subscription.SubscriptionStatus.REQUESTED) {
                    return true;
                }
            }
        }

        return false;
    }

    private List<Extension> removeBeforeDeactivation(List<Extension> extensions) {
        var index = 0;

        for (var extension : extensions) {
            if (extension.getUrl().equals(STATUS_BEFORE_DEACTIVATION)) {
                break;
            }
            index++;
        }

        if (index < extensions.size()) {
            extensions.remove(index);
        }

        return extensions;

    }

}
