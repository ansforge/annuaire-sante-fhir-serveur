package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.TokenExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoDbTokenExpressionTest {

    private SearchConfigService searchConfigService;
    private FhirSearchPath fhirPath;
    private ExpressionContext expressionContext;
    private MongoDbTokenExpression mongoDbTokenExpression;

    @BeforeEach
    void setUp() {
        searchConfigService = mock(SearchConfigService.class);
        fhirPath = new FhirSearchPath("testPath", "testResource", null);
        expressionContext = mock(ExpressionContext.class);

        when(expressionContext.getPrefix()).thenReturn("prefix_");

        var mockSearchParamConfig = mock(SearchParamConfig.class);
        when(mockSearchParamConfig.getIndexName()).thenReturn("indexName");
        when(searchConfigService.getSearchConfigByPath(fhirPath))
                .thenReturn(Optional.of(mockSearchParamConfig));

        mongoDbTokenExpression = new MongoDbTokenExpression(
                searchConfigService, fhirPath, "testSystem", "testValue", TokenExpression.Operator.EQUALS);
    }

    @Test
    void testInterpreterValidConfig() {
        Bson result = mongoDbTokenExpression.interpreter(expressionContext);
        assertNotNull(result);
        assertEquals(Filters.eq("prefix_indexName-sysval", "testSystem|testValue"), result);
    }

    @Test
    void testInterpreterInvalidConfig() {
        when(searchConfigService.getSearchConfigByPath(fhirPath)).thenReturn(Optional.empty());
        assertThrows(BadConfigurationException.class, () -> mongoDbTokenExpression.interpreter(expressionContext));
    }

    @Test
    void testGetters() {
        assertEquals("testSystem", mongoDbTokenExpression.getSystem());
        assertEquals("testValue", mongoDbTokenExpression.getValue());
    }
    @Test
    void testInterpreterWithOnlySystem() {
        mongoDbTokenExpression = new MongoDbTokenExpression(
                searchConfigService, fhirPath, "testSystem", null, TokenExpression.Operator.EQUALS);

        Bson result = mongoDbTokenExpression.interpreter(expressionContext);

        assertNotNull(result);
        assertEquals(Filters.eq("prefix_indexName-system", "testSystem"), result);
    }

    @Test
    void testInterpreterWithOnlyValue() {
        mongoDbTokenExpression = new MongoDbTokenExpression(
                searchConfigService, fhirPath, null, "testValue", TokenExpression.Operator.EQUALS);

        Bson result = mongoDbTokenExpression.interpreter(expressionContext);

        assertNotNull(result);
        assertEquals(Filters.eq("prefix_indexName-value", "testValue"), result);
    }

    @Test
    void testInterpreterWithNotOperator() {
        mongoDbTokenExpression = new MongoDbTokenExpression(
                searchConfigService, fhirPath, "testSystem", "testValue", TokenExpression.Operator.NOT);

        Bson result = mongoDbTokenExpression.interpreter(expressionContext);

        assertNotNull(result);
        assertEquals(Filters.ne("prefix_indexName-sysval", "testSystem|testValue"), result);
    }

    @Test
    void testInterpreterWithNotOperatorAndOnlySystem() {
        mongoDbTokenExpression = new MongoDbTokenExpression(
                searchConfigService, fhirPath, "testSystem", null, TokenExpression.Operator.NOT);

        Bson result = mongoDbTokenExpression.interpreter(expressionContext);

        assertNotNull(result);
        assertEquals(Filters.ne("prefix_indexName-system", "testSystem"), result);
    }

    @Test
    void testInterpreterWithNotOperatorAndOnlyValue() {
        mongoDbTokenExpression = new MongoDbTokenExpression(
                searchConfigService, fhirPath, null, "testValue", TokenExpression.Operator.NOT);

        Bson result = mongoDbTokenExpression.interpreter(expressionContext);

        assertNotNull(result);
        assertEquals(Filters.ne("prefix_indexName-value", "testValue"), result);
    }
    @Test
    void testSerialize() {
        ExpressionSerializer<Bson> serializer = mock(ExpressionSerializer.class);
        when(serializer.serialize(mongoDbTokenExpression)).thenReturn("testPath|testSystem|testValue|EQUALS");

        String result = mongoDbTokenExpression.serialize(serializer);

        assertNotNull(result);
        assertEquals("testPath|testSystem|testValue|EQUALS", result);
    }

    @Test
    void testDeserialize() {
        // Mock de ExpressionSerializer
        ExpressionSerializer<Bson> deserializer = mock(ExpressionSerializer.class);

        // Création d'une instance valide de MongoDbTokenExpression
        MongoDbTokenExpression expectedExpression = new MongoDbTokenExpression(
                searchConfigService, fhirPath, "testSystem", "testValue", TokenExpression.Operator.EQUALS);

        // Configuration du mock pour retourner l'instance attendue
        when(deserializer.deserialize(any())).thenReturn(expectedExpression);

        // Appel de la méthode à tester
        Expression<Bson> result = mongoDbTokenExpression.deserialize(deserializer);

        // Vérifications
        assertNull(result);

    }
}