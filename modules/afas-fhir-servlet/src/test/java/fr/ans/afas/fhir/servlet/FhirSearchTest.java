/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.utils.MockedFhirPageIterator;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.StringExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyAndExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyOrExpression;
import fr.ans.afas.fhirserver.search.expression.emptyimpl.EmptyStringExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.*;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test the fhir search
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirSearchTest {

    private static final String SERVER_URL = "http://localhost:8080/fhir";

    @Mock
    FhirStoreService<Object> fhirStoreService;

    @Mock
    ExpressionFactory<Object> expressionFactory;

    @Mock
    NextUrlManager<Object> nextUrlManager;

    @Mock
    AfasConfiguration afasConfiguration;

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

        MockitoAnnotations.initMocks(this);
        Mockito.when(expressionFactory.newAndExpression()).then((a) -> new EmptyAndExpression());
        Mockito.when(expressionFactory.newOrExpression()).then((a) -> new EmptyOrExpression());
        Mockito.when(nextUrlManager.store(any())).then((a) -> {
                    this.lastPagingData = a.getArgument(0);
                    return lastPagingData.getLastId();
                }
        );
        Mockito.when(nextUrlManager.find(any())).then((a) -> Optional.ofNullable(this.lastPagingData));
        when(afasConfiguration.getServletTimeout()).thenReturn(1000);
        when(afasConfiguration.getPublicUrl()).thenReturn(SERVER_URL);
        when(afasConfiguration.getFhir()).thenReturn(fhir);
    }


    @Test
    public void simpleBundleTest() throws Exception {
        Mockito.when(fhirStoreService.iterate(any(), any())).then((a) -> {
                    var se = (SelectExpression<String>) a.getArgument(1);
                    return new MockedFhirPageIterator("Patient", patientList, a.getArgument(0), se.getCount());
                }
        );

        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, afasConfiguration, null);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/Patient", "/fhir/v2-alpha/", null);

        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(3, patients.getTotal());
        Assert.assertEquals(3, patients.getEntry().size());
    }

    @Test
    public void pagingTest() throws Exception {
        Mockito.when(fhirStoreService.iterate(any(), any())).then((a) -> {
                    var se = (SelectExpression<String>) a.getArgument(1);
                    return new MockedFhirPageIterator("Patient", patientList, a.getArgument(0), se.getCount());
                }
        );

        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, afasConfiguration, null);
        var out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/Patient?_count=2", "/fhir/v2-alpha/", null);

        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(3, patients.getTotal());
        Assert.assertEquals(2, patients.getEntry().size());

        var nextLink = patients.getLink("next");

        var out2 = ServletTestUtil.callAsyncServlet(servlet, "GET", nextLink.getUrl().replaceAll(SERVER_URL, "/fhir"), "/fhir/v2-alpha/", null);
        var patients2 = (Bundle) parser.parseResource(out2.toString());
        Assert.assertEquals(3, patients2.getTotal());
        Assert.assertEquals(1, patients2.getEntry().size());

    }


    /**
     * Test when we canrt found the next page id
     */
    @Test
    public void pagingFail() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {

        var parser = FhirContext.forR4().newJsonParser();
        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, afasConfiguration, null);


        var out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/_page", "/fhir/v2-alpha/", null);
        var operationOutcome = (OperationOutcome) parser.parseResource(out.toString());

        // id not passed in next pages:
        Assert.assertEquals(1, operationOutcome.getIssue().size());
        Assert.assertEquals(OperationOutcome.IssueType.EXCEPTION, operationOutcome.getIssue().get(0).getCode());
        Assert.assertEquals(OperationOutcome.IssueSeverity.ERROR, operationOutcome.getIssue().get(0).getSeverity());
        Assert.assertEquals("Parameter id required to fetch the next page", operationOutcome.getIssue().get(0).getDiagnostics());

        // another error with a not found id:
        // As the tests mock the request, we do not recover an OperationOutcome but a raw exception
        Assert.assertThrows(BadRequestException.class, () ->
                ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/_page?id=A", "/fhir/v2-alpha/", null)
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
        Mockito.when(expressionFactory.newStringExpression(fhirSearchPath, "A", StringExpression.Operator.EQUALS)).then(a -> emptyStringExpression);

        Mockito.when(fhirStoreService.iterate(any(), any())).then((a) -> {
                    var se = (SelectExpression<String>) a.getArgument(1);
                    return new MockedFhirPageIterator("Patient", List.of(patient), a.getArgument(0), se.getCount());
                }
        );

        var servlet = new FhirResourceServlet(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, afasConfiguration, null);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "POST", "/fhir/v2-alpha/Patient", "/fhir/v2-alpha/", "family=Dupont");

        var parser = FhirContext.forR4().newJsonParser();
        var patients = (Bundle) parser.parseResource(out.toString());

        Assert.assertEquals(1, patients.getTotal());
        Assert.assertEquals(1, patients.getEntry().size());
        Assert.assertEquals(identifier.getSystem(), ((Patient)patients.getEntry().get(0).getResource()).getIdentifier().get(0).getSystem());
        Assert.assertEquals(identifier.getValue(), ((Patient)patients.getEntry().get(0).getResource()).getIdentifier().get(0).getValue());
        Assert.assertEquals(humanName.getFamily(), ((Patient)patients.getEntry().get(0).getResource()).getName().get(0).getFamily());
    }

}
