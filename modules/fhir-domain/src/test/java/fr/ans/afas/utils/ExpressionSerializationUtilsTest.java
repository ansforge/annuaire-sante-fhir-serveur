package fr.ans.afas.utils;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.expression.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionSerializationUtilsTest {

    @Test
    void testGetCodeForClass_Success() {
        assertEquals("0", ExpressionSerializationUtils.getCodeForClass(AndExpression.class));
        assertEquals("1", ExpressionSerializationUtils.getCodeForClass(OrExpression.class));
        assertEquals("2", ExpressionSerializationUtils.getCodeForClass(StringExpression.class));
        assertEquals("3", ExpressionSerializationUtils.getCodeForClass(QuantityExpression.class));
        assertEquals("4", ExpressionSerializationUtils.getCodeForClass(TokenExpression.class));
        assertEquals("5", ExpressionSerializationUtils.getCodeForClass(DateRangeExpression.class));
        assertEquals("6", ExpressionSerializationUtils.getCodeForClass(SelectExpression.class));
        assertEquals("7", ExpressionSerializationUtils.getCodeForClass(ReferenceExpression.class));
        assertEquals("8", ExpressionSerializationUtils.getCodeForClass(HasCondition.class));
    }


    @Test
    void testGetClassForCode_Success() {
        assertEquals(AndExpression.class, ExpressionSerializationUtils.getClassForCode("0"));
        assertEquals(OrExpression.class, ExpressionSerializationUtils.getClassForCode("1"));
        assertEquals(StringExpression.class, ExpressionSerializationUtils.getClassForCode("2"));
        assertEquals(QuantityExpression.class, ExpressionSerializationUtils.getClassForCode("3"));
        assertEquals(TokenExpression.class, ExpressionSerializationUtils.getClassForCode("4"));
        assertEquals(DateRangeExpression.class, ExpressionSerializationUtils.getClassForCode("5"));
        assertEquals(SelectExpression.class, ExpressionSerializationUtils.getClassForCode("6"));
        assertEquals(ReferenceExpression.class, ExpressionSerializationUtils.getClassForCode("7"));
        assertEquals(HasCondition.class, ExpressionSerializationUtils.getClassForCode("8"));
    }

    @Test
    void testGetClassForCode_CodeNotSupported() {
        String unsupportedCode = "9";

        Exception exception = assertThrows(SerializationException.class, () ->
                ExpressionSerializationUtils.getClassForCode(unsupportedCode));

        String expectedMessage = "Can't use expression in serialization. Code not supported: " + unsupportedCode;
        assertEquals(expectedMessage, exception.getMessage());
    }
}

