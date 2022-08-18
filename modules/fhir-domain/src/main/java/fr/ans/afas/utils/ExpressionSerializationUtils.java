/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.expression.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class to work with serialization of expressions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public final class ExpressionSerializationUtils {

    /**
     * Contains the class / code matching
     */
    private static final Map<Class<? extends Expression>, String> codeExpressionClass = new HashMap<>();
    /**
     * Contains the code matching / class
     */
    private static final Map<String, Class<? extends Expression>> classCodeExpression = new HashMap<>();

    static {
        codeExpressionClass.put(AndExpression.class, "0");
        codeExpressionClass.put(OrExpression.class, "1");
        codeExpressionClass.put(StringExpression.class, "2");
        codeExpressionClass.put(QuantityExpression.class, "3");
        codeExpressionClass.put(TokenExpression.class, "4");
        codeExpressionClass.put(DateRangeExpression.class, "5");
        codeExpressionClass.put(SelectExpression.class, "6");
        codeExpressionClass.put(ReferenceExpression.class, "7");
        for (var classCode : codeExpressionClass.entrySet()) {
            classCodeExpression.put(classCode.getValue(), classCode.getKey());
        }
    }

    private ExpressionSerializationUtils() {
    }

    /**
     * Get the code corresponding an expression
     *
     * @param expressionClass the class to get the code
     * @return the corresponding code
     */
    public static String getCodeForClass(Class<? extends Expression> expressionClass) {
        if (!codeExpressionClass.containsKey(expressionClass)) {
            throw new SerializationException("Can't use expression in serialization. Class not supported: " + expressionClass.getSimpleName());
        }
        return codeExpressionClass.get(expressionClass);
    }

    /**
     * Get the class corresponding to a code
     *
     * @param code the code to get the class
     * @return the corresponding class
     */
    public static Class<? extends Expression> getClassForCode(String code) {
        if (!classCodeExpression.containsKey(code)) {
            throw new SerializationException("Can't use expression in serialization. Code not supported: " + code);
        }
        return classCodeExpression.get(code);
    }
}
