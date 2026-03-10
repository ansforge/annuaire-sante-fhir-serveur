package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.HasCondition;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MongoDbChainedReferenceExpressionTest {

    private SearchConfigService searchConfigService;
    private ExpressionContext expressionContext;
    private FhirSearchPath fhirPath;
    private HasCondition<Bson> hasCondition;
    private MongoDbChainedReferenceExpression mongoDbChainedReferenceExpression;

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

        mongoDbChainedReferenceExpression = new MongoDbChainedReferenceExpression(
                searchConfigService, fhirPath,  hasCondition
        );
    }



    @Test
    void testInterpreterWithNullChain() {
        // Initialisation de hasCondition
        HasCondition<Bson> hasCondition = mock(HasCondition.class);

        MongoDbChainedReferenceExpression expression = spy(new MongoDbChainedReferenceExpression(
                searchConfigService, fhirPath, hasCondition));

        Bson result = expression.interpreter(expressionContext);

        assertNotNull(result, "Le résultat ne doit pas être null");
        assertEquals(Filters.eq("prefix_indexName-id", null).toBsonDocument(), result.toBsonDocument()
        );
    }


    @Test
    void testInterpreterWithInvalidPath() {
        when(searchConfigService.getSearchConfigByPath(fhirPath)).thenReturn(Optional.empty());

        MongoDbChainedReferenceExpression expression = new MongoDbChainedReferenceExpression(
                searchConfigService, fhirPath, hasCondition);

        assertThrows(BadConfigurationException.class, () -> expression.interpreter(expressionContext));
    }

    @Test
    void testInterpreterWithGetIdVerification() {
        MongoDbChainedReferenceExpression expression = spy(new MongoDbChainedReferenceExpression(
                searchConfigService, fhirPath, hasCondition));

        Class<?> clazz = MongoDbChainedReferenceExpression.class.getSuperclass();
        while (clazz != null) {
            try {
                var typeField = clazz.getDeclaredField("type");
                typeField.setAccessible(true);
                typeField.set(expression, "testType");

                var idField = clazz.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(expression, "testId");
                break; // Champs trouvés, on sort de la boucle
            } catch (NoSuchFieldException | IllegalAccessException e) {
                clazz = clazz.getSuperclass();
            }
        }



        Bson result = expression.interpreter(expressionContext);

        assertNotNull(result, "Le résultat ne doit pas être null");
        assertEquals(Filters.eq("prefix_indexName-reference", "testType/testId").toBsonDocument(), result.toBsonDocument(),
                "Le résultat BSON ne correspond pas à la valeur attendue");
    }

}