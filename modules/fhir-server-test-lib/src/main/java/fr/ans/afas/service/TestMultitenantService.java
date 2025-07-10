/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;

import fr.ans.afas.fhirserver.search.config.domain.Tenant;
import fr.ans.afas.fhirserver.service.MultiTenantService;

/***
 * A multi tenant service used for tests with only one tenant.
 */
public class TestMultitenantService implements MultiTenantService {

    public static final String TEST_TENANT1 = "tenant-1";

    static Tenant tenant1 = Tenant.builder()
            .path("/" + TEST_TENANT1)
            .name(TEST_TENANT1)
            .suffixCollection("some01")
            .dbname("mydb")
            .build();

    @Override
    public Tenant getTenant() {
        return tenant1;
    }

    @Override
    public Tenant getTenantByName(String currentTenant) {
        return tenant1;
    }
}
