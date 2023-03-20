/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import com.mongodb.client.MongoClient;
import fr.ans.afas.config.CleanRevisionDataConfiguration;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import org.hl7.fhir.r4.model.Device;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test the deletion of old revisions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class DeleteOldRevisionsIT {


    /**
     * The test db name
     */
    @Value("${spring.data.mongodb.database}")
    String dbName;

    /**
     * The configuration that schedule deletion of old revisions
     */
    @Autowired
    CleanRevisionDataConfiguration cleanRevisionDataConfiguration;

    /**
     * Service to access fhir data
     */
    @Autowired
    MongoDbFhirService mongoDbFhirService;

    /**
     * The mongodb client
     */
    @Autowired
    MongoClient mongoClient;

    @Before
    public void init() {
        mongoDbFhirService.deleteAll();
    }

    /**
     * Test the deletion of old revisions
     */
    @Test
    public void testDeleteRevision() {

        var idResource = "1234";
        var i = 1;
        var d = new Device();
        d.setId(idResource);
        d.setLotNumber("Lot " + i);
        mongoDbFhirService.store(List.of(d), true);
        i++;
        d.setLotNumber("Lot " + i);
        mongoDbFhirService.store(List.of(d), true);

        var deviceCollection = mongoClient.getDatabase(dbName).getCollection("Device");
        var countAll = deviceCollection.countDocuments();
        Assert.assertEquals(2, countAll);

        cleanRevisionDataConfiguration.cleanOldRevision();
        countAll = deviceCollection.countDocuments();
        Assert.assertEquals(2, countAll);

        // after 1000ms, data must be deleted (configured in application.properties):
        Awaitility.await().atMost(2000, TimeUnit.MILLISECONDS).until(() -> {
            cleanRevisionDataConfiguration.cleanOldRevision();
            return deviceCollection.countDocuments() == 1;
        });
    }

}
