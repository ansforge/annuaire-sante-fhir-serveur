/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.JoinPath;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.IndexService;
import fr.ans.afas.fhirserver.service.exception.IndexingException;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import fr.ans.afas.rass.service.impl.DefaultIndexService;
import fr.ans.afas.rass.service.impl.MongoQueryUtils;
import fr.ans.afas.rass.service.impl.exception.AlreadyRunningTaskException;
import fr.ans.afas.rass.service.json.GenericSerializer;
import org.awaitility.Awaitility;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Test the index service
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})

public class IndexResourcesIT {

    @Inject
    MongoDbFhirService mongoDbFhirService;

    /**
     * The test db name
     */
    @Value("${spring.data.mongodb.database}")
    String dbName;

    /**
     * The expression factory
     */
    @Inject
    ExpressionFactory<Bson> expressionFactory;


    @Inject
    IndexService indexService;

    @Inject
    GenericSerializer genericSerializer;

    @Inject
    MongoClient mongoClient;

    @Inject
    MongoMultiTenantService multiTenantService;

    /**
     * Stop docker
     */
    @AfterClass
    public static void shutdown() {
        WithMongoTest.clean();
    }

    @Before
    public void init() {
        mongoDbFhirService.deleteAll();
        this.initData();
    }


    /**
     * Given a bad configuration
     * When we launch the indexing job
     * Then we get an exception with the message "Search not supported on path..."
     */
    @Test
    public void badConfigurationTest() {

        var badSearchConfig = new CompositeSearchConfigService(List.of(new TenantSearchConfig() {{
            var organizationSearchConfig = FhirResourceSearchConfig.builder().name("Organization").build();
            var jp = new JoinPath();
            jp.setResource("Device");
            jp.setPath("badPath");
            jp.setField("badParam");
            organizationSearchConfig.setJoins(List.of(jp));
            this.getResources().add(organizationSearchConfig);
        }}));


        var defaultIndexService = new DefaultIndexService(mongoDbFhirService, expressionFactory, badSearchConfig, genericSerializer);
        var ex = Assert.assertThrows(IndexingException.class, () -> defaultIndexService.refreshIndexesSync(1));
        Assert.assertEquals("Search not supported on path: Device.badPath", ex.getCause().getMessage());
    }


    /**
     * Given a running job (the user launched the indexing)
     * When we launch the indexing job
     * Then we get an exception with the message "Indexing already running"
     */
    @Test
    public void doubleRunTest() throws InterruptedException, ExecutionException {
        var executor = Executors.newFixedThreadPool(2);
        var a = executor.submit(() -> indexService.refreshIndexes(1));
        var b = executor.submit(() -> indexService.refreshIndexes(1));

        Exception exceptionThrow = null;
        var done = false;

        try {
            a.get();
            done = true;
        } catch (Exception e) {
            exceptionThrow = e;
        }
        try {
            b.get();
            done = true;
        } catch (Exception e) {
            exceptionThrow = e;
        }

        Assert.assertTrue(done);
        Assert.assertNotNull(exceptionThrow);
        Assert.assertEquals("Indexing already running", exceptionThrow.getCause().getMessage());
        Assert.assertTrue(exceptionThrow.getCause() instanceof AlreadyRunningTaskException);
    }


    /**
     * Given a "fast join" in the configuration
     * When we launch the indexing
     * Then the sub resource is indexed in the parent resource
     */
    @Test
    public void nominalCaseTest() {
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> !indexService.isRunning());
        this.indexService.refreshIndexes(1);
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> !indexService.isRunning());
        var coll = multiTenantService.getCollection("Organization");
        var found = coll.find(MongoQueryUtils.wrapQueryWithRevisionDate(new Date().getTime(), Filters.eq(StorageConstants.INDEX_T_ID, "org1"))).first();
        var links = (Document) found.get("links");
        var devices = (ArrayList<Document>) links.get("Device");
        Assert.assertNotNull(devices);
        Assert.assertEquals(1, devices.size());
        var device = devices.get(0);
        Assert.assertEquals("Device/i1", device.get(StorageConstants.INDEX_T_FID));
        Assert.assertEquals("org1", ((ArrayList) device.get("t_owner-id")).get(0));
        Assert.assertEquals("1234", ((ArrayList) device.get("t_name")).get(0));

    }


    private void initData() {
        // Device:
        var device1 = new Device();
        device1.setId("i1");
        device1.addDeviceName().setName("1234");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        device1.setOwner(owner1);
        this.mongoDbFhirService.store(List.of(device1), false);

        // Organization:
        var org1 = new Organization();
        org1.setId("org1");
        this.mongoDbFhirService.store(List.of(org1), false);
        var org2 = new Organization();
        org2.setId("org2");
        this.mongoDbFhirService.store(List.of(org2), false);
    }


}
