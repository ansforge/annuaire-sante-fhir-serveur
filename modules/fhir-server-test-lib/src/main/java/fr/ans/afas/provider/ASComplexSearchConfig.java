/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.provider;

import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.ResourcePathConfig;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Organization;

import java.util.ArrayList;
import java.util.List;

/**
 * An advanced search configuration with extensions and custom fields for Organization and Device. This sample was based on the ANS Organization/Device profiles.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class ASComplexSearchConfig extends TenantSearchConfig {

    public static final String LAST_UPDATED = "_lastUpdated";
    public static final String INCLUDE_ALL = "*";
    public static final String INCLUDE_HEALTHCARESERVICE_ORGANIZATION = "HealthcareService:organization";
    public static final String INCLUDE_PRACTITIONERROLE_PARTOF = "PractitionerRole:partof";
    public static final String REVINCLUDE_DEVICE_ORGANIZATION = "Device:organization";
    public static final String REVINCLUDE_HEALTHCARESERVICE_ORGANIZATION = "HealthcareService:organization";
    public static final String REVINCLUDE_ORGANIZATION_ENDPOINT = "Organization:endpoint";
    public static final String REVINCLUDE_ORGANIZATION_PARTOF = "Organization:partof";
    public static final String REVINCLUDE_PRACTITIONERROLE_ORGANIZATION = "PractitionerRole:organization";
    public static final String REVINCLUDE_PRACTITIONERROLE_PRACTITIONER = "PractitionerRole:practitioner";

    public ASComplexSearchConfig() {
        var organizationSearchConfig = FhirResourceSearchConfig.builder().name("Organization").profile("https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-Organization").build();

        organizationSearchConfig.setSearchIncludes(
                INCLUDE_ALL,
                REVINCLUDE_DEVICE_ORGANIZATION);
        organizationSearchConfig.setSearchRevIncludes(
                REVINCLUDE_DEVICE_ORGANIZATION,
                INCLUDE_HEALTHCARESERVICE_ORGANIZATION,
                REVINCLUDE_ORGANIZATION_ENDPOINT,
                REVINCLUDE_ORGANIZATION_PARTOF,
                REVINCLUDE_PRACTITIONERROLE_ORGANIZATION,
                REVINCLUDE_PRACTITIONERROLE_PRACTITIONER
        );
        var params = new ArrayList<SearchParamConfig>();
        params.add(SearchParamConfig.builder().name(IAnyResource.SP_RES_ID).urlParameter(IAnyResource.SP_RES_ID).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_T_ID).resourcePaths(List.of(ResourcePathConfig.builder().path("id").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ACTIVE).urlParameter(Organization.SP_ACTIVE).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ACTIVE).resourcePaths(List.of(ResourcePathConfig.builder().path("active").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ADDRESS).urlParameter(Organization.SP_ADDRESS).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ADDRESS).resourcePaths(
                List.of(
                        ResourcePathConfig.builder().path("address|city").build(),
                        ResourcePathConfig.builder().path("address|country").build(),
                        ResourcePathConfig.builder().path("address|postalCode").build(),
                        ResourcePathConfig.builder().path("address|state").build(),
                        ResourcePathConfig.builder().path("address|use?.toCode()").build()
                )).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ADDRESS_CITY).urlParameter(Organization.SP_ADDRESS_CITY).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ADDRESS_CITY).resourcePaths(List.of(ResourcePathConfig.builder().path("address|city").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ADDRESS_COUNTRY).urlParameter(Organization.SP_ADDRESS_COUNTRY).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ADDRESS_COUNTRY).resourcePaths(List.of(ResourcePathConfig.builder().path("address|country").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ADDRESS_POSTALCODE).urlParameter(Organization.SP_ADDRESS_POSTALCODE).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ADDRESS_POSTALCODE).resourcePaths(List.of(ResourcePathConfig.builder().path("address|postalCode").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ADDRESS_STATE).urlParameter(Organization.SP_ADDRESS_STATE).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ADDRESS_STATE).resourcePaths(List.of(ResourcePathConfig.builder().path("address|state").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ADDRESS_USE).urlParameter(Organization.SP_ADDRESS_USE).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ADDRESS_USE).resourcePaths(List.of(ResourcePathConfig.builder().path("address|use?.toCode()").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_ENDPOINT).urlParameter(Organization.SP_ENDPOINT).searchType(StorageConstants.INDEX_TYPE_REFERENCE).description("").indexName(StorageConstants.INDEX_ORGANIZATION_ENDPOINT).resourcePaths(List.of(ResourcePathConfig.builder().path("endpoint").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_IDENTIFIER).urlParameter(Organization.SP_IDENTIFIER).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_ORGANIZATION_IDENTIFIER).resourcePaths(List.of(ResourcePathConfig.builder().path("identifier").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_NAME).urlParameter(Organization.SP_NAME).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_ORGANIZATION_NAME).resourcePaths(List.of(ResourcePathConfig.builder().path("name").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_PARTOF).urlParameter(Organization.SP_PARTOF).searchType(StorageConstants.INDEX_TYPE_REFERENCE).description("").indexName(StorageConstants.INDEX_ORGANIZATION_PARTOF).resourcePaths(List.of(ResourcePathConfig.builder().path("partOf").build())).build());
        params.add(SearchParamConfig.builder().name(Organization.SP_TYPE).urlParameter(Organization.SP_TYPE).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_ORGANIZATION_TYPE).resourcePaths(List.of(ResourcePathConfig.builder().path("type").build())).build());
        params.add(SearchParamConfig.builder().name(LAST_UPDATED).urlParameter(LAST_UPDATED).searchType(StorageConstants.INDEX_TYPE_DATE_RANGE).description("").indexName(StorageConstants.INDEX_T_LASTUPDATED).resourcePaths(List.of(ResourcePathConfig.builder().path("meta|lastUpdated").build())).build());
        params.add(SearchParamConfig.builder().name("pharmacy-licence").urlParameter("pharmacy-licence").searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName("t_pharmacy-licence").resourcePaths(List.of(ResourcePathConfig.builder().path("extension.?[#this.url=='https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Organization-PharmacyLicence']|value").build())).build());
        params.add(SearchParamConfig.builder().name("mailbox-mss").urlParameter("mailbox-mss").searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_ORGANIZATION_MAILBOX_MSS).resourcePaths(List.of(ResourcePathConfig.builder().path("extension.?[#this.url=='https://annuaire.sante.gouv.fr/fhir/StructureDefinition/MailboxMSS']|extension.?[#this.url=='value']|value").build())).build());
        organizationSearchConfig.setSearchParams(params);
        this.getResources().add(organizationSearchConfig);


        var deviceSearchConfig = FhirResourceSearchConfig.builder().name("Device").profile("https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-Device").build();
        deviceSearchConfig.setSearchIncludes(
                INCLUDE_ALL,
                REVINCLUDE_HEALTHCARESERVICE_ORGANIZATION
        );
        deviceSearchConfig.setSearchRevIncludes(
                REVINCLUDE_DEVICE_ORGANIZATION,
                REVINCLUDE_HEALTHCARESERVICE_ORGANIZATION,
                REVINCLUDE_ORGANIZATION_ENDPOINT,
                REVINCLUDE_ORGANIZATION_PARTOF,
                REVINCLUDE_PRACTITIONERROLE_ORGANIZATION,
                REVINCLUDE_PRACTITIONERROLE_PRACTITIONER
        );

        var deviceParams = new ArrayList<SearchParamConfig>();
        deviceParams.add(SearchParamConfig.builder().name(IAnyResource.SP_RES_ID).urlParameter(IAnyResource.SP_RES_ID).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_T_ID).resourcePaths(List.of(ResourcePathConfig.builder().path("id").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(LAST_UPDATED).urlParameter(LAST_UPDATED).searchType(StorageConstants.INDEX_TYPE_DATE_RANGE).description("").indexName(StorageConstants.INDEX_T_LASTUPDATED).resourcePaths(List.of(ResourcePathConfig.builder().path("meta.lastUpdated").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_IDENTIFIER).urlParameter(Device.SP_IDENTIFIER).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_DEVICE_IDENTIFIER).resourcePaths(List.of(ResourcePathConfig.builder().path("identifier").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_TYPE).urlParameter(Device.SP_TYPE).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_DEVICE_TYPE).resourcePaths(List.of(ResourcePathConfig.builder().path("type").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_STATUS).urlParameter(Device.SP_STATUS).searchType(StorageConstants.INDEX_TYPE_TOKEN).description("").indexName(StorageConstants.INDEX_DEVICE_STATUS).resourcePaths(List.of(ResourcePathConfig.builder().path("status?.toCode()").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_DEVICE_NAME).urlParameter(Device.SP_DEVICE_NAME).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_DEVICE_NAME).resourcePaths(List.of(ResourcePathConfig.builder().path("deviceName|name").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_MANUFACTURER).urlParameter(Device.SP_MANUFACTURER).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_DEVICE_MANUFACTURER).resourcePaths(List.of(ResourcePathConfig.builder().path("manufacturer").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_MODEL).urlParameter(Device.SP_MODEL).searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName(StorageConstants.INDEX_DEVICE_MODEL_NUMBER).resourcePaths(List.of(ResourcePathConfig.builder().path("modelNumber").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_ORGANIZATION).urlParameter(Device.SP_ORGANIZATION).searchType(StorageConstants.INDEX_TYPE_REFERENCE).description("").indexName(StorageConstants.INDEX_DEVICE_OWNER).resourcePaths(List.of(ResourcePathConfig.builder().path("owner").build())).build());
        deviceParams.add(SearchParamConfig.builder().name(Device.SP_LOCATION).urlParameter(Device.SP_LOCATION).searchType(StorageConstants.INDEX_TYPE_REFERENCE).description("").indexName(StorageConstants.INDEX_DEVICE_LOCATION).resourcePaths(List.of(ResourcePathConfig.builder().path("location").build())).build());
        deviceParams.add(SearchParamConfig.builder().name("number-authorization-arhgos").urlParameter("number-authorization-arhgos").searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName("t_number-authorization-arhgos").resourcePaths(List.of(ResourcePathConfig.builder().path("extension.?[#this.url=='https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Device-NumberAuthorizationARHGOS']|value").build())).build());

        deviceSearchConfig.setSearchParams(deviceParams);
        this.getResources().add(deviceSearchConfig);
    }
}
