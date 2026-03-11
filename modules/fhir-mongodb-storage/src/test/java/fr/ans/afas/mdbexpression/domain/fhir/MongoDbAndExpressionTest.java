package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MongoDbAndExpressionTest {

    @InjectMocks
    MongoDbAndExpression mongoDbAndExpression;

    @Mock
    ExpressionContext expressionContext;

    @Mock
    Expression<Bson> expression1;

    @Mock
    Expression<Bson> expression2;

    @Mock
    ExpressionSerializer expressionSerializer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void interpreter_SingleExpression() {
        Bson bson = Filters.eq("field", "value");
        when(expression1.interpreter(expressionContext)).thenReturn(bson);
        mongoDbAndExpression.getExpressions().add(expression1);

        Bson result = mongoDbAndExpression.interpreter(expressionContext);

        assertEquals(bson, result);
    }

    @Test
    void interpreter_MultipleExpressions() {
        Bson bson1 = Filters.eq("field1", "value1");
        Bson bson2 = Filters.eq("field2", "value2");
        when(expression1.interpreter(expressionContext)).thenReturn(bson1);
        when(expression2.interpreter(expressionContext)).thenReturn(bson2);
        mongoDbAndExpression.getExpressions().add(expression1);
        mongoDbAndExpression.getExpressions().add(expression2);

        Bson result = mongoDbAndExpression.interpreter(expressionContext);

        assertNotNull(result);
        assertInstanceOf(Bson.class, result);
    }

    @Test
    void interpreter_NoExpressions() {
        Bson result = mongoDbAndExpression.interpreter(expressionContext);

        assertNull(result);
    }

    @Test
    void interpreter_NullExpression() {
        when(expression1.interpreter(expressionContext)).thenReturn(null);
        mongoDbAndExpression.getExpressions().add(expression1);

        Bson result = mongoDbAndExpression.interpreter(expressionContext);

        assertNull(result);
    }

    @Test
    void serialize_Expression() {
        String serialized = "serializedExpression";
        when(expressionSerializer.serialize(mongoDbAndExpression)).thenReturn(serialized);

        String result = mongoDbAndExpression.serialize(expressionSerializer);

        assertEquals(serialized, result);
    }
    @Test
    void testDeserialize() {
        ExpressionSerializer<Bson> serializer = mock(ExpressionSerializer.class);

        // Appel de la méthode à tester
        Expression<Bson> result = mongoDbAndExpression.deserialize(serializer);

        // Vérification
        assertNull(result, "La méthode deserialize devrait retourner null.");
    }

    @Test
    void interpreter_ExpressionsWithNullValues() {
        Bson bson1 = Filters.eq("field1", "value1");
        when(expression1.interpreter(expressionContext)).thenReturn(bson1);
        when(expression2.interpreter(expressionContext)).thenReturn(null);

        mongoDbAndExpression.getExpressions().add(expression1);
        mongoDbAndExpression.getExpressions().add(expression2);

        Bson result = mongoDbAndExpression.interpreter(expressionContext);

        assertNotNull(result);
        assertEquals(bson1, result, "Le résultat devrait ignorer les expressions nulles.");
    }
}

