/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.sample.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
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
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Resource provider implementation for {@link PractitionerRole}.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Component
public class RassPractitionerRoleProvider<T> extends AsBaseResourceProvider<T> implements IResourceProvider {


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
     * Construct the RassPractitionerRole provider
     *
     * @param fhirStoreService the service that store fhir resources
     * @param nextUrlManager   Manager for next urls (paging)
     */
    @Autowired
    protected RassPractitionerRoleProvider(FhirStoreService<T> fhirStoreService, NextUrlManager<T> nextUrlManager) {
        super(fhirStoreService);
        this.nextUrlManager = nextUrlManager;
    }


    /**
     * Get the resource type for the provider
     *
     * @return the resource type
     */
    @Override
    public Class<PractitionerRole> getResourceType() {
        return PractitionerRole.class;
    }

    /**
     * Fhir read of a practitioner role
     *
     * @param theId the id of the resource to read
     * @return the resource
     */
    @Read()
    public PractitionerRole getResourceById(@IdParam IdType theId) {
        return (PractitionerRole) fhirStoreService.findById(FhirServerConstants.PRACTITIONERROLE_FHIR_RESOURCE_NAME, theId);
    }

    /**
     * Fhir search of a practitioner role
     *
     * @param theCount this fhir param _count
     * @param theId    the id
     * @return a bundle with a list of practitioner roles
     */
    @Search()
    public IBundleProvider search(
            @Count Integer theCount,
            @Description(shortDefinition = "l'id de la ressource")
            @OptionalParam(name = IAnyResource.SP_RES_ID)
            TokenAndListParam theId,
            @Description(shortDefinition = "Any identifier for the practitioner role")
            @OptionalParam(name = Practitioner.SP_IDENTIFIER)
            TokenAndListParam theIdentifier,

            @Description(shortDefinition = "Recherche les ressources PractitionerRole actives")
            @OptionalParam(name = PractitionerRole.SP_ACTIVE)
            TokenAndListParam theActive,
            @Description(shortDefinition = "Recherche sur le savoir-faire ou le Type de savoir-faire")
            @OptionalParam(name = PractitionerRole.SP_SPECIALTY)
            TokenAndListParam theSpecialty,
            @Description(shortDefinition = "Recherche sur la profession/ la catégorie professionnelle/ la fonction/ le genre d'activité/ le mode d'exercice ou la section tableau des pharmaciens")
            @OptionalParam(name = PractitionerRole.SP_ROLE)
            TokenAndListParam theRole,

            @Description(shortDefinition = "Recherche les exercices professionnels et les situation d'exercice rattachés aux professionnels de santé sélectionnés")
            @OptionalParam(name = PractitionerRole.SP_PRACTITIONER)
            ReferenceAndListParam thePractitioner,
            @Description(shortDefinition = "Recherche les exercices professionnels et les situation d'exercice rattachés à la structure sélectionnée")
            @OptionalParam(name = PractitionerRole.SP_ORGANIZATION)
            ReferenceAndListParam theOrganization,
            @IncludeParam(allow = {
                    "PractitionerRole:organization", "PractitionerRole:practitioner", "PractitionerRole:partof", "*"
            })
            Set<Include> theIncludes,
            @OptionalParam(name = "_total") String theTotal,
            @Since InstantType theSince


    ) throws BadSelectExpression {
        var selectExpression = new SelectExpression<>("PractitionerRole", expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.setTotalMode(theTotal);

        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_ROLE_FHIR_RESOURCE_NAME).path(IAnyResource.SP_RES_ID).build(), theId);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_ROLE_FHIR_RESOURCE_NAME).path(FhirServerConstants.PRACTITIONER_ROLE_PARAM_CONFIG_PATH_IDENTIFIER).build(), theIdentifier);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_ROLE_FHIR_RESOURCE_NAME).path(FhirServerConstants.PRACTITIONER_ROLE_PARAM_CONFIG_PATH_ACTIVE).build(), theActive);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_ROLE_FHIR_RESOURCE_NAME).path(FhirServerConstants.PRACTITIONER_ROLE_PARAM_CONFIG_PATH_ROLE).build(), theRole);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_ROLE_FHIR_RESOURCE_NAME).path(FhirServerConstants.PRACTITIONER_ROLE_PARAM_CONFIG_PATH_SPECIALTY).build(), theSpecialty);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_ROLE_FHIR_RESOURCE_NAME).path(FhirServerConstants.PRACTITIONER_ROLE_PARAM_CONFIG_PATH_PRACTITIONER).build(), thePractitioner);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.PRACTITIONER_ROLE_FHIR_RESOURCE_NAME).path(FhirServerConstants.PRACTITIONER_ROLE_PARAM_CONFIG_PATH_ORGANIZATION).build(), theOrganization);


        // relations:
        // if the user use _include with *, we recreate _include with all available include
        if (theIncludes != null && theIncludes.stream().anyMatch(ti -> "*".equals(ti.getValue()))) {
            selectExpression.fromFhirParams(Set.of(
                    new Include("PractitionerRole:organization"),
                    new Include("PractitionerRole:practitioner"),
                    new Include("PractitionerRole:partof")
            ));
        } else {
            selectExpression.fromFhirParams(theIncludes);
        }

        // since:
        if (theSince != null) {
            selectExpression.setSince(theSince.getValue());
        }

        return new AfasBundleProvider<>(fhirStoreService, selectExpression, nextUrlManager);
    }


    /**
     * Create a resource of type RassPractitionerRole
     *
     * @param practitionerRole the practitioner role to store
     * @return the operation outcome
     */
    @Create
    public MethodOutcome create(@ResourceParam PractitionerRole practitionerRole) {
        return (MethodOutcome) super.create(List.of(practitionerRole)).get(0);
    }

    /**
     * Update a resource of type RassPractitionerRole
     *
     * @param id               the id of the practitioner role
     * @param practitionerRole the practitioner role to update
     * @return the operation outcome
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam PractitionerRole practitionerRole) {
        return super.update(id, practitionerRole);
    }

    /**
     * Delete a resource of type RassPractitionerRole
     *
     * @param id the id of the device
     * @return the operation outcome
     */
    @Delete
    @Override
    public MethodOutcome delete(@IdParam IdType id) {
        return super.delete(id);
    }
}
