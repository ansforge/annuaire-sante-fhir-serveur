/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.AndExpression;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.utils.data.EmptyAndExpression;
import fr.ans.afas.utils.data.EmptyOrExpression;
import fr.ans.afas.utils.data.EmptyStringExpression;
import fr.ans.afas.utils.data.TestSearchConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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

    ExpressionFactory expressionFactory = Mockito.mock(ExpressionFactory.class);
    SearchConfig searchConfig = new TestSearchConfig();

    @Before
    public void init() {
        Mockito.when(expressionFactory.newAndExpression()).then((a) -> new EmptyAndExpression());
        Mockito.when(expressionFactory.newOrExpression()).then((a) -> new EmptyOrExpression());
        Mockito.when(expressionFactory.newStringExpression(pathString, "bla", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "bla", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString, "blo", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "blo", StringExpression.Operator.EXACT));
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
    public void testUrlDecodeEmpty() throws BadSelectExpression {
        // when the string is null:
        Assert.assertNull(FhirRequestParser.urlDecode(null));
        // when the string have no parameters:
        var expression = FhirRequestParser.parseSelectExpression("FhirResource", expressionFactory, searchConfig);
        Assert.assertEquals(0, ((AndExpression) expression.getExpression()).getExpressions().size());
    }

}
