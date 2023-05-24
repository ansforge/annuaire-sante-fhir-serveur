/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir;

import ca.uhn.fhir.rest.annotation.Transaction;
import ca.uhn.fhir.rest.annotation.TransactionParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import fr.ans.afas.fhirserver.provider.AsBaseResourceProvider;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TransactionalResourceProvider<T> extends AsBaseResourceProvider<T> {

    /**
     * Construct the base provider
     *
     * @param fhirStoreService the service that store fhir resources
     */
    public TransactionalResourceProvider(FhirStoreService<T> fhirStoreService) {
        super(fhirStoreService);
    }


    @Transaction
    public Bundle transaction(@TransactionParam Bundle input) {
        var resourceMap = new LinkedHashMap<String, List<Bundle.BundleEntryComponent>>();

        for (var entry : input.getEntry()) {
            var resourceName = entry.getResource().getResourceType().name();

            if (resourceMap.containsKey(resourceName)) {
                resourceMap.get(resourceName).add(entry);
            } else {
                var list = new LinkedList<Bundle.BundleEntryComponent>();
                list.add(entry);
                resourceMap.put(resourceName, list);
            }
        }

        var response = new Bundle();
        var resourceCounter = 0;

        for (var entries : resourceMap.values()) {
            var resp = handleResourceEntries(entries);

            for (var entry : resp.getEntry()) {
                response.addEntry(entry);
            }

            resourceCounter += resp.getTotal();
        }

        response.setTotal(resourceCounter);

        return response;
    }

    private Bundle handleResourceEntries(List<Bundle.BundleEntryComponent> entries) {
        var ids = new HashSet<>();
        var toCreateList = new ArrayList<DomainResource>();
        var toUpdateList = new ArrayList<DomainResource>();
        var toDeleteList = new ArrayList<DomainResource>();
        var toCreateOutcomes = new ArrayList<MethodOutcome>();
        var toUpdateOutcomes = new ArrayList<MethodOutcome>();
        var toDeleteOutcomes = new ArrayList<MethodOutcome>();

        // prepare the call
        for (var nextEntry : entries) {
            if (Bundle.HTTPVerb.POST.equals(nextEntry.getRequest().getMethod())) {
                toCreateList.add((DomainResource) nextEntry.getResource());
            } else if (Bundle.HTTPVerb.PUT.equals(nextEntry.getRequest().getMethod())) {
                ids.add(nextEntry.getId());
                toUpdateList.add((DomainResource) nextEntry.getResource());
            } else if (Bundle.HTTPVerb.DELETE.equals(nextEntry.getRequest().getMethod())) {
                ids.add(nextEntry.getId());
                toDeleteList.add((DomainResource) nextEntry.getResource());
            }
        }
        // calls:
        if (!toCreateList.isEmpty()) {
            toCreateOutcomes.addAll(create(toCreateList));
        }
        if (!toUpdateList.isEmpty()) {
            for (var update : toUpdateList) {
                toUpdateOutcomes.add(update(update.getIdElement(), update));
            }
        }
        if (!toDeleteList.isEmpty()) {
            for (var delete : toDeleteList) {
                toDeleteOutcomes.add(delete(delete.getIdElement()));
            }
        }

        // build the response:
        return buildResponse(toCreateOutcomes, toUpdateOutcomes, toDeleteOutcomes);
    }

    @NotNull
    private Bundle buildResponse(ArrayList<MethodOutcome> toCreateOutcomes, ArrayList<MethodOutcome> toUpdateOutcomes, ArrayList<MethodOutcome> toDeleteOutcomes) {
        var retVal = new Bundle();
        for (var toCreateOutcome : toCreateOutcomes) {
            Bundle.BundleEntryResponseComponent resp;
            if (Boolean.TRUE.equals(toCreateOutcome.getCreated())) {
                resp = this.createResponse("201 Created", toCreateOutcome);
            } else {
                resp = this.createResponse("422 Unprocessable Entity", toCreateOutcome);
            }
            retVal.addEntry().setResponse(resp);
        }

        for (var toUpdateOutcome : toUpdateOutcomes) {
            Bundle.BundleEntryResponseComponent resp;
            if (Boolean.TRUE.equals(toUpdateOutcome.getCreated())) {
                resp = this.createResponse("200 OK", toUpdateOutcome);
            } else {
                resp = this.createResponse("400 Bad Request", toUpdateOutcome);
            }
            retVal.addEntry().setResponse(resp);
        }

        for (var toDeleteOutcome : toDeleteOutcomes) {
            var resp = new Bundle.BundleEntryResponseComponent();
            resp.setStatus("200 OK");
            resp.setLocation(toDeleteOutcome.getId().getValue());
            retVal.addEntry().setResponse(resp);
        }

        retVal.setTotal(toCreateOutcomes.size() + toUpdateOutcomes.size());
        return retVal;
    }

    private Bundle.BundleEntryResponseComponent createResponse(String status, MethodOutcome outcome) {
        var resp = new Bundle.BundleEntryResponseComponent();
        resp.setStatus(status);
        resp.setLocation(outcome.getId().getValue());

        return resp;
    }


}
