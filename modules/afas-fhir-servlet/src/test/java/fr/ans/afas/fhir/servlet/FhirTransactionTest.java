/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet;

import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhir.servlet.servletutils.HttpUtils;
import fr.ans.afas.servlet.ServletTestUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
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
public class FhirTransactionTest extends BaseTest {

    private static final String SERVER_URL = "http://localhost:8080/fhir";


    @Before
    public void setUp() {
        super.setup();
    }


    @Test
    public void simpleTransaction() throws Exception {

        //All Device must have at least id field defined
        var d1 = new Device();
        d1.setId("1234");
        var d2 = new Device();
        d2.setId("4567");
        var d3 = new Device();
        d3.setId("6789");

        // convert to bundle
        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.TRANSACTION);
        // create:
        bundle.addEntry()
                .setResource(d1)
                .getRequest()
                .setUrl("Device")
                .setMethod(Bundle.HTTPVerb.POST);
        // update:
        bundle.addEntry()
                .setFullUrl("Device/4567")
                .setResource(d2)
                .getRequest()
                .setUrl("Device/4567")
                .setMethod(Bundle.HTTPVerb.PUT);
        // create:
        bundle.addEntry()
                .setResource(d3)
                .getRequest()
                .setUrl("Device")
                .setMethod(Bundle.HTTPVerb.POST);


        var parser = FhirContext.forR4().newJsonParser();
        var p1 = new Patient();
        p1.setId("id1");

        Mockito.when(fhirServerContext.getFhirStoreService().store(anyList(), anyBoolean(), anyBoolean())).then(a ->
                List.of(new IdType("Device", "1234", "1"),
                        new IdType("Device", "4567", "1")
                )
        );

        Mockito.when(fhirServerContext.getFhirStoreService().store(anySet(), anyBoolean(), anyBoolean())).then(a ->
                List.of(new IdType("Device", "6789", "1"))
        );

        var servlet = new FhirResourceServlet<>(fhirServerContext, afasConfiguration, fhirOperationFactory, messageSource);
        var res = ServletTestUtil.callAsyncServletWithResponse(servlet, "POST", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", "/fhir/" + HttpUtils.SERVLET_API_PATH + "/", parser.encodeResourceToString(bundle));

        Assert.assertEquals(200, res.getServletResponse().getStatus());
    }
}