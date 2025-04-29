/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhir.servlet.utils.MockedFhirPageIterator;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyAndExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyOrExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyStringExpression;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static fr.ans.afas.servlet.ServletTestUtil.SERVER_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test the fhir search
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirSearchTest extends BaseTest {

    PagingData<Object> lastPagingData;

    List<Patient> patientList;


    @Before
    public void setUp() throws BadLinkException {
        patientList = new ArrayList<>();

        var p1 = new Patient();
        var p2 = new Patient();
        var p3 = new Patient();

        patientList.add(p1);
        patientList.add(p2);
        patientList.add(p3);

        AfasConfiguration.Fhir fhir = new AfasConfiguration.Fhir();
        AfasConfiguration.Includes includes = new AfasConfiguration.Includes();
        includes.setBufferSize(10);
        fhir.setIncludes(includes);

        super.setup();
        Mockito.when(fhirServerContext.getExpressionFactory().newAndExpression()).then(a -> new EmptyAndExpression());
        Mockito.when(fhirServerContext.getExpressionFactory().newOrExpression()).then(a -> new EmptyOrExpression());
        Mockito.when(fhirServerContext.getNextUrlManager().store(any())).then(a -> {
                    this.lastPagingData = a.getArgument(0);
                    return lastPagingData.getLastId();
                }
        );
        Mockito.when(fhirServerContext.getNextUrlManager().find(any())).then(a -> Optional.ofNullable(this.lastPagingData));
        when(afasConfiguration.getServletTimeout()).thenReturn(1000);
        when(afasConfiguration.getPublicUrl()).thenReturn(SERVER_URL);
        when(afasConfiguration.getFhir()).thenReturn(fhir);


    }


    @Test
    public void simpleBundleTest() throws Exception {
        Mockito.when(fhirServerContext.getFhirStoreService().iterate(any(), any())).then(a -> {
                    var se = (SelectExpression<String>) a.getArgument(1);
                    return new MockedFhirPageIterator("Patient", patientList, a.getArgument(0), se.getCount());
                }
        );

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(3, patients.getTotal());
        Assert.assertEquals(3, patients.getEntry().size());
    }

    @Test
    public void pagingTest() throws Exception {
        Mockito.when(fhirServerContext.getFhirStoreService().iterate(any(), any())).then(a -> {
                    var se = (SelectExpression<String>) a.getArgument(1);
                    return new MockedFhirPageIterator("Patient", patientList, a.getArgument(0), se.getCount());
                }
        );

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient?_count=2", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(3, patients.getTotal());
        Assert.assertEquals(2, patients.getEntry().size());

        var nextLink = patients.getLink("next");

        //We didn't call TenantFilter in this test, but we add to next link the tenant path, this is because we have to add tenant path in contextPath to do test works
        var out2 = ServletTestUtil.callAsyncServlet(servlet, "GET", nextLink.getUrl().replaceAll(SERVER_URL+"/fhir/" + HttpUtils.SERVLET_API_PATH, ""), "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);
        System.out.println(out2);
        var patients2 = (Bundle) parser.parseResource(out2.toString());
        Assert.assertEquals(3, patients2.getTotal());
        Assert.assertEquals(1, patients2.getEntry().size());

    }


    /**
     * Test when we cant found the next page id
     */
    @Test
    public void pagingFail() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        // Mock the message source to return the expected message
        Mockito.when(messageSource.getMessage(Mockito.eq("error.parameter.id.required"), any(), any(Locale.class)))
                .thenReturn("Parameter id required to fetch the next page");
        var parser = FhirContext.forR4().newJsonParser();
        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);


        var out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/_page", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);
        var operationOutcome = (OperationOutcome) parser.parseResource(out.toString());

        // id not passed in next pages:
        Assert.assertEquals(1, operationOutcome.getIssue().size());
        Assert.assertEquals(OperationOutcome.IssueType.EXCEPTION, operationOutcome.getIssue().get(0).getCode());
        Assert.assertEquals(OperationOutcome.IssueSeverity.ERROR, operationOutcome.getIssue().get(0).getSeverity());
        Assert.assertEquals("Parameter id required to fetch the next page", operationOutcome.getIssue().get(0).getDiagnostics());

        // another error with a not found id:
        // As the tests mock the request, we do not recover an OperationOutcome but a raw exception
        Assert.assertThrows(BadRequestException.class, () ->
                ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/_page?id=A", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null)
        );

    }


    /**
     * Test the _search endpoint (used with http POST)
     */
    @Test
    public void searchWithPostTest() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Patient patient = new Patient();
        Identifier identifier = new Identifier();
        identifier.setSystem("system");
        identifier.setValue("ID-1");
        patient.addIdentifier(identifier);

        HumanName humanName = new HumanName();
        humanName.setFamily("Dupont");
        patient.addName(humanName);

        FhirSearchPath fhirSearchPath = FhirSearchPath.builder().resource("Patient").path("id").build();
        EmptyStringExpression emptyStringExpression = new EmptyStringExpression(fhirSearchPath, "A", StringExpression.Operator.EQUALS);
        Mockito.when(fhirServerContext.getExpressionFactory().newStringExpression(fhirSearchPath, "A", StringExpression.Operator.EQUALS)).then(a -> emptyStringExpression);

        Mockito.when(fhirServerContext.getFhirStoreService().iterate(any(), any())).then(a -> {
                    var se = (SelectExpression<String>) a.getArgument(1);
                    return new MockedFhirPageIterator("Patient", List.of(patient), a.getArgument(0), se.getCount());
                }
        );

        SearchParamConfig searchParamConfig = new SearchParamConfig();
        searchParamConfig.setName("test");
        searchParamConfig.setSearchType("string");
        Mockito.when(fhirServerContext.getSearchConfigService().getSearchConfigByPath(any())).thenReturn(Optional.of(searchParamConfig));

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "POST", "/fhir/v2/tenant-1/Patient", "/fhir/v2/tenant-1/", "family=Dupont", "application/x-www-form-urlencoded");

        System.out.println(out.toString());
        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(1, patients.getTotal());
        Assert.assertEquals(1, patients.getEntry().size());
        Assert.assertEquals(identifier.getSystem(), ((Patient) patients.getEntry().get(0).getResource()).getIdentifier().get(0).getSystem());
        Assert.assertEquals(identifier.getValue(), ((Patient) patients.getEntry().get(0).getResource()).getIdentifier().get(0).getValue());
        Assert.assertEquals(humanName.getFamily(), ((Patient) patients.getEntry().get(0).getResource()).getName().get(0).getFamily());
    }

    @Test
    public void searchWithPostThrowExceptionTest() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Mockito.when(fhirServerContext.getSecurityService()).thenReturn(null);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "POST", "/fhir/v2/tenant-1/Patient", "/fhir/v2/tenant-1/", "family=Dupont", "application/json");

        var parser = FhirContext.forR4().newJsonParser();
        var operationOutcome = (OperationOutcome) parser.parseResource(out.toString());



        Assert.assertEquals("exception", operationOutcome.getIssue().get(0).getCode().toCode());
    }

    @Test
    public void searchWithPutThrowExceptionTest() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "PUT", "/fhir/v2/tenant-1/____", "/fhir/v2/tenant-1/", "family=Dupont", "application/json");

        var parser = FhirContext.forR4().newJsonParser();
        var operationOutcome = (OperationOutcome) parser.parseResource(out.toString());



        Assert.assertEquals("exception", operationOutcome.getIssue().get(0).getCode().toCode());
    }
}
