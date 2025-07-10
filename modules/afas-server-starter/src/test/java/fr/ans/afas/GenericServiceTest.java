/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.fhir.AfasBundleProvider;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test different search
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SimpleTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})

public class GenericServiceTest extends BaseTest {


    @AfterClass
    public static void shutdown() {
        WithMongoTest.clean();
    }

    @After
    public void clean() {
        fhirStoreService.deleteAll();
    }

    /**
     * Setup test context
     */
    @Before
    public void init() {
        createSampleData(fhirStoreService, true, true);
    }

    /**
     * Read a Device by id
     */
    @Test
    public void testRead() {
        var device = (Device) this.fhirStoreService.findById("Device", new IdType("device1"));

        Assert.assertEquals("model1", device.getModelNumber());
        Assert.assertEquals("device1", device.getIdElement().getIdPart());
    }

    /**
     * Search Devices
     */
    @Test
    public void testDeviceSearch() throws BadDataFormatException {
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.setCount(2);

        var selectCount = fhirStoreService.count(selectExpression);
        Assert.assertEquals(3L, (long) selectCount.getTotal());

        selectExpression.fromFhirParams(FhirSearchPath.builder().resource("Device").path(Device.IDENTIFIER.getParamName()).build(), new TokenAndListParam().addAnd(new TokenParam("1")));
        selectCount = fhirStoreService.count(selectExpression);
        Assert.assertEquals(1, (long) selectCount.getTotal());
    }

    /**
     * Serch Devices by name
     */
    @Test
    public void testDeviceNameSearch() throws BadDataFormatException {
        final var deviceName = "Device";
        final var deviceNameParameter = Device.DEVICE_NAME.getParamName();
        var selectExpression = new SelectExpression<>(deviceName, expressionFactory);

        // single match search
        var stringSearchParam = new StringAndListParam().addAnd(new StringParam("Some dName"));
        selectExpression
                .fromFhirParams(
                        FhirSearchPath.builder().resource(deviceName).path(deviceNameParameter).build(),
                        stringSearchParam
                );

        var selectCount = fhirStoreService.count(selectExpression);
        Assert.assertEquals(2, (long) selectCount.getTotal());

        // single exact search
        stringSearchParam = new StringAndListParam().addAnd(new StringParam("Some dName", true));
        selectExpression = new SelectExpression<>(deviceName, expressionFactory);
        selectExpression
                .fromFhirParams(
                        FhirSearchPath.builder().resource(deviceName).path(deviceNameParameter).build(),
                        stringSearchParam
                );

        selectCount = fhirStoreService.count(selectExpression);
        Assert.assertEquals(1, (long) selectCount.getTotal());

        // multiple exact or search
        stringSearchParam = new StringAndListParam()
                .addAnd(new StringOrListParam().addOr(new StringParam("Some dName", true)).addOr(new StringParam("Some dName 2", true)));
        selectExpression = new SelectExpression<>(deviceName, expressionFactory);
        selectExpression
                .fromFhirParams(
                        FhirSearchPath.builder().resource(deviceName).path(deviceNameParameter).build(),
                        stringSearchParam
                );

        selectCount = fhirStoreService.count(selectExpression);
        Assert.assertEquals(2, (long) selectCount.getTotal());
    }

    @Test
    public void testDevicePagination() throws BadDataFormatException {
        final var deviceName = "Device";
        final var deviceNameParameter = Device.DEVICE_NAME.getParamName();
        var selectExpression = new SelectExpression<>(deviceName, expressionFactory);

        // single match search
        var stringSearchParam = new StringAndListParam();

        selectExpression
                .fromFhirParams(
                        FhirSearchPath.builder().resource(deviceName).path(deviceNameParameter).build(),
                        stringSearchParam
                ).setCount(1);

        var pageResult = fhirStoreService.search(null, selectExpression);
        Assert.assertEquals(1, pageResult.getPage().size());

        var device = (Device) pageResult.getPage().get(0);
        var deviceId = device.getIdElement().getIdPart();

        // get page two
        pageResult = fhirStoreService.search(pageResult.getContext(), selectExpression);
        Assert.assertEquals(1, pageResult.getPage().size());

        device = (Device) pageResult.getPage().get(0);
        deviceId = device.getIdElement().getIdPart();
        Assert.assertEquals("device2", deviceId);

        // get page three
        pageResult = fhirStoreService.search(pageResult.getContext(), selectExpression);
        Assert.assertEquals(1, pageResult.getPage().size());

        device = (Device) pageResult.getPage().get(0);
        deviceId = device.getIdElement().getIdPart();
        Assert.assertEquals("device3", deviceId);

        // get page four that doesn't exist
        pageResult = fhirStoreService.search(pageResult.getContext(), selectExpression);
        Assert.assertEquals(0, pageResult.getPage().size());
    }

    /**
     * Search Devices by Organization
     */
    @Test
    public void testDeviceOrganizationSearch() throws BadDataFormatException {
        // single search
        final var resourceName = "Device";
        final var organizationParameter = Device.ORGANIZATION.getParamName();

        var selectExpression = new SelectExpression<>(resourceName, expressionFactory);
        selectExpression.setCount(2);

        var searchParams = FhirSearchPath.builder().resource(resourceName).path(organizationParameter).build();
        var tokenParam = new ReferenceAndListParam().addAnd(new ReferenceOrListParam().add(new ReferenceParam("Organization", "org1")));

        selectExpression.fromFhirParams(searchParams, tokenParam);

        var selectCount = fhirStoreService.count(selectExpression);

        Assert.assertEquals(1, (long) selectCount.getTotal());

        // multiple search
        selectExpression = new SelectExpression<>(resourceName, expressionFactory);
        selectExpression.setCount(2);

        searchParams = FhirSearchPath.builder().resource(resourceName).path(organizationParameter).build();
        tokenParam = new ReferenceAndListParam().addAnd(new ReferenceOrListParam()
                .addOr(new ReferenceParam("Organization", "org1"))
                .addOr(new ReferenceParam("Organization", "org 2")));

        selectExpression.fromFhirParams(searchParams, tokenParam);

        selectCount = fhirStoreService.count(selectExpression);

        Assert.assertEquals(2, (long) selectCount.getTotal());
    }


    /**
     * Test the search of organization with _revinclude (device)
     */
    @Test
    public void testOrganizationRevincludeDeviceSearch() throws BadDataFormatException {
        Set<Include> theRevIncludes = Set.of(new Include("Device:organization"));
        var selectExpression = new SelectExpression<>("Organization", expressionFactory);

        var searchParams = FhirSearchPath.builder().resource("Organization").path("_id").build();
        var tokenParam = new TokenAndListParam().addAnd(new TokenOrListParam().add(new TokenParam("org1")));
        selectExpression.fromFhirParams(searchParams, tokenParam);

        selectExpression.fromFhirParamsRevInclude(theRevIncludes);

        var result = new AfasBundleProvider<>(fhirStoreService, selectExpression, nextUrlManager);

        Assert.assertEquals(2, result.getAllResources().size());
        Assert.assertTrue(result.getAllResources().get(0) instanceof Organization);
        Assert.assertTrue(result.getAllResources().get(1) instanceof Device);
    }

    @Test
    public void testTransactionalBundleHandling() {
        // clean & check that there's no data
        this.clean();
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        var search = fhirStoreService.search(null, selectExpression);
        Assert.assertEquals(0, search.getPage().size());

        // get resources to handle
        var elems = List.of(this.createSampleData(fhirStoreService, false, true));
        var idsPrefixMapping = Map.of(
                "Device", "device",
                "Organization", "org",
                "Practitioner", "p",
                "HealthcareService", "hca",
                "PractitionerRole", "pr"
        );

        for (var resources : elems) {
            // convert to bundle
            var bundle = new Bundle();
            bundle.setType(Bundle.BundleType.TRANSACTION);
            // create:
            bundle.addEntry()
                    .setResource(resources.get(0))
                    .getRequest()
                    .setUrl(resources.get(0).getResourceType().name())
                    .setMethod(Bundle.HTTPVerb.POST);
            // update:
            bundle.addEntry()
                    .setFullUrl(resources.get(1).getResourceType().name() + "/" + resources.get(1).getIdElement().getValue())
                    .setResource(resources.get(1))
                    .getRequest()
                    .setUrl(resources.get(0).getResourceType().name())
                    .setMethod(Bundle.HTTPVerb.PUT);
            // create:
            bundle.addEntry()
                    .setResource(resources.get(2))
                    .getRequest()
                    .setUrl(resources.get(2).getResourceType().name())
                    .setMethod(Bundle.HTTPVerb.POST);
            // create:
            bundle.addEntry()
                    .setResource(resources.get(3))
                    .getRequest()
                    .setUrl(resources.get(3).getResourceType().name())
                    .setMethod(Bundle.HTTPVerb.POST);

            // call transaction
            Bundle response = this.transactionalResourceProvider.transaction(bundle);

            var resp1 = (Bundle.BundleEntryResponseComponent) response.getEntry().get(0).getResponse();
            var resp2 = (Bundle.BundleEntryResponseComponent) response.getEntry().get(1).getResponse();
            var resp3 = (Bundle.BundleEntryResponseComponent) response.getEntry().get(2).getResponse();
            var resp4 = (Bundle.BundleEntryResponseComponent) response.getEntry().get(3).getResponse();

            Assert.assertEquals(4, response.getTotal());

            var resourceName = resources.get(0).getResourceType().name();
            // we only test id for update because created id are generated
            Assert.assertTrue(resp1.getLocation().startsWith(resourceName));
            Assert.assertTrue(resp2.getLocation().startsWith(resourceName));
            Assert.assertTrue(resp3.getLocation().endsWith("/" + idsPrefixMapping.get(resourceName) + "2"));

            selectExpression = new SelectExpression<>(resourceName, expressionFactory);

            search = fhirStoreService.search(null, selectExpression);
            Assert.assertEquals(3, search.getPage().size());

            resourceName = resources.get(3).getResourceType().name();
            Assert.assertTrue(resp4.getLocation().startsWith(resourceName));

            selectExpression = new SelectExpression<>(resourceName, expressionFactory);

            search = fhirStoreService.search(null, selectExpression);
            Assert.assertEquals(1, search.getPage().size());
        }
    }


}
