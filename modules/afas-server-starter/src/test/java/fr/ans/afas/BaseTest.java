/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.TransactionalResourceProvider;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.utils.TenantUtil;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.*;
import org.springframework.boot.test.web.server.LocalServerPort;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class to create some generic data
 */
public abstract class BaseTest {

    /**
     * The Fhir context
     */
    protected static final FhirContext ctx = FhirContext.forR4();
    private static final String TEST_TENANT_1 = "tenant-1";
    /**
     * Date formater to use dates in tests
     */
    final DateFormat dateFormatForTests = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
    @Inject
    protected TransactionalResourceProvider<?> transactionalResourceProvider;
    /**
     * Service to access fhir data
     */
    @Inject
    protected FhirStoreService<Bson> fhirStoreService;
    @Inject
    protected NextUrlManager<Bson> nextUrlManager;
    @Inject
    protected ExpressionFactory<Bson> expressionFactory;
    /**
     * The port of the server used in tests
     */
    @LocalServerPort
    int port;

    protected List<DomainResource> createSampleData(FhirStoreService<Bson> fhirStoreService, boolean store, boolean createOrganization) {
        TenantUtil.setCurrentTenant(TEST_TENANT_1);
        var extArhgos = "https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Device-NumberAuthorizationARHGOS";
        var rassDevice1 = new Device();
        String DEVICE_ID_1 = "device1";
        rassDevice1.setId(DEVICE_ID_1);
        rassDevice1.addIdentifier().setSystem("http://samplesysyem").setValue("1");
        rassDevice1.addDeviceName().setName("Some dName");
        var location1 = new Reference();
        location1.setReference("Location/loc1");
        rassDevice1.setLocation(location1);
        rassDevice1.setManufacturer("man1");
        rassDevice1.setModelNumber("model1");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        rassDevice1.setOwner(owner1);
        var type = new CodeableConcept();
        type.addCoding().setSystem("http://types/").setCode("type1");
        type.addCoding().setSystem("http://part1/").setCode("other1");
        rassDevice1.setType(type);
        rassDevice1.setStatus(Device.FHIRDeviceStatus.ACTIVE);
        rassDevice1.addExtension().setUrl(extArhgos).setValue(new StringType("56565.6456.45789531230001"));

        var rassDevice2 = new Device();
        String deviceId2 = "device2";
        rassDevice2.setId(deviceId2);
        rassDevice2.addDeviceName().setName("Some dName 2");
        rassDevice2.addIdentifier().setSystem("http://samplesysyem").setValue("2");
        var location2 = new Reference();
        location2.setReference("Location/loc 2");
        rassDevice2.setLocation(location2);
        rassDevice2.setManufacturer("man 2");
        rassDevice2.setModelNumber("model 2");
        var owner2 = new Reference();
        owner2.setReference("Organization/org 2");
        rassDevice2.setOwner(owner2);
        var type2 = new CodeableConcept();
        type2.addCoding().setSystem("http://types/").setCode("type2");
        type2.addCoding().setSystem("http://part1/").setCode("other2");
        rassDevice2.setType(type2);
        rassDevice2.setStatus(Device.FHIRDeviceStatus.INACTIVE);
        rassDevice2.addExtension().setUrl(extArhgos).setValue(new StringType("56565.6456.45789531230002"));

        var rassDevice3 = new Device();
        String DEVICE_ID_3 = "device3";
        rassDevice3.setId(DEVICE_ID_3);
        rassDevice3.addIdentifier().setSystem("http://samplesysyem").setValue("3");
        var location3 = new Reference();
        location3.setReference("Location/loc3");
        rassDevice3.setLocation(location3);
        rassDevice3.setManufacturer("man3");
        rassDevice3.setModelNumber("model3");
        var owner3 = new Reference();
        owner3.setReference("Organization/org3");
        rassDevice3.setOwner(owner3);
        var type3 = new CodeableConcept();
        type3.addCoding().setSystem("http://types/").setCode("type2");
        type3.addCoding().setSystem("http://part1/").setCode("other3");
        type3.addCoding().setSystem("http://part1/").setCode("type2");
        rassDevice3.setType(type3);
        rassDevice3.setStatus(Device.FHIRDeviceStatus.INACTIVE);


        var toSave = new ArrayList<DomainResource>();
        toSave.add(rassDevice1);
        toSave.add(rassDevice2);
        toSave.add(rassDevice3);

        if (store) {
            fhirStoreService.store(List.of(rassDevice1, rassDevice2, rassDevice3), false);
        }

        if (createOrganization) {
            var orgs = createSampleOrganizations();
            toSave.addAll(orgs);
            if (store) {
                fhirStoreService.store(orgs, false);
            }
        }


        return toSave;
    }


    /**
     * Create sample organizations
     *
     * @return sample organizations
     */
    protected List<Organization> createSampleOrganizations() {
        try {
            var organization1 = new Organization();
            organization1.setId("org1");
            organization1.getMeta().setLastUpdated(dateFormatForTests.parse("2022-12-01-14:20"));
            organization1.addType().addCoding().setSystem("http://ssss/").setCode("type3");
            organization1.addIdentifier().setSystem("http://samplesysyem").setValue("1").setType(new CodeableConcept(new Coding("https://mos.esante.gouv.fr/NOS/TRE_G07-TypeIdentifiantStructure/FHIR/TRE-G07-TypeIdentifiantStructure", "0", null)));
            organization1.setActiveElement(new BooleanType(true));
            organization1.setName("Some name of organization");
            organization1.addAddress().setCity("City one").setCountry("Country one").setPostalCode("Postal code one").setState("State one").setUse(Address.AddressUse.HOME);
            organization1.addExtension().setUrl("https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Organization-PharmacyLicence").setValue(new StringType("LICENCE1"));
            var partOf1 = new Reference();
            partOf1.setReference("Organization/org2");
            organization1.setPartOf(partOf1);

            var organization2 = new Organization();
            organization2.setId("org2");
            organization2.getMeta().setLastUpdated(dateFormatForTests.parse("2022-11-01-14:20"));
            organization2.addType().addCoding().setSystem("http://ssss/").setCode("type1");
            organization2.getTypeFirstRep().addCoding().setSystem("http://ssss/").setCode("type2");
            organization2.addIdentifier().setSystem("http://samplesysyem").setValue("2").setType(new CodeableConcept(new Coding("https://mos.esante.gouv.fr/NOS/TRE_G07-TypeIdentifiantStructure/FHIR/TRE-G07-TypeIdentifiantStructure", "1", null)));
            organization2.setActive(false);
            organization2.setName("Other name of organization");
            organization2.addAddress().setCity("City two").setCountry("Country  two").setPostalCode("Postal code  two").setState("State  two").setUse(Address.AddressUse.WORK);
            organization2.addExtension().setUrl("https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Organization-PharmacyLicence").setValue(new StringType("LICENCE2"));
            var partOf3 = new Reference();
            partOf3.setReference("Organization/org3");
            organization2.setPartOf(partOf3);

            var organization3 = new Organization();
            organization3.setId("org3");
            organization3.getMeta().setLastUpdated(dateFormatForTests.parse("2022-10-01-14:20"));
            organization3.addType().addCoding().setSystem("http://ssss/").setCode("type1");
            organization3.setName("Third");
            var org1Ref = new Reference();
            org1Ref.setReference("Organization/org1");
            organization3.setEndpoint(List.of(org1Ref));
            var partOf2 = new Reference();
            partOf2.setReference("Organization/org2");
            organization3.setPartOf(partOf2);
            organization3.addExtension().setUrl("https://annuaire.sante.gouv.fr/fhir/StructureDefinition/Organization-PharmacyLicence").setValue(new StringType("LICENCE3"));

            // to test dates:
            var organization4 = new Organization();
            organization4.setId("org4");
            organization4.getMeta().setLastUpdated(dateFormatForTests.parse("2122-04-10-10:20"));
            var organization5 = new Organization();
            organization5.setId("org5");
            organization5.getMeta().setLastUpdated(dateFormatForTests.parse("2122-04-11-10:20"));
            var organization6 = new Organization();
            organization6.getMeta().setLastUpdated(dateFormatForTests.parse("2123-04-11-10:20"));
            organization6.setId("org6");

            return List.of(organization1, organization2, organization3, organization4, organization5, organization6);
        } catch (Exception e) {
            throw new RuntimeException("Error generating sample data on organization", e);
        }
    }
}
