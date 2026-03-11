package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.OrExpression;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.fhirserver.search.expression.TokenExpression;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MongoDbExpressionFactoryTest {

    static final FhirSearchPath pathHasLink = FhirSearchPath.builder().resource("FhirResourceSub").path("reference_sub_path").build();

    @Test
    void referenceExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());

        var path = FhirSearchPath.builder().path("referencePath").resource("TestResource").build();
        var eRef = expressionFactory.newReferenceExpression(path, "A/12");

        assertEquals(path, eRef.getFhirPath());
        assertEquals("A", eRef.getType());
        assertEquals("12", eRef.getId());

        assertThrows(BadConfigurationException.class, () -> expressionFactory.newReferenceExpression(path, "A/12/az"));
        assertThrows(BadConfigurationException.class, () -> expressionFactory.newReferenceExpression(path, "A/"));
    }

    @Test
    void quantityExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("quantityPath").resource("TestResource").build();
        var qRef = expressionFactory.newQuantityExpression(path, 1, QuantityExpression.Operator.LT);

        assertEquals(path, qRef.getFhirPath());
        assertEquals(QuantityExpression.Operator.LT, qRef.getOperator());
        assertEquals(1, qRef.getValue());
    }





    @Test
    void hasExpressionTokenTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var paramPath = FhirSearchPath.builder().resource(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME).path(TestSearchConfigService.FHIR_RESOURCE_SUB_STRING_PATH).build();
        var linkPath = FhirSearchPath.builder().resource(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME).path(TestSearchConfigService.FHIR_RESOURCE_SUB_REFERENCE_PATH).build();

        var hasCondition = expressionFactory.newHasExpression(linkPath, paramPath, List.of("a|1", "2"));
        assertEquals(linkPath, hasCondition.getFhirPath());
        assertEquals(2, ((OrExpression) hasCondition.getExpressions().get(0)).getExpressions().size());

        var or = ((OrExpression) hasCondition.getExpressions().get(0));
        var exp1 = (MongoDbStringExpression) or.getExpressions().get(0);
        var exp2 = (MongoDbStringExpression) or.getExpressions().get(1);
        assertEquals("a|1", exp1.getValue());
        assertEquals("2", exp2.getValue());
        assertEquals("string_sub_path", exp2.getFhirPath().getPath());
        assertEquals("FhirResourceSub", exp2.getFhirPath().getResource());
    }
    @Test
    void newHasExpressionUnsupportedTypeTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var unsupportedPath = FhirSearchPath.builder().resource("TestResource").path("unsupported_path").build();

        assertThrows(BadConfigurationException.class, () ->
                        expressionFactory.newHasExpression(pathHasLink, unsupportedPath, List.of("value")),
                "Reverse chained search (_has) is only supported on string and token params ");
    }
    @Test
    void newTokenInExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("tokenPath").resource("TestResource").build();
        var tokenInExpression = expressionFactory.newTokenInExpression(path, "system", List.of("value1", "value2"));

        assertEquals(path, tokenInExpression.getFhirPath());
        assertEquals("system", tokenInExpression.getSystem());
        assertEquals(List.of("value1", "value2"), tokenInExpression.getValues());
    }
    @Test
    void newTokenNotInExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("tokenPath").resource("TestResource").build();
        var tokenNotInExpression = expressionFactory.newTokenNotInExpression(path, "system", List.of("value1", "value2"));

        assertEquals(path, tokenNotInExpression.getFhirPath());
        assertEquals("system", tokenNotInExpression.getSystem());
        assertEquals(List.of("value1", "value2"), tokenNotInExpression.getValues());
    }
    @Test
    void newDateRangeExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("datePath").resource("TestResource").build();
        var date = new Date();
        var dateRangeExpression = expressionFactory.newDateRangeExpression(path, date, TemporalPrecisionEnum.DAY, ParamPrefixEnum.GREATERTHAN);

        assertEquals(path, dateRangeExpression.getFhirPath());
        assertEquals(date, dateRangeExpression.getDate());
        assertEquals(TemporalPrecisionEnum.DAY, dateRangeExpression.getPrecision());
        assertEquals(ParamPrefixEnum.GREATERTHAN, dateRangeExpression.getPrefix());
    }
    @Test
    void newOrExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var orExpression = expressionFactory.newOrExpression();

        assertNotNull(orExpression);
        assertTrue(orExpression instanceof MongoDbOrExpression);
    }

    @Test
    void newAndExpressionTest() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var andExpression = expressionFactory.newAndExpression();

        assertNotNull(andExpression);
        assertTrue(andExpression instanceof MongoDbAndExpression);
    }

    @Test
    public void testNewTokenHasCondition() {
        // Initialisation de l'usine d'expressions avec une configuration de test
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());

        // Création des chemins nécessaires
        var linkPath = FhirSearchPath.builder()
                .resource(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME)
                .path("parentPath")
                .build();

        var paramPath = FhirSearchPath.builder()
                .resource(TestSearchConfigService.FHIR_RESOURCE_SUB_NAME)
                .path("token_sub_path")
                .build();

        // Appel de la méthode newHasExpression avec des valeurs de type token
        var hasCondition = expressionFactory.newHasExpression(linkPath, paramPath, List.of("system1|value1", "value2"));

        // Vérifications
        assertEquals(linkPath, hasCondition.getFhirPath());
        assertEquals(1, hasCondition.getExpressions().size());

        var orExpression = (OrExpression<?>) hasCondition.getExpressions().get(0);
        assertEquals(2, orExpression.getExpressions().size());

        var tokenExp1 = (TokenExpression<?>) orExpression.getExpressions().get(0);
        assertEquals("system1", tokenExp1.getSystem());
        assertEquals("value1", tokenExp1.getValue());

        var tokenExp2 = (TokenExpression<?>) orExpression.getExpressions().get(1);
        assertNull(tokenExp2.getSystem());
        assertEquals("value2", tokenExp2.getValue());
    }

    @Test
    void testNewReferenceExpressionWithoutChain() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("referencePath").resource("TestResource").build();

        var eRef = expressionFactory.newReferenceExpression(path, "Type/123");

        assertEquals("Type", eRef.getType());
        assertEquals("123", eRef.getId());
        assertEquals(path, eRef.getFhirPath());
    }



    @Test
    void testNewHasExpressionWithUnsupportedType() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var unsupportedPath = FhirSearchPath.builder().resource("TestResource").path("unsupported_path").build();

        assertThrows(BadConfigurationException.class, () -> expressionFactory.newHasExpression(pathHasLink, unsupportedPath, List.of("value")));
    }

    @Test
    void testNewTokenHasConditionWithEmptyValues() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var paramPath = FhirSearchPath.builder().resource("TestResource").path("tokenPath").build();

        assertThrows(BadConfigurationException.class, () -> expressionFactory.newHasExpression(pathHasLink, paramPath, List.of()));
    }



    @Test
    void testNewReferenceExpressionWithInvalidReferenceFormat() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("referencePath").resource("TestResource").build();

        assertThrows(BadConfigurationException.class, () -> expressionFactory.newReferenceExpression(path, "/"));
        assertThrows(BadConfigurationException.class, () -> expressionFactory.newReferenceExpression(path, "Type/123/Extra"));
    }

    @Test
    void testNewReferenceExpressionWithValidReferenceWithoutType() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("referencePath").resource("TestResource").build();

        var eRef = expressionFactory.newReferenceExpression(path, "123");

        assertNotNull(eRef);
        assertEquals("123", eRef.getId());
        assertNull(eRef.getType());
        assertEquals(path, eRef.getFhirPath());
    }

    @Test
    void testNewReferenceExpressionWithValidReferenceWithType() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());
        var path = FhirSearchPath.builder().path("referencePath").resource("TestResource").build();

        var eRef = expressionFactory.newReferenceExpression(path, "Type/123");

        assertNotNull(eRef);
        assertEquals("Type", eRef.getType());
        assertEquals("123", eRef.getId());
        assertEquals(path, eRef.getFhirPath());
    }

    void testChainedParamDoesNotExist() {
        var expressionFactory = new MongoDbExpressionFactory(new TestSearchConfigService().applyTestSearchConfigComplete());

        // Création d'un chemin FHIR avec un paramètre chaîné inexistant
        var invalidChainedPath = FhirSearchPath.builder()
                .path("invalidPath")
                .resource("TestResource")
                .chain("nonExistentSubPath")
                .build();

        // Vérification que l'exception est levée
        assertThrows(BadConfigurationException.class, () -> {
            expressionFactory.newReferenceExpression(invalidChainedPath, "Type/123");
        }, "Chained param doesn't exist: TestResource.nonExistentSubPath");
    }




}