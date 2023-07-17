/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import fr.ans.afas.fhirserver.search.data.TotalMode;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.Device;
import org.junit.AfterClass;
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

/**
 * Test the count mode in search
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class CountModeIT {


    @Inject
    MongoDbFhirService mongoDbFhirService;


    /**
     * The expression factory
     */
    @Inject
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


    @Test
    public void testNoneMode() {


        var d = new Device();
        d.setId("ID1");
        var d2 = new Device();
        d2.setId("ID2");
        this.mongoDbFhirService.store(List.of(d, d2), true);

        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.setTotalMode(TotalMode.NONE);
        var countResult = this.mongoDbFhirService.count(selectExpression);
        Assert.assertNull(countResult.getTotal());

        selectExpression.setTotalMode(TotalMode.ALWAYS);
        countResult = this.mongoDbFhirService.count(selectExpression);
        Assert.assertNotNull(countResult.getTotal());

        selectExpression.setTotalMode(TotalMode.BEST_EFFORT);
        countResult = this.mongoDbFhirService.count(selectExpression);
        Assert.assertNotNull(countResult.getTotal());


    }

}
