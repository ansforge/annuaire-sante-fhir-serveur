package fr.ans.afas.fhirserver.search.expression;

import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenInExpressionTest {

    private TokenInExpression<String> tokenInExpression;
    private Expression<String> testExpression;

    @BeforeEach
    void setUp() {
        FhirSearchPath fhirPath = new FhirSearchPath("testPath", "testResource",null);
        String system = "testSystem";
        List<String> values = List.of("value1", "value2", "value3");

        tokenInExpression = new TokenInExpression<>(fhirPath, system, values) {
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
        assertNotNull(tokenInExpression.getFhirPath());
        assertEquals("testSystem", tokenInExpression.getSystem());
        assertEquals(List.of("value1", "value2", "value3"), tokenInExpression.getValues());
    }

    @Test
    void testAddExpression() {
        tokenInExpression.addExpression(testExpression);

        assertEquals(1, tokenInExpression.getExpressions().size());
        assertEquals("testExpression", tokenInExpression.getExpressions().get(0).toString());
    }

    @Test
    void testInMethod() {
        tokenInExpression.in(testExpression);

        assertEquals(1, tokenInExpression.getExpressions().size());
        assertEquals("testExpression", tokenInExpression.getExpressions().get(0).toString());
    }


    @Test
    void testToStringMethod() {
        tokenInExpression.in(testExpression);

        String expected = "testExpression IN";
        assertEquals(expected, tokenInExpression.toString());
    }
}