/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service.impl;

import ca.uhn.fhir.model.api.Include;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.IndexService;
import fr.ans.afas.fhirserver.service.exception.IndexingException;
import fr.ans.afas.rass.service.impl.exception.AlreadyRunningTaskException;
import fr.ans.afas.rass.service.json.GenericSerializer;
import fr.ans.afas.utils.TenantUtil;
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
     * A boolean to ensure that only one job is launched at a time
     */
    @SuppressWarnings("java:S3077") // AtomicBoolean is thread safe
    static volatile AtomicBoolean isRunning = new AtomicBoolean(false);
    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    FhirStoreService<Bson> fhirStoreService;

    ExpressionFactory<Bson> expressionFactory;

    SearchConfigService searchConfigService;

    GenericSerializer genericSerializer;


    @Inject
    public DefaultIndexService(FhirStoreService<Bson> fhirStoreService, ExpressionFactory<Bson> expressionFactory, SearchConfigService searchConfigService, GenericSerializer genericSerializer) {
        this.fhirStoreService = fhirStoreService;
        this.expressionFactory = expressionFactory;
        this.searchConfigService = searchConfigService;
        this.genericSerializer = genericSerializer;
    }


    @Override
    public void refreshIndexes(long fromDate) throws IndexingException {
        var tenant = TenantUtil.getCurrentTenant();
        if (isRunning.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                        TenantUtil.setCurrentTenant(tenant);
                        refreshIndexesSync(fromDate);
                    }
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
            logger.info("[IndexService] Début d'indexation (depuis {})", new Date(fromDate));

            for (var targetResource : searchConfigService.getResources()) {
                logger.info("[IndexService] ➤ Traitement de la ressource '{}'", targetResource);

                var toInclude = new HashSet<Include>();
                var joins = searchConfigService.getJoinsByFhirResource(targetResource);

                if (joins != null && !joins.isEmpty()) {
                    logger.info("[IndexService]   → Joins trouvés : {}", joins.size());

                    var referenceFields = new HashMap<String, String>();
                    for (var j : joins) {
                        logger.debug("[IndexService]     - Join: {} via path '{}'", j.getResource(), j.getPath());
                        toInclude.add(new Include(j.getResource() + ":" + j.getPath()));
                        referenceFields.put(j.getResource(), j.getField());
                    }

                    try {
                        var se = new SelectExpression<>(targetResource, expressionFactory);
                        se.setCount(1000);
                        se.setSince(new Date(fromDate));
                        se.fromFhirParamsRevInclude(toInclude);

                        var pageResult = this.fhirStoreService.search(null, se);
                        int totalResources = 0;
                        int pageCount = 0;


                        do {
                            var page = pageResult.getPage();
                            if (page == null || page.isEmpty()) {
                                logger.debug("[IndexService]     ↪️ Aucune ressource à indexer pour {}", targetResource);
                            } else {
                                logger.debug("[IndexService]     ↪️ Page {}: {} ressources", ++pageCount, page.size());
                                totalResources += page.size();

                                index(targetResource, page, referenceFields);
                                pageResult = this.fhirStoreService.search(pageResult.getContext(), se);
                            }
                        } while (pageResult.isHasNext());

                        logger.info("[IndexService]   → Terminé pour '{}'. Total ressources traitées : {}", targetResource, totalResources);

                    } catch (Exception e) {
                        logger.error("[IndexService] ❌ Erreur lors de l’indexation de '{}'", targetResource, e);
                        throw new IndexingException(e);
                    }
                } else {
                    logger.warn("[IndexService] ⚠ Aucun join défini pour '{}'", targetResource);
                }
            }

            logger.info("[IndexService] ✅ Indexation terminée");
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
            var sub = joins.stream().filter(f -> hasSameId(f, t, referenceFields.get(t.fhirType()))).toList();
            for (var s : sub) {
                var existing = workspace.stream()
                        .filter(r -> r.getResource().getIdElement().equals(s.getIdElement()))
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    existing.getSubResources().add(t);
                } else {
                    var resourceAndSubResources = new ResourceAndSubResources(s, new ArrayList<>(List.of(t)));
                    workspace.add(resourceAndSubResources);
                }
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
