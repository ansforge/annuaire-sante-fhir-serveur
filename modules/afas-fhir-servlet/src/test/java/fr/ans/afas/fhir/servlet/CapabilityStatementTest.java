/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.StringWriter;

import static org.mockito.Mockito.when;

/**
 * Test the capability statement generation
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class CapabilityStatementTest {

    @Mock
    FhirStoreService<Object> fhirStoreService;

    @Mock
    ExpressionFactory<Object> expressionFactory;

    @Mock
    NextUrlManager<Object> nextUrlManager;

    @Mock
    AfasConfiguration afasConfiguration;

    @Mock
    SearchConfig searchConfig = new TestSearchConfig();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(afasConfiguration.getServletTimeout()).thenReturn(1000);
        when(afasConfiguration.getPublicUrl()).thenReturn("http://localhost:8080/fhir/");
        when(afasConfiguration.getFhir()).thenReturn(new AfasConfiguration.Fhir());
    }

    @Test
    public void test() throws Exception {

        var servlet = new FhirResourceServlet<>(fhirStoreService, expressionFactory, new TestSearchConfig(), nextUrlManager, afasConfiguration, null);
        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "GET", "/fhir/v2-alpha/metadata", "/fhir/v2-alpha/", null);

        var parser = FhirContext.forR4().newJsonParser();
        var capabilityStatement = (CapabilityStatement) parser.parseResource(out.toString());

        Assert.assertEquals("@Ans", capabilityStatement.getCopyright());
        Assert.assertEquals(Enumerations.FHIRVersion._4_0_1, capabilityStatement.getFhirVersion());
        Assert.assertEquals("application/fhir+json", capabilityStatement.getFormat().get(0).getCode());
        Assert.assertEquals("json", capabilityStatement.getFormat().get(1).getCode());


        var rest = capabilityStatement.getRest();
        Assert.assertEquals(1, rest.size());
        var resource = rest.get(0);
        var params = resource.getResource().get(0).getSearchParam();
        Assert.assertEquals(6, params.size());

        var p1 = params.get(0);
        Assert.assertEquals("tokenPath", p1.getName());
        Assert.assertEquals("Some doc", p1.getDocumentation());
        Assert.assertEquals("token", p1.getType().toCode());


    }


}
