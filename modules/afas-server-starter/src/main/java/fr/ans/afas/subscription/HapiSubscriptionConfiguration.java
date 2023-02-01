/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.subscription;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.SubscriptionProvider;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class HapiSubscriptionConfiguration {


    @Bean
    @Autowired
    SubscriptionProvider subscriptionProvider(FhirStoreService<?> fhirStoreService, FhirContext fhirContext, ExpressionFactory<?> expressionFactory, NextUrlManager nextUrlManager) {
        return new SubscriptionProvider(fhirStoreService, fhirContext, expressionFactory, nextUrlManager);
    }

}
