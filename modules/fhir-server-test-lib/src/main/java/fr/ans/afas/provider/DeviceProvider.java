/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
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
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;

import java.util.Set;

/**
 * A simple Device provider with standard fhir parameters.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class DeviceProvider<T> extends AsBaseResourceProvider<T> implements IResourceProvider {


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


    public DeviceProvider(FhirStoreService<T> fhirStoreService, FhirContext fhirContext, ExpressionFactory<T> expressionFactory, NextUrlManager<T> nextUrlManager) {
        super(fhirStoreService);
        this.fhirContext = fhirContext;
        this.expressionFactory = expressionFactory;
        this.nextUrlManager = nextUrlManager;
    }

    @Search()
    public IBundleProvider search(@Count Integer theCount,
                                  @OptionalParam(name = Device.SP_IDENTIFIER)
                                          TokenAndListParam theIdentifier,
                                  @OptionalParam(name = Device.SP_DEVICE_NAME)
                                              StringAndListParam theName,
                                  @OptionalParam(name = Device.SP_ORGANIZATION)
                                              ReferenceAndListParam theOwner,
                                  @Description(shortDefinition = "Recherche sur le type de l'équipement matériel lourd")
                                      @OptionalParam(name = Device.SP_TYPE)
                                              TokenAndListParam theType,
                                  @OptionalParam(name = "_lastUpdated")
                                              DateRangeParam theLastUpdated,
                                  @IncludeParam(reverse = true)
                                              Set<Include> theRevIncludes,
                                  @IncludeParam(allow = {
                                          "Device:organization", "*"
                                  })
                                              Set<Include> theIncludes
    ) throws BadDataFormatException {//
        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path(Device.SP_IDENTIFIER).build(), theIdentifier);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path(Device.SP_DEVICE_NAME).build(), theName);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path(Device.SP_ORGANIZATION).build(), theOwner);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path(Device.SP_TYPE).build(), theType);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path("_lastUpdated").build(), theLastUpdated);


        // if the user use _include with *, we recreate _include with all available include
        if (theIncludes != null && theIncludes.stream().anyMatch(ti -> "*".equals(ti.getValue()))) {
            selectExpression.fromFhirParams(Set.of(new Include("Device:organization")));
        } else {
            selectExpression.fromFhirParams(theIncludes);
        }
        selectExpression.fromFhirParamsRevInclude(theRevIncludes);

        return new AfasBundleProvider<>(fhirStoreService, selectExpression, nextUrlManager);
    }


    @Override
    public Class<Device> getResourceType() {
        return Device.class;
    }

    @Read()
    public Device getResourceById(@IdParam IdType theId) {
        return (Device) fhirStoreService.findById(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, theId);
    }
}
