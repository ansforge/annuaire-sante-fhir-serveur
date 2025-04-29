/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.error.ErrorWriter;
import fr.ans.afas.fhir.servlet.metadata.CapabilityStatementWriteListener;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.FhirResourceSearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.TenantSearchConfig;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.servlet.ServletTestUtil;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test the capability statement generation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class CapabilityStatementTest extends BaseTest {

    private CapabilityStatementWriteListener<Object> listener;
    private AsyncContext mockAsyncContext;
    private ServletOutputStream mockServletOutputStream;
    private FhirServerContext<Object> mockFhirServerContext;
    private HttpServletResponse mockResponse;
    private StringWriter stringWriter;

    @Before
    public void setUp() throws IOException {
        super.setup();
        when(afasConfiguration.getServletTimeout()).thenReturn(1000);
        when(afasConfiguration.getPublicUrl()).thenReturn("http://localhost:8080/fhir/");
        when(afasConfiguration.getFhir()).thenReturn(new AfasConfiguration.Fhir());

        mockAsyncContext = Mockito.mock(AsyncContext.class);
        mockServletOutputStream = Mockito.mock(ServletOutputStream.class);
        mockFhirServerContext = Mockito.mock(FhirServerContext.class);
        mockResponse =  Mockito.mock(HttpServletResponse.class);


        listener = new CapabilityStatementWriteListener<>(mockFhirServerContext, mockServletOutputStream, mockAsyncContext);
        mockResponse = Mockito.mock(HttpServletResponse.class);







    }

    @Test
    public void test() throws Exception {
        SearchParamConfig searchParamConfig = new SearchParamConfig();
        searchParamConfig.setName("tokenPath");
        searchParamConfig.setSearchType("token");
        searchParamConfig.setDescription("Some doc");

        SearchParamConfig searchParamConfig2 = new SearchParamConfig();
        searchParamConfig2.setName("stringPath");
        searchParamConfig2.setSearchType("string");
        searchParamConfig2.setDescription("Other doc");

        FhirResourceSearchConfig fhirResourceSearchConfig = new FhirResourceSearchConfig();
        fhirResourceSearchConfig.setProfile("Patient Profile");
        fhirResourceSearchConfig.setName("Patient");
        fhirResourceSearchConfig.setCanRead(true);
        fhirResourceSearchConfig.setSearchIncludes("*","Patient:partof");
        fhirResourceSearchConfig.setSearchRevIncludes("*","Patient:partof");
        fhirResourceSearchConfig.setSearchParams(List.of(searchParamConfig, searchParamConfig2));

        TenantSearchConfig tenantSearchConfig = new TenantSearchConfig();
        tenantSearchConfig.setCopyright("@Ans");
        tenantSearchConfig.setResources(List.of(fhirResourceSearchConfig));

        Mockito.when(fhirServerContext.getSearchConfigService().getServerSearchConfig()).thenReturn(tenantSearchConfig);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var resp = ServletTestUtil.callAsyncServletWithResponse(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/metadata", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        //TODO Modifier Servlet TestUtil pour qui reenvoi un 200 au lieu de 0 par defaut?
        //Assert.assertEquals(200, resp.getServletResponse().getStatus());

        var parser = FhirContext.forR4().newJsonParser();
        var capabilityStatement = (CapabilityStatement) parser.parseResource(resp.getWriter().toString());

        Assert.assertEquals("@Ans", capabilityStatement.getCopyright());
        Assert.assertEquals(Enumerations.FHIRVersion._4_0_1, capabilityStatement.getFhirVersion());
        Assert.assertEquals("application/fhir+json", capabilityStatement.getFormat().get(0).getCode());
        Assert.assertEquals("json", capabilityStatement.getFormat().get(1).getCode());


        var rest = capabilityStatement.getRest();
        Assert.assertEquals(1, rest.size());
        var resource = rest.get(0);
        var params = resource.getResource().get(0).getSearchParam();
        Assert.assertEquals(2, params.size());

        var p1 = params.get(0);
        Assert.assertEquals("tokenPath", p1.getName());
        Assert.assertEquals("Some doc", p1.getDocumentation());
        Assert.assertEquals("token", p1.getType().toCode());


    }

    @Test
    public void testAddInteractions() throws Exception {
        // Setup: Mock the FhirResourceSearchConfig and CapabilityStatementRestResourceComponent
        FhirResourceSearchConfig fhirResourceSearchConfig = Mockito.mock(FhirResourceSearchConfig.class);
        CapabilityStatement.CapabilityStatementRestResourceComponent resourceComponent = new CapabilityStatement.CapabilityStatementRestResourceComponent();

        // Mock the behavior of fhirResourceSearchConfig
        when(fhirResourceSearchConfig.isCanDelete()).thenReturn(true);
        when(fhirResourceSearchConfig.isCanRead()).thenReturn(true);
        when(fhirResourceSearchConfig.isCanWrite()).thenReturn(true);

        // Call the method to test
        CapabilityStatementWriteListener.addInteractions(fhirResourceSearchConfig, resourceComponent);

        // Verify that the correct interactions were added
        Assert.assertEquals(5, resourceComponent.getInteraction().size());
        Assert.assertEquals(CapabilityStatement.TypeRestfulInteraction.DELETE, resourceComponent.getInteraction().get(0).getCode());
        Assert.assertEquals(CapabilityStatement.TypeRestfulInteraction.SEARCHTYPE, resourceComponent.getInteraction().get(1).getCode());
        Assert.assertEquals(CapabilityStatement.TypeRestfulInteraction.READ, resourceComponent.getInteraction().get(2).getCode());
        Assert.assertEquals(CapabilityStatement.TypeRestfulInteraction.CREATE, resourceComponent.getInteraction().get(3).getCode());
        Assert.assertEquals(CapabilityStatement.TypeRestfulInteraction.UPDATE, resourceComponent.getInteraction().get(4).getCode());
    }

    @Test
    public void testWriteMeta() throws Exception {
        SearchParamConfig searchParamConfig = new SearchParamConfig();
        searchParamConfig.setName("tokenPath");
        searchParamConfig.setSearchType("token");
        searchParamConfig.setDescription("Some doc");

        SearchParamConfig searchParamConfig2 = new SearchParamConfig();
        searchParamConfig2.setName("stringPath");
        searchParamConfig2.setSearchType("string");
        searchParamConfig2.setDescription("Other doc");

        FhirResourceSearchConfig fhirResourceSearchConfig = new FhirResourceSearchConfig();
        fhirResourceSearchConfig.setProfile("Patient Profile");
        fhirResourceSearchConfig.setName("Patient");
        fhirResourceSearchConfig.setCanRead(true);
        fhirResourceSearchConfig.setSearchIncludes("*","Patient:partof");
        fhirResourceSearchConfig.setSearchRevIncludes("*","Patient:partof");
        fhirResourceSearchConfig.setSearchParams(List.of(searchParamConfig, searchParamConfig2));

        TenantSearchConfig tenantSearchConfig = new TenantSearchConfig();
        //tenantSearchConfig.setCopyright("@Ans");
        tenantSearchConfig.setResources(List.of(fhirResourceSearchConfig));
        tenantSearchConfig.setImplementationGuideUrl("urlImplementationGuide");

        Mockito.when(fhirServerContext.getSearchConfigService().getServerSearchConfig()).thenReturn(tenantSearchConfig);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var resp = ServletTestUtil.callAsyncServletWithResponse(servlet, "GET", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/metadata", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", null);

        //TODO Modifier Servlet TestUtil pour qui reenvoi un 200 au lieu de 0 par defaut?
        //Assert.assertEquals(200, resp.getServletResponse().getStatus());

        var parser = FhirContext.forR4().newJsonParser();
        var capabilityStatement = (CapabilityStatement) parser.parseResource(resp.getWriter().toString());

        Assert.assertEquals("[CanonicalType[urlImplementationGuide]]", capabilityStatement.getImplementationGuide().toString());
        Assert.assertEquals(Enumerations.FHIRVersion._4_0_1, capabilityStatement.getFhirVersion());
        Assert.assertEquals("application/fhir+json", capabilityStatement.getFormat().get(0).getCode());
        Assert.assertEquals("json", capabilityStatement.getFormat().get(1).getCode());


        var rest = capabilityStatement.getRest();
        Assert.assertEquals(1, rest.size());
        var resource = rest.get(0);
        var params = resource.getResource().get(0).getSearchParam();
        Assert.assertEquals(2, params.size());

        var p1 = params.get(0);
        Assert.assertEquals("tokenPath", p1.getName());
        Assert.assertEquals("Some doc", p1.getDocumentation());
        Assert.assertEquals("token", p1.getType().toCode());
    }

    @Test
    public void testWriteError() throws Exception {
        // Llamar al método con un mensaje de error y un código de estado
        String testMessage = "Test error message";
        int testStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        // Simular que el PrintWriter de la respuesta no cause problemas
        StringWriter stringWriter = new StringWriter();
        PrintWriter mockPrintWriter = new PrintWriter(stringWriter);

        when(mockAsyncContext.getResponse()).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(testStatus);
        when(mockResponse.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                mockPrintWriter.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
                // No-op
            }
        });

        Throwable testThrowable = new RuntimeException(testMessage);
        // Llamar al método onError
        listener.onError(testThrowable);

        // Verificar que el contexto asincrónico fue completado
        Mockito.verify(mockAsyncContext).complete();
    }

}
