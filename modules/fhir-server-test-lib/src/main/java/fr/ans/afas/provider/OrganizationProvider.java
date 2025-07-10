/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.fhir.AfasBundleProvider;
import fr.ans.afas.fhirserver.provider.AsBaseResourceProvider;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;

/**
 * A simple Organization provider with standard fhir parameters.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class OrganizationProvider<T> extends AsBaseResourceProvider<T> implements IResourceProvider {

    /**
     * The fhir context
     */
    final FhirContext fhirContext;

    /**
     * The expression factory
     */
    final ExpressionFactory<T> expressionFactory;


    /**
     * Manager for next urls (paging)
     */
    final NextUrlManager<T> nextUrlManager;


    public OrganizationProvider(FhirStoreService<T> fhirStoreService, FhirContext fhirContext, ExpressionFactory<T> expressionFactory, NextUrlManager<T> nextUrlManager) {
        super(fhirStoreService);
        this.fhirContext = fhirContext;
        this.expressionFactory = expressionFactory;
        this.nextUrlManager = nextUrlManager;
    }

    @Search()
    public IBundleProvider search(@Count Integer theCount,
                                  @OptionalParam(name = IAnyResource.SP_RES_ID)
                                  TokenAndListParam theId,
                                  @OptionalParam(name = Organization.SP_IDENTIFIER)
                                      TokenAndListParam theIdentifier,
                                  @OptionalParam(name = "_lastUpdated")
                                      DateRangeParam theLastUpdated,
                                  @OptionalParam(name = Organization.SP_NAME)
                                      StringAndListParam theName) throws BadDataFormatException {//
        var selectExpression = new SelectExpression<>(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(IAnyResource.SP_RES_ID).build(), theId);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_IDENTIFIER).build(), theIdentifier);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_NAME).build(), theName);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path("_lastUpdated").build(), theLastUpdated);

        return new AfasBundleProvider<>(fhirStoreService, selectExpression, nextUrlManager);
    }

    @Override
    public Class<Organization> getResourceType() {
        return Organization.class;
    }

    @Read()
    public Organization getResourceById(@IdParam IdType theId) {
        return (Organization) fhirStoreService.findById(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME, theId);
    }
}
