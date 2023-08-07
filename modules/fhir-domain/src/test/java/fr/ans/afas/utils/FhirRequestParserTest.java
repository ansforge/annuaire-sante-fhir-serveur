/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhirserver.http.FhirRequestParser;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.*;
import fr.ans.afas.utils.data.TestSearchConfig;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

/**
 * Test the parsing of fhir requests
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class FhirRequestParserTest {

    final FhirSearchPath pathString = FhirSearchPath.builder().resource("FhirResource").path("string_path").build();

    final ExpressionFactory<?> expressionFactory = mock(ExpressionFactory.class);
    final SearchConfig searchConfig = new TestSearchConfig();


    @BeforeEach
    public void init() {
        Mockito.when(expressionFactory.newAndExpression()).then((a) -> new EmptyAndExpression());
        Mockito.when(expressionFactory.newOrExpression()).then((a) -> new EmptyOrExpression());
        Mockito.when(expressionFactory.newStringExpression(pathString, "bla", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "bla", StringExpression.Operator.EXACT));
        Mockito.when(expressionFactory.newStringExpression(pathString, "blo", StringExpression.Operator.EXACT)).then((a) -> new EmptyStringExpression(pathString, "blo", StringExpression.Operator.EXACT));

        //FhirSearchPath path, Date value, TemporalPrecisionEnum precision, ParamPrefixEnum queryQualifier
        Mockito.when(expressionFactory.newDateRangeExpression(Mockito.any(), Mockito.any(Date.class), Mockito.any(TemporalPrecisionEnum.class), Mockito.any(ParamPrefixEnum.class)))
                .then((a) -> new EmptyDateExpression(a.getArgument(0), a.getArgument(1), a.getArgument(2), a.getArgument(3)));

        IncludeExpression<?> mockIncludeDevice = mock(IncludeExpression.class);
        Mockito.when(mockIncludeDevice.getType()).thenReturn("Device");
        Mockito.when(mockIncludeDevice.getName()).thenReturn("organization");

        IncludeExpression<?> mockIncludePractitionerRoleOrganization = mock(IncludeExpression.class);
        Mockito.when(mockIncludePractitionerRoleOrganization.getType()).thenReturn("PractitionerRole");
        Mockito.when(mockIncludePractitionerRoleOrganization.getName()).thenReturn("organization");

        IncludeExpression<?> mockIncludePractitionerRolePartof = mock(IncludeExpression.class);
        Mockito.when(mockIncludePractitionerRolePartof.getType()).thenReturn("PractitionerRole");
        Mockito.when(mockIncludePractitionerRolePartof.getName()).thenReturn("partof");

        IncludeExpression<?> mockIncludePractitionerRolePractitioner = mock(IncludeExpression.class);
        Mockito.when(mockIncludePractitionerRolePractitioner.getType()).thenReturn("PractitionerRole");
        Mockito.when(mockIncludePractitionerRolePractitioner.getName()).thenReturn("practitioner");

        IncludeExpression<?> mockIncludeOrganization = mock(IncludeExpression.class);
        Mockito.when(mockIncludeOrganization.getType()).thenReturn("Organization");
        Mockito.when(mockIncludeOrganization.getName()).thenReturn("partof");

        Mockito.when(expressionFactory.newIncludeExpression("Device", "organization")).then((a) -> mockIncludeDevice);
        Mockito.when(expressionFactory.newIncludeExpression("PractitionerRole", "organization")).then((a) -> mockIncludePractitionerRoleOrganization);
        Mockito.when(expressionFactory.newIncludeExpression("PractitionerRole", "partof")).then((a) -> mockIncludePractitionerRolePartof);
        Mockito.when(expressionFactory.newIncludeExpression("PractitionerRole", "practitioner")).then((a) -> mockIncludePractitionerRolePractitioner);
        Mockito.when(expressionFactory.newIncludeExpression("Organization", "partof")).then((a) -> mockIncludeOrganization);
    }

    @Test
    public void testSelectExpressionParsing() throws BadSelectExpression, BadDataFormatException {
        var expression = FhirRequestParser.parseSelectExpression("FhirResource?_count=49&string_path:exact=bla,bla&string_path:exact=blo", expressionFactory, searchConfig);
        Assert.assertEquals("FhirResource", expression.getFhirResource());
        Assert.assertEquals(49, (int) expression.getCount());
        Assert.assertEquals(2, ((EmptyAndExpression) expression.getExpression()).getExpressions().size());
    }

    @Test
    public void testSelectExpressionParsingIncludes() throws BadSelectExpression, BadDataFormatException {
        var expression = FhirRequestParser.parseSelectExpression("Organization?_include=", expressionFactory, searchConfig);
        Assert.assertEquals(0, expression.getIncludes().size());

        expression = FhirRequestParser.parseSelectExpression("Organization?_include=Organization:partof", expressionFactory, searchConfig);
        Assert.assertEquals(1, expression.getIncludes().size());
        List<String> includes = expression.getIncludes().stream().map(i -> i.getType() + ":" + i.getName()).collect(Collectors.toList());
        Assert.assertTrue(includes.contains("Organization:partof"));

        expression = FhirRequestParser.parseSelectExpression("PractitionerRole?_include=PractitionerRole:partof&_include=PractitionerRole:organization", expressionFactory, searchConfig);
        Assert.assertEquals(2, expression.getIncludes().size());
        includes = expression.getIncludes().stream().map(i -> i.getType() + ":" + i.getName()).collect(Collectors.toList());
        Assert.assertTrue(includes.contains("PractitionerRole:partof"));
        Assert.assertTrue(includes.contains("PractitionerRole:organization"));

        expression = FhirRequestParser.parseSelectExpression("PractitionerRole?_include=*", expressionFactory, searchConfig);
        Assert.assertEquals(3, expression.getIncludes().size());
        includes = expression.getIncludes().stream().map(i -> i.getType() + ":" + i.getName()).collect(Collectors.toList());
        Assert.assertTrue(includes.contains("PractitionerRole:partof"));
        Assert.assertTrue(includes.contains("PractitionerRole:organization"));
        Assert.assertTrue(includes.contains("PractitionerRole:practitioner"));
    }

    @Test
    public void testSelectExpressionParsingRevIncludes() throws BadSelectExpression, BadDataFormatException {
        var expression = FhirRequestParser.parseSelectExpression("Organization?_revinclude=", expressionFactory, searchConfig);
        Assert.assertEquals(0, expression.getIncludes().size());

        expression = FhirRequestParser.parseSelectExpression("Organization?_revinclude=PractitionerRole:organization", expressionFactory, searchConfig);
        Assert.assertEquals("Organization", expression.getFhirResource());
        Assert.assertEquals(1, expression.getRevincludes().size());
        List<String> includes = expression.getRevincludes().stream().map(i -> i.getType() + ":" + i.getName()).collect(Collectors.toList());
        Assert.assertTrue(includes.contains("PractitionerRole:organization"));

        expression = FhirRequestParser.parseSelectExpression("Organization?_revinclude=Device:organization", expressionFactory, searchConfig);
        Assert.assertEquals("Organization", expression.getFhirResource());
        Assert.assertEquals(1, expression.getRevincludes().size());
        includes = expression.getRevincludes().stream().map(i -> i.getType() + ":" + i.getName()).collect(Collectors.toList());
        Assert.assertTrue(includes.contains("Device:organization"));

        expression = FhirRequestParser.parseSelectExpression("Organization?_revinclude=PractitionerRole:organization&_revinclude=Device:organization", expressionFactory, searchConfig);
        Assert.assertEquals("Organization", expression.getFhirResource());
        Assert.assertEquals(2, expression.getRevincludes().size());
        includes = expression.getRevincludes().stream().map(i -> i.getType() + ":" + i.getName()).collect(Collectors.toList());
        Assert.assertTrue(includes.contains("PractitionerRole:organization"));
        Assert.assertTrue(includes.contains("Device:organization"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Organization?_include=:partof",
            "Organization?_include=Organization:",
            "Organization?_include=Organization",
            "Organization?_include=Organ",
            "Organization?_include=Organ:partof",
            "Organization?_include=Organization:partofs",
            "Organization?_include=Practitioner:partof",
            "Organization?_include=PractitionerRole:organization"
    })
    public void testSelectExpressionParsingIncludesThrowsException(String path) throws BadDataFormatException {
        Assert.assertThrows(BadSelectExpression.class,
                () -> FhirRequestParser.parseSelectExpression(path, expressionFactory, searchConfig));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Organization?_revinclude=:organization",
            "Organization?_revinclude=PractitionerRole:",
            "Organization?_revinclude=PractitionerRole",
            "Organization?_revinclude=Pract",
            "Organization?_revinclude=Pract:organization",
            "Organization?_revinclude=PractitionerRole:organizations",
            "Organization?_revinclude=Practitioner:organization"
    })
    public void testSelectExpressionParsingRevIncludesThrowsException(String path) throws BadDataFormatException {
        Assert.assertThrows(BadSelectExpression.class,
                () -> FhirRequestParser.parseSelectExpression(path, expressionFactory, searchConfig));
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
    public void testDateParsing() throws BadSelectExpression, BadDataFormatException {
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
    public void testUrlDecodeEmpty() throws BadSelectExpression, BadDataFormatException {
        // when the string is null:
        Assert.assertNull(FhirRequestParser.urlDecode(null));
        // when the string have no parameters:
        var expression = FhirRequestParser.parseSelectExpression("FhirResource", expressionFactory, searchConfig);
        Assert.assertEquals(0, ((AndExpression<?>) expression.getExpression()).getExpressions().size());
    }

    @Test
    public void testUnsupportedSearch() throws BadSelectExpression, BadDataFormatException {
        Assert.assertThrows(BadSelectExpression.class,
                ()-> FhirRequestParser.parseSelectExpression("FhirResource?_count=49&not_exist_path=Patient/00001", expressionFactory, searchConfig));
    }

}
