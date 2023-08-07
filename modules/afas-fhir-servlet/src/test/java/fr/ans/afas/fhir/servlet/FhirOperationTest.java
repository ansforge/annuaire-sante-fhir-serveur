/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.operation.IndexResourceOperation;
import fr.ans.afas.fhir.servlet.operation.IndexResourceStatusOperation;
import fr.ans.afas.fhir.servlet.service.FhirOperationFactory;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.IndexService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Test operations
 */
public class FhirOperationTest {

    private static final String SERVER_URL = "http://localhost:8080/fhir";

    @Mock
    FhirStoreService<Object> fhirStoreService;

    @Mock
    ExpressionFactory<Object> expressionFactory;

    @Mock
    NextUrlManager<Object> nextUrlManager;

    @Mock
    FhirOperationFactory fhirOperationFactory;

    @Mock
    AfasConfiguration afasConfiguration;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void launchIndexOperation() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var indexService = Mockito.mock(IndexService.class);

        Mockito.when(fhirOperationFactory.findOperationByName(Mockito.any(), Mockito.any())).then((a) -> new IndexResourceOperation(a.getArgument(1), indexService));

        var servlet = new FhirResourceServlet(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, afasConfiguration, fhirOperationFactory);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/$index?fromDate=2023-07-25", "/fhir/v2-alpha/", "");

        var parser = FhirContext.forR4().newJsonParser();
        var outcome = (OperationOutcome) parser.parseResource(out.toString());
        Assert.assertEquals(OperationOutcome.IssueSeverity.INFORMATION, outcome.getIssue().get(0).getSeverity());
        Assert.assertEquals(OperationOutcome.IssueType.PROCESSING, outcome.getIssue().get(0).getCode());
    }


    @Test
    public void launchIndexStatusOperation() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var indexService = Mockito.mock(IndexService.class);

        var tests = Map.of(Boolean.TRUE, "Running", Boolean.FALSE, "Not running");

        for (var test : tests.entrySet()) {
            // ok
            Mockito.when(indexService.isRunning()).then((e) -> test.getKey());

            Mockito.when(fhirOperationFactory.findOperationByName(Mockito.any(), Mockito.any())).then((a) -> new IndexResourceStatusOperation(a.getArgument(1), indexService));
            var servlet = new FhirResourceServlet(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, afasConfiguration, fhirOperationFactory);
            StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/$index-status", "/fhir/v2-alpha/", "");

            var parser = FhirContext.forR4().newJsonParser();
            var outcome = (OperationOutcome) parser.parseResource(out.toString());
            Assert.assertEquals(OperationOutcome.IssueSeverity.INFORMATION, outcome.getIssue().get(0).getSeverity());
            Assert.assertEquals(OperationOutcome.IssueType.VALUE, outcome.getIssue().get(0).getCode());
            Assert.assertEquals(test.getValue(), outcome.getIssue().get(0).getDiagnostics());
        }

    }
}
