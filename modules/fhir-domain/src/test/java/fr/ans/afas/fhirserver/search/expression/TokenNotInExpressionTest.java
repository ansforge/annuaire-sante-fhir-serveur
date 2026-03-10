package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenNotInExpressionTest {

    private TokenNotInExpression<String> tokenNotInExpression;
    private Expression<String> testExpression;

    @BeforeEach
    void setUp() {
        FhirSearchPath fhirPath = new FhirSearchPath("testPath", "testResource",null);
        String system = "testSystem";
        List<String> values = List.of("value1", "value2", "value3");

        tokenNotInExpression = new TokenNotInExpression<>(fhirPath, system, values) {
            @Override
            public String interpreter(ExpressionContext expressionContext) {
                return "";
            }

            @Override
            public String serialize(ExpressionSerializer<String> expressionSerializer) {
                return "";
            }

            @Override
            public Expression<String> deserialize(ExpressionSerializer<String> expressionDeserializer) {
                return null;
            }
        };

        testExpression = new Expression<>() {
            @Override
            public String interpreter(ExpressionContext expressionContext) {
                return "";
            }

            @Override
            public String serialize(ExpressionSerializer<String> expressionSerializer) {
                return "";
            }

            @Override
            public Expression<String> deserialize(ExpressionSerializer<String> expressionDeserializer) {
                return null;
            }

            @Override
            public String toString() {
                return "testExpression";
            }
        };
    }

    @Test
    void testConstructorInitialization() {
        assertNotNull(tokenNotInExpression.getFhirPath());
        assertEquals("testSystem", tokenNotInExpression.getSystem());
        assertEquals(List.of("value1", "value2", "value3"), tokenNotInExpression.getValues());
    }

    @Test
    void testAddExpression() {
        tokenNotInExpression.addExpression(testExpression);

        assertEquals(1, tokenNotInExpression.getExpressions().size());
        assertEquals("testExpression", tokenNotInExpression.getExpressions().get(0).toString());
    }

    @Test
    void testNotInMethod() {
        tokenNotInExpression.notIn(testExpression);

        assertEquals(1, tokenNotInExpression.getExpressions().size());
        assertEquals("testExpression", tokenNotInExpression.getExpressions().get(0).toString());
    }

    @Test
    void testToStringMethod() {
        tokenNotInExpression.notIn(testExpression);

        String expected = "testExpression NIN";
        assertEquals(expected, tokenNotInExpression.toString());
    }
}