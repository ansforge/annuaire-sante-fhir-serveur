/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;

/**
 * Test the fhir PUT operation to follow the https://www.hl7.org/fhir/http.html#update specifications
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirCreateTest extends BaseTest {


    @Before
    public void setUp() {
        super.setup();
        Mockito.when(afasConfiguration.getPublicUrl()).thenAnswer((p) -> "http://a/fhir/");
    }


    /**
     * If we post an invalid resource, we have to get a 400 Bad Request.
     *
     * @throws Exception
     */
    @Test
    public void postInvalidResourceTest() throws Exception {
        var invalidResource = "{invalid-json}"; // Representa un recurso no válido

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "POST", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", invalidResource);

        Assert.assertEquals(422, res.getServletResponse().getStatus());
    }

    /**
     * If we post to an invalid endpoint, we have to get a 404 Not Found.
     *
     * @throws Exception
     */
    @Test
    public void postInvalidEndpointTest() throws Exception {
        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "POST", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/InvalidResource", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(404, res.getServletResponse().getStatus());
    }

    /**
     * If we post a resource that violates business rules, we have to get a 422 Unprocessable Entity.
     *
     * @throws Exception
     */
    @Test
    public void postUnprocessableEntityTest() throws Exception {
        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();
        p1.setId("id1");

        // Simular una excepción que representa una violación de reglas de negocio
        Mockito.when(fhirServerContext.getFhirStoreService().store(any(), anyBoolean())).thenThrow(new UnprocessableEntityException("Business rule violation"));

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "POST", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(422, res.getServletResponse().getStatus());
    }

    /**
     * If we post a resource  exist, we have to get a 201 and the header "Location".
     */
    @Test
    public void postResourceCreatedTest() throws Exception {
        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();
        p1.setId("id1");

        Mockito.when(fhirServerContext.getFhirStoreService().store(any(), anyBoolean())).thenReturn(List.of(new IdType("Patient", "id1", "1")));
        Mockito.when(fhirServerContext.getFhirStoreService().findById(anyString(), any(IIdType.class))).thenReturn(p1);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);

        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "POST", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(p1));

        Assert.assertEquals(201, res.getServletResponse().getStatus());
        Assert.assertEquals("http://a/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", res.getServletResponse().getHeader("Location"));
    }


}
