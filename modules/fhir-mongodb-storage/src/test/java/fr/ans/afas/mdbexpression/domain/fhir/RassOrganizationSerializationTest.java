/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.fasterxml.jackson.core.JsonFactory;
import com.jayway.jsonpath.JsonPath;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfig;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.mdbexpression.domain.fhir.searchconfig.ASComplexSearchConfig;
import fr.ans.afas.rass.service.json.GenericSerializer;
import fr.ans.afas.utils.FhirDateUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Test the serialization of {@link Organization} for MongoDB
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
public class RassOrganizationSerializationTest {

    /**
     * Config used for tests
     */
    final SearchConfig searchConfig = new CompositeSearchConfig(List.of(new ASComplexSearchConfig()));
    /**
     * Id of the test organization
     */
    final String ORGANIZATION_1_ID = "org1";
    /**
     * Last updated of the test organization
     */
    final Date ORGANIZATION_1_LAST_UPDATED = new Date();
    /**
     * System of the type of the test organization
     */
    final String ORGANIZATION_1_TYPE_SYSTEM = "http://ssss/";
    /**
     * Value of the type of the test organization
     */
    final String ORGANIZATION_1_TYPE_VALUE = "type3";
    /**
     * System of the identifier of the test organization
     */
    final String ORGANIZATION_1_IDENTIFIER_SYSTEM = "http://samplesysyem";
    /**
     * Value of the identifier of the test organization
     */
    final String ORGANIZATION_1_IDENTIFIER_VALUE = "1";
    /**
     * Name of the test organization
     */
    final String ORGANIZATION_1_NAME = "Some name of organization";
    /**
     * City of the address of the test organization
     */
    final String ORGANIZATION_1_ADDRESS_CITY = "City one";
    /**
     * Country of the address of the test organization
     */
    final String ORGANIZATION_1_ADDRESS_COUNTRY = "Country one";
    /**
     * Postal code of the address of the test organization
     */
    final String ORGANIZATION_1_ADDRESS_POSTAL_CODE = "Postal code one";
    /**
     * State of the address of the test organization
     */
    final String ORGANIZATION_1_ADDRESS_STATE = "State one";
    /**
     * The use (fhir) of the address of the test organization
     */
    final Address.AddressUse ORGANIZATION_1_ADDRESS_USE = Address.AddressUse.HOME;
    /**
     * The Pharmacy Licence of the test organization
     */
    final String ORGANIZATION_1_PHARMACY_LICENCE = "LICENCE1";
    /**
     * The "part of" reference type for the test organization
     */
    final String ORGANIZATION_1_PART_OF_TYPE = "Organization";
    /**
     * The "part of" reference id for the test organization
     */
    final String ORGANIZATION_1_PART_OF_ID = "org2";
    /**
     * The serializer
     */
    final GenericSerializer rassOrganizationResourceSerializer = new GenericSerializer(searchConfig, FhirContext.forR4());

    /**
     * Test the organization Serialization against the default SearchConfigService.
     *
     * @throws IOException if an error occur with the parser
     */
    @Test
    public void testDefaultFields() throws IOException {
        var tz = TimeZone.getDefault();
        var offset = ZoneOffset.ofTotalSeconds(tz.getRawOffset() / 1000);
        var writer = new StringWriter();
        var factory = new JsonFactory();
        var generator = factory.createGenerator(writer);
        rassOrganizationResourceSerializer.serialize(getSampleOrganization(), generator, null);
        generator.close();

        var jsonAsString = writer.toString();
        var jsonContext = JsonPath.parse(jsonAsString);

        // test all indexes fields:
        Assert.assertEquals(ORGANIZATION_1_ID, jsonContext.read("$." + StorageConstants.INDEX_T_ID));
        Assert.assertEquals((ORGANIZATION_1_LAST_UPDATED.getTime()) / 1000L + offset.getTotalSeconds(), ((Integer) jsonContext.read("$." + StorageConstants.INDEX_T_LASTUPDATED)).longValue());
        Assert.assertEquals(FhirDateUtils.getTimeInPrecision(ORGANIZATION_1_LAST_UPDATED, TemporalPrecisionEnum.SECOND), ((Integer) jsonContext.read("$." + StorageConstants.INDEX_T_LASTUPDATED_SECOND)).longValue());
        Assert.assertEquals(FhirDateUtils.getTimeInPrecision(ORGANIZATION_1_LAST_UPDATED, TemporalPrecisionEnum.MINUTE), ((Integer) jsonContext.read("$." + StorageConstants.INDEX_T_LASTUPDATED_MINUTE)).longValue());
        Assert.assertEquals(FhirDateUtils.getTimeInPrecision(ORGANIZATION_1_LAST_UPDATED, TemporalPrecisionEnum.DAY), ((Integer) jsonContext.read("$." + StorageConstants.INDEX_T_LASTUPDATED_DATE)).longValue());
        Assert.assertEquals(FhirDateUtils.getTimeInPrecision(ORGANIZATION_1_LAST_UPDATED, TemporalPrecisionEnum.MONTH), ((Integer) jsonContext.read("$." + StorageConstants.INDEX_T_LASTUPDATED_MONTH)).longValue());
        Assert.assertEquals(FhirDateUtils.getTimeInPrecision(ORGANIZATION_1_LAST_UPDATED, TemporalPrecisionEnum.YEAR), ((Integer) jsonContext.read("$." + StorageConstants.INDEX_T_LASTUPDATED_YEAR)).longValue());

        Assert.assertEquals(ORGANIZATION_1_TYPE_SYSTEM, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_TYPE + StorageConstants.SYSTEM_SUFFIX + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_TYPE_VALUE, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_TYPE + StorageConstants.VALUE_SUFFIX + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_TYPE_SYSTEM + "|" + ORGANIZATION_1_TYPE_VALUE, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_TYPE + StorageConstants.SYSVAL_SUFFIX + "[0]"));

        Assert.assertEquals(ORGANIZATION_1_IDENTIFIER_SYSTEM, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_IDENTIFIER + StorageConstants.SYSTEM_SUFFIX + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_IDENTIFIER_VALUE, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_IDENTIFIER + StorageConstants.VALUE_SUFFIX + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_IDENTIFIER_SYSTEM + "|" + ORGANIZATION_1_IDENTIFIER_VALUE, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_IDENTIFIER + StorageConstants.SYSVAL_SUFFIX + "[0]"));

        Assert.assertEquals(ORGANIZATION_1_NAME, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_NAME + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_ADDRESS_CITY, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_ADDRESS_CITY + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_ADDRESS_COUNTRY, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_ADDRESS_COUNTRY + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_ADDRESS_POSTAL_CODE, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_ADDRESS_POSTALCODE + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_ADDRESS_STATE, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_ADDRESS_STATE + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_ADDRESS_USE.toCode(), jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_ADDRESS_USE + "-value[0]"));
        Assert.assertEquals(ORGANIZATION_1_PHARMACY_LICENCE, jsonContext.read("$." + "t_pharmacy-licence" + "[0]"));
        Assert.assertEquals(ORGANIZATION_1_PART_OF_TYPE + "/" + ORGANIZATION_1_PART_OF_ID, jsonContext.read("$.t_partof-reference[0]"));
        Assert.assertEquals(ORGANIZATION_1_PART_OF_ID, jsonContext.read("$." + StorageConstants.INDEX_ORGANIZATION_PARTOF + "-id[0]"));

    }

    /**
     * Create a sample organization for tests
     *
     * @return the sample organization
     */
    private Organization getSampleOrganization() {
        var organization1 = new Organization();
        organization1.setId(ORGANIZATION_1_ID);
        organization1.getMeta().setLastUpdated(ORGANIZATION_1_LAST_UPDATED);
        organization1.addType().addCoding().setSystem(ORGANIZATION_1_TYPE_SYSTEM).setCode(ORGANIZATION_1_TYPE_VALUE);
        organization1.addIdentifier().setSystem(ORGANIZATION_1_IDENTIFIER_SYSTEM).setValue(ORGANIZATION_1_IDENTIFIER_VALUE);
        organization1.setActiveElement(new BooleanType(true));
        organization1.setName(ORGANIZATION_1_NAME);
        organization1.addAddress().setCity(ORGANIZATION_1_ADDRESS_CITY).setCountry(ORGANIZATION_1_ADDRESS_COUNTRY).setPostalCode(ORGANIZATION_1_ADDRESS_POSTAL_CODE).setState(ORGANIZATION_1_ADDRESS_STATE).setUse(ORGANIZATION_1_ADDRESS_USE);
        organization1.addExtension().setUrl("https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Organization-PharmacyLicence").setValue(new StringType(ORGANIZATION_1_PHARMACY_LICENCE));
        var partOf = new Reference();
        partOf.setReference(ORGANIZATION_1_PART_OF_TYPE + "/" + ORGANIZATION_1_PART_OF_ID);
        organization1.setPartOf(partOf);
        return organization1;
    }


}
