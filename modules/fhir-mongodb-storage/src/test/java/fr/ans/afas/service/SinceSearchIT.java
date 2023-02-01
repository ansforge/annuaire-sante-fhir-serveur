package fr.ans.afas.service;


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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

/***
 * Test the "_since" parameter
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class SinceSearchIT {


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
     * Test that the since parameter only return some elements
     */
    @Test
    public void testNominalSinceSearch() {


        var d = new Device();
        d.setId("ID1");
        this.mongoDbFhirService.store(List.of(d), false);

        var dt = new Date();

        var d2 = new Device();
        d2.setId("ID2");
        this.mongoDbFhirService.store(List.of(d2), false);

        // search without since and with to control the difference:
        var selectExpression = new SelectExpression<Bson>("Device", expressionFactory);
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(2, all.getPage().size());

        // search with since parameter at now:
        selectExpression.setSince(dt);
        all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(1, all.getPage().size());


    }


    @Test
    public void testUpdatedElement() {
        var d = new Device();
        d.setId("ID1");
        this.mongoDbFhirService.store(List.of(d), false);

        var dt = new Date();

        var selectExpression = new SelectExpression<Bson>("Device", expressionFactory);
        selectExpression.setSince(dt);
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(0, all.getPage().size());

        d.getMeta().setLastUpdated(new Date());
        this.mongoDbFhirService.store(List.of(d), false);
        all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(1, all.getPage().size());


    }

    @Test
    public void testPaging() {

        var d = new Device();
        d.setId("ID");
        this.mongoDbFhirService.store(List.of(d), false);

        var dt = new Date();

        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), false);
        }

        var selectExpression = new SelectExpression<Bson>("Device", expressionFactory);
        selectExpression.setSince(dt);
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(2, all.getPage().size());

        all = this.mongoDbFhirService.search(all.getContext(), selectExpression);
        Assert.assertEquals(1, all.getPage().size());
    }


    @Test
    public void testCount() {

        var d = new Device();
        d.setId("ID");
        this.mongoDbFhirService.store(List.of(d), false);


        var dt = new Date();

        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), false);
        }

        var selectExpression = new SelectExpression<Bson>("Device", expressionFactory);
        var count = this.mongoDbFhirService.count("Device", selectExpression);
        selectExpression.setSince(dt);
        var countSince = this.mongoDbFhirService.count("Device", selectExpression);
        Assert.assertEquals(4, (long) count.getTotal());
        Assert.assertEquals(3, (long) countSince.getTotal());

    }


}
