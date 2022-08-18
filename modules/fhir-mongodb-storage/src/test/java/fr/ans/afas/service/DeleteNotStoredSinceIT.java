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

import java.util.List;

/**
 * Test the deletion of fhir element with an old storage date
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class DeleteNotStoredSinceIT {


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
     * Test the deletion of old elements
     */
    @Test
    public void testDeleteOldElements() throws InterruptedException {

        var t1 = System.currentTimeMillis();
        var d1 = new Device();
        d1.setId("1234-1");
        d1.setLotNumber("Lot 1");

        var d2 = new Device();
        d2.setId("1234-2");
        d2.setLotNumber("Lot 2");
        Thread.sleep(1);
        mongoDbFhirService.store(List.of(d1), true);
        Thread.sleep(1);
        var t2 = System.currentTimeMillis();
        mongoDbFhirService.store(List.of(d2), true);
        Thread.sleep(1);
        var t3 = System.currentTimeMillis();

        var deviceCollection = mongoClient.getDatabase(dbName).getCollection("Device");
        Assert.assertEquals(2, deviceCollection.countDocuments());

        // if the date is older we delete nothing:
        mongoDbFhirService.deleteElementsNotStoredSince(t1);
        Assert.assertEquals(2, deviceCollection.countDocuments());

        // if the date is between the first and the second insert, we delete the first inset:
        mongoDbFhirService.deleteElementsNotStoredSince(t2);
        Assert.assertEquals(1, deviceCollection.countDocuments());

        // if the date is after all inserts, we delete all:
        mongoDbFhirService.deleteElementsNotStoredSince(t3);
        Assert.assertEquals(0, deviceCollection.countDocuments());

    }


    /**
     * Test the deletion of old elements on updated elements
     */
    @Test
    public void testDeleteOldElementsOnUpdated() throws InterruptedException {

        var t1 = System.currentTimeMillis();
        var d1 = new Device();
        d1.setId("1234-1");
        d1.setLotNumber("Lot 1");

        var d2 = new Device();
        d2.setId("1234-2");
        d2.setLotNumber("Lot 2");

        // fist insert
        mongoDbFhirService.store(List.of(d1), true);
        mongoDbFhirService.store(List.of(d2), true);


        // second insert, the db will contain 2 versions of elements
        d1.setLotNumber("Lot 1 updated");
        d2.setLotNumber("Lot 2 updated");

        Thread.sleep(1);
        mongoDbFhirService.store(List.of(d1), true);
        Thread.sleep(1);
        var t2 = System.currentTimeMillis();
        mongoDbFhirService.store(List.of(d2), true);
        Thread.sleep(1);
        var t3 = System.currentTimeMillis();

        // 2 versions of 2 devices:
        var deviceCollection = mongoClient.getDatabase(dbName).getCollection("Device");
        Assert.assertEquals(4, deviceCollection.countDocuments());


        // if the date is older we delete nothing:
        mongoDbFhirService.deleteElementsNotStoredSince(t1);
        Assert.assertEquals(4, deviceCollection.countDocuments());

        // if the date is between the first and the second insert, we delete the first inset:
        mongoDbFhirService.deleteElementsNotStoredSince(t2);
        Assert.assertEquals(2, deviceCollection.countDocuments());

        // if the date is after all inserts, we delete all:
        mongoDbFhirService.deleteElementsNotStoredSince(t3);
        Assert.assertEquals(0, deviceCollection.countDocuments());
    }

}
