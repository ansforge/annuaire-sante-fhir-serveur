/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;

import fr.ans.afas.fhirserver.search.data.TotalMode;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.MongoMultiTenantService;
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
 * Test the count mode in search for best effort (count resolution based on time)
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "afas.fhir.max-count-calculation-time=1")
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})

public class CountModeBestEffortIT {


    @Inject
    MongoDbFhirService mongoDbFhirService;


    /**
     * The expression factory
     */
    @Inject
    ExpressionFactory<Bson> expressionFactory;

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
    }


    @Test
    public void testBestEffortMode() {

        var d = new Device();
        d.setId("ID1");
        var d2 = new Device();
        d2.setId("ID2");
        this.mongoDbFhirService.store(List.of(d, d2), true);

        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        // simulate a slow query:
        selectExpression.getExpression().addExpression(new Expression<>() {
            @Override
            public Bson interpreter(ExpressionContext expressionContext) {
                return org.bson.Document.parse(("{$expr:{ $function: {body: \"function() { return sleep(100000); }\",args: [ ],lang: \"js\"} }}}"));
            }

            @Override
            public String serialize(ExpressionSerializer<Bson> expressionSerializer) {
                return null;
            }

            @Override
            public Expression<Bson> deserialize(ExpressionSerializer<Bson> expressionDeserializer) {
                return null;
            }
        });

        selectExpression.setTotalMode(TotalMode.BEST_EFFORT);
        var countResult = this.mongoDbFhirService.count(selectExpression);
        Assert.assertNull(countResult.getTotal());


    }

}
