/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils.data;


import fr.ans.afas.fhirserver.search.config.BaseSearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Configuration used for tests
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class TestSearchConfigService extends BaseSearchConfigService {

    /**
     * The name of the Sample Fhir Resource
     */
    public static final String FHIR_RESOURCE_NAME = "FhirResource";

    /**
     * The name of the Sample Fhir Resource Device
     */
    public static final String FHIR_RESOURCE_DEVICE = "Device";

    /**
     * The name of the Sample Fhir Resource Practitioner Role
     */
    public static final String FHIR_RESOURCE_PRACTITIONER_ROLE = "PractitionerRole";

    /**
     * The name of the Sample Fhir Resource Organization
     */
    public static final String FHIR_RESOURCE_ORGANIZATION = "Organization";

    /**
     * The exemple path for a token
     */
    public static final String FHIR_RESOURCE_TOKEN_PATH = "token_path";
    /**
     * The exemple path for a string
     */
    public static final String FHIR_RESOURCE_STRING_PATH = "string_path";
    /**
     * The exemple path for a reference
     */
    public static final String FHIR_RESOURCE_REFERENCE_PATH = "reference_path";
    /**
     * The exemple path for a date
     */
    public static final String FHIR_RESOURCE_DATE_PATH = "date_path";
    /**
     * The exemple path for a quantity
     */
    public static final String FHIR_RESOURCE_QUANTITY_PATH = "quantity_path";
    /**
     * The exemple db path for a token
     */
    public static final String FHIR_RESOURCE_DB_TOKEN_PATH = "t_token_path";
    /**
     * The exemple db path for a string
     */
    public static final String FHIR_RESOURCE_DB_STRING_PATH = "t_string_path";
    /**
     * The exemple db path for a reference
     */
    public static final String FHIR_RESOURCE_DB_REFERENCE_PATH = "t_reference_path";
    /**
     * The exemple db path for a quantity
     */
    public static final String FHIR_RESOURCE_DB_QUANTITY_PATH = "t_quantity_path";
    /**
     * The exemple db path for a date
     */
    public static final String FHIR_RESOURCE_DB_DATE_PATH = "t_date_path";


    /**
     * Setup a test config
     */
    public TestSearchConfigService() {
        super(TenantSearchConfig.builder().tenantConfig(Tenant.builder().build()).resources(List.of()).build());
        var list = new ArrayList<SearchParamConfig>();
        list.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_TOKEN_PATH)
                .name("tokenPath")
                .searchType("token")
                .indexName(FHIR_RESOURCE_DB_TOKEN_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("tokenPath").build()))
                .build());

        list.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_STRING_PATH)
                .name("stringPath")
                .searchType("string")
                .indexName(FHIR_RESOURCE_DB_STRING_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("stringPath").build()))
                .build());

        list.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_DATE_PATH)
                .name("datePath")
                .searchType("date")
                .indexName(FHIR_RESOURCE_DB_DATE_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("datePath").build()))
                .build());

        list.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_QUANTITY_PATH)
                .name("quantityPath")
                .searchType("quantity")
                .indexName(FHIR_RESOURCE_DB_QUANTITY_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("quantityPath").build()))
                .build());

        list.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_REFERENCE_PATH)
                .name("referencePath")
                .searchType("reference")
                .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("referencePath").build()))
                .build());

        configs.put(FHIR_RESOURCE_NAME, FhirResourceSearchConfig.builder().name(FHIR_RESOURCE_NAME).searchParams(list).build());
        configs.put(FHIR_RESOURCE_DEVICE, FhirResourceSearchConfig.builder().name(FHIR_RESOURCE_DEVICE)
                .searchParams(
                        List.of(SearchParamConfig.builder()
                                .urlParameter("organization")
                                .name("includePath")
                                .searchType("reference")
                                .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                                .resourcePaths(List.of(ResourcePathConfig.builder().path("includePath").build()))
                                .build())
                ).build());

        configs.put(FHIR_RESOURCE_PRACTITIONER_ROLE, FhirResourceSearchConfig.builder().name(FHIR_RESOURCE_PRACTITIONER_ROLE)
                .searchParams(
                        List.of(
                                SearchParamConfig.builder()
                                        .urlParameter("partof")
                                        .name("includePath")
                                        .searchType("reference")
                                        .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                                        .resourcePaths(List.of(ResourcePathConfig.builder().path("includePath").build()))
                                        .build(),
                                SearchParamConfig.builder()
                                        .urlParameter("organization")
                                        .name("includePath")
                                        .searchType("reference")
                                        .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                                        .resourcePaths(List.of(ResourcePathConfig.builder().path("includePath").build()))
                                        .build(),
                                SearchParamConfig.builder()
                                        .urlParameter("practitioner")
                                        .name("includePath")
                                        .searchType("reference")
                                        .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                                        .resourcePaths(List.of(ResourcePathConfig.builder().path("includePath").build()))
                                        .build()
                        )
                ).build());

        configs.put(FHIR_RESOURCE_ORGANIZATION, FhirResourceSearchConfig.builder().name(FHIR_RESOURCE_ORGANIZATION)
                .searchParams(
                        List.of(SearchParamConfig.builder()
                                .urlParameter("partof")
                                .name("includePath")
                                .searchType("reference")
                                .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                                .resourcePaths(List.of(ResourcePathConfig.builder().path("includePath").build()))
                                .build())
                ).build());
    }
}
