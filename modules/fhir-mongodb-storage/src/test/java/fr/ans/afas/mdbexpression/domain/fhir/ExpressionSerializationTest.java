/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.AndExpression;
import fr.ans.afas.fhirserver.search.expression.QuantityExpression;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.mdbexpression.domain.fhir.serialization.MongoDbExpressionSerializer;
import org.bson.conversions.Bson;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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


    final Date date = new Date();
    final String sampleId = "123456";
    final long quantity = 1234;
    final String system = "https://sample";
    final String code = "aCode";
    final String string = "sampleString";
    final String string2 = "sampleString2";

    final FhirSearchPath fhirPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_DATE_PATH).build();
    final FhirSearchPath fhirPath2 = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_NAME).path(TestSearchConfig.FHIR_RESOURCE_STRING_PATH).build();

    final MongoDbDateRangeExpression mongoDbDateRangeExpression = new MongoDbDateRangeExpression(testSearchConfig, fhirPath, date, TemporalPrecisionEnum.YEAR, ParamPrefixEnum.EQUAL);
    final MongoDbReferenceExpression mongoDbReferenceExpression = new MongoDbReferenceExpression(testSearchConfig, fhirPath, TestSearchConfig.FHIR_RESOURCE_NAME, sampleId);
    final MongoDbQuantityExpression mongoDbQuantityExpression = new MongoDbQuantityExpression(testSearchConfig, fhirPath, quantity, QuantityExpression.Operator.EQUALS);
    final MongoDbTokenExpression mongoDbTokenExpression = new MongoDbTokenExpression(testSearchConfig, fhirPath, system, code);
    final MongoDbStringExpression mongoDbStringExpression = new MongoDbStringExpression(testSearchConfig, fhirPath, string, StringExpression.Operator.EQUALS);
    final MongoDbStringExpression mongoDbStringExpression2 = new MongoDbStringExpression(testSearchConfig, fhirPath2, string2, StringExpression.Operator.CONTAINS);

    final Set<Include> theInclude = Set.of(
            new Include("Organization:a"),
            new Include("Organization:b")
    );

    @Test
    public void testAndSerialization() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);

        selectExpression.getExpression().addExpression(mongoDbDateRangeExpression);
        selectExpression.getExpression().addExpression(mongoDbReferenceExpression);
        selectExpression.getExpression().addExpression(mongoDbQuantityExpression);
        selectExpression.getExpression().addExpression(mongoDbTokenExpression);
        selectExpression.getExpression().addExpression(mongoDbStringExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);


        Assert.assertTrue(deserialized instanceof SelectExpression);
        Assert.assertTrue(((SelectExpression<Bson>) deserialized).getExpression() instanceof AndExpression);
        var andExpression = (AndExpression<Bson>) ((SelectExpression<Bson>) deserialized).getExpression();
        Assert.assertEquals(5, andExpression.getExpressions().size());
        Assert.assertFalse(andExpression.toString().isEmpty());
        Assert.assertTrue(andExpression.getExpressions().get(0) instanceof MongoDbDateRangeExpression);
        Assert.assertTrue(andExpression.getExpressions().get(1) instanceof MongoDbReferenceExpression);
        Assert.assertTrue(andExpression.getExpressions().get(2) instanceof MongoDbQuantityExpression);
        Assert.assertTrue(andExpression.getExpressions().get(3) instanceof MongoDbTokenExpression);
        Assert.assertTrue(andExpression.getExpressions().get(4) instanceof MongoDbStringExpression);
    }

    @Test
    public void testOrSerialization() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);

        selectExpression.getExpression().addExpression(mongoDbDateRangeExpression);
        var or = new MongoDbOrExpression();
        selectExpression.getExpression().addExpression(or);
        or.addExpression(mongoDbStringExpression);
        or.addExpression(mongoDbStringExpression2);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        Assert.assertTrue(deserialized instanceof SelectExpression);
        Assert.assertTrue(((SelectExpression<Bson>) deserialized).getExpression() instanceof AndExpression);
        var andExpression = (AndExpression<Bson>) ((SelectExpression<Bson>) deserialized).getExpression();
        Assert.assertFalse(andExpression.toString().isEmpty());
        Assert.assertEquals(2, andExpression.getExpressions().size());
        Assert.assertTrue(andExpression.getExpressions().get(0) instanceof MongoDbDateRangeExpression);
        Assert.assertTrue(andExpression.getExpressions().get(1) instanceof MongoDbOrExpression);
        var orExpression = (MongoDbOrExpression) andExpression.getExpressions().get(1);
        Assert.assertFalse(orExpression.toString().isEmpty());
        Assert.assertTrue(orExpression.getExpressions().get(0) instanceof MongoDbStringExpression);
        Assert.assertTrue(orExpression.getExpressions().get(1) instanceof MongoDbStringExpression);
    }

    @Test
    public void testDateSerialization() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbDateRangeExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression<Bson>) ((SelectExpression<Bson>) deserialized).getExpression();

        var mongoDbDateRangeExpression = (MongoDbDateRangeExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(date.getTime(), mongoDbDateRangeExpression.getDate().getTime());
        Assert.assertEquals(fhirPath.getResource(), mongoDbDateRangeExpression.getFhirPath().getResource());
        Assert.assertEquals(fhirPath.getPath(), mongoDbDateRangeExpression.getFhirPath().getPath());
        Assert.assertEquals(TemporalPrecisionEnum.YEAR, mongoDbDateRangeExpression.getPrecision());
        Assert.assertEquals(ParamPrefixEnum.EQUAL, mongoDbDateRangeExpression.getPrefix());
    }


    @Test
    public void testTokenSerialization() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbTokenExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression<Bson>) ((SelectExpression<Bson>) deserialized).getExpression();

        var mongoDbTokenExpression1 = (MongoDbTokenExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(system, mongoDbTokenExpression1.getSystem());
        Assert.assertEquals(code, mongoDbTokenExpression1.getValue());
        Assert.assertEquals(fhirPath.getPath(), mongoDbTokenExpression1.getFhirPath().getPath());
    }


    @Test
    public void testQuantitySerialization() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbQuantityExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression<Bson>) ((SelectExpression<Bson>) deserialized).getExpression();

        var mongoDbQuantityExpression1 = (MongoDbQuantityExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(QuantityExpression.Operator.EQUALS, mongoDbQuantityExpression1.getOperator());
        Assert.assertEquals((double) quantity, mongoDbQuantityExpression1.getValue());
        Assert.assertEquals(fhirPath.getPath(), mongoDbQuantityExpression1.getFhirPath().getPath());
    }

    @Test
    public void testStringSerialization() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbStringExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression<Bson>) ((SelectExpression<Bson>) deserialized).getExpression();

        var mongoDbStringExpression1 = (MongoDbStringExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(StringExpression.Operator.EQUALS, mongoDbStringExpression1.getOperator());
        Assert.assertEquals(string, mongoDbStringExpression1.getValue());
        Assert.assertEquals(fhirPath.getPath(), mongoDbStringExpression1.getFhirPath().getPath());
    }

    @Test
    public void testReferenceSerialization() {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.getExpression().addExpression(mongoDbReferenceExpression);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = mongoDbExpressionSerializer.deserialize(serialized);

        var andExpression = (AndExpression<Bson>) ((SelectExpression<Bson>) deserialized).getExpression();

        var mongoDbReferenceExpression1 = (MongoDbReferenceExpression) andExpression.getExpressions().get(0);
        Assert.assertEquals(mongoDbReferenceExpression.getType(), mongoDbReferenceExpression1.getType());
        Assert.assertEquals(sampleId, mongoDbReferenceExpression1.getId());
        Assert.assertEquals(fhirPath.getPath(), mongoDbReferenceExpression1.getFhirPath().getPath());
    }


    @Test
    public void testRevIncludeSerialization() throws BadDataFormatException {
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.fromFhirParamsRevInclude(theInclude);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = (SelectExpression<Bson>) mongoDbExpressionSerializer.deserialize(serialized);

        var rv = new ArrayList<>(deserialized.getRevincludes());
        Assert.assertEquals(2, rv.size());
        Assert.assertEquals("Organization", rv.get(0).getType());
        Assert.assertTrue("b".equals(rv.get(0).getName()) || "a".equals(rv.get(0).getName()));
        Assert.assertEquals("Organization", rv.get(1).getType());
        Assert.assertTrue("b".equals(rv.get(1).getName()) || "a".equals(rv.get(1).getName()));

    }

    @Test
    public void testHasConditionSerialization() {
        var selectExpression = new SelectExpression<>(TestSearchConfig.FHIR_RESOURCE_NAME, expressionFactory);
        var paramPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_SUB_NAME).path(TestSearchConfig.FHIR_RESOURCE_SUB_STRING_PATH).build();
        var linkPath = FhirSearchPath.builder().resource(TestSearchConfig.FHIR_RESOURCE_SUB_NAME).path(TestSearchConfig.FHIR_RESOURCE_SUB_REFERENCE_PATH).build();
        var hasCondition = expressionFactory.newHasExpression(linkPath, paramPath, List.of("my name is"));
        selectExpression.getHasConditions().add(hasCondition);

        var serialized = mongoDbExpressionSerializer.serialize(selectExpression);
        var deserialized = (SelectExpression<Bson>) mongoDbExpressionSerializer.deserialize(serialized);

        Assert.assertEquals(1, deserialized.getHasConditions().size());

        var h = deserialized.getHasConditions().get(0);
        Assert.assertEquals(TestSearchConfig.FHIR_RESOURCE_SUB_NAME, h.getFhirPath().getResource());
        Assert.assertEquals(TestSearchConfig.FHIR_RESOURCE_SUB_REFERENCE_PATH, h.getFhirPath().getPath());
        Assert.assertEquals(1, h.getExpressions().size());
        var subEx = ((MongoDbOrExpression) h.getExpressions().get(0)).getExpressions().get(0);
        Assert.assertTrue(subEx instanceof StringExpression);
        Assert.assertEquals(TestSearchConfig.FHIR_RESOURCE_SUB_STRING_PATH, ((StringExpression<Bson>) subEx).getFhirPath().getPath());
        Assert.assertEquals(TestSearchConfig.FHIR_RESOURCE_SUB_NAME, ((StringExpression<Bson>) subEx).getFhirPath().getResource());
        Assert.assertEquals("my name is", ((StringExpression<Bson>) subEx).getValue());


    }

}
