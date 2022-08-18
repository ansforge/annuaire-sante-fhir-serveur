/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.utils.FhirDateUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Test the mongodb implementation of expressions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class ExpressionTest {

    /**
     * Config used for tests
     */
    final TestSearchConfig testSearchConfig = new TestSearchConfig();

    /**
     * Test date expression with precision cases
     */
    @Test
    public void testDateRangePrecisionExpression() {

        var expressionContext = new ExpressionContext();
        var fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_DATE_PATH).build();
        var date = new Date();
        var prefix = ParamPrefixEnum.EQUAL;

        // Test by year:
        var mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.YEAR, prefix);
        var result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_YEAR + "', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.YEAR) + "}", result.toString());

        // Test by month:
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MONTH, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MONTH + "', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MONTH) + "}", result.toString());

        // Test by month:
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.DAY, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_DAY + "', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.DAY) + "}", result.toString());

        // Test by minute:
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MINUTE, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MINUTE + "', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MINUTE) + "}", result.toString());

        // Test by second:
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.SECOND, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_SECOND + "', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.SECOND) + "}", result.toString());

        // Test by ms:
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

    }


    /**
     * Test date expression with prefix cases
     */
    @Test
    public void testDateRangePrefixExpression() {
        var expressionContext = new ExpressionContext();
        var fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_DATE_PATH).build();
        var date = new Date();
        var prefix = ParamPrefixEnum.EQUAL;

        // equals :
        var mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        var result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // gt:
        prefix = ParamPrefixEnum.GREATERTHAN;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', operator='$gt', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // gte:
        prefix = ParamPrefixEnum.GREATERTHAN_OR_EQUALS;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', operator='$gte', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // lt:
        prefix = ParamPrefixEnum.LESSTHAN;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', operator='$lt', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // lte:
        prefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', operator='$lte', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // ne:
        prefix = ParamPrefixEnum.NOT_EQUAL;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', operator='$ne', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // sa:
        prefix = ParamPrefixEnum.STARTS_AFTER;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', operator='$gt', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // eb:
        prefix = ParamPrefixEnum.ENDS_BEFORE;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + MongoDbDateRangeExpression.SUFFIX_MILLI + "', operator='$lt', value=" + FhirDateUtils.getTimeInPrecision(date, TemporalPrecisionEnum.MILLI) + "}", result.toString());

        // ap:
        prefix = ParamPrefixEnum.APPROXIMATE;
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MILLI, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        var dateMin = (long) (date.getTime() / MongoDbDateRangeExpression.APPROX_FACTOR);
        var dateMax = (long) (date.getTime() * MongoDbDateRangeExpression.APPROX_FACTOR);
        Assert.assertEquals("And Filter{filters=[Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + "', operator='$gt', value=" + dateMin + "}, Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + "', operator='$lt', value=" + dateMax + "}]}", result.toString());

        // test the ap with another precision to ensure that the precision doesn't break the ap
        mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.MONTH, prefix);
        result = mongoDbDateRangeExpression.interpreter(expressionContext);
        Assert.assertEquals("And Filter{filters=[Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + "', operator='$gt', value=" + dateMin + "}, Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_DATE_PATH + "', operator='$lt', value=" + dateMax + "}]}", result.toString());

    }


    /**
     * Test reference expression
     */
    @Test
    public void testReferenceExpression() {
        var expressionContext = new ExpressionContext();
        var fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_REFERENCE_PATH).build();
        var id = "FhirResource";
        var type = "123456789";

        var mongoDbCodingExpression = new MongoDbReferenceExpression(testSearchConfig, fhirPath, type, id);
        var result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_REFERENCE_PATH + MongoDbReferenceExpression.REFERENCE_DB_SUFFIX + "', value=" + type + "/" + id + "}", result.toString());

        var mongoDbCodingExpressionIdOnly = new MongoDbReferenceExpression(testSearchConfig, fhirPath, null, id);
        result = mongoDbCodingExpressionIdOnly.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_REFERENCE_PATH + MongoDbReferenceExpression.ID_DB_SUFFIX + "', value=" + id + "}", result.toString());


    }

    /**
     * Test quantity expression.
     */
    @Test
    public void testQuantityExpression() {
        var expressionContext = new ExpressionContext();
        var fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_QUANTITY_PATH).build();
        var quantity = 987654654.12345678;

        // eq
        var mongoDbCodingExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.EQUALS);
        var result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_QUANTITY_PATH + "', value=" + quantity + "}", result.toString());

        // lt
        mongoDbCodingExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.LT);
        result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_QUANTITY_PATH + "', operator='$lt', value=" + quantity + "}", result.toString());

        // gt
        mongoDbCodingExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.GT);
        result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_QUANTITY_PATH + "', operator='$gt', value=" + quantity + "}", result.toString());

        // gte
        mongoDbCodingExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.GE);
        result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_QUANTITY_PATH + "', operator='$gte', value=" + quantity + "}", result.toString());

        // lte
        mongoDbCodingExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.LE);
        result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_QUANTITY_PATH + "', operator='$lte', value=" + quantity + "}", result.toString());

        // ne
        mongoDbCodingExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.NE);
        result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_QUANTITY_PATH + "', operator='$ne', value=" + quantity + "}", result.toString());


    }

    /**
     * Test the token search
     */
    @Test
    public void testTokenExpression() {
        var expressionContext = new ExpressionContext();
        var fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_TOKEN_PATH).build();
        var system = "system_from_param";
        var code = "code_from_param";

        var mongoDbCodingExpression = new MongoDbTokenExpression(testSearchConfig, fhirPath, system, code);
        var result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_TOKEN_PATH + MongoDbTokenExpression.TOKEN_DB_PATH_SUFFIX_SYSVAL + "', value=" + system + "|" + code + "}", result.toString());

        mongoDbCodingExpression = new MongoDbTokenExpression(testSearchConfig, fhirPath, system, null);
        result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_TOKEN_PATH + MongoDbTokenExpression.TOKEN_DB_PATH_SUFFIX_SYSTEM + "', value=" + system + "}", result.toString());

        mongoDbCodingExpression = new MongoDbTokenExpression(testSearchConfig, fhirPath, null, code);
        result = mongoDbCodingExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_TOKEN_PATH + MongoDbTokenExpression.TOKEN_DB_PATH_SUFFIX_VALUE + "', value=" + code + "}", result.toString());
    }

    /**
     * Test the string expression search. There is 3 cases with the FHIR Operators (eq, contains, exact). /FhirResource?string_path=Sample
     */
    @Test
    public void testStringExpression() {
        var expressionContext = new ExpressionContext();
        var fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_STRING_PATH).build();
        var string = "Sample";
        var mongoDbStringExpression = new MongoDbStringExpression(testSearchConfig, fhirPath, string, StringExpression.Operator.EQUALS);
        var result = mongoDbStringExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_STRING_PATH + "', operator='$eq', value=BsonRegularExpression{pattern='^" + string + "', options='i'}}", result.toString());

        mongoDbStringExpression = new MongoDbStringExpression(testSearchConfig, fhirPath, string, StringExpression.Operator.CONTAINS);
        result = mongoDbStringExpression.interpreter(expressionContext);
        Assert.assertEquals("Operator Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_STRING_PATH + "', operator='$eq', value=BsonRegularExpression{pattern='" + string + "', options='i'}}", result.toString());

        mongoDbStringExpression = new MongoDbStringExpression(testSearchConfig, fhirPath, string, StringExpression.Operator.EXACT);
        result = mongoDbStringExpression.interpreter(expressionContext);
        Assert.assertEquals("Filter{fieldName='" + TestSearchConfig.FHIR_RESOURCE_DB_STRING_PATH + "', value=" + string + "}", result.toString());
    }
}
