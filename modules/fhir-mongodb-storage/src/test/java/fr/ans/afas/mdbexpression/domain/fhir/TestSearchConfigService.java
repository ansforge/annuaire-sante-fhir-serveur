/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir;

import fr.ans.afas.fhirserver.search.config.BaseSearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.ResourcePathConfig;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;

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
     * The example db path for a string
     */
    public static final String FHIR_RESOURCE_DB_STRING_PATH = "t_string_path";
    /**
     * The example db path for a reference
     */
    public static final String FHIR_RESOURCE_DB_REFERENCE_PATH = "t_reference_path";
    /**
     * The example db path for a quantity
     */
    public static final String FHIR_RESOURCE_DB_QUANTITY_PATH = "t_quantity_path";
    /**
     * The example db path for a date
     */
    public static final String FHIR_RESOURCE_DB_DATE_PATH = "t_date_path";

    /**
     * The name of the Sample FhirResourceSub resouce (FhirResourceSub is linked to FhirResource with FhirResourceSub.parent)
     */
    public static final String FHIR_RESOURCE_SUB_NAME = "FhirResourceSub";

    /**
     * The exemple path for a token for the sub resource
     */
    public static final String FHIR_RESOURCE_SUB_TOKEN_PATH = "token_sub_path";
    /**
     * The exemple path for a string for the sub resource
     */
    public static final String FHIR_RESOURCE_SUB_STRING_PATH = "string_sub_path";
    /**
     * The exemple path for a reference for the sub resource
     */
    public static final String FHIR_RESOURCE_SUB_REFERENCE_PATH = "reference_sub_path";
    /**
     * The exemple db path for a token for the sub resource
     */
    public static final String FHIR_RESOURCE_SUB_DB_TOKEN_PATH = "t_token_sub_path";
    /**
     * The example db path for a string for the sub resource
     */
    public static final String FHIR_RESOURCE_SUB_DB_STRING_PATH = "t_string_sub_path";
    /**
     * The example db path for a reference for the sub resource
     */
    public static final String FHIR_RESOURCE_SUB_DB_REFERENCE_PATH = "t_reference_sub_path";

    public static final String REFERENCE_PATH_REFERENCE_TYPE = "referenceType";


    public TestSearchConfigService() {
        super(new TenantSearchConfig());
    }

    /**
     * Setup a test config complete
     *
     * @return TestSearchConfig
     */
    public TestSearchConfigService applyTestSearchConfigComplete() {
        var listFhirResource = new ArrayList<SearchParamConfig>();
        listFhirResource.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_TOKEN_PATH)
                .name("tokenPath")
                .searchType("token")
                .indexName(FHIR_RESOURCE_DB_TOKEN_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("tokenPath").build()))
                .build());

        listFhirResource.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_STRING_PATH)
                .name("stringPath")
                .searchType("string")
                .indexName(FHIR_RESOURCE_DB_STRING_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("stringPath").build()))
                .build());

        listFhirResource.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_DATE_PATH)
                .name("datePath")
                .searchType("date")
                .indexName(FHIR_RESOURCE_DB_DATE_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("datePath").build()))
                .build());

        listFhirResource.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_QUANTITY_PATH)
                .name("quantityPath")
                .searchType("quantity")
                .indexName(FHIR_RESOURCE_DB_QUANTITY_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("quantityPath").build()))
                .build());

        listFhirResource.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_REFERENCE_PATH)
                .name("referencePath")
                .searchType("reference")
                .referenceType(REFERENCE_PATH_REFERENCE_TYPE)
                .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("referencePath").build()))
                .build());

        configs.put(FHIR_RESOURCE_NAME, FhirResourceSearchConfig.builder().name(FHIR_RESOURCE_NAME).profile("http").searchParams(listFhirResource).build());


        // the sub resource:
        var listFhirResourceSub = new ArrayList<SearchParamConfig>();
        listFhirResourceSub.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_SUB_TOKEN_PATH)
                .name("tokenPath")
                .searchType("token")
                .indexName(FHIR_RESOURCE_SUB_DB_TOKEN_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("tokenPath").build()))
                .build());

        listFhirResourceSub.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_SUB_STRING_PATH)
                .name("stringPath")
                .searchType("string")
                .indexName(FHIR_RESOURCE_SUB_DB_STRING_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("stringPath").build()))
                .build());

        listFhirResourceSub.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_SUB_REFERENCE_PATH)
                .name("parent")
                .searchType("reference")
                .indexName(FHIR_RESOURCE_SUB_DB_REFERENCE_PATH)
                .referenceType(REFERENCE_PATH_REFERENCE_TYPE)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("parentPath").build()))
                .build());

        configs.put(FHIR_RESOURCE_SUB_NAME, FhirResourceSearchConfig.builder().name(FHIR_RESOURCE_SUB_NAME).profile("http").searchParams(listFhirResourceSub).build());

        return this;
    }

    /**
     * Setup a test config to test only a reference path without reference type
     *
     * @return TestSearchConfig
     */
    public TestSearchConfigService applyTestSearchConfigReferencePathWithoutReferenceType() {
        var listFhirResource = new ArrayList<SearchParamConfig>();
        listFhirResource.add(SearchParamConfig.builder()
                .urlParameter(FHIR_RESOURCE_REFERENCE_PATH)
                .name("referencePath")
                .searchType("reference")
                .indexName(FHIR_RESOURCE_DB_REFERENCE_PATH)
                .resourcePaths(List.of(ResourcePathConfig.builder().path("referencePath").build()))
                .build());

        configs.put(FHIR_RESOURCE_NAME, FhirResourceSearchConfig.builder().name(FHIR_RESOURCE_NAME).profile("http").searchParams(listFhirResource).build());

        return this;
    }
}
