/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.AndExpression;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.mdbexpression.domain.fhir.serialization.MongoDbExpressionSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test the serialization of Expression for MongoDB
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class ExpressionSerializationTest {

    final TestSearchConfig testSearchConfig = new TestSearchConfig();
    final MongoDbExpressionFactory expressionFactory = new MongoDbExpressionFactory(testSearchConfig);
    final MongoDbExpressionSerializer mongoDbExpressionSerializer = new MongoDbExpressionSerializer(expressionFactory, testSearchConfig);


    Date date = new Date();
    String sampleId = "123456";
    long quantity = 1234;
    String system = "https://sample";
    String code = "aCode";
    String string = "sampleString";
    String string2 = "sampleString2";

    FhirSearchPath fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_DATE_PATH).build();
    FhirSearchPath fhirPath2 = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_STRING_PATH).build();

    MongoDbDateRangeExpression mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.YEAR, ParamPrefixEnum.EQUAL);
    MongoDbReferenceExpression mongoDbReferenceExpression = new MongoDbReferenceExpression(testSearchConfig, fhirPath, TestSearchConfig.FHIR_RESOURCE_NAME, sampleId);
    MongoDbQuantityExpression mongoDbQuantityExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.EQUALS);
    MongoDbTokenExpression mongoDbTokenExpression = new MongoDbTokenExpression(testSearchConfig, fhirPath, system, code);
    MongoDbStringExpression mongoDbStringExpression = new MongoDbStringExpression(testSearchConfig, fhirPath, string, StringExpression.Operator.EQUALS);
    MongoDbStringExpression mongoDbStringExpression2 = new MongoDbStringExpression(testSearchConfig, fhirPath2, string2, StringExpression.Operator.CONTAINS);

    Set<Include> theInclude = Set.of(
            new Include("Organization:a"),
            new Include("Organization:b")
    );

    @Test
    public void testAndSerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);

        selectExpression.getExpression().addExpression(mongoDbDateRangeExpression);
        selectExpression.getExpression().addExpression(mongoDbReferenceExpression);
        selectExpression.getExpression().addExpression(mongoDbQuantityExpression);
        selectExpression.getExpression().addExpression(mongoDbTokenExpression);
        selectExpression.getExpression().addExpression(mongoDbStringExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);


        Assert.assertTrue(deserialized instanceof SelectExpression);
        Assert.assertTrue(((SelectExpression) deserialized).getExpression() instanceof AndExpression);
        var andExpression = (AndExpression) ((SelectExpression) deserialized).getExpression();
        Assert.assertEquals(5, andExpression.getExpressions().size());
        Assert.assertTrue(andExpression.getExpressions().get(0) instanceof MongoDbDateRangeExpression);
        Assert.assertTrue(andExpression.getExpressions().get(1) instanceof MongoDbReferenceExpression);
        Assert.assertTrue(andExpression.getExpressions().get(2) instanceof MongoDbQuantityExpression);
        Assert.assertTrue(andExpression.getExpressions().get(3) instanceof MongoDbTokenExpression);
        Assert.assertTrue(andExpression.getExpressions().get(4) instanceof MongoDbStringExpression);
    }

    @Test
    public void testOrSerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);

        selectExpression.getExpression().addExpression(mongoDbDateRangeExpression);
        var or = new MongoDbOrExpression();
        selectExpression.getExpression().addExpression(or);
        or.addExpression(mongoDbStringExpression);
        or.addExpression(mongoDbStringExpression2);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        Assert.assertTrue(deserialized instanceof SelectExpression);
        Assert.assertTrue(((SelectExpression) deserialized).getExpression() instanceof AndExpression);
        var andExpression = (AndExpression) ((SelectExpression) deserialized).getExpression();
        Assert.assertEquals(2, andExpression.getExpressions().size());
        Assert.assertTrue(andExpression.getExpressions().get(0) instanceof MongoDbDateRangeExpression);
        Assert.assertTrue(andExpression.getExpressions().get(1) instanceof MongoDbOrExpression);
        var orExpression = (MongoDbOrExpression) andExpression.getExpressions().get(1);
        Assert.assertTrue(orExpression.getExpressions().get(0) instanceof MongoDbStringExpression);
        Assert.assertTrue(orExpression.getExpressions().get(1) instanceof MongoDbStringExpression);
    }

    @Test
    public void testDateSerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbDateRangeExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression) ((SelectExpression) deserialized).getExpression();

        var mongoDbDateRangeExpression = (MongoDbDateRangeExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(date.getTime(), mongoDbDateRangeExpression.getDate().getTime());
        Assert.assertEquals(fhirPath.getResource(), mongoDbDateRangeExpression.getFhirPath().getResource());
        Assert.assertEquals(fhirPath.getPath(), mongoDbDateRangeExpression.getFhirPath().getPath());
        Assert.assertEquals(TemporalPrecisionEnum.YEAR, mongoDbDateRangeExpression.getPrecision());
        Assert.assertEquals(ParamPrefixEnum.EQUAL, mongoDbDateRangeExpression.getPrefix());
    }


    @Test
    public void testTokenSerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbTokenExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression) ((SelectExpression) deserialized).getExpression();

        var mongoDbTokenExpression1 = (MongoDbTokenExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(system, mongoDbTokenExpression1.getSystem());
        Assert.assertEquals(code, mongoDbTokenExpression1.getValue());
        Assert.assertEquals(fhirPath.getPath(), mongoDbTokenExpression1.getFhirPath().getPath());
    }


    @Test
    public void testQuantitySerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbQuantityExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression) ((SelectExpression) deserialized).getExpression();

        var mongoDbQuantityExpression1 = (MongoDbQuantityExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(QuantityExpression.Operator.EQUALS, mongoDbQuantityExpression1.getOperator());
        Assert.assertEquals((double) quantity, mongoDbQuantityExpression1.getValue());
        Assert.assertEquals(fhirPath.getPath(), mongoDbQuantityExpression1.getFhirPath().getPath());
    }

    @Test
    public void testStringSerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbStringExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression) ((SelectExpression) deserialized).getExpression();

        var mongoDbStringExpression1 = (MongoDbStringExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(StringExpression.Operator.EQUALS, mongoDbStringExpression1.getOperator());
        Assert.assertEquals(string, mongoDbStringExpression1.getValue());
        Assert.assertEquals(fhirPath.getPath(), mongoDbStringExpression1.getFhirPath().getPath());
    }

    @Test
    public void testReferenceSerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbReferenceExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression) ((SelectExpression) deserialized).getExpression();

        var mongoDbReferenceExpression1 = (MongoDbReferenceExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(mongoDbReferenceExpression.getType(), mongoDbReferenceExpression1.getType());
        Assert.assertEquals(sampleId, mongoDbReferenceExpression1.getId());
        Assert.assertEquals(fhirPath.getPath(), mongoDbReferenceExpression1.getFhirPath().getPath());
    }


    @Test
    public void testRevIncludeSerialization() {
        SelectExpression selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.fromFhirParamsRevInclude(theInclude);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = (SelectExpression) mongoDbExpressionSerializer.deserialize(serialized);

        var rv = ((Set<MongoDbIncludeExpression>) deserialized.getRevincludes()).stream().collect(Collectors.toList());
        Assert.assertEquals(2, rv.size());
        Assert.assertEquals("Organization", rv.get(0).getType());
        Assert.assertTrue("b".equals(rv.get(0).getName()) || "a".equals(rv.get(0).getName()));
        Assert.assertEquals("Organization", rv.get(1).getType());
        Assert.assertTrue("b".equals(rv.get(1).getName()) || "a".equals(rv.get(1).getName()));

    }


}
