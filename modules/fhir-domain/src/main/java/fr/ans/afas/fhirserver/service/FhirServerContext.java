/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service;


import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import lombok.Getter;

/**
 * A context that contains services and managers
 *
 * @param <T>
 */
@Getter
public class FhirServerContext<T> {

    protected final FhirStoreService<T> fhirStoreService;
    protected final MultiTenantService multiTenantService;
    protected final ExpressionFactory<T> expressionFactory;
    protected final SearchConfigService searchConfigService;
    protected final NextUrlManager<T> nextUrlManager;
    protected final FhirContext fhirContext;
    protected final SecurityService securityService;

    public FhirServerContext(FhirStoreService<T> fhirStoreService, MultiTenantService multiTenantService, ExpressionFactory<T> expressionFactory, SearchConfigService searchConfigService, NextUrlManager<T> nextUrlManager, SecurityService securityService) {
        this.fhirStoreService = fhirStoreService;
        this.multiTenantService = multiTenantService;
        this.expressionFactory = expressionFactory;
        this.searchConfigService = searchConfigService;
        this.nextUrlManager = nextUrlManager;
        this.securityService = securityService;
        this.fhirContext = FhirContext.forR4();
    }
}
