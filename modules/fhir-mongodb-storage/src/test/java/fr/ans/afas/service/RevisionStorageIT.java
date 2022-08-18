/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.junit.AfterClass;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/***
 * Test the storage of revisions for Fhir resources
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class RevisionStorageIT {

    /**
     * The storage service
     */
    @Autowired
    MongoDbFhirService mongoDbFhirService;

    /**
     * The mongodb client
     */
    @Autowired
    MongoClient mongoClient;

    /**
     * The name of the mongodb database
     */
    @Value("${afas.mongodb.dbname}")
    String dbName;

    @AfterClass
    public static void shutdown() {
        WithMongoTest.clean();
    }

    @Before
    public void init() {
        mongoDbFhirService.deleteAll();
    }

    /**
     * Test the versioning system. When we update an object, the version must be incremented only if the data change.
     */
    @Test
    public void testVersioning() throws InterruptedException {
        var idResource = "12345";
        var i = 1;
        var d = new Device();
        d.setId(idResource);
        d.setLotNumber("Lot " + i);
        mongoDbFhirService.store(List.of(d), true);
        i++;
        d.setLotNumber("Lot " + i);
        mongoDbFhirService.store(List.of(d), true);

        d.setLotNumber("Lot " + i);
        mongoDbFhirService.store(List.of(d), true);

        var ret = mongoDbFhirService.findById("Device", new IdType(idResource));
        Assert.assertEquals("2", ret.getMeta().getVersionId());
    }

    /**
     * When we create/update a resource, the last updated must be set to the current date
     */
    @Test
    public void testLastUpdated() {

        // insert:
        var startDate = new Date();
        var idResource = "12346";
        var i = 1;
        var d = new Device();
        d.setId(idResource);
        d.setLotNumber("Lot " + i);
        mongoDbFhirService.store(List.of(d), true);

        var checkPoint1 = new Date();
        var ret = mongoDbFhirService.findById("Device", new IdType(idResource));
        Assert.assertTrue(ret.getMeta().getLastUpdated().before(checkPoint1));
        Assert.assertTrue(ret.getMeta().getLastUpdated().after(startDate));

        // update with no changes (date must not change):
        var checkPoint2 = new Date();
        d.setLotNumber("Lot " + i);
        mongoDbFhirService.store(List.of(d), true);
        ret = mongoDbFhirService.findById("Device", new IdType(idResource));
        Assert.assertTrue(ret.getMeta().getLastUpdated().before(checkPoint2));
        Assert.assertTrue(ret.getMeta().getLastUpdated().after(startDate));

        // update with changes (date must change):
        var checkPoint3 = new Date();
        d.setLotNumber("Lot " + (i + 1));
        mongoDbFhirService.store(List.of(d), true);
        ret = mongoDbFhirService.findById("Device", new IdType(idResource));
        Assert.assertTrue(ret.getMeta().getLastUpdated().before(new Date()));
        Assert.assertTrue(ret.getMeta().getLastUpdated().after(checkPoint3));
    }


    /**
     * Test the ability to store multiple documents at once
     */
    @Test
    public void testUpdateMultipleDocumentsAtOnce() {
        var toInsert = new ArrayList<Device>();
        for (var i = 0; i < 4; i++) {
            var d = new Device();
            d.setId("123-" + i);
            d.setLotNumber("Lot " + i);
            toInsert.add(d);
        }
        // we insert 2 elements :
        mongoDbFhirService.store(toInsert.subList(0, 2), true);

        // we modify 2 elements :
        toInsert.get(0).setLotNumber(toInsert.get(0).getLotNumber() + "-bis");
        toInsert.get(1).setLotNumber(toInsert.get(1).getLotNumber() + "-bis");

        // we insert 4 elements :
        mongoDbFhirService.store(toInsert, true);

        var ret = mongoDbFhirService.findById("Device", new IdType("123-" + 0));
        Assert.assertEquals("Lot 0-bis", ((Device) ret).getLotNumber());
        ret = mongoDbFhirService.findById("Device", new IdType("123-" + 1));
        Assert.assertEquals("Lot 1-bis", ((Device) ret).getLotNumber());
        ret = mongoDbFhirService.findById("Device", new IdType("123-" + 2));
        Assert.assertEquals("Lot 2", ((Device) ret).getLotNumber());
    }

    /**
     * Test the update of documents already present. Only the "_lastWriteDate" attribute must change.
     */
    @Test
    public void testUpdateWriteDateAttribute() throws InterruptedException {
        var d = new Device();
        d.setId("123idupdate");
        d.setLotNumber("Lot 1");
        var t1 = new Date().getTime();
        var collection = mongoClient.getDatabase(dbName).getCollection("Device");

        Thread.sleep(10);
        // create
        mongoDbFhirService.store(List.of(d), true);
        var col = collection.find(new BasicDBObject()).cursor();
        var device = col.next();
        Assert.assertFalse(col.hasNext());
        var date = (long) device.get(MongoDbFhirService.LAST_WRITE_DATE);
        Thread.sleep(10);
        var t2 = new Date().getTime();
        Assert.assertTrue(t1 < date && t2 > date);

        // update and no modification
        Thread.sleep(10);
        mongoDbFhirService.store(List.of(d), true);

        col = collection.find(new BasicDBObject()).cursor();
        device = col.next();
        Assert.assertFalse(col.hasNext());
        date = (long) device.get(MongoDbFhirService.LAST_WRITE_DATE);

        var t3 = new Date().getTime();
        Assert.assertTrue(t2 < date);
        Assert.assertTrue(t3 > date);


        // update and modify
        Thread.sleep(10);
        d.setLotNumber("OtherNumber");
        mongoDbFhirService.store(List.of(d), true);
        Thread.sleep(10);
        var t4 = new Date().getTime();
        col = collection.find(new BasicDBObject()).cursor();
        var device1 = col.next();
        var device2 = col.next();
        Assert.assertFalse(col.hasNext());
        var date1 = (long) device1.get(MongoDbFhirService.LAST_WRITE_DATE);
        var date2 = (long) device2.get(MongoDbFhirService.LAST_WRITE_DATE);

        Assert.assertTrue(t3 < date1 && t4 > date1);
        Assert.assertTrue(t3 < date2 && t4 > date2);


    }


}
