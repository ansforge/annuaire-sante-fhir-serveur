/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas;

import com.mongodb.client.MongoClients;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/***
 * Test that we can use the mongodb with WithMongoTest
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SampleApp.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
@ActiveProfiles("full")
public class SimpleWithMongoTest {

    /**
     * Uri of the mongodb server
     */
    @Value("${afas.mongodb.uri}")
    String mongoUri;


    /**
     * Test that we can access
     */
    @Test
    public void testMongoAccess() {
        try (var client = MongoClients.create(mongoUri)) {
            Assert.assertTrue(client.listDatabaseNames().iterator().hasNext());
        }
    }

}
