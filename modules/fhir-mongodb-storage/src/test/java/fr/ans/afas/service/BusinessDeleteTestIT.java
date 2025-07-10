/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;


import com.mongodb.client.MongoClient;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test the business deletion of fhir element
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})

public class BusinessDeleteTestIT {


    /**
     * Service to access fhir data
     */
    @Inject
    MongoDbFhirService mongoDbFhirService;

    /**
     * The mongodb client
     */
    @Inject
    MongoClient mongoClient;

    @Inject
    MongoMultiTenantService multiTenantService;

    @Before
    public void init() {
        mongoDbFhirService.deleteAll();
    }


    /**
     * Test the deletion
     */
    @Test
    public void testDelete() {
        // Given 2 devices :

        var d1 = new Device();
        d1.setId("1234-1");
        d1.setLotNumber("Lot 1");
        var d2 = new Device();
        d2.setId("1234-2");
        d2.setLotNumber("Lot 2");

        mongoDbFhirService.store(List.of(d1, d2), true);
        var id1 = new IdType("1234-1");
        var value = mongoDbFhirService.findById("Device", id1);
        Assert.assertNotNull(value);

        // When we delete the resource:
        mongoDbFhirService.businessDelete("Device", id1);


        // then the resource must be not found
        value = mongoDbFhirService.findById("Device", id1);
        Assert.assertNull(value);


        // but old revisions stay in database
        var deviceCollection = multiTenantService.getCollection("Device");
        assertEquals(2, deviceCollection.countDocuments());
    }

}
