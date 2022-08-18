/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search;

/**
 * Constants used in the Fhir Server
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public final class FhirServerConstants {


    // EXTENTIONS:
    public static final String MSS_EXTENSION_URL = "https://annuaire.sante.gouv.fr/fhir/StructureDefinition/MailboxMSS";
    public static final String MSS_EXTENSION_URL_VALUE = "value";
    public static final String MSS_EXTENSION_URL_TYPE = "type";
    public static final String MSS_EXTENSION_URL_SERVICE = "service";
    public static final String MSS_EXTENSION_URL_DIGITIZATION = "digitization";
    public static final String MSS_EXTENSION_URL_DESCRIPTION = "description";

    public static final String DEVICE_FHIR_RESOURCE_NAME = "Device";
    public static final String HEALTHCARESERVICE_FHIR_RESOURCE_NAME = "HealthcareService";
    public static final String ORGANIZATION_FHIR_RESOURCE_NAME = "Organization";
    public static final String PRACTITIONER_FHIR_RESOURCE_NAME = "Practitioner";
    public static final String PRACTITIONERROLE_FHIR_RESOURCE_NAME = "PractitionerRole";
    public static final String PRACTITIONER_ROLE_FHIR_RESOURCE_NAME = "PractitionerRole";


    // PRACTITIONER ROLE
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_IDENTIFIER = "identifier";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_LAST_UPDATED = "_lastUpdated";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_ACTIVE = "active";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_ROLE = "role";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_SPECIALTY = "specialty";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_TYPE_SMARTCARD = "type-smartcard";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_PRACTITIONER = "practitioner";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_ORGANIZATION = "organization";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_NAME_PREFIX = "name.prefix";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_NAME_SUFFIX = "name.suffix";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_FAMILY = "family";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_GIVEN = "given";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_MAILBOX_MSS = "mailbox-mss";
    public static final String PRACTITIONER_ROLE_PARAM_CONFIG_PATH_NB_SMARTCARD = "number-smartcard";


    private FhirServerConstants() {
    }
}
