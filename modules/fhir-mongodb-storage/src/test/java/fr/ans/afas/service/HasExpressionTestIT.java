/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;


import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.exception.BadParametersException;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.HasCondition;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbOrExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbStringExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbTokenExpression;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.impl.DefaultIndexService;
import fr.ans.afas.rass.service.json.GenericSerializer;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.*;
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
 * Test FHIR _has expressions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})
public class HasExpressionTestIT {

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
        this.initData();
    }


    @Test
    public void testSimpleHas() {
        var selectExpression = new SelectExpression<>("Organization", expressionFactory);

        // the has expression:
        var or = new MongoDbOrExpression();
        var stringExpression = new MongoDbStringExpression(searchConfig, FhirSearchPath.builder().resource("Device").path("device-name").build(), "1234", StringExpression.Operator.EQUALS);
        or.addExpression(stringExpression);
        var hasCondition = new HasCondition<Bson>(FhirSearchPath.builder().resource("Device").path("organization").build());
        hasCondition.addExpression(or);
        selectExpression.addHasCondition(hasCondition);


        indexJoins();

        var r2 = this.mongoDbFhirService.count(selectExpression);
        Assert.assertEquals((Long) 1L, r2.getTotal());
        var r = this.mongoDbFhirService.search(null, selectExpression);

        Assert.assertEquals(1, r.getPage().size());
    }


    @Test
    public void testMultipleHas() {


        // Organization?_has:Device:owner:device-name=1234&_has:PractitionerRole:organization:
        var selectExpression = new SelectExpression<>("Organization", expressionFactory);

        // the has expression for device:
        var or = new MongoDbOrExpression();
        var stringExpression = new MongoDbStringExpression(searchConfig, FhirSearchPath.builder().resource("Device").path("device-name").build(), "1234", StringExpression.Operator.EQUALS);
        or.addExpression(stringExpression);
        var hasCondition = new HasCondition<Bson>(FhirSearchPath.builder().resource("Device").path("organization").build());
        hasCondition.addExpression(or);
        selectExpression.addHasCondition(hasCondition);

        // the has expression for practitionerRole:
        var orPr = new MongoDbOrExpression();
        var tokenExpressionPr = new MongoDbTokenExpression(searchConfig, FhirSearchPath.builder().resource("PractitionerRole").path("role").build(), null, "code1");
        orPr.addExpression(tokenExpressionPr);
        var hasConditionPr = new HasCondition<Bson>(FhirSearchPath.builder().resource("PractitionerRole").path("organization").build());
        hasConditionPr.addExpression(orPr);
        selectExpression.addHasCondition(hasConditionPr);

        indexJoins();

        var r2 = this.mongoDbFhirService.count(selectExpression);
        Assert.assertEquals((Long) 1L, r2.getTotal());


        // one condition is ko:
        var orPrKo = new MongoDbOrExpression();
        var tokenExpressionPrKo = new MongoDbTokenExpression(searchConfig, FhirSearchPath.builder().resource("PractitionerRole").path("role").build(), null, "code2");
        orPrKo.addExpression(tokenExpressionPrKo);
        var hasConditionPrKo = new HasCondition<Bson>(FhirSearchPath.builder().resource("PractitionerRole").path("organization").build());
        hasConditionPrKo.addExpression(orPrKo);
        selectExpression.addHasCondition(hasConditionPrKo);

        r2 = this.mongoDbFhirService.count(selectExpression);
        Assert.assertEquals((Long) 0L, r2.getTotal());
    }


    @Test
    public void testHasWithSearchContext() {

        var selectExpression = new SelectExpression<>("Practitioner", expressionFactory);
        // the has expression:
        var or = new MongoDbOrExpression();
        var stringExpression = new MongoDbTokenExpression(searchConfig, FhirSearchPath.builder().resource("PractitionerRole").path("role").build(), null, "code1");
        var stringExpressio2n = new MongoDbTokenExpression(searchConfig, FhirSearchPath.builder().resource("PractitionerRole").path("role").build(), null, "code2");
        or.addExpression(stringExpression);
        or.addExpression(stringExpressio2n);
        var hasCondition = new HasCondition<Bson>(FhirSearchPath.builder().resource("PractitionerRole").path("practitioner").build());
        hasCondition.addExpression(or);
        selectExpression.addHasCondition(hasCondition);
        selectExpression.setCount(1);
        var r2 = this.mongoDbFhirService.count(selectExpression);
        Assert.assertEquals((Long) 2L, r2.getTotal());


        var r = this.mongoDbFhirService.search(null, selectExpression);
        var r3 = this.mongoDbFhirService.search(r.getContext(), selectExpression);

        Assert.assertEquals(1, r.getPage().size());
        Assert.assertEquals(1, r3.getPage().size());

    }

    @Test
    public void testBadHasParameter() {
        var selectExpression = new SelectExpression<>("Organization", expressionFactory);
        // the has expression:
        var or = new MongoDbOrExpression();
        var stringExpression = new MongoDbStringExpression(searchConfig, FhirSearchPath.builder().resource("Device").path("device-name").build(), "1234", StringExpression.Operator.EQUALS);
        or.addExpression(stringExpression);
        var hasCondition = new HasCondition<Bson>(FhirSearchPath.builder().resource("Device").path("error").build());
        hasCondition.addExpression(or);
        selectExpression.addHasCondition(hasCondition);
        // bad search param:
        SelectExpression<Bson> finalSelectExpression = selectExpression;
        Assert.assertThrows(BadConfigurationException.class, () -> this.mongoDbFhirService.count(finalSelectExpression));


        // bad relation type:
        selectExpression = new SelectExpression<>("Organization", expressionFactory);
        // the has expression:
        or = new MongoDbOrExpression();
        stringExpression = new MongoDbStringExpression(searchConfig, FhirSearchPath.builder().resource("Device").path("device-name").build(), "1234", StringExpression.Operator.EQUALS);
        or.addExpression(stringExpression);
        hasCondition = new HasCondition<>(FhirSearchPath.builder().resource("Device").path("device-name").build());
        hasCondition.addExpression(or);
        selectExpression.addHasCondition(hasCondition);
        SelectExpression<Bson> finalSelectExpression1 = selectExpression;
        Assert.assertThrows(BadParametersException.class, () -> this.mongoDbFhirService.count(finalSelectExpression1));
    }


    private void initData() {
        // Device:
        var device1 = new Device();
        device1.setId("i1");
        device1.addDeviceName().setName("1234");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        device1.setOwner(owner1);
        this.mongoDbFhirService.store(List.of(device1), false);

        // Organization:
        var org1 = new Organization();
        org1.setId("org1");
        this.mongoDbFhirService.store(List.of(org1), false);
        var org2 = new Organization();
        org2.setId("org2");
        this.mongoDbFhirService.store(List.of(org2), false);


        // Practitioner:
        var practitioner1 = new Practitioner();
        practitioner1.setId("p1");
        practitioner1.addName().setFamily("someFamily1");
        var practitioner2 = new Practitioner();
        practitioner2.setId("p2");
        practitioner2.addName().setFamily("someFamily2");
        var practitioner3 = new Practitioner();
        practitioner3.setId("p3");
        practitioner3.addName().setFamily("someFamily3");
        this.mongoDbFhirService.store(List.of(practitioner1, practitioner2, practitioner3), false);

        // PractitionerRole:
        var practitionerRole1 = new PractitionerRole();
        practitionerRole1.setId("pr1");
        practitionerRole1.addCode().addCoding().setCode("code1");
        var r1 = new Reference();
        r1.setType("Practitioner");
        r1.setId("p1");
        r1.setReference("Practitioner/p1");
        var rO1 = new Reference();
        rO1.setType("Organization");
        rO1.setId("org1");
        rO1.setReference("Organization/org1");
        practitionerRole1.setPractitioner(r1);
        practitionerRole1.setOrganization(rO1);
        var practitionerRole2 = new PractitionerRole();
        practitionerRole2.setId("pr2");
        practitionerRole2.addCode().addCoding().setCode("code2");
        var r2 = new Reference();
        r2.setType("Practitioner");
        r2.setId("p2");
        r2.setReference("Practitioner/p2");
        practitionerRole2.setPractitioner(r2);
        var practitionerRole3 = new PractitionerRole();
        practitionerRole3.setId("pr3");
        practitionerRole3.addCode().addCoding().setCode("code3");
        practitionerRole3.setPractitioner(r1);
        this.mongoDbFhirService.store(List.of(practitionerRole1, practitionerRole2, practitionerRole3), false);

    }

    /**
     * Trigger the join indexation
     */
    private void indexJoins() {
        var genericSerializer = new GenericSerializer(searchConfig, FhirContext.forR4());
        var defaultIndexService = new DefaultIndexService(mongoDbFhirService, expressionFactory, searchConfig, genericSerializer);
        defaultIndexService.refreshIndexesSync(1);
    }
}
