/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.servlet.utils.MockedFhirPageIterator;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyAndExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyOrExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Test the fhir search
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirSearchTest {

    @Mock
    FhirStoreService<Object> fhirStoreService;

    @Mock
    ExpressionFactory<Object> expressionFactory;

    @Mock
    NextUrlManager<Object> nextUrlManager;

    PagingData<Object> lastPagingData;


    @Before
    public void setUp() throws BadLinkException {

        var patientList = new ArrayList<Patient>();

        var p1 = new Patient();
        var p2 = new Patient();
        var p3 = new Patient();

        patientList.add(p1);
        patientList.add(p2);
        patientList.add(p3);


        MockitoAnnotations.initMocks(this);
        Mockito.when(expressionFactory.newAndExpression()).then((a) -> new EmptyAndExpression());
        Mockito.when(expressionFactory.newOrExpression()).then((a) -> new EmptyOrExpression());
        Mockito.when(fhirStoreService.iterate(Mockito.any(), Mockito.any())).then((a) ->
                new MockedFhirPageIterator("Patient", patientList, a.getArgument(0))
        );
        Mockito.when(nextUrlManager.store(Mockito.any())).then((a) -> {
                    this.lastPagingData = a.getArgument(0);
                    return lastPagingData.getLastId();
                }
        );
        Mockito.when(nextUrlManager.find(Mockito.any())).then((a) -> Optional.ofNullable(this.lastPagingData));

    }


    @Test
    public void simpleBundleTest() throws Exception {

        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, FhirContext.forR4(), "");
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/Patient", "/fhir/v2-alpha/");

        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(3, patients.getTotal());
        Assert.assertEquals(3, patients.getEntry().size());
    }

    @Test
    public void pagingTest() throws Exception {

        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, FhirContext.forR4(), "");
        var out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/Patient?_count=2", "/fhir/v2-alpha/");

        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(3, patients.getTotal());
        Assert.assertEquals(2, patients.getEntry().size());

        var nextLink = patients.getLink("next");

        var out2 = ServletTestUtil.callAsyncServlet(servlet, "GET", nextLink.getUrl(), "/fhir/v2-alpha/");
        var patients2 = (Bundle) parser.parseResource(out2.toString());
        Assert.assertEquals(3, patients2.getTotal());
        Assert.assertEquals(1, patients2.getEntry().size());

    }


    /**
     * Test when we cant found the next page id
     */
    @Test
    public void pagingFail() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        var parser = FhirContext.forR4().newJsonParser();
        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, FhirContext.forR4(), "");


        var out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/_page", "/fhir/v2-alpha/");
        var operationOutcome = (OperationOutcome) parser.parseResource(out.toString());

        // id not passed in next pages:
        Assert.assertEquals(1, operationOutcome.getIssue().size());
        Assert.assertEquals(OperationOutcome.IssueType.EXCEPTION, operationOutcome.getIssue().get(0).getCode());
        Assert.assertEquals(OperationOutcome.IssueSeverity.ERROR, operationOutcome.getIssue().get(0).getSeverity());
        Assert.assertEquals("Parameter id required to fetch the next page", operationOutcome.getIssue().get(0).getDiagnostics());

        // another error with a not found id:
        // As the tests mock the request, we do not recover an OperationOutcome but a raw exception
        Assert.assertThrows(BadRequestException.class, () ->
                ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/_page?id=A", "/fhir/v2-alpha/")
        );

    }

}
