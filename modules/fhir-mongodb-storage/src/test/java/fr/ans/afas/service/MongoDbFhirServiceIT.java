package fr.ans.afas.service;


import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbDateRangeExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbIncludeExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbStringExpression;
import fr.ans.afas.rass.service.MongoDbFhirService;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Test the mongodb search service
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class MongoDbFhirServiceIT {


    @Inject
    MongoDbFhirService mongoDbFhirService;


    /**
     * The expression factory
     */
    @Inject
    ExpressionFactory<Bson> expressionFactory;


    @Inject
    SearchConfig searchConfig;

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
     * The empty save is a special case with no calculation
     */
    @Test
    public void testEmptySave() {
        Assert.assertEquals(0, this.mongoDbFhirService.store(List.of(), false).size());
    }


    /**
     * The include/revinclude
     */
    @Test
    public void testInclude() {
        var device1 = new Device();
        device1.setId("i1");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        device1.setOwner(owner1);
        this.mongoDbFhirService.store(List.of(device1), false);

        var org1 = new Organization();
        org1.setId("org1");
        this.mongoDbFhirService.store(List.of(org1), false);

        //
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getIncludes().add(new MongoDbIncludeExpression(searchConfig, "Device", "organization"));
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(2, all.getPage().size());

        var selectExpressionRev = new SelectExpression<>("Organization", expressionFactory);
        selectExpressionRev.getRevincludes().add(new MongoDbIncludeExpression(searchConfig, "Device", "organization"));
        selectExpressionRev.setCount(2);
        var allRev = this.mongoDbFhirService.search(null, selectExpressionRev);
        Assert.assertEquals(2, allRev.getPage().size());
    }


    /**
     * Test the paging with params
     */
    @Test
    public void testPagingWithParams() {

        var cal = GregorianCalendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        var d = new Device();
        d.setId("ID");
        d.getMeta().setLastUpdated(cal.getTime());
        this.mongoDbFhirService.store(List.of(d), false);

        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), true);
        }

        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getExpression().addExpression(new MongoDbDateRangeExpression(searchConfig, FhirSearchPath.builder().path("_lastUpdated").resource("Device").build(), new Date(), TemporalPrecisionEnum.YEAR, ParamPrefixEnum.GREATERTHAN_OR_EQUALS));
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(2, all.getPage().size());

        all = this.mongoDbFhirService.search(all.getContext(), selectExpression);
        Assert.assertEquals(1, all.getPage().size());

    }

    /**
     * Test the paging without params
     */
    @Test
    public void testPagingWithoutParams() {

        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), true);
        }

        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(2, all.getPage().size());

        all = this.mongoDbFhirService.search(all.getContext(), selectExpression);
        Assert.assertEquals(1, all.getPage().size());

    }


    /**
     * Test the storage of a not allowed element
     */
    @Test(expected = CantWriteFhirResource.class)
    public void testStoreNotAllowed() {
        var d2 = new Patient();
        d2.setId("ID");
        this.mongoDbFhirService.store(List.of(d2), true);
    }

    /**
     * Test the search of a not allowed element
     */
    @Test(expected = BadRequestException.class)
    public void testSearchNotAllowed() {
        var selectExpression = new SelectExpression<>("Patient", expressionFactory);
        selectExpression.setCount(2);
        this.mongoDbFhirService.search(null, selectExpression);
    }


    /**
     * Test the revinclude of a not allowed element
     */
    @Test(expected = BadRequestException.class)
    public void testSearchNotAllowedRevInclude() {

        var device1 = new Device();
        device1.setId("i1");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        device1.setOwner(owner1);
        this.mongoDbFhirService.store(List.of(device1), false);


        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getRevincludes().add(new MongoDbIncludeExpression(searchConfig, "Patient", "not-important"));
        selectExpression.setCount(2);
        this.mongoDbFhirService.search(null, selectExpression);
    }


    /**
     * Test the search on a non-configured element
     */
    @Test(expected = BadConfigurationException.class)
    public void testSearchOnBadParameter() {
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getExpression().addExpression(new MongoDbStringExpression(searchConfig, FhirSearchPath.builder().resource("Device").path("no-exist").build(), "a", StringExpression.Operator.EXACT));
        this.mongoDbFhirService.search(null, selectExpression);
    }


    /***
     * Test the storage of multiple resource types
     */
    @Test(expected = CantWriteFhirResource.class)
    public void testStoreMultipleTypes() {
        this.mongoDbFhirService.store(List.of(new Device(), new Organization()), true);
    }


}
