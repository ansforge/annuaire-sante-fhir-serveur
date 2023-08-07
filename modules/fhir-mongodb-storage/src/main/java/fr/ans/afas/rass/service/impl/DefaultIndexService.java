/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service.impl;

import ca.uhn.fhir.model.api.Include;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.IndexService;
import fr.ans.afas.fhirserver.service.exception.IndexingException;
import fr.ans.afas.rass.service.impl.exception.AlreadyRunningTaskException;
import fr.ans.afas.rass.service.json.GenericSerializer;
import org.bson.conversions.Bson;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.SpelEvaluationException;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DefaultIndexService implements IndexService {

    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * A boolean to ensure that only one job is launched at a time
     */
    @SuppressWarnings("java:S3077") // AtomicBoolean is thread safe
    static volatile AtomicBoolean isRunning = new AtomicBoolean(false);

    FhirStoreService<Bson> fhirStoreService;

    ExpressionFactory<Bson> expressionFactory;

    SearchConfig searchConfig;

    GenericSerializer genericSerializer;


    @Inject
    public DefaultIndexService(FhirStoreService<Bson> fhirStoreService, ExpressionFactory<Bson> expressionFactory, SearchConfig searchConfig, GenericSerializer genericSerializer) {
        this.fhirStoreService = fhirStoreService;
        this.expressionFactory = expressionFactory;
        this.searchConfig = searchConfig;
        this.genericSerializer = genericSerializer;
    }


    @Override
    public void refreshIndexes(long fromDate) throws IndexingException {
        if (isRunning.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() ->
                refreshIndexesSync(fromDate)
            );
        } else {
            throw new AlreadyRunningTaskException("Indexing already running");
        }

    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }


    @Override
    public void refreshIndexesSync(long fromDate) {
        try {
            for (var targetCollectionName : searchConfig.getResources()) {
                var toInclude = new HashSet<Include>();
                var joins = searchConfig.getJoinsByFhirResource(targetCollectionName);
                if (joins != null) {
                    var referenceFields = new HashMap<String, String>();
                    for (var j : joins) {
                        toInclude.add(new Include(j.getResource() + ":" + j.getPath()));
                        referenceFields.put(j.getResource(), j.getField());
                    }
                    try {
                        var se = new SelectExpression<>(targetCollectionName, expressionFactory);
                        se.setCount(1000);
                        se.setSince(new Date(fromDate));
                        se.fromFhirParamsRevInclude(toInclude);
                        var pageResult = this.fhirStoreService.search(null, se);
                        do {
                            var page = pageResult.getPage();

                            // reindex resources:
                            index(targetCollectionName, page, referenceFields);

                            // and paginate:
                            pageResult = this.fhirStoreService.search(pageResult.getContext(), se);
                        } while (pageResult.isHasNext());


                    } catch (Exception e) {
                        throw new IndexingException(e);
                    }
                }
            }
        } finally {
            isRunning.set(false);
        }
    }


    private void index(String mainResource, List<DomainResource> resources, Map<String, String> referenceFields) {
        var mainResources = resources.stream()
                .collect(Collectors.partitioningBy(r -> r.getIdElement().getResourceType().equalsIgnoreCase(mainResource)));

        var targets = mainResources.get(Boolean.FALSE);
        var joins = mainResources.get(Boolean.TRUE);
        var workspace = new ArrayList<ResourceAndSubResources>();

        for (var t : targets) {
            var sub = joins.stream().filter(f -> hasSameId(f, t, referenceFields.get(t.fhirType()))).collect(Collectors.toList());
            for (var s : sub) {
                var resourceAndSubResources = new ResourceAndSubResources(s, List.of(t));
                workspace.add(resourceAndSubResources);
            }
        }
        this.fhirStoreService.storeWithDependencies(workspace, false, true);
    }

    private boolean hasSameId(IBaseResource main, IBaseResource sub, String referenceField) {
        try {
            var vals = genericSerializer.extractValues(sub, referenceField);
            for (var val : vals) {
                if (val != null) {
                    if (!(val instanceof Reference)) {
                        throw new BadConfigurationException("Join can only be configured on reference fields. Not a reference: " + sub.getClass().getName() + "." + referenceField);
                    }
                    return (((Reference) val).getReference().equals(main.getIdElement().getResourceType() + "/" + main.getIdElement().getIdPart()));
                }
            }
        } catch (SpelEvaluationException e) {
            // notion to do the field is not found
            logger.debug("field not found in the resource");
        }
        return false;
    }

}
