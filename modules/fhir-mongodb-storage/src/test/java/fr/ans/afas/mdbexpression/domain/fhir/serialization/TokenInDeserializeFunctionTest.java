package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbTokenInExpression;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TokenInDeserializeFunctionTest {

    @Test
    void testProcess() {

        SearchConfigService searchConfigService = mock(SearchConfigService.class);
        ExpressionFactory<Bson> expressionFactory = mock(ExpressionFactory.class);
        ExpressionSerializer<Bson> expressionDeserializer = mock(ExpressionSerializer.class);

        String serializedValue = "TestSystem$value1$value2$Organization$type";

        TokenInDeserializeFunction function = new TokenInDeserializeFunction();

        var result = function.process(searchConfigService, expressionFactory, expressionDeserializer, serializedValue);

        assertTrue(result instanceof MongoDbTokenInExpression);
        MongoDbTokenInExpression expression = (MongoDbTokenInExpression) result;
        assertEquals("TestSystem", expression.getSystem());
        assertEquals(List.of("value1", "value2"), expression.getValues());
        assertEquals("Organization", expression.getFhirPath().getResource());
        assertEquals("type", expression.getFhirPath().getPath());
    }

}