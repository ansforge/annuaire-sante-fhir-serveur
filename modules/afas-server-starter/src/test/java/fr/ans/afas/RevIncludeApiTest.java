/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Organization;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * End-to-end tests for the api.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SimpleTestApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})

public class RevIncludeApiTest extends BaseTest {


    /**
     * The Fhir client
     */
    protected static IGenericClient client;
    /**
     * context path
     */
    final String fhirPath = "/fhir/" + HttpUtils.SERVLET_API_PATH + "/tenant-1";

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

    @Test
    public void testOrganizationRevIncludeSearch() {
        var resultName = (Bundle) client.search().forResource(Organization.class).revInclude(Device.INCLUDE_ORGANIZATION.asNonRecursive()).where(Organization.IDENTIFIER.exactly().codes("1")).execute();
        Assert.assertEquals(1, resultName.getTotal());
        Assert.assertEquals(2, resultName.getEntry().size());
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
