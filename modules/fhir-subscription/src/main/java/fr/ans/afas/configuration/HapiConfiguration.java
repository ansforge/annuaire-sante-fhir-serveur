/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.configuration;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.SubscriptionProvider;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class HapiConfiguration {


    @Bean
    <T> SubscriptionProvider<T> subscriptionProvider(FhirStoreService<T> fhirStoreService, FhirContext fhirContext, ExpressionFactory<T> expressionFactory, NextUrlManager<T> nextUrlManager,
                                                     @Value("afas.fhir.next-url-encryption-key") String secretKey) {
        return new SubscriptionProvider<>(fhirStoreService, fhirContext, expressionFactory, nextUrlManager, secretKey);
    }


}
