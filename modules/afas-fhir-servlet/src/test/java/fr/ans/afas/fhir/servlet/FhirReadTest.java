/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet;


import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test the fhir read operation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirReadTest {


    private static final String SERVER_URL = "http://localhost:8080/fhir";

    @Mock
    FhirStoreService<Object> fhirStoreService;

    @Mock
    ExpressionFactory<Object> expressionFactory;

    @Mock
    NextUrlManager<Object> nextUrlManager;


    Patient p1 = new Patient();


    @Before
    public void setUp() throws BadLinkException {


        p1.setId("id1");

        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void simpleReadTest() throws Exception {
        Mockito.when(fhirStoreService.findById(any(), any())).then((a) ->
                p1
        );

        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, SERVER_URL);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/Patient/id1", "/fhir/v2-alpha/", null);
        var parser = FhirContext.forR4().newJsonParser();
        var patient = (Patient) parser.parseResource(out.toString());
        Assert.assertNotNull(patient);
    }


    @Test
    public void notFoundTest() throws Exception {
        Mockito.when(fhirStoreService.findById(any(), any())).then((a) ->
                null
        );

        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, SERVER_URL);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/Patient/id2", "/fhir/v2-alpha/", null);
        var parser = FhirContext.forR4().newJsonParser();
        var error404 = (OperationOutcome) parser.parseResource(out.toString());
        Assert.assertNotNull(error404);
        Assert.assertEquals("exception", error404.getIssue().get(0).getCode().toCode());
        Assert.assertEquals("error", error404.getIssue().get(0).getSeverity().toCode());
        Assert.assertEquals("Resource not found with id: id2", error404.getIssue().get(0).getDiagnostics());
    }

}
