package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoDbTokenInExpressionTest {

    private SearchConfigService searchConfigService;
    private FhirSearchPath fhirPath;
    private ExpressionContext expressionContext;
    private MongoDbTokenInExpression mongoDbTokenInExpression;

    @BeforeEach
    void setUp() {
        searchConfigService = mock(SearchConfigService.class);
        fhirPath = new FhirSearchPath("testPath", "testResource",null);
        expressionContext = mock(ExpressionContext.class);

        when(expressionContext.getPrefix()).thenReturn("prefix_");


        var mockSearchParamConfig = mock(SearchParamConfig.class);
        when(mockSearchParamConfig.getIndexName()).thenReturn("indexName");
        when(searchConfigService.getSearchConfigByPath(fhirPath))
                .thenReturn(Optional.of(mockSearchParamConfig));

        mongoDbTokenInExpression = new MongoDbTokenInExpression(
                searchConfigService, fhirPath, "testSystem", List.of("value1", "value2"));
    }

    @Test
    void testInterpreterValidConfig() {
        Bson result = mongoDbTokenInExpression.interpreter(expressionContext);
        assertNotNull(result);
        assertEquals(Filters.in("prefix_indexName-sysval", List.of("testSystem|value1", "testSystem|value2")), result);
    }

    @Test
    void testInterpreterInvalidConfig() {
        when(searchConfigService.getSearchConfigByPath(fhirPath)).thenReturn(Optional.empty());
        assertThrows(BadConfigurationException.class, () -> mongoDbTokenInExpression.interpreter(expressionContext));
    }

    @Test
    void testSerialize() {
        ExpressionSerializer<Bson> serializer = mock(ExpressionSerializer.class);
        when(serializer.serialize(mongoDbTokenInExpression)).thenReturn("serializedExpression");

        String result = mongoDbTokenInExpression.serialize(serializer);
        assertEquals("serializedExpression", result);
    }


    @Test
    void testInGeneration() {
        var expression = new MongoDbTokenInExpression(
                searchConfigService, fhirPath, "testSystem", List.of("value1", "value2"));

        Bson result = expression.interpreter(expressionContext);

        assertNotNull(result);
        assertEquals(Filters.in("prefix_indexName-sysval", List.of("testSystem|value1", "testSystem|value2")), result);
    }
    @Test
    void testDeserialize() {
        ExpressionSerializer<Bson> serializer = mock(ExpressionSerializer.class);

        // Appel de la méthode à tester
        Expression<Bson> result = mongoDbTokenInExpression.deserialize(serializer);

        // Vérification
        assertNull(result, "La méthode deserialize devrait retourner null.");
    }
}