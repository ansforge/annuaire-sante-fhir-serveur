/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.configuration;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.hook.FhirHookManager;
import fr.ans.afas.hook.FhirHookPublisher;
import fr.ans.afas.repository.SubscriptionMessageRepository;
import fr.ans.afas.service.SignatureService;
import fr.ans.afas.service.SubscriptionManager;
import fr.ans.afas.service.impl.MongoSubscriptionManager;
import fr.ans.afas.task.SubscriptionCron;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring configuration for the subscription system
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@EnableScheduling
public class SpringConfiguration {

    @Value("${afas.fhir.subscription.pageSize}")
    private int pageSize;

    @Value("${fhir.hook.request.max-try:10}")
    private int maxWebhookTry;

    @Value("${fhir.hook.request.timeout:20}")
    private int timeout;

    @Value("${fhir.hook.request.wait-before-retry:20}")
    private int waitBeforeRetry;

    @Value("${fhir.hook.request.nb-max:1000}")
    private int nbRequestMax;

    @Bean
    SubscriptionManager subscriptionManager(FhirContext context, SubscriptionMessageRepository subscriptionMessageRepository, SignatureService signService) {
        return new MongoSubscriptionManager(context, subscriptionMessageRepository, signService);
    }

    @Bean
    FhirHookPublisher fhirHookPublisher(SignatureService signatureService) {
        return new FhirHookPublisher(timeout, signatureService);
    }

    @Bean
    <T> SubscriptionCron subscriptionCron(ExpressionFactory<T> expressionFactory, FhirStoreService<T> fhirStoreService, SearchConfig searchConfig, SubscriptionManager subscriptionManager) {
        return new SubscriptionCron<T>(expressionFactory, fhirStoreService, searchConfig, subscriptionManager, this.pageSize);
    }

    @Bean
    <T> FhirHookManager fhirHookManager(SubscriptionMessageRepository subscriptionMessageRepository, FhirStoreService<T> fhirStoreService, FhirHookPublisher publisher) {
        return new FhirHookManager<T>(subscriptionMessageRepository, fhirStoreService, publisher, this.maxWebhookTry, this.pageSize, this.waitBeforeRetry, this.nbRequestMax);
    }

}
