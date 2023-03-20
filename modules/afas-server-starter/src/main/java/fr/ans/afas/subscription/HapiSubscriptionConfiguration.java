/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.subscription;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.GlobalProvider;
import fr.ans.afas.fhir.SubscriptionProvider;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.service.SubscriptionOperationService;
import fr.ans.afas.service.impl.DefaultSubscriptionOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class HapiSubscriptionConfiguration<T> {


    @Bean
    @Autowired
    SubscriptionOperationService subscriptionOperationService(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory) {
        return new DefaultSubscriptionOperationService(fhirStoreService, expressionFactory);
    }


    @Bean
    @Autowired
    SubscriptionProvider subscriptionProvider(FhirStoreService<T> fhirStoreService, FhirContext fhirContext, ExpressionFactory<T> expressionFactory, NextUrlManager<T> nextUrlManager,
                                              @Value("afas.fhir.next-url-encryption-key") String secretKey) {
        return new SubscriptionProvider<T>(fhirStoreService, fhirContext, expressionFactory, nextUrlManager, secretKey);
    }


    @Bean
    @Autowired
    GlobalProvider globalProvider(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory) {
        return new GlobalProvider(subscriptionOperationService(fhirStoreService, expressionFactory));
    }

}
