/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.sample.sample;

import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.r4.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Insert sample data for the demo
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Configuration
public class InsertSampleData {

    /**
     * The store service
     */
    @Autowired
    FhirStoreService<?> storeService;

    /**
     * Create and insert one device
     */
    @PostConstruct
    public void insertSampleData() {
        var d1 = new Device();
        d1.setId("id-1");
        d1.addDeviceName().setName("My device name " + System.currentTimeMillis());
        d1.addIdentifier().setSystem("http://system.org/").setValue("ID-1");
        d1.setLotNumber("lot1");
        storeService.store(List.of(d1), true);
    }

}
