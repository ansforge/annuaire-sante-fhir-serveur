/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.sample.hapi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.fhir.AfasBundleProvider;
import fr.ans.afas.fhirserver.provider.AsBaseResourceProvider;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.FhirServerConstants;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.r4.model.Device;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Hapi controller for the FHIR device resource.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Component
public class DeviceProvider extends AsBaseResourceProvider implements IResourceProvider {

    /**
     * The expression factory
     */
    @Autowired
    ExpressionFactory<?> expressionFactory;

    @Autowired
    ExpressionSerializer expressionSerializer;

    /**
     * The encrypter for urls
     */
    @Autowired
    SerializeUrlEncrypter serializeUrlEncrypter;

    /**
     * Construct the base provider
     * * @param fhirStoreService the service that store fhir resources
     */
    @Autowired
    protected DeviceProvider(FhirStoreService<?> fhirStoreService, ExpressionFactory<?> expressionFactory) {
        super("Device", fhirStoreService);
        this.expressionFactory = expressionFactory;
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
        return new AfasBundleProvider<>(fhirStoreService, expressionSerializer, selectExpression, serializeUrlEncrypter);
    }

    @Override
    public Class<Device> getResourceType() {
        return Device.class;
    }
}
