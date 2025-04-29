/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.sample.sample;

import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DomainResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

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
        var l = new ArrayList<DomainResource>();
        for (var i = 0; i < 3; i++) {
            var d1 = new Device();
            d1.setId("id-" + i);
            d1.addDeviceName().setName("My device name " + System.currentTimeMillis());
            d1.addIdentifier().setSystem("http://system.org/").setValue("ID-" + i);
            d1.setLotNumber("lot" + i);
            l.add(d1);
            // flush each x:
            if (i % 2 == 0) {
                storeService.store(l, true);
                l.clear();
            }
        }


    }

}
