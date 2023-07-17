/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Organization;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import java.util.List;

/**
 * End-to-end tests for the api.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(Parameterized.class)
@SpringBootTest(classes = SimpleTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class GenericApiTest extends BaseTest {


    /**
     * The Fhir client
     */
    protected static IGenericClient client;
    /**
     * We have to use the rule here because @RunWith is used by Parameterized
     */
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();
    /**
     * context path
     */
    final String fhirPath;

    public GenericApiTest(String path) {
        this.fhirPath = path;
    }

    /**
     * Test the new and the old api
     *
     * @return apis paths
     */
    @Parameterized.Parameters
    public static Iterable<String> data() {
        return List.of("/fhir", "/fhir/v2-alpha");
    }

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
        createSampleData(fhirStoreService, true, true);
    }

    /**
     * Read a resource by id
     */
    @Test
    public void testRead() {
        var device = client.read().resource(Device.class).withId("device1").encodedJson().prettyPrint().execute();
        Assert.assertEquals("model1", device.getModelNumber());
        Assert.assertEquals("device1", device.getIdElement().getIdPart());
    }


    /**
     * Search resources
     */
    @Test
    public void testDeviceSearch() {
        // if we search all:
        var result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().count(2).execute();
        Assert.assertEquals(3, result.getTotal());
        result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.IDENTIFIER.exactly().code("1")).count(2).execute();
        Assert.assertEquals(1, result.getTotal());
    }

    /**
     * Serch resources by string
     */
    @Test
    public void testDeviceNameSearch() {
        var result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.DEVICE_NAME.matches().value("Some dName")).execute();
        Assert.assertEquals(2, result.getTotal());
        result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.DEVICE_NAME.matchesExactly().value("Some dName")).execute();
        Assert.assertEquals(1, result.getTotal());
        result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.DEVICE_NAME.matchesExactly().values("Some dName", "Some dName 2")).execute();
        Assert.assertEquals(2, result.getTotal());
    }

    /**
     * Search resources by token
     */
    @Test
    public void testDeviceTypeSearch() {
        var result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.TYPE.exactly().code("type2")).count(2).execute();
        Assert.assertEquals(2, result.getTotal());
        result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.TYPE.exactly().systemAndCode("http://part1/", "type2")).count(2).execute();
        Assert.assertEquals(1, result.getTotal());
    }

    /**
     * Test the paging
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
     * Test the search of resources with _include
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
     * Search resource by reference
     */
    @Test
    public void testDeviceOrganizationSearch() {
        var result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.ORGANIZATION.hasId("Organization/org1")).count(2).execute();
        Assert.assertEquals(1, result.getTotal());
        result = (Bundle) client.search().forResource(Device.class).encodedJson().prettyPrint().where(Device.ORGANIZATION.hasAnyOfIds("Organization/org1", "Organization/org 2")).count(2).execute();
        Assert.assertEquals(2, result.getTotal());
    }


    /**
     * Test the search with date
     */
    @Test
    public void testOrganizationLastupdatedSearch() {

        // gt search:
        var dateParam = new StringClientParam("_lastUpdated");
        var resultName = (Bundle) client.search().forResource(Organization.class).where(dateParam.matches().value("gt2022-11-01T14:20")).execute();
        Assert.assertEquals(4, resultName.getTotal());

        resultName = (Bundle) client.search().forResource(Organization.class).where(dateParam.matches().value("ge2022-11-01T14:20")).execute();
        Assert.assertEquals(5, resultName.getTotal());

        // before search:
        resultName = (Bundle) client.search().forResource(Organization.class).where(dateParam.matches().value("lt2022-11-01T14:20")).execute();
        Assert.assertEquals(1, resultName.getTotal());

        // between search:
        resultName = (Bundle) client.search().forResource(Organization.class).where(dateParam.matches().value("ge2022-11-01T14:20")).and(dateParam.matches().value("lt2023-03-01")).execute();
        Assert.assertEquals(2, resultName.getTotal());

    }

    /**
     * Create the client with the good port and a Hapi interceptor to add the token in the headers.
     * Note that the token is only used for write operations
     */
    protected void setupClient() {
        client = ctx.newRestfulGenericClient("http://localhost:" + getServerPort() + fhirPath);
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
}
