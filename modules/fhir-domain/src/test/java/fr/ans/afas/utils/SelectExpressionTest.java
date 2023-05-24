/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.exception.BadParametersException;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class SelectExpressionTest {


    static FhirSearchPath pathString = FhirSearchPath.builder().resource("FhirResource").path("string_path").build();
    static FhirSearchPath pathString2 = FhirSearchPath.builder().resource("FhirResource").path("string_path2").build();
    static FhirSearchPath pathToken = FhirSearchPath.builder().resource("FhirResource").path("token_path").build();
    static FhirSearchPath pathDate = FhirSearchPath.builder().resource("FhirResource").path("date_path").build();
    static FhirSearchPath pathReference = FhirSearchPath.builder().resource("FhirResource").path("reference_path").build();
    static FhirSearchPath pathUri = FhirSearchPath.builder().resource("FhirResource").path("uri_path").build();
    static Date testDate = new Date(1663849410512L);
    ExpressionFactory<?> expressionFactory = Mockito.mock(ExpressionFactory.class);

    @Before
    public void setup() {


        Mockito.when(expressionFactory.newAndExpression()).then((a) -> new EmptyAndExpression());
        Mockito.when(expressionFactory.newOrExpression()).then((a) -> new EmptyOrExpression());
        Mockito.when(expressionFactory.newStringExpression(pathString, "bla", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "bla", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString, "blo", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "blo", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString2, "bla", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString2, "bla", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString2, "blo", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString2, "blo", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newTokenExpression(pathToken, "s", "bla")).then((a) -> new EmptyTokenExpression(pathToken, "s", "bla"));
        Mockito.when(expressionFactory.newDateRangeExpression(Mockito.any(), Mockito.any(), Mockito.isA(TemporalPrecisionEnum.class), Mockito.isA(ParamPrefixEnum.class))).then((a) -> new EmptyDateExpression(pathDate, testDate, TemporalPrecisionEnum.DAY, ParamPrefixEnum.GREATERTHAN));
        Mockito.when(expressionFactory.newReferenceExpression(pathReference, "FhirResource/1")).then((a) -> new EmptyReferenceExpression(pathReference, "FhirResource", "1"));
        Mockito.when(expressionFactory.newStringExpression(pathUri, "http://url", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathUri, "http://url", StringExpression.Operator.EXACT));

    }

    @Test
    public void testFromStringParams() {
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
    public void testFromTokenParams() {
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
    public void testFromDateParams() {
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
    public void testFromReferenceParams() {
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
    public void testFromUriParams() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        var uriAndListParam = new UriAndListParam();
        var uriOrListParam = new UriOrListParam();
        uriOrListParam.add(new UriParam("http://url"));
        uriAndListParam.addAnd(uriOrListParam);

        se.fromFhirParams(pathUri, uriAndListParam);
        // we have to or expression:
        var oe = (OrExpression<?>) ((AndExpression<?>) se.getExpression()).getExpressions().get(0);
        Assert.assertEquals(1, oe.getExpressions().size());
        Assert.assertEquals("http://url", ((StringExpression<?>) oe.getExpressions().get(0)).getValue());
        Assert.assertEquals(pathUri, ((StringExpression<?>) oe.getExpressions().get(0)).getFhirPath());
        Assert.assertEquals(StringExpression.Operator.EXACT, ((StringExpression<?>) oe.getExpressions().get(0)).getOperator());

    }


    @Test
    public void testSetCount() {
        var se = new SelectExpression<>("FhirResource", expressionFactory);


        se.setCount(null);
        Assert.assertEquals(SelectExpression.DEFAULT_PAGE_SIZE, (int) se.getCount());

        se.setCount(1);
        Assert.assertEquals(1, (int) se.getCount());
        se.setCount(null);
        Assert.assertEquals(1, (int) se.getCount());

        Assert.assertThrows(BadParametersException.class, () -> {
            se.setCount(Integer.MAX_VALUE);
        });

    }

}
