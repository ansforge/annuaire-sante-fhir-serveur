/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service;


import fr.ans.afas.fhirserver.search.config.domain.Tenant;

public interface MultiTenantService {

    Tenant getTenant();

    Tenant getTenantByName(String currentTenant);
}
