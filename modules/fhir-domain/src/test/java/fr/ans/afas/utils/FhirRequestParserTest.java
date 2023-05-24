/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyAndExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyDateExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyOrExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyStringExpression;
import fr.ans.afas.utils.data.TestSearchConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Test the parsing of fhir requests
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FhirRequestParserTest {

    FhirSearchPath pathString = FhirSearchPath.builder().resource("FhirResource").path("string_path").build();

    ExpressionFactory<?> expressionFactory = Mockito.mock(ExpressionFactory.class);
    SearchConfig searchConfig = new TestSearchConfig();


    @Before
    public void init() {
        Mockito.when(expressionFactory.newAndExpression()).then((a) -> new EmptyAndExpression());
        Mockito.when(expressionFactory.newOrExpression()).then((a) -> new EmptyOrExpression());
        Mockito.when(expressionFactory.newStringExpression(pathString, "bla", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "bla", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString, "blo", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "blo", StringExpression.Operator.EXACT));

        //FhirSearchPath path, Date value, TemporalPrecisionEnum precision, ParamPrefixEnum queryQualifier
        Mockito.when(expressionFactory.newDateRangeExpression(Mockito.any(), Mockito.any(Date.class), Mockito.any(TemporalPrecisionEnum.class), Mockito.any(ParamPrefixEnum.class)))
                .then((a) -> new EmptyDateExpression(a.getArgument(0), a.getArgument(1), a.getArgument(2), a.getArgument(3)));
    }

    @Test
    public void testSelectExpressionParsing() throws BadSelectExpression {


        var expression = FhirRequestParser.parseSelectExpression("FhirResource?_count=49&string_path:exact=bla,bla&string_path:exact=blo", expressionFactory, searchConfig);

        Assert.assertEquals("FhirResource", expression.getFhirResource());
        Assert.assertEquals(49, (int) expression.getCount());
        Assert.assertEquals(2, ((EmptyAndExpression) expression.getExpression()).getExpressions().size());


    }


    @Test
    public void testQueryStringParsing() {

        // simple:
        var params = FhirRequestParser.parseParameters("name=Jean");
        Assert.assertEquals("name", params.get(0).getParamName());
        Assert.assertEquals("Jean", params.get(0).getParamValues().get(0));
        Assert.assertNull(params.get(0).getModifier());


        // multiple values:
        params = FhirRequestParser.parseParameters("name=Jean,Martin");
        Assert.assertEquals("name", params.get(0).getParamName());
        Assert.assertEquals("Jean", params.get(0).getParamValues().get(0));
        Assert.assertEquals("Martin", params.get(0).getParamValues().get(1));
        Assert.assertNull(params.get(0).getModifier());


        // multiple params:
        params = FhirRequestParser.parseParameters("name=Jean,Martin&name=Julie");
        Assert.assertEquals("name", params.get(0).getParamName());
        Assert.assertEquals("Jean", params.get(0).getParamValues().get(0));
        Assert.assertEquals("Julie", params.get(1).getParamValues().get(0));


        // modifier params:
        params = FhirRequestParser.parseParameters("name=Jean,Martin&name:contains=Julie");
        Assert.assertNull(params.get(0).getModifier());
        Assert.assertEquals("contains", params.get(1).getModifier());

    }


    @Test
    public void testDateParsing() throws BadSelectExpression {
        var expression = FhirRequestParser.parseSelectExpression("FhirResource?_count=49&date_path=eq2013,eq2023&date_path=gt2013-05-01", expressionFactory, searchConfig);
        var rootExpression = (AndExpression<?>) expression.getExpression();
        Assert.assertEquals(2, rootExpression.getExpressions().size());
        var firstOr = (OrExpression<?>) rootExpression.getExpressions().get(0);
        var secondOr = (OrExpression<?>) rootExpression.getExpressions().get(1);
        Assert.assertEquals(2, firstOr.getExpressions().size());
        var firstDate = (DateRangeExpression<?>) firstOr.getExpressions().get(0);
        var secondDate = (DateRangeExpression<?>) firstOr.getExpressions().get(1);

        Assert.assertEquals("date_path", firstDate.getFhirPath().getPath());
        Assert.assertEquals(ParamPrefixEnum.EQUAL, firstDate.getPrefix());
        Assert.assertEquals(ParamPrefixEnum.EQUAL, secondDate.getPrefix());
        Assert.assertEquals(TemporalPrecisionEnum.YEAR, firstDate.getPrecision());
        Assert.assertEquals(TemporalPrecisionEnum.YEAR, secondDate.getPrecision());

        var gc = new GregorianCalendar();
        gc.setTime(firstDate.getDate());
        Assert.assertEquals(2013, gc.get(Calendar.YEAR));
        gc.setTime(secondDate.getDate());
        Assert.assertEquals(2023, gc.get(Calendar.YEAR));


        var thirdDate = (DateRangeExpression<?>) secondOr.getExpressions().get(0);
        gc.setTime(thirdDate.getDate());
        Assert.assertEquals(2013, gc.get(Calendar.YEAR));
        Assert.assertEquals(4, gc.get(Calendar.MONTH));
        Assert.assertEquals(1, gc.get(Calendar.DATE));

    }


    @Test
    public void testUrlDecodeEmpty() throws BadSelectExpression {
        // when the string is null:
        Assert.assertNull(FhirRequestParser.urlDecode(null));
        // when the string have no parameters:
        var expression = FhirRequestParser.parseSelectExpression("FhirResource", expressionFactory, searchConfig);
        Assert.assertEquals(0, ((AndExpression<?>) expression.getExpression()).getExpressions().size());
    }

    @Test(expected = BadSelectExpression.class)
    public void testUnsupportedSearch() throws BadSelectExpression {
        FhirRequestParser.parseSelectExpression("FhirResource?_count=49&reference_path=Patient/00001", expressionFactory, searchConfig);

    }

}
