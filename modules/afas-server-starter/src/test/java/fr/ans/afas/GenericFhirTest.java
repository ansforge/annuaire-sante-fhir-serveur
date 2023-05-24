/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.fhir.AfasBundleProvider;
import fr.ans.afas.fhir.TransactionalResourceProvider;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;


@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SimpleTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class GenericFhirTest {

    /**
     * The Fhir context
     */
    protected static final FhirContext ctx = FhirContext.forR4();
    /**
     * The Fhir client
     */
    protected static IGenericClient client;

    /**
     * The port of the server used in tests
     */
    @LocalServerPort
    int port;

    @Inject
    TransactionalResourceProvider<?> transactionalResourceProvider;

    /**
     * Service to access fhir data
     */
    @Inject
    private FhirStoreService<Bson> fhirStoreService;

    @Inject
    private NextUrlManager<Bson> nextUrlManager;


    @Inject
    private ExpressionFactory<Bson> expressionFactory;

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
        setupClient();
        createSampleData(true, true);
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
    public void testDeviceSearch() {
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
    public void testDeviceNameSearch() {
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
    public void testDevicePagination() {
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
    public void testDeviceOrganizationSearch() {
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
     * Test the paging of Devices
     */
    @Test
    public void testDevicePagingSearch() {
        var bundle = (Bundle) client.search().forResource(Device.class)
                .count(1)
                .execute();
        // page 1:
        Assert.assertEquals(1, bundle.getEntry().size());
        // page 2:
        bundle.getLink("next").setUrl(bundle.getLink("next").getUrl().replaceAll("http://localhost:8080", "http://localhost:" + this.getServerPort()));
        bundle = client.loadPage().next(bundle).execute();
        Assert.assertEquals(1, bundle.getEntry().size());
        // page 3:
        bundle.getLink("next").setUrl(bundle.getLink("next").getUrl().replaceAll("http://localhost:8080", "http://localhost:" + this.getServerPort()));
        bundle = client.loadPage().next(bundle).execute();
        Assert.assertEquals(1, bundle.getEntry().size());
        // page 4 doesnt exit:
        Assert.assertNull(bundle.getLink("next"));
    }

    /**
     * Test the search of device with _include
     */
    @Test
    public void testDeviceIncludeSearch() {
        var resultName = (Bundle) client.search().forResource(Device.class).include(Device.INCLUDE_ORGANIZATION.asNonRecursive()).where(Device.IDENTIFIER.exactly().codes("1")).execute();
        Assert.assertEquals(1, resultName.getTotal());
        Assert.assertEquals(2, resultName.getEntry().size());

        var resultNameAll = (Bundle) client.search().forResource(Device.class).include(Device.INCLUDE_ALL.asNonRecursive()).where(Device.IDENTIFIER.exactly().codes("1")).execute();
        Assert.assertEquals(1, resultNameAll.getTotal());
        Assert.assertEquals(2, resultNameAll.getEntry().size());
    }

    /**
     * Test the search of organization with _revinclude (device)
     */
    @Test
    public void testOrganizationRevincludeDeviceSearch() {
        Set<Include> theRevIncludes = Set.of(new Include("Device:organization"));
        var selectExpression = new SelectExpression<>("Organization", expressionFactory);
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
        var elems = List.of(this.createSampleData(false, true));
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

    /**
     * Create the client with the good port and a Hapi interceptor to add the token in the headers.
     * Note that the token is only used for write operations
     */
    protected void setupClient() {
        client = ctx.newRestfulGenericClient("http://localhost:" + getServerPort() + "/fhir");
        client.registerInterceptor(new LoggingInterceptor(false));
    }

    /**
     * Get the port of the server
     *
     * @return the port of the server
     */
    protected int getServerPort() {
        return this.port;
    }


    protected List<DomainResource> createSampleData(boolean store, boolean createOrganization) {
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

        if (store) {
            this.fhirStoreService.store(List.of(rassDevice1, rassDevice2, rassDevice3), false);
        }

        if (createOrganization) {
            var organization1 = new Organization();
            organization1.setId("org1");
            var partOf1 = new Reference();
            partOf1.setReference("Organization/org2");
            organization1.setPartOf(partOf1);

            if (store) {
                this.fhirStoreService.store(List.of(organization1), false);
            }

            return List.of(rassDevice1, rassDevice2, rassDevice3, organization1);
        }

        return List.of(rassDevice1, rassDevice2, rassDevice3);
    }
}
