/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.fhir.servlet.operation.StoreWithDependenciesOperation;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test operations
 */
public class FhirWriteOperationTest extends BaseTest {

    private static final FhirContext fhirContext = FhirContext.forR4();

    @Before
    public void setUp() {
        super.setup();
    }

    @Test
    public void launchIndexOperationOKTest() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var fhirStoreService = Mockito.mock(FhirStoreService.class);

        Mockito.when(fhirOperationFactory.findWriteOperationByName(any(), any(), any())).then((a) -> new StoreWithDependenciesOperation(a.getArgument(1), fhirStoreService, a.getArgument(2)));

        var captor = ArgumentCaptor.forClass(List.class);

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var patient1 = new Patient();
        patient1.setId("1234");
        var patient2 = new Patient();
        patient2.setId("12345");
        var device1 = new Device().setId("5678");
        var device2 = new Device().setId("56789");
        var device3 = new Device().setId("567890");

        var payload = List.of(patient1, patient2);

        patient1.setContained(List.of(device1, device2));
        patient2.setContained(List.of(device3));

        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.addEntry().setResource(payload.get(0));
        bundle.addEntry().setResource(payload.get(1));

        var parameters = new Parameters();
        parameters.addParameter().setName("bundle").setResource(bundle);

        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "POST", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/$store-with-deps", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", fhirContext.newJsonParser().encodeResourceToString(parameters));

        var outcome = (OperationOutcome) fhirContext.newJsonParser().parseResource(out.toString());
        Assert.assertEquals(OperationOutcome.IssueSeverity.INFORMATION, outcome.getIssue().get(0).getSeverity());
        Assert.assertEquals(OperationOutcome.IssueType.VALUE, outcome.getIssue().get(0).getCode());
        Assert.assertEquals("Done", outcome.getIssue().get(0).getDiagnostics());

        verify(fhirStoreService, times(1)).storeWithDependencies(captor.capture(), anyBoolean(), anyBoolean());
        var captured = (List<ResourceAndSubResources>) captor.getValue();
        assertEquals(2, captured.size());
        assertEquals(2, captured.get(0).getSubResources().size());
        assertEquals(1, captured.get(1).getSubResources().size());
        assertEquals(device2.getId(), captured.get(0).getSubResources().get(1).getId());
        assertEquals(device3.getId(), captured.get(1).getSubResources().get(0).getId());
    }

    @Test
    public void launchIndexOperationKOTest() throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        var fhirStoreService = Mockito.mock(FhirStoreService.class);

        Mockito.when(fhirOperationFactory.findWriteOperationByName(any(), any(), any())).then((a) -> new StoreWithDependenciesOperation(a.getArgument(1), fhirStoreService, a.getArgument(2)));

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);

        StringWriter out = ServletTestUtil.callAsyncServlet(servlet, "POST", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/$store-with-deps", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", "pas un json valide");

        var outcome = (OperationOutcome) fhirContext.newJsonParser().parseResource(out.toString());
        Assert.assertEquals(OperationOutcome.IssueSeverity.ERROR, outcome.getIssue().get(0).getSeverity());
        Assert.assertEquals(OperationOutcome.IssueType.EXCEPTION, outcome.getIssue().get(0).getCode());
        Assert.assertNotEquals("Done", outcome.getIssue().get(0).getDiagnostics());
    }
}
