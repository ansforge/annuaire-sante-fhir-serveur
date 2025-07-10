/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.exception.BadParametersException;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.*;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

/**
 * Test the select expression system
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
class SelectExpressionTest {


    static final FhirSearchPath pathString = FhirSearchPath.builder().resource("FhirResource").path("string_path").build();
    static final FhirSearchPath pathString2 = FhirSearchPath.builder().resource("FhirResource").path("string_path2").build();
    static final FhirSearchPath pathToken = FhirSearchPath.builder().resource("FhirResource").path("token_path").build();
    static final FhirSearchPath pathDate = FhirSearchPath.builder().resource("FhirResource").path("date_path").build();
    static final FhirSearchPath pathReference = FhirSearchPath.builder().resource("FhirResource").path("reference_path").build();
    static final FhirSearchPath pathUri = FhirSearchPath.builder().resource("FhirResource").path("uri_path").build();
     static final FhirSearchPath pathHasParam = FhirSearchPath.builder().resource("FhirResourceSub").path("sub_path").build();
    static final FhirSearchPath pathHasLink = FhirSearchPath.builder().resource("FhirResourceSub").path("sub_link").build();
    static final Date testDate = new Date(1663849410512L);
    final ExpressionFactory<?> expressionFactory = Mockito.mock(ExpressionFactory.class);

    @BeforeEach
    public void setup() {
        Mockito.when(expressionFactory.newAndExpression()).then(a -> new EmptyAndExpression());
        Mockito.when(expressionFactory.newOrExpression()).then(a -> new EmptyOrExpression());
        Mockito.when(expressionFactory.newStringExpression(pathString, "bla", StringExpression.Operator.EXACT)).then(a -> new EmptyStringExpression(pathString, "bla", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString, "blo", StringExpression.Operator.EXACT)).then(a -> new EmptyStringExpression(pathString, "blo", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString2, "bla", StringExpression.Operator.EXACT)).then(a -> new EmptyStringExpression(pathString2, "bla", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString2, "blo", StringExpression.Operator.EXACT)).then(a -> new EmptyStringExpression(pathString2, "blo", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newTokenExpression(pathToken, "id", "123",TokenExpression.Operator.NOT)).then(a -> new EmptyTokenExpression(pathToken, "id", "123",TokenExpression.Operator.NOT));
        Mockito.when(expressionFactory.newTokenExpression(pathToken, "s", "bla",TokenExpression.Operator.EQUALS)).then(a -> new EmptyTokenExpression(pathToken, "s", "bla",TokenExpression.Operator.EQUALS));
        Mockito.when(expressionFactory.newDateRangeExpression(Mockito.any(), Mockito.any(), Mockito.isA(TemporalPrecisionEnum.class), Mockito.isA(ParamPrefixEnum.class))).then(a -> new EmptyDateExpression(pathDate, testDate, TemporalPrecisionEnum.DAY, ParamPrefixEnum.GREATERTHAN));
        Mockito.when(expressionFactory.newReferenceExpression(pathReference, "FhirResource/1")).then(a -> new EmptyReferenceExpression(pathReference, "FhirResource", "1"));
        Mockito.when(expressionFactory.newStringExpression(pathUri, "http://url", StringExpression.Operator.EXACT)).then(a -> new EmptyStringExpression(pathUri, "http://url", StringExpression.Operator.EXACT));

        Mockito.when(expressionFactory.newHasExpression(pathHasLink, pathHasParam, List.of("v1"))).then(a -> {
            var hc = new HasCondition(pathHasLink);
            hc.addExpression(new EmptyStringExpression(pathHasParam, "v1", StringExpression.Operator.CONTAINS));
            return hc;
        });
        Mockito.when(expressionFactory.newTokenExpression(pathToken, null, "1234",TokenExpression.Operator.EQUALS)).then(a -> new EmptyTokenExpression(pathToken, "http://identifier", "1234",TokenExpression.Operator.EQUALS) {
        });
        Mockito.when(expressionFactory.newTokenExpression(pathToken, "http://identifier", null,TokenExpression.Operator.EQUALS)).then(a -> new EmptyTokenExpression(pathToken, "http://identifier", "1234",TokenExpression.Operator.EQUALS) {
        });
        Mockito.when(expressionFactory.newTokenExpression(pathToken, "http://identifier", "1234",TokenExpression.Operator.EQUALS)).then(a -> new EmptyTokenExpression(pathToken, "http://identifier", "1234",TokenExpression.Operator.EQUALS) {
        });
        Mockito.when(expressionFactory.newTokenExpression(pathToken, "http://identifier", "1234",TokenExpression.Operator.EQUALS)).then(a -> new EmptyTokenExpression(pathToken, "http://identifier", "1234",TokenExpression.Operator.EQUALS) {
        });

        Mockito.when(expressionFactory.newTokenExpression(
                pathToken,
                "https://mos.esante.gouv.fr/NOS/TRE_R66-CategorieEtablissement/FHIR/TRE-R66-CategorieEtablissement",
                "159",
                TokenExpression.Operator.NOT
        )).then(a -> new EmptyTokenExpression(
                pathToken,
                "https://mos.esante.gouv.fr/NOS/TRE_R66-CategorieEtablissement/FHIR/TRE-R66-CategorieEtablissement",
                "159",
                TokenExpression.Operator.NOT
        ));

        Mockito.when(expressionFactory.newTokenExpression(
                pathToken,
                "https://mos.esante.gouv.fr/NOS/TRE_R02-SecteurActivite/FHIR/TRE-R02-SecteurActivite",
                "SA01",
                TokenExpression.Operator.EQUALS
        )).then(a -> new EmptyTokenExpression(
                pathToken,
                "https://mos.esante.gouv.fr/NOS/TRE_R02-SecteurActivite/FHIR/TRE-R02-SecteurActivite",
                "SA01",
                TokenExpression.Operator.EQUALS
        ));


    }

    @Test
    void testFromStringParams() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        var salp = new StringAndListParam();
        var solp = new StringOrListParam();
        solp.add(new StringParam("bla").setExact(true));
        salp.addAnd(solp);

        se.fromFhirParams(List.of(pathString, pathString2), salp);

        var oe = (OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0);
        var oeSubOr = (OrExpression<?>) ((OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0)).getExpressions().get(0);
        // we have to or expression:
        Assert.assertEquals(2, oe.getExpressions().size());
        Assert.assertEquals("bla", ((StringExpression<?>) oeSubOr.getExpressions().get(0)).getValue());
        Assert.assertEquals(pathString, ((StringExpression<?>) oeSubOr.getExpressions().get(0)).getFhirPath());
        Assert.assertEquals(StringExpression.Operator.EXACT, ((StringExpression<?>) oeSubOr.getExpressions().get(0)).getOperator());
    }




    @Test
    void testFromTokenParams() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        var salp = new TokenAndListParam();
        var solp = new TokenOrListParam();
        solp.add(new TokenParam("s", "bla"));
        salp.addAnd(solp);

        se.fromFhirParams(pathToken, salp);
        // we have to or expression:
        var oe = (OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertEquals("bla", ((TokenExpression<?>) oe.getExpressions().get(0)).getValue());
        Assert.assertEquals(pathToken, ((TokenExpression<?>) oe.getExpressions().get(0)).getFhirPath());
        Assert.assertEquals("s", ((TokenExpression<?>) oe.getExpressions().get(0)).getSystem());
    }

    @Test
    void testFromTokenParamsOperatorNot() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        var salp = new TokenAndListParam();
        var solp = new TokenOrListParam();
        TokenParam id = new TokenParam("id", "123");
        id.setModifier(TokenParamModifier.NOT);
        solp.add(id);
        salp.addAnd(solp);

        se.fromFhirParams(pathToken, salp);
        // we have to or expression:
        var oe = (OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertEquals("123", ((TokenExpression<?>) oe.getExpressions().get(0)).getValue());
        Assert.assertEquals(pathToken, ((TokenExpression<?>) oe.getExpressions().get(0)).getFhirPath());
        Assert.assertEquals("id", ((TokenExpression<?>) oe.getExpressions().get(0)).getSystem());
        Assert.assertEquals(TokenExpression.Operator.NOT, ((TokenExpression<?>) oe.getExpressions().get(0)).getOperator());
    }






    @ParameterizedTest
    @CsvSource(value = {
            "'  ',XXXXXXX",
            "'  http://identifier  ', XXXXXXX",
            "' http://systemnotexist  ', 1234",
            "http://identifier, XXXXXX"
    })
    void testFromTokenParamsWithSpacesNotResultsExpected(String system, String code) {
        var selectExpression = new SelectExpression<>("FhirResource", expressionFactory);
        TokenAndListParam tokenAndListParam = new TokenAndListParam();
        var tokenOrListParam = new TokenOrListParam();
        tokenOrListParam.add(new TokenParam(system, code));
        tokenAndListParam.addAnd(tokenOrListParam);

        selectExpression.fromFhirParams(pathToken, tokenAndListParam);

        var oe = (OrExpression<?>) ((AndExpression<?>) selectExpression.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertNull(oe.getExpressions().get(0));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "'  ',1234",
            "'  http://identifier  ', 1234",
            "'  http://identifier  ', '  1234  '",
            "http://identifier, '  1234  '",
            "' http://identifier ', '  '",
            "http://identifier, '  '"
    })
    void testFromTokenParamsWithSpacesResultsExpected(String system, String code) {
        var selectExpression = new SelectExpression<>("FhirResource", expressionFactory);
        TokenAndListParam tokenAndListParam = new TokenAndListParam();
        var tokenOrListParam = new TokenOrListParam();
        tokenOrListParam.add(new TokenParam(system, code));
        tokenAndListParam.addAnd(tokenOrListParam);

        selectExpression.fromFhirParams(pathToken, tokenAndListParam);

        var oe = (OrExpression<?>) ((AndExpression<?>) selectExpression.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertEquals("1234", ((TokenExpression<?>) oe.getExpressions().get(0)).getValue());
        Assert.assertEquals(pathToken, ((TokenExpression<?>) oe.getExpressions().get(0)).getFhirPath());
        Assert.assertEquals("http://identifier", ((TokenExpression<?>) oe.getExpressions().get(0)).getSystem());
    }

    @Test
    void testFromDateParams() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        var drp = new DateRangeParam();
        drp.setLowerBound(new DateParam(ParamPrefixEnum.GREATERTHAN, "2022-09-22"));


        se.fromFhirParams(pathDate, drp);
        // we have to or expression:
        var oe = (OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertEquals(testDate, ((DateRangeExpression<?>) oe.getExpressions().get(0)).getDate());
        Assert.assertEquals(pathDate, ((DateRangeExpression<?>) oe.getExpressions().get(0)).getFhirPath());
        Assert.assertEquals(TemporalPrecisionEnum.DAY, ((DateRangeExpression<?>) oe.getExpressions().get(0)).getPrecision());
        Assert.assertEquals(ParamPrefixEnum.GREATERTHAN, ((DateRangeExpression<?>) oe.getExpressions().get(0)).getPrefix());

    }


    @Test
    void testFromReferenceParams() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        var salp = new ReferenceAndListParam();
        var solp = new ReferenceOrListParam();
        solp.add(new ReferenceParam("FhirResource/1"));
        salp.addAnd(solp);

        se.fromFhirParams(pathReference, salp);
        // we have to or expression:
        var oe = (OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertEquals("1", ((EmptyReferenceExpression) oe.getExpressions().get(0)).getId());
        Assert.assertEquals("FhirResource", ((EmptyReferenceExpression) oe.getExpressions().get(0)).getType());
        Assert.assertEquals(pathReference, ((EmptyReferenceExpression) oe.getExpressions().get(0)).getFhirPath());

    }

    @Test
    void testFromHasParams() {

        // simple has:
        var se = new SelectExpression<>("FhirResource", expressionFactory);

        var hasAndListParam = new HasAndListParam();
        var hasOrListParam = new HasOrListParam();
        hasOrListParam.add(new HasParam("FhirResourceSub", "sub_link", "sub_path", "v1"));
        hasAndListParam.addAnd(hasOrListParam);

        se.fromFhirParams(hasAndListParam);
        var oe = se.getHasConditions();
        Assert.assertEquals(1, oe.size());
        Assert.assertEquals("FhirResourceSub", oe.get(0).getFhirPath().getResource());
        Assert.assertEquals("sub_link", oe.get(0).getFhirPath().getPath());
        Assert.assertEquals(StringExpression.Operator.CONTAINS, ((EmptyStringExpression) oe.get(0).getExpressions().get(0)).getOperator());
        Assert.assertEquals("v1", ((EmptyStringExpression) oe.get(0).getExpressions().get(0)).getValue());
        Assert.assertEquals("sub_path", ((EmptyStringExpression) oe.get(0).getExpressions().get(0)).getFhirPath().getPath());
        Assert.assertEquals("FhirResourceSub", ((EmptyStringExpression) oe.get(0).getExpressions().get(0)).getFhirPath().getResource());

        // multiple has (Logical AND):
        se = new SelectExpression<>("FhirResource", expressionFactory);
        var hasAndListParam2 = new HasAndListParam();
        var hasOrListParam2 = new HasOrListParam();
        hasOrListParam2.add(new HasParam("FhirResourceSub", "sub_link", "sub_path", "v1"));

        hasAndListParam2.addAnd(hasOrListParam);
        hasAndListParam2.addAnd(hasOrListParam2);
        se.fromFhirParams(hasAndListParam2);

        var andResult = se.getHasConditions();
        Assert.assertEquals(2, andResult.size());

    }

    @Test
    void testFromUriParams() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);
        var uriAndListParam = new UriAndListParam();
        var uriOrListParam = new UriOrListParam();
        uriOrListParam.add(new UriParam("http://url"));
        uriAndListParam.addAnd(uriOrListParam);
        se.fromFhirParams(pathUri, uriAndListParam);
        var oe = (OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertEquals("http://url", ((StringExpression<?>) oe.getExpressions().get(0)).getValue());
        Assert.assertEquals(pathUri, ((StringExpression<?>) oe.getExpressions().get(0)).getFhirPath());
        Assert.assertEquals(StringExpression.Operator.EXACT, ((StringExpression<?>) oe.getExpressions().get(0)).getOperator());

    }




    @Test
    void testSetCount() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        se.setCount(null);
        Assert.assertEquals(SelectExpression.DEFAULT_PAGE_SIZE, (int) se.getCount());

        se.setCount(1);
        Assert.assertEquals(1, (int) se.getCount());
        se.setCount(null);
        Assert.assertEquals(1, (int) se.getCount());

        Assert.assertThrows(BadParametersException.class, () -> se.setCount(Integer.MAX_VALUE));

    }

    @Test
    void testOrganizationTypeParams() {
        var se = new SelectExpression<>("Organization", expressionFactory);

        // Create TokenAndListParam and TokenOrListParam for the query parameters
        var salp = new TokenAndListParam();

        // First parameter with NOT operator
        var solpNot = new TokenOrListParam();
        var paramNot = new TokenParam("https://mos.esante.gouv.fr/NOS/TRE_R66-CategorieEtablissement/FHIR/TRE-R66-CategorieEtablissement", "159");
        paramNot.setModifier(TokenParamModifier.NOT);
        solpNot.add(paramNot);
        salp.addAnd(solpNot);

        // Second parameter with EQUALS operator
        var solpEquals = new TokenOrListParam();
        solpEquals.add(new TokenParam("https://mos.esante.gouv.fr/NOS/TRE_R02-SecteurActivite/FHIR/TRE-R02-SecteurActivite", "SA01"));
        salp.addAnd(solpEquals);

        // Convert parameters to FHIR expressions
        se.fromFhirParams(pathToken, salp);

        // Retrieve and verify the generated expressions
        var andExpression = (AndExpression<?>) se.getExpression();
        Assert.assertNotNull(andExpression);
        Assert.assertEquals(2, andExpression.getExpressions().size());

        // Verify the first expression (NOT operator)
        var oeNot = (OrExpression<?>) andExpression.getExpressions().get(0);
        Assert.assertEquals(1, oeNot.getExpressions().size());
        var tokenExpressionNot = (TokenExpression<?>) oeNot.getExpressions().get(0);
        Assert.assertEquals("159", tokenExpressionNot.getValue());
        Assert.assertEquals(pathToken, tokenExpressionNot.getFhirPath());
        Assert.assertEquals("https://mos.esante.gouv.fr/NOS/TRE_R66-CategorieEtablissement/FHIR/TRE-R66-CategorieEtablissement", tokenExpressionNot.getSystem());
        Assert.assertEquals(TokenExpression.Operator.NOT, tokenExpressionNot.getOperator());

        // Verify the second expression (EQUALS operator)
        var oeEquals = (OrExpression<?>) andExpression.getExpressions().get(1);
        Assert.assertEquals(1, oeEquals.getExpressions().size());
        var tokenExpressionEquals = (TokenExpression<?>) oeEquals.getExpressions().get(0);
        Assert.assertEquals("SA01", tokenExpressionEquals.getValue());
        Assert.assertEquals(pathToken, tokenExpressionEquals.getFhirPath());
        Assert.assertEquals("https://mos.esante.gouv.fr/NOS/TRE_R02-SecteurActivite/FHIR/TRE-R02-SecteurActivite", tokenExpressionEquals.getSystem());
        Assert.assertEquals(TokenExpression.Operator.EQUALS, tokenExpressionEquals.getOperator());


    }

}
