package fr.ans.afas.service;

import com.mongodb.client.MongoClient;
import fr.ans.afas.config.CleanRevisionDataConfiguration;
import fr.ans.afas.fhirserver.service.exception.TooManyElementToDeleteException;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import org.hl7.fhir.r4.model.Device;
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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

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
    public void testDeleteOldElements() throws InterruptedException, TooManyElementToDeleteException {

        var t1 = System.currentTimeMillis();

        var d1 = new Device();
        d1.setId("1234-1");
        d1.setLotNumber("Lot 1");

        var d2 = new Device();
        d2.setId("1234-2");
        d2.setLotNumber("Lot 2");

        await().atLeast(1, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - t1 > 1);

        mongoDbFhirService.store(List.of(d1), true);

        await().atLeast(1, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - t1 > 5);
        var t2 = System.currentTimeMillis();
        mongoDbFhirService.store(List.of(d2), true);
        await().atLeast(1, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - t1 > 10);

        var deviceCollection = mongoClient.getDatabase(dbName).getCollection("Device");
        assertEquals(2, deviceCollection.countDocuments());


        // add some device because the server can't suppress more that 15% of data:
        for (var i = 0; i < 100; i++) {
            var d = new Device();
            d.setId("a-" + i);
            this.mongoDbFhirService.store(List.of(d), true);
        }


        // if the date is older we delete nothing:
        mongoDbFhirService.deleteElementsNotStoredSince(t1);
        assertEquals(102, deviceCollection.countDocuments());

        // if the date is between the first and the second insert, we delete the first inset:
        mongoDbFhirService.deleteElementsNotStoredSince(t2);
        assertEquals(101, deviceCollection.countDocuments());
    }


    /**
     * Test the deletion of old elements on updated elements
     */
    @Test
    public void testDeleteOldElementsOnUpdated() throws TooManyElementToDeleteException {

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

        await().atLeast(1, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - t1 > 5);
        mongoDbFhirService.store(List.of(d1), true);
        await().atLeast(1, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - t1 > 10);
        var t2 = System.currentTimeMillis();
        mongoDbFhirService.store(List.of(d2), true);
        await().atLeast(1, TimeUnit.MILLISECONDS).until(() -> System.currentTimeMillis() - t1 > 15);

        // 2 versions of 2 devices:
        var deviceCollection = mongoClient.getDatabase(dbName).getCollection("Device");
        assertEquals(4, deviceCollection.countDocuments());


        // add some device because the server can't suppress more that 15% of data:
        for (var i = 0; i < 100; i++) {
            var d = new Device();
            d.setId("a-" + i);
            this.mongoDbFhirService.store(List.of(d), true);
        }

        // if the date is older we delete nothing:
        mongoDbFhirService.deleteElementsNotStoredSince(t1);
        assertEquals(104, deviceCollection.countDocuments());

        // if the date is between the first and the second insert, we delete the first inset:
        mongoDbFhirService.deleteElementsNotStoredSince(t2);
        assertEquals(102, deviceCollection.countDocuments());

    }


    /**
     * Test an error when we try de delete too much elements
     */
    @Test(expected = TooManyElementToDeleteException.class)
    public void testDeleteError() throws InterruptedException, TooManyElementToDeleteException {


        var d1 = new Device();
        d1.setId("1234-1");
        d1.setLotNumber("Lot 1");

        var d2 = new Device();
        d2.setId("1234-2");
        d2.setLotNumber("Lot 2");
        mongoDbFhirService.store(List.of(d1, d2), true);


        var t1 = System.currentTimeMillis();

        var d3 = new Device();
        d3.setId("1234-3");
        d3.setLotNumber("Lot 3");
        mongoDbFhirService.store(List.of(d3), true);


        mongoDbFhirService.deleteElementsNotStoredSince(t1);

    }

}
