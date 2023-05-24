/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.sample.hapi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.fhir.AfasBundleProvider;
import fr.ans.afas.fhir.TransactionalResourceProvider;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

/**
 * Hapi controller for the FHIR device resource.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Component
public class DeviceProvider<T> extends TransactionalResourceProvider<T> implements IResourceProvider {

    /**
     * The expression factory
     */
    @Inject
    ExpressionFactory<T> expressionFactory;

    @Inject
    ExpressionSerializer<T> expressionSerializer;

    /**
     * The encrypter for urls
     */
    @Inject
    SerializeUrlEncrypter serializeUrlEncrypter;

    @Inject
    NextUrlManager<T> nextUrlManager;

    /**
     * Construct the base provider
     * * @param fhirStoreService the service that store fhir resources
     */
    @Inject
    protected DeviceProvider(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory) {
        super(fhirStoreService);
        this.expressionFactory = expressionFactory;
    }

    /**
     * Read a resource by ID
     *
     * @param theId the id to find
     * @return the found device
     */
    @Read()
    public Device getResourceById(@IdParam IdType theId) {
        return (Device) fhirStoreService.findById(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, theId);
    }


    @Search()
    public IBundleProvider search(@Count Integer theCount,
                                  @Description(shortDefinition = "Recherche sur l'identifiant de l'équipement matériel lourd")
                                  @OptionalParam(name = Device.SP_IDENTIFIER)
                                  TokenAndListParam theIdentifier,
                                  @Description(shortDefinition = "The device name")
                                  @OptionalParam(name = Device.SP_DEVICE_NAME)
                                  StringAndListParam theName) {

        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path(Device.SP_IDENTIFIER).build(), theIdentifier);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path(Device.SP_DEVICE_NAME).build(), theName);
        return new AfasBundleProvider<T>(fhirStoreService, selectExpression, nextUrlManager);
    }


    /**
     * Update a resource of type Device
     *
     * @param id     the id of the device
     * @param device the device to update
     * @return the operation outcome
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam Device device) {
        return super.update(id, device);
    }

    /**
     * Delete a resource of type RassDevice
     *
     * @param id the id of the device
     * @return the operation outcome
     */
    @Delete
    @Override
    public MethodOutcome delete(@IdParam IdType id) {
        return super.delete(id);
    }

    /**
     * Create a resource of type RassDevice
     *
     * @param device the device to store
     * @return the operation outcome
     */
    @Create
    public MethodOutcome create(@ResourceParam Device device) {
        return super.create(List.of(device)).get(0);
    }


    @Override
    public Class<Device> getResourceType() {
        return Device.class;
    }
}
