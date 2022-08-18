/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPage;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.Device;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * Test the consistency of a search when data are updated
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class SearchInRevisionIT {

    @Autowired
    MongoDbFhirService mongoDbFhirService;


    /**
     * The expression factory
     */
    @Autowired
    ExpressionFactory<Bson> expressionFactory;

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
    }

    /**
     * Test the search consistency
     * We insert 10 values, we fetch each pages 1 by 1 element
     * At the middle of the fetch, we update all values
     * The search result must return old versions
     */
    @Test
    public void testSearchConsistency() {
        final var count = 10;
        insert(count, "first-set");
        var sE = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        sE.setCount(1);
        Map searchContext = new HashMap<String, Object>();
        for (var i = 0; i < count; i++) {
            FhirPage page = mongoDbFhirService.search("Device", 1, searchContext, sE);
            Assert.assertTrue(((Device) page.getPage().get(0)).getLotNumber().contains("first-set"));
            searchContext = page.getContext();
            if (i == 4) {
                insert(count, "second-set");
            }
        }

        // search again and the data must be the new one:
        searchContext = new HashMap<String, Object>();
        for (var i = 0; i < count; i++) {
            FhirPage page = mongoDbFhirService.search("Device", 1, searchContext, sE);
            Assert.assertTrue(((Device) page.getPage().get(0)).getLotNumber().contains("second-set"));
            searchContext = page.getContext();
        }
    }

    /**
     * Insert sample data (RassDevice)
     *
     * @param count  number of Fhir resources
     * @param suffix the suffix added to value (LotNumber)
     */
    private void insert(int count, String suffix) {
        for (var i = 0; i < count; i++) {
            var d = new Device();
            d.setId("id-" + i);
            d.setLotNumber("Lot " + i + "-" + suffix);
            mongoDbFhirService.store(List.of(d), true);
        }
    }

}
