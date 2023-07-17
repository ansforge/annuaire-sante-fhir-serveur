/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.task;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import fr.ans.afas.service.SubscriptionManager;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;
import java.util.stream.Collectors;

/**
 * The cron task that periodically look for change in resources with {@link Subscription}
 *
 * @param <T>
 */
public class SubscriptionCron<T> {

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    final ExpressionFactory<T> expressionFactory;
    final FhirStoreService<T> fhirStoreService;
    final SearchConfig searchConfig;

    final SubscriptionManager subscriptionManager;

    private final int pageSize;

    public SubscriptionCron(ExpressionFactory<T> expressionFactory, FhirStoreService<T> fhirStoreService,
                            SearchConfig searchConfig, SubscriptionManager subscriptionManager, int pageSize) {
        this.expressionFactory = expressionFactory;
        this.fhirStoreService = fhirStoreService;
        this.searchConfig = searchConfig;
        this.subscriptionManager = subscriptionManager;
        this.pageSize = pageSize;
    }

    /**
     * For each subscription, we try to find updated elements, and we create {@link fr.ans.afas.domain.SubscriptionMessage} if a resource match
     */
    @Scheduled(fixedDelayString = "${fhir.subscription.process.delay:30000}")
    public void findUpdated() {
        var selectSubscriptions = new SelectExpression<>(FhirServerConstants.SUBSCRIPTION_FHIR_RESOURCE_NAME, this.expressionFactory);
        selectSubscriptions.setCount(pageSize);

        var subscriptionResultSize = 0;
        SearchContext subscriptionContext = null;

        do {
            var subscriptions = fhirStoreService.search(subscriptionContext, selectSubscriptions);
            subscriptionResultSize = subscriptions.getPage().size();
            logger.debug("Iterating Subscription Page - page size : {}", subscriptionResultSize);

            for (var s : subscriptions.getPage()) {
                var subscription = (Subscription) s;

                if (Subscription.SubscriptionStatus.ACTIVE.equals(subscription.getStatus())) {
                    try {
                        this.handleSubscription(subscription);
                    } catch (BadSelectExpression | BadRequestException | BadDataFormatException badSelectExpression) {
                        logger.error("Error parsing the criteria for the subscription {}", subscription.getId());
                        subscription.setStatus(Subscription.SubscriptionStatus.ERROR);
                        subscription.setError("Error parsing the criteria for the subscription");
                    }
                }
            }
            this.fhirStoreService.store(subscriptions.getPage().stream().map(DomainResource.class::cast).collect(Collectors.toList()), false, false);

            subscriptionContext = subscriptions.getContext();
        } while (subscriptionResultSize == pageSize);
    }

    private void handleSubscription(Subscription subscription) throws BadSelectExpression, BadDataFormatException {
        var dt = (DateTimeType) getCreateLastDateCheckExtension(subscription).getValue();
        var toDate = new Date();
        var query = FhirRequestParser.parseSelectExpression(subscription.getCriteria(), this.expressionFactory, this.searchConfig);

        logger.debug("Subscription criteria : {}", subscription.getCriteria());
        logger.debug("datetime : {}", dt.getValue());
        logger.debug("toDate : {}", toDate);

        query.getExpression()
                .addExpression(expressionFactory.newDateRangeExpression(FhirSearchPath.builder()
                        .path("_lastUpdated")
                        .resource(query.getFhirResource())
                        .build(), dt.getValue(), TemporalPrecisionEnum.MILLI, ParamPrefixEnum.GREATERTHAN_OR_EQUALS))
                .addExpression(expressionFactory.newDateRangeExpression(FhirSearchPath.builder()
                        .path("_lastUpdated")
                        .resource(query.getFhirResource())
                        .build(), toDate, TemporalPrecisionEnum.MILLI, ParamPrefixEnum.LESSTHAN_OR_EQUALS))
        ;
        query.setCount(pageSize);
        getCreateLastDateCheckExtension(subscription).setValue(new DateTimeType(toDate, TemporalPrecisionEnum.MILLI));

        var resourceResultSize = 0;
        SearchContext resourceContext = null;

        do {
            var resourceToNotify = fhirStoreService.search(resourceContext, query);
            resourceResultSize = resourceToNotify.getPage().size();

            for (var r : resourceToNotify.getPage()) {
                logger.info("Subscription notification on {}:{} {}", r.getIdElement().getResourceType(), r.getIdElement().getIdPart(), subscriptionManager);
                subscriptionManager.sendMessage(subscription.getId(), r);
            }

            resourceContext = resourceToNotify.getContext();
        } while (resourceResultSize == pageSize);
    }


    /**
     * Get or create the subscription extension that contains the date of the last check
     *
     * @param subscription the subscription
     * @return the extension
     */
    Extension getCreateLastDateCheckExtension(Subscription subscription) {
        var ext = subscription.getExtensionByUrl("lastDateCheck");
        if (ext == null) {
            ext = subscription.addExtension();
            ext.setUrl("lastDateCheck");
            ext.setValue(new DateTimeType(subscription.getMeta().getLastUpdated()));
        }
        return ext;
    }

}
