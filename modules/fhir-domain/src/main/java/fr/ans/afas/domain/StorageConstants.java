/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.domain;

/**
 * Constants used in database configuration
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public final class StorageConstants {

    public static final String INDEX_TYPE_TOKEN = "token";
    public static final String INDEX_TYPE_STRING = "string";
    public static final String INDEX_TYPE_REFERENCE = "reference";
    public static final String INDEX_TYPE_DATE_RANGE = "date";
    public static final String INDEX_T_ID = "t_id";
    public static final String INDEX_T_FID = "t_fid";
    public static final String INDEX_T_PROFILE = "t_profile";
    public static final String INDEX_T_LASTUPDATED = "t_lastupdated";
    public static final String INDEX_T_LASTUPDATED_SECOND = "t_lastupdated-second";
    public static final String INDEX_T_LASTUPDATED_MINUTE = "t_lastupdated-minute";
    public static final String INDEX_T_LASTUPDATED_DATE = "t_lastupdated-day";
    public static final String INDEX_T_LASTUPDATED_MONTH = "t_lastupdated-month";
    public static final String INDEX_T_LASTUPDATED_YEAR = "t_lastupdated-year";
    public static final String INDEX_T_IDENTIFIER = "t_identifier";
    public static final String VALUE_SUFFIX = "-value";
    public static final String SYSTEM_SUFFIX = "-system";
    public static final String SYSVAL_SUFFIX = "-sysval";
    public static final String INSENSITIVE_SUFFIX = "-i";
    public static final String ID_SUFFIX = "-id";
    public static final String REFERENCE_SUFFIX = "-reference";
    public static final String TYPE_SUFFIX = "-type";
    public static final String CITY_SUFFIX = "-city";
    public static final String COUNTY_SUFFIX = "-country";
    public static final String POSTALCODE_SUFFIX = "-postalcode";
    public static final String STATE_SUFFIX = "-state";
    public static final String USE_VALUE_SUFFIX = "-use-value";
    public static final String HUMAN_NAME_PREFIX_SUFFIX = "-prefix";
    public static final String HUMAN_NAME_SUFFIX_SUFFIX = "-suffix";
    public static final String HUMAN_NAME_FAMILY_SUFFIX = "-family";
    public static final String HUMAN_NAME_GIVEN_SUFFIX = "-given";
    // Organization
    public static final String INDEX_ORGANIZATION_TYPE = "t_type";

    //////////////////////////
    // Specific indexes:
    //////////////////////////
    public static final String INDEX_ORGANIZATION_IDENTIFIER = "t_identifier";
    public static final String INDEX_ORGANIZATION_NAME = "t_name";

    public static final String INDEX_ORGANIZATION_MAILBOX_MSS = "t_mailbox-mss";
    public static final String INDEX_ORGANIZATION_PARTOF = "t_partof";
    public static final String INDEX_ORGANIZATION_ACTIVE = "t_active";
    public static final String INDEX_ORGANIZATION_ADDRESS = "t_address";
    public static final String INDEX_ORGANIZATION_ADDRESS_CITY = "t_address-city";
    public static final String INDEX_ORGANIZATION_ADDRESS_COUNTRY = "t_address-country";
    public static final String INDEX_ORGANIZATION_ADDRESS_POSTALCODE = "t_address-postalcode";
    public static final String INDEX_ORGANIZATION_ADDRESS_STATE = "t_address-state";
    public static final String INDEX_ORGANIZATION_ADDRESS_USE = "t_address-use";
    public static final String INDEX_ORGANIZATION_ENDPOINT = "t_endpoint";
    // Device
    public static final String INDEX_DEVICE_IDENTIFIER = "t_identifier";
    public static final String INDEX_DEVICE_NAME = "t_name";
    public static final String INDEX_DEVICE_LOCATION = "t_location";
    public static final String INDEX_DEVICE_MANUFACTURER = "t_manufacturer";
    public static final String INDEX_DEVICE_MODEL_NUMBER = "t_model-number";
    public static final String INDEX_DEVICE_OWNER = "t_owner";
    public static final String INDEX_DEVICE_STATUS = "t_status";
    public static final String INDEX_DEVICE_TYPE = "t_type";
    // Practitioner
    public static final String INDEX_PRACTITIONER_IDENTIFIER = "t_identifier";
    public static final String INDEX_PRACTITIONER_ACTIVE = "t_active";
    public static final String INDEX_PRACTITIONER_NAME = "t_name";

    // Healthcare service
    public static final String INDEX_HEALTHCARESERVICE_IDENTIFIER = "t_identifier";
    public static final String INDEX_HEALTHCARESERVICE_ACTIVE = "t_active";
    public static final String INDEX_HEALTHCARESERVICE_CHARACTERISTIC = "t_characteristic";
    public static final String INDEX_HEALTHCARESERVICE_CATEGORY = "t_category";
    public static final String INDEX_HEALTHCARESERVICE_TYPE = "t_type";
    public static final String INDEX_HEALTHCARESERVICE_PROVIDED_BY = "t_provided-by";


    // Practitioner Role
    public static final String INDEX_PRACTITIONER_ROLE_IDENTIFIER = "t_identifier";
    public static final String INDEX_PRACTITIONER_ROLE_ACTIVE = "t_active";
    public static final String INDEX_PRACTITIONER_ROLE_SPECIALTY = "t_specialty";
    public static final String INDEX_PRACTITIONER_ROLE_PRACTITIONER = "t_practitioner";
    public static final String INDEX_PRACTITIONER_ROLE_ORGANIZATION = "t_organization";
    public static final String INDEX_PRACTITIONER_ROLE_PARTOF = "t_partof";
    public static final String INDEX_PRACTITIONER_ROLE_NAME = "t_name";
    public static final String INDEX_PRACTITIONER_ROLE_NAME_PREFIX = "t_name-prefix";
    public static final String INDEX_PRACTITIONER_ROLE_NAME_SUFFIX = "t_name-suffix";
    public static final String INDEX_PRACTITIONER_ROLE_FAMILY = "t_family";
    public static final String INDEX_PRACTITIONER_ROLE_GIVEN = "t_given";
    public static final String INDEX_PRACTITIONER_ROLE_ROLE = "t_role";


    private StorageConstants() {
    }
}
