/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.servlet.ServletTestUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author aelqadim
 */
public class FhirDeleteTest extends BaseTest {

    @Before
    public void setUp() {
        super.setup();
        Mockito.when(afasConfiguration.getPublicUrl()).thenReturn("http://a/fhir/");
    }

    /**
     * If we delete a resource that exists, we should get a 204 No Content.
     *
     * @throws Exception
     */
    @Test
    public void deleteResourceSuccessfully() throws Exception {
        // Mock FhirStoreService to return true when delete is called
        Mockito.when(fhirServerContext.getFhirStoreService().businessDelete(anyString(), any(IIdType.class))).thenReturn(true);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "DELETE", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        Assert.assertEquals(HttpServletResponse.SC_NO_CONTENT, res.getServletResponse().getStatus());
    }

    /**
     * If we delete a resource that does not exist, we should get a 404 Not Found.
     *
     * @throws Exception
     */
    @Test
    public void deleteNonExistentResource() throws Exception {
        // Mock FhirStoreService to return false when delete is called
        Mockito.when(this.fhirServerContext.getFhirStoreService().businessDelete(anyString(), any(IIdType.class))).thenReturn(false);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "DELETE", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, res.getServletResponse().getStatus());
    }

    /**
     * If we delete a resource and the method is not allowed, we should get a 405 Method Not Allowed.
     *
     * @throws Exception
     */
    @Test
    public void deleteResourceMethodNotAllowed() throws Exception {
        // Mock FhirStoreService to throw MethodNotAllowedException
        Mockito.when(this.fhirServerContext.getFhirStoreService().businessDelete(anyString(), any(IIdType.class))).thenThrow(new MethodNotAllowedException("Method not allowed"));

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "DELETE", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        Assert.assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, res.getServletResponse().getStatus());
    }

    @Test
    public void deleteThrowErrorServerExceptionBecauseFhirStoreServiceIsNull() throws Exception {
        Mockito.when(this.fhirServerContext.getFhirStoreService()).thenReturn(null);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "DELETE", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/Patient/id1", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        Assert.assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, res.getServletResponse().getStatus());
    }

}
