/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.sample.hapi;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
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
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

/**
 * Hapi controller for the FHIR healthcareService resource.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Component
public class HealthcareServiceProvider<T> extends TransactionalResourceProvider<T> implements IResourceProvider {

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
    protected HealthcareServiceProvider(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory) {
        super(fhirStoreService);
        this.expressionFactory = expressionFactory;
    }

    /**
     * Read a resource by ID
     *
     * @param theId the id to find
     * @return the found healthcareService
     */
    @Read()
    public HealthcareService getResourceById(@IdParam IdType theId) {
        return (HealthcareService) fhirStoreService.findById(FhirServerConstants.HEALTHCARESERVICE_FHIR_RESOURCE_NAME, theId);
    }


    @Search()
    public IBundleProvider search(@Count Integer theCount,
                                  @Description(shortDefinition = "Recherche sur l'identifiant")
                                  @OptionalParam(name = HealthcareService.SP_IDENTIFIER)
                                          TokenAndListParam theIdentifier,
                                  @Description(shortDefinition = "Sélectionner le profil de la ressource Healthcare Service. Pour les activités de soins http://interop.esante.gouv.fr/ig/fhir/annuaire-donnee-publique/StructureDefinition/as-healthcareservice-healthcare-activity / ; Pour les équipements sociaux http://interop.esante.gouv.fr/ig/fhir/annuaire-donnee-publique/StructureDefinition/as-healthcareservice-social-equipment")
                                      @OptionalParam(name = ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE)
                                      ca.uhn.fhir.rest.param.UriAndListParam theSearchForProfile,
                                  @Description(shortDefinition = "Renvoie uniquement les ressources qui ont été mises à jour pour la dernière fois comme spécifié par la période donnée")
                                      @OptionalParam(name = "_lastUpdated")
                                      DateRangeParam theLastUpdated) {

        var selectExpression = new SelectExpression<>(FhirServerConstants.HEALTHCARESERVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.HEALTHCARESERVICE_FHIR_RESOURCE_NAME).path(HealthcareService.SP_IDENTIFIER).build(), theIdentifier);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.HEALTHCARESERVICE_FHIR_RESOURCE_NAME).path(ca.uhn.fhir.rest.api.Constants.PARAM_PROFILE).build(), theSearchForProfile);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.HEALTHCARESERVICE_FHIR_RESOURCE_NAME).path("_lastUpdated").build(), theLastUpdated);
        return new AfasBundleProvider<T>(fhirStoreService, selectExpression, nextUrlManager);
    }


    /**
     * Update a resource of type HealthcareService
     *
     * @param id     the id of the healthcareService
     * @param healthcareService the healthcareService to update
     * @return the operation outcome
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam HealthcareService healthcareService) {
        return super.update(id, healthcareService);
    }

    /**
     * Delete a resource of type RassHealthcareService
     *
     * @param id the id of the healthcareService
     * @return the operation outcome
     */
    @Delete
    @Override
    public MethodOutcome delete(@IdParam IdType id) {
        return super.delete(id);
    }

    /**
     * Create a resource of type RassHealthcareService
     *
     * @param healthcareService the healthcareService to store
     * @return the operation outcome
     */
    @Create
    public MethodOutcome create(@ResourceParam HealthcareService healthcareService) {
        return super.create(List.of(healthcareService)).get(0);
    }


    @Override
    public Class<HealthcareService> getResourceType() {
        return HealthcareService.class;
    }
}
