/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.CompositeSearchConfig;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbOrExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbStringExpression;
import fr.ans.afas.mdbexpression.domain.fhir.searchconfig.ASComplexSearchConfig;
import fr.ans.afas.rass.service.impl.MongoQueryUtils;
import org.bson.conversions.Bson;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test the select expression optimization
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class QueryOptimizationTest {


    @Test
    public void hasOptimizationTest() {

        var searchConfig = new CompositeSearchConfig(List.of(new ASComplexSearchConfig()));
        var expressionFactory = new MongoDbExpressionFactory(searchConfig);


        var selectExpression = new SelectExpression<>("Organization", expressionFactory);
        // the has expression:
        var or = new MongoDbOrExpression();
        var stringExpression = new MongoDbStringExpression(searchConfig, FhirSearchPath.builder().resource("Device").path("device-name").build(), "1234", StringExpression.Operator.EQUALS);
        or.addExpression(stringExpression);
        var hasCondition = new HasCondition<Bson>(FhirSearchPath.builder().resource("Device").path("organization").build());
        hasCondition.addExpression(or);
        selectExpression.addHasCondition(hasCondition);

        MongoQueryUtils.optimizeQuery(searchConfig, selectExpression);


        Assert.assertEquals(0, selectExpression.getHasConditions().size());

        var orExp = (OrExpression<Bson>) ((AndExpression<Bson>) selectExpression.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, orExp.getExpressions().size());
        var stringExpressionFound = (StringExpression<Bson>) orExp.getExpressions().get(0);
        Assert.assertEquals("1234", stringExpressionFound.getValue());
        Assert.assertEquals("Organization", stringExpressionFound.getFhirPath().getResource());
        Assert.assertEquals("links.Device.device-name", stringExpressionFound.getFhirPath().getPath());


    }

}
