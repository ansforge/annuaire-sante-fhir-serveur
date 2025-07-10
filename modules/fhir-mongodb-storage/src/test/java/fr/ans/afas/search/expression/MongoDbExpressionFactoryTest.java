/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.OrExpression;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.TokenExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbExpressionFactory;
import fr.ans.afas.mdbexpression.domain.fhir.TestSearchConfigService;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Test the mongo db expression factory implementation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbExpressionFactoryTest {

    static final FhirSearchPath pathHasParam = FhirSearchPath.builder().resource(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME).path("string_sub_path").build();
    static final FhirSearchPath pathHasParamToken = FhirSearchPath.builder().resource(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME).path("token_sub_path").build();
    static final FhirSearchPath pathHasLink = FhirSearchPath.builder().resource(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME).path("parentPath").build();


    @Test
    public void referenceExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());

        var path = FhirSearchPath.builder().path(TestSearchConfigService.FHIR_RESOURCE_REFERENCE_PATH).resource(TestSearchConfigService.FHIR_RESOURCE_NAME).build();
        var eRef = expressionFactory.newReferenceExpression(path, "A/12");

        Assert.assertEquals(path, eRef.getFhirPath());
        Assert.assertEquals("A", eRef.getType());
        Assert.assertEquals("12", eRef.getId());

        Assert.assertThrows(BadConfigurationException.class, () -> expressionFactory.newReferenceExpression(path, "A/12/az"));

        Assert.assertThrows(BadConfigurationException.class, () -> expressionFactory.newReferenceExpression(path, "A/"));
    }

    @Test
    public void quantityExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("quantityPath").resource(TestSearchConfigService.FHIR_RESOURCE_NAME).build();
        var qRef = expressionFactory.newQuantityExpression(path, 1, QuantityExpression.Operator.LT);

        Assert.assertEquals(path, qRef.getFhirPath());
        Assert.assertEquals(QuantityExpression.Operator.LT, qRef.getOperator());
        Assert.assertEquals(1, qRef.getValue());
    }


    @Test
    public void hasExpressionStringTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var hasCondString = expressionFactory.newHasExpression(pathHasLink, pathHasParam, List.of("val1", "val2"));

        Assert.assertEquals(pathHasLink, hasCondString.getFhirPath());
        Assert.assertEquals(2, ((OrExpression) hasCondString.getExpressions().get(0)).getExpressions().size());

        var or = ((OrExpression) hasCondString.getExpressions().get(0));
        var exp1 = (StringExpression) or.getExpressions().get(0);
        var exp2 = (StringExpression) or.getExpressions().get(1);
        Assert.assertEquals("val1", exp1.getValue());
        Assert.assertEquals("val2", exp2.getValue());
        Assert.assertEquals("string_sub_path", exp2.getFhirPath().getPath());
        Assert.assertEquals(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME, exp2.getFhirPath().getResource());

    }


    @Test
    public void hasExpressionTokenTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var hasCondToken = expressionFactory.newHasExpression(pathHasLink, pathHasParamToken, List.of("a|1", "2"));

        Assert.assertEquals(pathHasLink, hasCondToken.getFhirPath());
        Assert.assertEquals(2, ((OrExpression) hasCondToken.getExpressions().get(0)).getExpressions().size());

        var or = ((OrExpression) hasCondToken.getExpressions().get(0));
        var exp1 = (TokenExpression) or.getExpressions().get(0);
        var exp2 = (TokenExpression) or.getExpressions().get(1);
        Assert.assertEquals("1", exp1.getValue());
        Assert.assertEquals("a", exp1.getSystem());
        Assert.assertEquals("2", exp2.getValue());
        Assert.assertNull(exp2.getSystem());
        Assert.assertEquals("token_sub_path", exp2.getFhirPath().getPath());
        Assert.assertEquals(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME, exp2.getFhirPath().getResource());

    }


}
