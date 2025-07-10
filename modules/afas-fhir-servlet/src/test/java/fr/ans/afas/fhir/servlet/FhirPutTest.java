/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;


import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.servlet.ServletTestUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

/**
 * Test the fhir PUT operation to follow the https://www.hl7.org/fhir/http.html#update specifications
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirPutTest extends BaseTest {


    @Before
    public void setUp() {

        super.setup();

        Mockito.when(afasConfiguration.getPublicUrl()).thenAnswer(p -> "http://a/fhir/");
    }


    /**
     * If we put a resource that doesn't exist, we have to get a 201 and the header "Location".
     *
     * @throws Exception
     */
    @Test
    public void putNotExistTest() throws Exception {

        var beforeInsert = new Date().getTime() / 1000;

        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();
        p1.setId("id1");
        Mockito.when(fhirServerContext.getFhirStoreService().store(any(), anyBoolean())).then(a ->
                List.of(new IdType("Patient", "id1", "1"))
        );


        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "PUT", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(201, res.getServletResponse().getStatus());
        Assert.assertEquals("1", res.getServletResponse().getHeader("ETag"));

        // We test that the date is set:
        var dtModified = res.getServletResponse().getHeader("Last-Modified");
        var zonedDateTime = ZonedDateTime.from(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH).parse(dtModified));
        var instant = zonedDateTime.toInstant();
        long millis = instant.toEpochMilli() / 1000;
        var afterInsert = new Date().getTime() / 1000;

        Assert.assertTrue(beforeInsert <= millis && afterInsert >= millis);
        Assert.assertEquals("http://a/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", res.getServletResponse().getHeader("Location"));
    }


    /**
     * If we put a resource that exist, we have to get a 200 and the header "Location".
     *
     * @throws Exception
     */
    @Test
    public void putExistTest() throws Exception {
        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();
        p1.setId("idexist");
        Mockito.when(fhirServerContext.getFhirStoreService().store(any(), anyBoolean())).then(a ->
                List.of(new IdType("Patient", "idexist", "2"))
        );

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "PUT", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/idexist", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(200, res.getServletResponse().getStatus());
        Assert.assertEquals("http://a/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/idexist", res.getServletResponse().getHeader("Location"));
    }

    /**
     * Put a resource with a id that is different between the url and the resource. The server must respond a 400 and an OperationOutcome.
     */
    @Test
    public void putWithDifferentIds() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();
        p1.setId("1");

        // Mock the message source to return the expected message
        Mockito.when(messageSource.getMessage(Mockito.eq("error.id.doesnt.match"), any(), any(Locale.class)))
                .thenReturn("Id provided in URL: 2. Id provided in resource: Patient/1");


        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "PUT", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/2", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(400, res.getServletResponse().getStatus());
        var oO = (OperationOutcome) parser.parseResource(res.getWriter().getBuffer().toString());
        Assert.assertEquals(OperationOutcome.IssueSeverity.ERROR, oO.getIssue().get(0).getSeverity());
        Assert.assertEquals(OperationOutcome.IssueType.EXCEPTION, oO.getIssue().get(0).getCode());
        Assert.assertEquals("Id provided in URL: 2. Id provided in resource: Patient/1", oO.getIssue().get(0).getDiagnostics());

    }

    @Test
    public void putIdDoesntMatchTest() throws Exception {
        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();
        p1.setId("id2");

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "PUT", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(400, res.getServletResponse().getStatus());
    }

    @Test
    public void putThrowErrorServerExceptionBecauseMessageSourceNullTest() throws Exception {
        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, null);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "PUT", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res.getServletResponse().getStatus());
    }

}
