/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.provider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.util.Assert;

import java.util.*;

/**
 * Base class to create Hapi providers
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public abstract class AsBaseResourceProvider {


    /**
     * The fhir resource name (Device, Organization...)
     */
    protected String resourceName;

    /**
     * The service that store fhir resources
     */
    protected FhirStoreService fhirStoreService;

    /**
     * Construct the base provider
     *
     * @param resourceName     the resource name (Device, Organization...)
     * @param fhirStoreService the service that store fhir resources
     */
    protected AsBaseResourceProvider(String resourceName, FhirStoreService<?> fhirStoreService) {
        Assert.notNull(fhirStoreService, "fhirStoreService must not be null");
        this.fhirStoreService = fhirStoreService;
        this.resourceName = resourceName;
    }

    /**
     * Create a resource
     *
     * @param resources the resource to store
     * @return the operation outcomes
     */
    public List<MethodOutcome> create(List<IBaseResource> resources) {
        for (var resource : resources) {
            if (resource.getIdElement().isEmpty()) {
                resource.setId(UUID.randomUUID().toString());
            }
        }

        var methodOutcomes = new ArrayList<MethodOutcome>();
        var ids = (List<IIdType>) this.fhirStoreService.store(resources, true);
        for (var id : ids) {
            var retVal = new MethodOutcome();
            retVal.setId(new IdType(resourceName, id.getIdPart(), id.getVersionIdPart()));
            methodOutcomes.add(retVal);
        }
        return methodOutcomes;
    }

    /**
     * Update a fhir resource
     *
     * @param id       the id of the resource
     * @param resource the resource to update
     * @return the operation outcome
     */
    public MethodOutcome update(@IdParam IdType id, @ResourceParam IBaseResource resource) {

        var ids = (List<IIdType>) this.fhirStoreService.store(Set.of(resource), true);
        if (!ids.isEmpty()) {
            var retVal = new MethodOutcome();
            retVal.setId(new IdType(resourceName, ids.get(0).getIdPart(), ids.get(0).getVersionIdPart()));

            var outcome = new OperationOutcome();
            outcome.addIssue().setDiagnostics("One minor issue detected");
            retVal.setOperationOutcome(outcome);

            return retVal;
        }
        throw new UnprocessableEntityException("Unknown error");
    }


    @Transaction
    public Bundle transaction(@TransactionParam Bundle theInput) {
        var ids = new HashSet<>();
        var toCreate = new ArrayList<IBaseResource>();
        var toUpdate = new ArrayList<IBaseResource>();
        var toDelete = new ArrayList<IBaseResource>();
        var toCreateOutcomes = new ArrayList<MethodOutcome>();
        var toUpdateOutcomes = new ArrayList<MethodOutcome>();

        // prepare the call
        for (var nextEntry : theInput.getEntry()) {
            if (Bundle.HTTPVerb.POST.equals(nextEntry.getRequest().getMethod())) {
                toCreate.add(nextEntry.getResource());
            } else if (Bundle.HTTPVerb.PUT.equals(nextEntry.getRequest().getMethod())) {
                ids.add(nextEntry.getId());
                toUpdate.add(nextEntry.getResource());
            } else if (Bundle.HTTPVerb.DELETE.equals(nextEntry.getRequest().getMethod())) {
                ids.add(nextEntry.getId());
                toDelete.add(nextEntry.getResource());
            }
        }
        // calls:
        if (!toCreate.isEmpty()) {
            toCreateOutcomes.addAll(create(toCreate));
        }
        if (!toUpdate.isEmpty()) {
            toUpdateOutcomes.addAll(create(toUpdate));
        }


        // build the response:
        var retVal = new Bundle();
        for (var toCreateOutcome : toCreateOutcomes) {
            var resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("201 Created");
            resp.setLocation(toCreateOutcome.getId().getValue());
            retVal.addEntry().setResponse(resp);
        }

        for (var toUpdateOutcome : toUpdateOutcomes) {
            var resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("200 OK");
            resp.setLocation(toUpdateOutcome.getId().getValue());
            retVal.addEntry().setResponse(resp);
        }

        retVal.setTotal(toCreateOutcomes.size() + toUpdateOutcomes.size());

        return retVal;
    }

}
