/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.sample.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.StringAndListParam;
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
import org.hl7.fhir.r4.model.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resource provider implementation for {@link Organization}.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Component
public class RassOrganizationProvider<T> extends AsBaseResourceProvider<T> implements IResourceProvider {


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
     * Construct the RassOrganization provider
     *
     * @param fhirStoreService the service that store fhir resources
     * @param nextUrlManager   Manager for next urls (paging)
     */
    @Autowired
    protected RassOrganizationProvider(FhirStoreService<T> fhirStoreService, NextUrlManager<T> nextUrlManager) {
        super(fhirStoreService);
        this.nextUrlManager = nextUrlManager;
    }

    /**
     * Get the resource type for the provider
     *
     * @return the resource type
     */
    @Override
    public Class<Organization> getResourceType() {
        return Organization.class;
    }

    /**
     * Fhir read of an organization
     *
     * @param theId the id of the resource to read
     * @return the resource
     */
    @Read()
    public Organization getResourceById(@IdParam IdType theId) {
        return (Organization) fhirStoreService.findById(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME, theId);
    }

    /**
     * Fhir search of an organization
     *
     * @param theCount             this fhir param _count
     * @param theId                the id
     * @param theActive            if the organization is active
     * @param theAddress           the address
     * @param theAddressCity       the city in the address
     * @param theAddressCountry    the country in the address
     * @param theAddressPostalcode the postal code in the address
     * @param theAddressState      the state in the address
     * @param theAddressUse        the use of the address
     * @param theEndpoint          the endpoint
     * @param theIdentifier        the identifier
     * @param theName              the name
     * @param thePartof            the fhir param _partof
     * @param theType              the type
     * @return a bundle with a list of organization
     */
    // The number of parameters is due to the Hapi framework
    @SuppressWarnings("java:S107")
    @Search()
    public IBundleProvider search(
            @Count Integer theCount,
            @Description(shortDefinition = "Recherche sur l'id de la ressource Organization")
            @OptionalParam(name = IAnyResource.SP_RES_ID)
                    TokenAndListParam theId,
            @Description(shortDefinition = "Recherche que les ressources Organizations actives")
            @OptionalParam(name = Organization.SP_ACTIVE)
                    TokenAndListParam theActive,
            @Description(shortDefinition = "Recherche sur (une partie) de l'adresse de la structure.")
            @OptionalParam(name = Organization.SP_ADDRESS)
                    StringAndListParam theAddress,
            @Description(shortDefinition = "Recherche sur la commune spécifiée dans une adresse")
            @OptionalParam(name = Organization.SP_ADDRESS_CITY)
                    StringAndListParam theAddressCity,
            @Description(shortDefinition = "Recherche sur le pays spécifié dans une adresse")
            @OptionalParam(name = Organization.SP_ADDRESS_COUNTRY)
                    StringAndListParam theAddressCountry,
            @Description(shortDefinition = "Recherche sur le code postal spécifié dans une adresse")
            @OptionalParam(name = Organization.SP_ADDRESS_POSTALCODE)
                    StringAndListParam theAddressPostalcode,
            @Description(shortDefinition = "A state specified in an address")
            @OptionalParam(name = Organization.SP_ADDRESS_STATE)
                    StringAndListParam theAddressState,
            @Description(shortDefinition = "Recherche sur un code use spécifié dans adresse")
            @OptionalParam(name = Organization.SP_ADDRESS_USE)
                    TokenAndListParam theAddressUse,
            @Description(shortDefinition = "Technical endpoints providing access to services operated for the organization")
            @OptionalParam(name = Organization.SP_ENDPOINT)
                    ReferenceAndListParam theEndpoint,
            @Description(shortDefinition = "Recherche sur tous les identifiants des structures")
            @OptionalParam(name = Organization.SP_IDENTIFIER)
                    TokenAndListParam theIdentifier,
            @Description(shortDefinition = "Recherche sur la raison sociale des structures")
            @OptionalParam(name = Organization.SP_NAME)
                    StringAndListParam theName,
            @Description(shortDefinition = "Recherche tous les établissements géographiques rattachés à une même entité juridique")
            @OptionalParam(name = Organization.SP_PARTOF)
                    ReferenceAndListParam thePartof,
            @Description(shortDefinition = "Recherche sur le type de structure/ code APE/ catégorie juridique/ secteur d'activité/ catégorie d'établissement ou le code SPH de la structure")
            @OptionalParam(name = Organization.SP_TYPE)
                    TokenAndListParam theType,
            @OptionalParam(name = "_total") String theTotal,
            @Since InstantType theSince

    ) throws BadSelectExpression, BadDataFormatException {
        var selectExpression = new SelectExpression<>("Organization", expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.setTotalMode(theTotal);

        // Fhir filters:
        var addressUseSearchPath = FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_ADDRESS_USE).build();
        var addressStateSearchPath = FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_ADDRESS_STATE).build();
        var addressPostalCodeSearchPath = FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_ADDRESS_POSTALCODE).build();
        var addressCountrySearchPath = FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_ADDRESS_COUNTRY).build();
        var addressCitySearchPath = FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_ADDRESS_CITY).build();

        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(IAnyResource.SP_RES_ID).build(), theId);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_ACTIVE).build(), theActive);
        selectExpression.fromFhirParams(addressCitySearchPath, theAddressCity);
        selectExpression.fromFhirParams(addressCountrySearchPath, theAddressCountry);
        selectExpression.fromFhirParams(addressPostalCodeSearchPath, theAddressPostalcode);
        selectExpression.fromFhirParams(addressStateSearchPath, theAddressState);
        selectExpression.fromFhirParams(addressUseSearchPath, theAddressUse);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_NAME).build(), theName);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_IDENTIFIER).build(), theIdentifier);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_ENDPOINT).build(), theEndpoint);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_PARTOF).build(), thePartof);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.ORGANIZATION_FHIR_RESOURCE_NAME).path(Organization.SP_TYPE).build(), theType);


        // address is a special parameter that match multiple parameters:
        selectExpression.fromFhirParams(
                List.of(
                        addressCitySearchPath,
                        addressCountrySearchPath,
                        addressPostalCodeSearchPath,
                        addressStateSearchPath,
                        addressUseSearchPath
                )
                , theAddress);


        // since:
        if (theSince != null) {
            selectExpression.setSince(theSince.getValue());
        }

        return new AfasBundleProvider<>(fhirStoreService, selectExpression, nextUrlManager);
    }


    /**
     * Create a resource of type RassOrganization
     *
     * @param organization the organization to store
     * @return the operation outcome
     */
    @Create
    public MethodOutcome create(@ResourceParam Organization organization) {
        return super.create(List.of(organization)).get(0);
    }

    /**
     * Update a resource of type RassOrganization
     *
     * @param id           the id of the organization
     * @param organization the organization to update
     * @return the operation outcome
     */
    @Update
    public MethodOutcome update(@IdParam IdType id, @ResourceParam Organization organization) {
        return super.update(id, organization);
    }

    /**
     * Delete a resource of type RassOrganization
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
