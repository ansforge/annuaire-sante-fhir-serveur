/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.sample.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.exception.BadSelectExpression;
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
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Component
public class RassPractitionerProvider<T> extends AsBaseResourceProvider<T> implements IResourceProvider {


    /**
     * Manager for next urls (paging)
     */
    final NextUrlManager<T> nextUrlManager;
    /**
     * The fhir context
     */
    @Autowired
    FhirContext fhirContext;
    /**
     * The expression factory
     */
    @Autowired
    ExpressionFactory<T> expressionFactory;


    /**
     * Construct the RassPractitioner provider
     *
     * @param fhirStoreService the service that store fhir resources
     * @param nextUrlManager   Manager for next urls (paging)
     */
    @Autowired
    protected RassPractitionerProvider(FhirStoreService<T> fhirStoreService, NextUrlManager<T> nextUrlManager) {
        super(fhirStoreService);
        this.nextUrlManager = nextUrlManager;
    }


    /**
     * Get the resource type for the provider
     *
     * @return the resource type
     */
    @Override
    public Class<Practitioner> getResourceType() {
        return Practitioner.class;
    }

    /**
     * Fhir read of a practitioner
     *
     * @param theId the id of the resource to read
     * @return the resource
     */
    @Read()
    public Practitioner getResourceById(@IdParam IdType theId) {
        return (Practitioner) fhirStoreService.findById(FhirServerConstants.PRACTITIONER_FHIR_RESOURCE_NAME, theId);
    }

    /**
     * Fhir search of a practitioner
     *
     * @param theCount this fhir param _count
     * @param theId    the id
     * @return a bundle with a list of practitioners
     */
    @Search()
    public IBundleProvider search(
            @Count Integer theCount,
            @Description(shortDefinition = "The ID of the resource")
            @OptionalParam(name = IAnyResource.SP_RES_ID)
                    TokenAndListParam theId,
            @Description(shortDefinition = "Recherche sur tous les identifiants des professionnels de santé")
            @OptionalParam(name = Practitioner.SP_IDENTIFIER)
                    TokenAndListParam theIdentifier,
            @Description(shortDefinition = "Renvoie uniquement les ressources qui ont été mises à jour pour la dernière fois comme spécifié par la période donnée")
            @IncludeParam(reverse = true)
                    Set<Include> theRevIncludes,
            @OptionalParam(name = "_total") String theTotal,
            @Since InstantType theSince


    ) throws BadSelectExpression, BadDataFormatException {
        var selectExpression = new SelectExpression<>("Practitioner", expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.setTotalMode(theTotal);

        // Fhir filters:
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_FHIR_RESOURCE_NAME).path(IAnyResource.SP_RES_ID).build(), theId);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_FHIR_RESOURCE_NAME).path(Practitioner.SP_IDENTIFIER).build(), theIdentifier);

        selectExpression.fromFhirParamsRevInclude(theRevIncludes);


        // since:
        if (theSince != null) {
            selectExpression.setSince(theSince.getValue());
        }

        return new AfasBundleProvider<>(fhirStoreService, selectExpression, nextUrlManager);
    }


    /**
     * Create a resource of type RassPractitioner
     *
     * @param practitioner the practitioner to store
     * @return the operation outcome
     */
    @Create
    public MethodOutcome create(@ResourceParam Practitioner practitioner) {
        return super.create(List.of(practitioner)).get(0);
    }

    /**
     * Update a resource of type RassPractitioner
     *
     * @param id           the id of the practitioner
     * @param practitioner the practitioner to update
     * @return the operation outcome
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam Practitioner practitioner) {
        return super.update(id, practitioner);
    }

    /**
     * Delete a resource of type RassPractitioner
     *
     * @param id the id of the device
     * @return the operation outcome
     */
    @Override
    @Delete
    public MethodOutcome delete(@IdParam IdType id) {
        return super.delete(id);
    }
}
