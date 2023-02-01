/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.configuration;

import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.ResourcePathConfig;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import org.hl7.fhir.r4.model.Subscription;

import java.util.List;


/***
 * Search config for subscriptions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class SubscriptionSearchConfig extends ServerSearchConfig {

    public static final String INDEX_SUBSCRIPTION_STATUS = "t_status";
    public static final String INDEX_SUBSCRIPTION_ENDPOINT = "t_endpoint";
    public static final String INDEX_SUBSCRIPTION_CRITERIA = "t_criteria";
    public static final String INDEX_SUBSCRIPTION_TYPE = "t_type";
    public static final String INDEX_SUBSCRIPTION_PAYLOAD = "t_payload";

    public SubscriptionSearchConfig() {
        var fhirResourceSearchConfig = new FhirResourceSearchConfig();
        fhirResourceSearchConfig.setName("Subscription");
        fhirResourceSearchConfig.setProfile("Subscription");
        fhirResourceSearchConfig.setSearchParams(List.of(
                SearchParamConfig.builder().name(Subscription.SP_STATUS).urlParameter(Subscription.SP_STATUS).resourcePaths(List.of(ResourcePathConfig.builder().path("status?.toCode()").build())).indexName(INDEX_SUBSCRIPTION_STATUS).searchType(StorageConstants.INDEX_TYPE_TOKEN).build(),
                SearchParamConfig.builder().name(Subscription.SP_URL).urlParameter(Subscription.SP_URL).resourcePaths(List.of(ResourcePathConfig.builder().path("channel|endpoint").build())).indexName(INDEX_SUBSCRIPTION_ENDPOINT).searchType(StorageConstants.INDEX_TYPE_STRING).build(),
                SearchParamConfig.builder().name(Subscription.SP_CRITERIA).urlParameter(Subscription.SP_CRITERIA).resourcePaths(List.of(ResourcePathConfig.builder().path("criteria").build())).indexName(INDEX_SUBSCRIPTION_CRITERIA).searchType(StorageConstants.INDEX_TYPE_STRING).build(),
                SearchParamConfig.builder().name(Subscription.SP_TYPE).urlParameter(Subscription.SP_TYPE).resourcePaths(List.of(ResourcePathConfig.builder().path("channel|type?.toCode()").build())).indexName(INDEX_SUBSCRIPTION_TYPE).searchType(StorageConstants.INDEX_TYPE_TOKEN).build(),
                SearchParamConfig.builder().name(Subscription.SP_PAYLOAD).urlParameter(Subscription.SP_PAYLOAD).resourcePaths(List.of(ResourcePathConfig.builder().path("channel|payload").build())).indexName(INDEX_SUBSCRIPTION_PAYLOAD).searchType(StorageConstants.INDEX_TYPE_TOKEN).build()
        ));
        this.setResources(List.of(fhirResourceSearchConfig));
    }
}
