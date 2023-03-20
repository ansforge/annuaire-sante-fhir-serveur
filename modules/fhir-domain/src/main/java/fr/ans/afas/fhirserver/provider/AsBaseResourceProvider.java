/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */
/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.provider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.r4.model.DomainResource;
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
public abstract class AsBaseResourceProvider<T> {

    /**
     * The trial-use _total parameter to specify the way that the system calculate the count of a search
     * Allowed values are :
     * <ul>
     *     <li>none</li>
     *     <li>estimate</li>
     *     <li>accurate</li>
     * </ul>
     */
    public static final TokenClientParam TOTAL_PARAM = new TokenClientParam("_param");

    /**
     * The service that store fhir resources
     */
    protected FhirStoreService<T> fhirStoreService;

    /**
     * Construct the base provider
     *
     * @param fhirStoreService the service that store fhir resources
     */
    protected AsBaseResourceProvider(FhirStoreService<T> fhirStoreService) {
        Assert.notNull(fhirStoreService, "fhirStoreService must not be null");
        this.fhirStoreService = fhirStoreService;
    }

    /**
     * Create a resource
     *
     * @param resources the resource to store
     * @return the operation outcomes
     */
    public List<MethodOutcome> create(Collection<? extends DomainResource> resources) {
        for (var resource : resources) {
            resource.setId(UUID.randomUUID().toString());
        }

        var methodOutcomes = new ArrayList<MethodOutcome>();
        var ids = this.fhirStoreService.store(resources, true);
        for (var id : ids) {
            var retVal = new MethodOutcome();
            retVal.setId(new IdType(id.getResourceType(), id.getIdPart(), id.getVersionIdPart()));
            retVal.setCreated(true);
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
    public MethodOutcome update(IdType id, DomainResource resource) {
        var outcome = new MethodOutcome();

        if (id == null) {
            var operationOutcome = this.createOperationOutcomeError(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.INVALID);
            outcome.setOperationOutcome(operationOutcome);
            outcome.setCreated(false);

            return outcome;
        }

        var ids = this.fhirStoreService.store(Set.of(resource), true);
        if (!ids.isEmpty()) {
            outcome.setId(new IdType(resource.getResourceType().toString(), ids.get(0).getIdPart(), ids.get(0).getVersionIdPart()));
            outcome.setCreated(true);
            return outcome;
        }
        throw new UnprocessableEntityException("Unknown error");
    }

    public MethodOutcome delete(@IdParam IdType id) {
        var outcome = new MethodOutcome();

        var ret = this.fhirStoreService.delete(id.getResourceType(), id);
        if (!ret) {
            var operationOutcome = this.createOperationOutcomeError(OperationOutcome.IssueSeverity.ERROR, OperationOutcome.IssueType.NOTFOUND);
            outcome.setOperationOutcome(operationOutcome);
            outcome.setCreated(false);
        }
        outcome.setId(new IdType(id.getResourceType(), id.getIdPart(), id.getVersionIdPart()));

        return outcome;
    }

    private OperationOutcome createOperationOutcomeError(OperationOutcome.IssueSeverity severity, OperationOutcome.IssueType type) {
        var operationOutcome = new OperationOutcome();
        var issueComponent = new OperationOutcome.OperationOutcomeIssueComponent();
        issueComponent.setSeverity(severity);
        issueComponent.setCode(type);
        operationOutcome.addIssue(issueComponent);

        return operationOutcome;
    }

}
