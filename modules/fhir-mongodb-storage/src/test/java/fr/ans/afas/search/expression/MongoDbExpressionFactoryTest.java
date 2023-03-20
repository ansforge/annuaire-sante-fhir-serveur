/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.TestSearchConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the mongo db expression factory implementation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbExpressionFactoryTest {


    @Test
    public void referenceExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfig());

        var path = FhirSearchPath.builder().path(TestSearchConfig.FHIR_RESOURCE_REFERENCE_PATH).resource(TestSearchConfig.FHIR_RESOURCE_NAME).build();
        var eRef = expressionFactory.newReferenceExpression(path, "A/12");

        Assert.assertEquals(path, eRef.getFhirPath());
        Assert.assertEquals("A", eRef.getType());
        Assert.assertEquals("12", eRef.getId());

        Assert.assertThrows(BadConfigurationException.class, () -> {
            expressionFactory.newReferenceExpression(path, "A/12/az");
        });

        Assert.assertThrows(BadConfigurationException.class, () -> {
            expressionFactory.newReferenceExpression(path, "A/");
        });
    }

    @Test
    public void quantityExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfig());
        var path = FhirSearchPath.builder().path("quantityPath").resource(TestSearchConfig.FHIR_RESOURCE_NAME).build();
        var qRef = expressionFactory.newQuantityExpression(path, 1, QuantityExpression.Operator.LT);

        Assert.assertEquals(path, qRef.getFhirPath());
        Assert.assertEquals(QuantityExpression.Operator.LT, qRef.getOperator());
        Assert.assertEquals(1, qRef.getValue());
    }

}
