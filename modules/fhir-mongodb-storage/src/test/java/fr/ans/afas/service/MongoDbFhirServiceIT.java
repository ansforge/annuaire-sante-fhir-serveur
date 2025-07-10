/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;


import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.exception.ResourceNotFoundException;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.TokenExpression;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbDateRangeExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbIncludeExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbStringExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbTokenExpression;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.MongoMultiTenantService;
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
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
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
    SearchConfigService searchConfigService;

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


    /**
     * The empty save is a special case with no calculation
     */
    @Test
    public void testEmptySave() {
        Assert.assertEquals(0, this.mongoDbFhirService.store(List.of(), false, false).size());
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
        this.mongoDbFhirService.store(List.of(device1), false, false);

        var org1 = new Organization();
        org1.setId("org1");
        this.mongoDbFhirService.store(List.of(org1), false, false);

        //
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getIncludes().add(new MongoDbIncludeExpression(searchConfigService, "Device", "organization"));
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals(2, all.getPage().size());

        var selectExpressionRev = new SelectExpression<>("Organization", expressionFactory);
        selectExpressionRev.getRevincludes().add(new MongoDbIncludeExpression(searchConfigService, "Device", "organization"));
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
        this.mongoDbFhirService.store(List.of(d), false, false);

        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), true, false);
        }

        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getExpression().addExpression(new MongoDbDateRangeExpression(searchConfigService, FhirSearchPath.builder().path("_lastUpdated").resource("Device").build(), new Date(), TemporalPrecisionEnum.YEAR, ParamPrefixEnum.GREATERTHAN_OR_EQUALS));
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
            this.mongoDbFhirService.store(List.of(d2), true, false);
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
        this.mongoDbFhirService.store(List.of(d2), true, false);
    }

    /**
     * Test the search of a not allowed element
     */
    @Test(expected = ResourceNotFoundException.class)
    public void testSearchNotAllowed() {
        var selectExpression = new SelectExpression<>("Patient", expressionFactory);
        selectExpression.setCount(2);
        this.mongoDbFhirService.search(null, selectExpression);
    }


    /**
     * Test the revinclude of a not allowed element
     */
    @Test(expected = ResourceNotFoundException.class)
    public void testSearchNotAllowedRevInclude() {

        var device1 = new Device();
        device1.setId("i1");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        device1.setOwner(owner1);
        this.mongoDbFhirService.store(List.of(device1), false, false);


        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getRevincludes().add(new MongoDbIncludeExpression(searchConfigService, "Patient", "not-important"));
        selectExpression.setCount(2);
        this.mongoDbFhirService.search(null, selectExpression);
    }


    /**
     * Test the search on a non-configured element
     */
    @Test(expected = BadConfigurationException.class)
    public void testSearchOnBadParameter() {
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getExpression().addExpression(new MongoDbStringExpression(searchConfigService, FhirSearchPath.builder().resource("Device").path("no-exist").build(), "a", StringExpression.Operator.EXACT));
        this.mongoDbFhirService.search(null, selectExpression);
    }


    /***
     * Test the storage of multiple resource types
     */
    @Test(expected = CantWriteFhirResource.class)
    public void testStoreMultipleTypes() {
        this.mongoDbFhirService.store(List.of(new Device(), new Organization()), true);
    }


    @Test
    public void testSearchWithIterator() {
        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), true);
        }
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.setCount(2);
        var all = this.mongoDbFhirService.iterate(null, selectExpression);

        var count = 0;
        while (all.hasNext()) {
            all.next();
            count++;
        }
        Assert.assertEquals(2, count);
    }


    @Test
    public void testSearchWithIteratorNextPage() {
        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), true);
        }
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.setCount(2);
        var fhirPageIterator = this.mongoDbFhirService.iterate(null, selectExpression);

        fhirPageIterator.next();
        var sc = fhirPageIterator.searchContext();

        // next Page
        fhirPageIterator = this.mongoDbFhirService.iterate(sc, selectExpression);
        var count = 0;
        while (fhirPageIterator.hasNext()) {
            fhirPageIterator.next();
            count++;
        }
        Assert.assertEquals(2, count);
    }


    @Test
    public void testSearchWithIteratorWithParams() {
        for (var i = 0; i < 3; i++) {
            var d2 = new Device();
            d2.setId("ID" + i);
            this.mongoDbFhirService.store(List.of(d2), true);
        }
        var selectExpression = new SelectExpression<>("Device", expressionFactory);
        selectExpression.getExpression().addExpression(new MongoDbTokenExpression(searchConfigService, FhirSearchPath.builder().path("_id").resource("Device").build(), null, "ID1", TokenExpression.Operator.EQUALS));
        selectExpression.setCount(1);
        var fhirPageIterator = this.mongoDbFhirService.iterate(null, selectExpression);

        var count = 0;
        while (fhirPageIterator.hasNext()) {
            fhirPageIterator.next();
            count++;
        }
        Assert.assertEquals(1, count);
    }

    @Test(expected = ResourceNotFoundException.class)
    public void testSearchWithIteratorInError() {
        var selectExpression = new SelectExpression<>("DeviceNotFound", expressionFactory);
        this.mongoDbFhirService.iterate(null, selectExpression);
    }

}
