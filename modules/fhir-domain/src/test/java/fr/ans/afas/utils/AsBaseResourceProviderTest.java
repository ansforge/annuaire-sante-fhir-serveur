/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.utils;


import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import fr.ans.afas.fhirserver.provider.AsBaseResourceProvider;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Test the base provider
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AsBaseResourceProviderTest {


    @Test
    public void createTest() {
        var d1 = new Device();
        d1.setId("01");
        var p = new SomeAsBaseResourceProvider();
        var created = p.create(List.of(d1));
        Assert.assertEquals(true, created.get(0).getCreated());
        Assert.assertEquals("01", created.get(0).getId().getIdPart());
        Assert.assertEquals("Device", created.get(0).getId().getResourceType());
    }

    @Test
    public void updateTest() {
        var d1 = new Device();
        d1.setId("01");
        var p = new SomeAsBaseResourceProvider();
        var methodOutcome = p.update(d1.getIdElement(), d1);
        Assert.assertEquals(true, methodOutcome.getCreated());
        Assert.assertEquals("01", methodOutcome.getId().getIdPart());
        Assert.assertEquals("Device", methodOutcome.getId().getResourceType());


        var badMethodOutcome = p.update(null, d1);
        Assert.assertEquals(false, badMethodOutcome.getCreated());
        var issue = ((OperationOutcome) badMethodOutcome.getOperationOutcome()).getIssue().get(0);
        Assert.assertEquals(OperationOutcome.IssueType.INVALID, issue.getCode());

        p.setUpdateOk(false);
        var idType = new IdType();
        Assert.assertThrows(UnprocessableEntityException.class, () ->
                p.update(idType, d1)
        );


    }

    @Test
    public void deleteTest() {
        var d1 = new Device();
        d1.setId("01");
        var p = new SomeAsBaseResourceProvider();
        p.setDeleteOk(true);
        var methodOutcome = p.delete(d1.getIdElement());
        Assert.assertEquals("01", methodOutcome.getId().getIdPart());

        p.setDeleteOk(false);

        var outcome = p.delete(d1.getIdElement());
        Assert.assertEquals(OperationOutcome.IssueType.NOTFOUND, ((OperationOutcome) outcome.getOperationOutcome()).getIssue().get(0).getCode());
        Assert.assertEquals(OperationOutcome.IssueSeverity.ERROR, ((OperationOutcome) outcome.getOperationOutcome()).getIssue().get(0).getSeverity());


    }

    /**
     * Some provider extension to test the object
     */
    @Setter
    public static class SomeAsBaseResourceProvider extends AsBaseResourceProvider<String> {
        static FhirStoreService<String> service = Mockito.mock(FhirStoreService.class);
        boolean deleteOk = true;
        boolean updateOk = true;

        protected SomeAsBaseResourceProvider() {
            super(service);

            Mockito.when(service.store(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean())).then((a) -> {
                if (updateOk) {
                    var ret = new ArrayList<IIdType>();
                    var id = new IdType("Device", "01");
                    id.setParts("http://a", "Device", "01", null);
                    ret.add(id);
                    return ret;
                } else {
                    return new ArrayList<>();
                }
            });

            Mockito.when(service.delete(Mockito.any(), Mockito.any())).then((a) -> deleteOk);


        }
    }

}
