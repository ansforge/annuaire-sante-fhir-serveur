/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;


import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test the fhir read operation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirReadTest extends BaseTest {


    Patient p1 = new Patient();


    @Before
    public void setUp() {

        super.setup();
        p1.setId("id1");

    }


    @Test
    public void simpleReadTest() throws Exception {
        Mockito.when(fhirServerContext.getFhirStoreService().findById(any(), any())).then(a ->
                p1
        );

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);
        var parser = FhirContext.forR4().newJsonParser();
        var patient = (Patient) parser.parseResource(out.toString());
        Assert.assertNotNull(patient);
    }


    @Test
    public void notFoundTest() throws Exception {
        Mockito.when(fhirServerContext.getFhirStoreService().findById(any(), any())).then(a ->
                null
        );

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var output = ServletTestUtil.callAsyncServletWithResponse(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id2", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);
        var parser = FhirContext.forR4().newJsonParser();
        var error404 = (OperationOutcome) parser.parseResource(output.getWriter().toString());
        Assert.assertNotNull(error404);
        Assert.assertEquals(404, output.getServletResponse().getStatus());
        Assert.assertEquals("exception", error404.getIssue().get(0).getCode().toCode());
        Assert.assertEquals("error", error404.getIssue().get(0).getSeverity().toCode());
        Assert.assertEquals("Resource not found with id: id2", error404.getIssue().get(0).getDiagnostics());
    }


    @Test
    public void internalServerErrorTest() throws Exception {
        Mockito.when(fhirServerContext.getFhirStoreService().findById(any(), any())).thenThrow(new RuntimeException("some error"));

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var output = ServletTestUtil.callAsyncServletWithResponse(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id2", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);
        var parser = FhirContext.forR4().newJsonParser();
        var error500 = (OperationOutcome) parser.parseResource(output.getWriter().toString());
        Assert.assertNotNull(error500);
        Assert.assertEquals(500, output.getServletResponse().getStatus());
        Assert.assertEquals("exception", error500.getIssue().get(0).getCode().toCode());
        Assert.assertEquals("error", error500.getIssue().get(0).getSeverity().toCode());
        Assert.assertEquals("Unexpected error", error500.getIssue().get(0).getDiagnostics());
    }

}
