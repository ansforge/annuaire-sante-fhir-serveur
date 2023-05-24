/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.subscription;

import fr.ans.afas.configuration.HapiConfiguration;
import fr.ans.afas.configuration.SpringConfiguration;
import fr.ans.afas.configuration.SubscriptionSearchConfig;
import fr.ans.afas.service.SignatureService;
import fr.ans.afas.service.impl.HMacSha256SignatureService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@ConditionalOnProperty(value = "afas.fhir.subscription.enabled", havingValue = "true")
@EnableMongoRepositories(basePackages = "fr.ans.afas")
@Import({HapiSubscriptionConfiguration.class, SpringConfiguration.class})
public class SubscriptionConfiguration {

    @Bean
    HapiConfiguration hapiConfiguration() {
        return new HapiConfiguration();
    }

    @Bean
    SubscriptionSearchConfig subscriptionSearchConfig() {
        return new SubscriptionSearchConfig();
    }


    @Bean
    SignatureService signatureService(@Value("${afas.fhir.subscription.hmacKey}") String key) {
        return new HMacSha256SignatureService(key);
    }


}
