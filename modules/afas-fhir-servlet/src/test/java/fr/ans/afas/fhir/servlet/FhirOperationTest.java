/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.servlet.operation.IndexResourceOperation;
import fr.ans.afas.fhir.servlet.operation.IndexResourceStatusOperation;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhirserver.service.IndexService;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Test operations
 */
public class FhirOperationTest extends BaseTest {

    private static final String SERVER_URL = "http://localhost:8080/fhir";


    @Before
    public void setUp() {
        super.setup();
    }

    @Test
    public void launchIndexOperation() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var indexService = Mockito.mock(IndexService.class);

        Mockito.when(fhirOperationFactory.findOperationByName(Mockito.any(), Mockito.any())).then((a) -> new IndexResourceOperation(a.getArgument(1), indexService));

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/$index?fromDate=2023-07-25", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", "");

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
            var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
            StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/$index-status", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", "");

            var parser = FhirContext.forR4().newJsonParser();
            var outcome = (OperationOutcome) parser.parseResource(out.toString());
            Assert.assertEquals(OperationOutcome.IssueSeverity.INFORMATION, outcome.getIssue().get(0).getSeverity());
            Assert.assertEquals(OperationOutcome.IssueType.VALUE, outcome.getIssue().get(0).getCode());
            Assert.assertEquals(test.getValue(), outcome.getIssue().get(0).getDiagnostics());
        }

    }
}
