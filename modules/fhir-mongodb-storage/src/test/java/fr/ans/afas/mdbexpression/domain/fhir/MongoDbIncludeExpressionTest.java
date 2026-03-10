package fr.ans.afas.mdbexpression.domain.fhir;

import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MongoDbIncludeExpressionTest {

    @Mock
    private SearchConfigService searchConfigService;

    @Mock
    private ExpressionContext expressionContext;

    @Mock
    private ExpressionSerializer<Bson> expressionSerializer;

    private MongoDbIncludeExpression includeExpression;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        includeExpression = new MongoDbIncludeExpression(searchConfigService, "Patient", "name");
    }

    @Test
    void testInterpreterWithValidConfig() {
        // Mock d'une configuration valide
        var mockSearchParamConfig = mock(SearchParamConfig.class);
        when(mockSearchParamConfig.getIndexName()).thenReturn("indexName");
        when(searchConfigService.getSearchConfigByResourceAndParamName("Patient", "name"))
                .thenReturn(Optional.of(mockSearchParamConfig));

        // Appel de la méthode à tester
        Bson result = includeExpression.interpreter(expressionContext);

        // Vérification
        assertNull(result, "Le résultat devrait être null car la méthode retourne null.");
    }

    @Test
    void testInterpreterWithMissingConfig() {
        // Mock d'une configuration manquante
        when(searchConfigService.getSearchConfigByResourceAndParamName("Patient", "name"))
                .thenReturn(Optional.empty());

        // Vérification de l'exception
        assertThrows(BadConfigurationException.class,
                () -> includeExpression.interpreter(expressionContext),
                "Une BadConfigurationException aurait dû être levée.");
    }

    @Test
    void testSerialize() {
        // Mock de la sérialisation
        when(expressionSerializer.serialize(includeExpression)).thenReturn("serializedExpression");

        // Appel de la méthode à tester
        String result = includeExpression.serialize(expressionSerializer);

        // Vérification
        assertEquals("serializedExpression", result, "La sérialisation devrait retourner 'serializedExpression'.");
    }

    @Test
    void testDeserialize() {
        // Appel de la méthode à tester
        Expression<Bson> result = includeExpression.deserialize(expressionSerializer);

        // Vérification
        assertNull(result, "La méthode deserialize devrait retourner null.");
    }
}