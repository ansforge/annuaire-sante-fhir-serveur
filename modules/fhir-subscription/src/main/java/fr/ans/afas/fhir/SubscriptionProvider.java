/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.fhirserver.provider.AsBaseResourceProvider;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPage;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;

import java.util.LinkedList;
import java.util.List;

/**
 * Resource provider implementation for {@link org.hl7.fhir.r4.model.Subscription}.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class SubscriptionProvider<T> extends AsBaseResourceProvider<T> implements IResourceProvider {

    private static final String STATUS_BEFORE_DEACTIVATION = "statusBeforeDeactivation";

    /**
     * The fhir context
     */
    final FhirContext fhirContext;

    /**
     * The expression factory
     */
    final ExpressionFactory<T> expressionFactory;

    /**
     * Manage next urls
     */
    final NextUrlManager nextUrlManager;

    /**
     * Construct the RassDevice provider
     *
     * @param fhirStoreService the service that store fhir resources
     */
    public SubscriptionProvider(FhirStoreService<T> fhirStoreService, FhirContext fhirContext, ExpressionFactory<T> expressionFactory, NextUrlManager nextUrlManager) {
        super(fhirStoreService);
        this.fhirContext = fhirContext;
        this.expressionFactory = expressionFactory;
        this.nextUrlManager = nextUrlManager;
    }

    /**
     * Get the resource type for the provider
     *
     * @return the resource type
     */
    @Override
    public Class<Subscription> getResourceType() {
        return Subscription.class;
    }


    /**
     * Read a resource by ID
     *
     * @param theId the id to find
     * @return the found subscription
     */
    @Read()
    public Subscription getResourceById(@IdParam IdType theId) {
        return (Subscription) fhirStoreService.findById(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME, theId);
    }


    /**
     * Fhir search of a subscription
     *
     * @param theCount  the fhir _count
     * @param theId     the id
     * @param theStatus the status
     * @return the bundle with resources found
     */
    @Search()
    public IBundleProvider search(@Count Integer theCount,
                                  @Description(shortDefinition = "Recherche sur l'id de la ressource Subscription")
                                  @OptionalParam(name = IAnyResource.SP_RES_ID)
                                          TokenAndListParam theId,
                                  @Description(shortDefinition = "Recherche sur le status de la subscription")
                                  @OptionalParam(name = Subscription.SP_STATUS)
                                          TokenAndListParam theStatus,
                                  @Description(shortDefinition = "Recherche sur le critère de la subscription")
                                  @OptionalParam(name = Subscription.SP_CRITERIA)
                                          StringAndListParam theCriteria,
                                  @Description(shortDefinition = "Recherche sur le url (payload) de la subscription")
                                  @OptionalParam(name = Subscription.SP_URL)
                                          StringAndListParam theUrl,
                                  @Description(shortDefinition = "Recherche sur le type de payload de la subscription")
                                  @OptionalParam(name = Subscription.SP_PAYLOAD)
                                          TokenAndListParam thePayload,
                                  @Description(shortDefinition = "Recherche sur le type de la subscription")
                                  @OptionalParam(name = Subscription.SP_TYPE)
                                          TokenAndListParam theType
    ) {
        var selectExpression = new SelectExpression<>(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME).path(IAnyResource.SP_RES_ID).build(), theId);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME).path(Subscription.SP_STATUS).build(), theStatus);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME).path(Subscription.SP_CRITERIA).build(), theCriteria);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME).path(Subscription.SP_URL).build(), theUrl);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME).path(Subscription.SP_PAYLOAD).build(), thePayload);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME).path(Subscription.SP_TYPE).build(), theType);


        return new fr.ans.afas.fhir.AfasBundleProvider<>(fhirStoreService, selectExpression, nextUrlManager);
    }


    /**
     * Update a resource of type Subscription
     *
     * @param id           the id of the device
     * @param subscription the subscription to update
     * @return the operation outcome
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam Subscription subscription) {
        return super.update(id, subscription);
    }

    @Operation(name = "$deactivate-all", idempotent = false)
    public void deactivateAllSubscription() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME, expressionFactory);

        FhirPage subscriptionPage = fhirStoreService.search(null, selectExpression);
        var resources = subscriptionPage.getPage();
        var subscriptions = new LinkedList<Subscription>();

        for (var resource : resources) {
            var subscription = (Subscription) resource;

            subscription.addExtension(STATUS_BEFORE_DEACTIVATION, new StringType(subscription.getStatus().toCode()));
            subscription.setStatus(Subscription.SubscriptionStatus.OFF);
            subscriptions.add(subscription);
        }

        fhirStoreService.store(subscriptions, true);
    }

    @Operation(name = "$activate-all", idempotent = false)
    public void activateAllSubscription() {
        var resourceName = FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME;
        var selectExpression = new SelectExpression<>(resourceName, expressionFactory);

        FhirPage subscriptionPage = fhirStoreService.search(null, selectExpression);
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

        fhirStoreService.store(subscriptions, true);
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
        int index = 0;

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