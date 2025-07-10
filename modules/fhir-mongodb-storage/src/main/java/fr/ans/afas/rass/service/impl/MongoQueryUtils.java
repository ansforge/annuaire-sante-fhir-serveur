/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service.impl;

import com.mongodb.MongoCommandException;
import com.mongodb.MongoExecutionTimeoutException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.JoinPath;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import fr.ans.afas.rass.service.CloseableWrapper;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to generate some mongodb requests
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoQueryUtils {

    /**
     * Search attribute for the valid from date
     */
    public static final String VALID_FROM_ATTRIBUTE = "_validFrom";
    /**
     * Search attribute for the valid to date
     */
    public static final String VALID_TO_ATTRIBUTE = "_validTo";
    /**
     * attribute that store the last write date. The date is updated even if the object is not updated
     */
    public static final String LAST_WRITE_DATE = "_lastWriteDate";
    /**
     * Search attribute for the id
     */
    public static final String ID_ATTRIBUTE = "_id";
    /**
     * Search attribute for the revision
     */
    public static final String REVISION_ATTRIBUTE = "_revision";


    /**
     * Count results of the query
     *
     * @param searchConfigService the search config
     * @param collection          the collection
     * @param selectExpression    the select expression
     * @param c                   the count option
     * @return the result (the count)
     */
    public static CountResult count(SearchConfigService searchConfigService, MongoCollection<Document> collection, SelectExpression<Bson> selectExpression, CountOptions c, MongoMultiTenantService mongoMultiTenantService) {
        var searchRevision = new Date().getTime();
        try {
            optimizeQuery(searchConfigService, selectExpression);
            if (hasAggregation(selectExpression)) {
                var agg = AggregationUtils.generateAggregation(searchConfigService, selectExpression, searchRevision, null, mongoMultiTenantService);
                agg.add(new Document("$count", "c"));
                var aggregate = collection.aggregate(agg).maxTime(c.getMaxTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
                try (var it = aggregate.iterator()) {
                    Integer count;
                    if (it.hasNext()) {
                        count = (Integer) it.next().get("c");
                    } else {
                        count = 0;
                    }
                    return CountResult.builder().total(count.longValue()).build();
                }
            } else {
                var query = selectExpression.interpreter();
                var wrappedQuery = addSinceParam(selectExpression, wrapQueryWithRevisionDate(searchRevision, query));
                return CountResult.builder().total(collection.countDocuments(wrappedQuery, c)).build();
            }
        } catch (MongoExecutionTimeoutException | MongoCommandException e) {
            return CountResult.builder().total(null).build();
        }
    }

    /**
     * Search the first page and get the mongodb cursor
     *
     * @param searchConfigService the search config
     * @param pageSize            the page size
     * @param selectExpression    the select expression
     * @param collection          the mongo collection where to search
     * @param searchRevision      the searchRevision
     * @return the mongodb cursor
     */
    @NotNull
    public static CloseableWrapper<MongoCursor<Document>> searchFirstPage(SearchConfigService searchConfigService, int pageSize, SelectExpression<Bson> selectExpression, MongoCollection<Document> collection, Long searchRevision, MongoMultiTenantService mongoMultiTenantService) {
        optimizeQuery(searchConfigService, selectExpression);
        if (hasAggregation(selectExpression)) {
            var documentList = AggregationUtils.generateAggregation(searchConfigService, selectExpression, searchRevision, null, mongoMultiTenantService);
            documentList.add(new Document("$limit", pageSize + 1));
            AggregateIterable<Document> aggregate = collection.aggregate(documentList);

            return CloseableWrapper.<MongoCursor<Document>>builder()
                    .content(aggregate::cursor)
                    .build();
        } else {
            return findFirstPageWithSearch(pageSize, selectExpression, collection, searchRevision, searchConfigService);
        }
    }

    /**
     * Search the next page and get the mongodb cursor
     *
     * @param pageSize         the page size
     * @param searchContext    the context of the search
     * @param selectExpression the select expression
     * @param collection       the mongo collection where to search
     * @param savedLastId      the last id
     * @return the mongodb cursor
     */
    @NotNull
    public static CloseableWrapper<MongoCursor<Document>> searchNextPage(SearchConfigService searchConfigService, int pageSize, SearchContext searchContext, SelectExpression<Bson> selectExpression, MongoCollection<Document> collection, String savedLastId, MongoMultiTenantService mongoMultiTenantService) {
        var searchRevision = searchContext.getRevision();
        if (hasAggregation(selectExpression)) {
            var documentList = AggregationUtils.generateAggregation(searchConfigService, selectExpression, searchRevision, savedLastId, mongoMultiTenantService);
            documentList.add(new Document("$limit", pageSize + 1));
            AggregateIterable<Document> aggregate = collection.aggregate(documentList);

            return CloseableWrapper.<MongoCursor<Document>>builder()
                    .content(aggregate::cursor)
                    .build();
        }
        return findNextPageWithSearch(pageSize, searchContext, selectExpression, collection, savedLastId, searchConfigService);

    }


    /**
     * Search the next page and get the mongodb cursor
     *
     * @param pageSize            the page size
     * @param searchContext       the context of the search
     * @param selectExpression    the select expression
     * @param collection          the mongo collection where to search
     * @param savedLastId         the last id
     * @param searchConfigService
     * @return the mongodb cursor
     */
    @NotNull
    private static CloseableWrapper<MongoCursor<Document>> findNextPageWithSearch(int pageSize, SearchContext searchContext, SelectExpression<Bson> selectExpression, MongoCollection<Document> collection, String savedLastId, SearchConfigService searchConfigService) {
        try {
            var searchRevision = searchContext.getRevision();
            Bson filters = Optional.ofNullable(selectExpression.interpreter())
                    .map(r -> Filters.and(
                            Filters.lt(VALID_FROM_ATTRIBUTE, searchRevision),
                            Filters.gte(VALID_TO_ATTRIBUTE, searchRevision),
                            Filters.gt(ID_ATTRIBUTE, new ObjectId(savedLastId)),
                            r))
                    .orElseGet(() -> Filters.and(
                            Filters.lt(VALID_FROM_ATTRIBUTE, searchRevision),
                            Filters.gte(VALID_TO_ATTRIBUTE, searchRevision),
                            Filters.gt(ID_ATTRIBUTE, new ObjectId(savedLastId))));

            addSinceParam(selectExpression, filters);

            var projection = generateProjection(searchConfigService, selectExpression.getFhirResource(), searchContext.getElements());

            FindIterable<Document> documents = collection
                    .find(filters)
                    .projection(projection)
                    .sort(Sorts.ascending(ID_ATTRIBUTE))
                    .limit(pageSize + 1);

            return CloseableWrapper.<MongoCursor<Document>>builder()
                    .content(documents::iterator)
                    .build();
        } catch (IllegalArgumentException illegalArgumentException) {
            log.info("Bad request", illegalArgumentException);
            throw new BadRequestException("Bad request");
        }


    }

    /**
     * Search the first page and get the mongodb cursor
     *
     * @param pageSize            the page size
     * @param selectExpression    the select expression
     * @param collection          the mongo collection where to search
     * @param searchRevision      the searchRevision
     * @param searchConfigService
     * @return the mongodb cursor
     */
    @NotNull
    private static CloseableWrapper<MongoCursor<Document>> findFirstPageWithSearch(int pageSize,
                                                                                   SelectExpression<Bson> selectExpression,
                                                                                   MongoCollection<Document> collection,
                                                                                   Long searchRevision,
                                                                                   SearchConfigService searchConfigService) {

        var bson = Optional.ofNullable(selectExpression.interpreter())
                .map(r -> Filters.and(Filters.gte(VALID_TO_ATTRIBUTE, searchRevision), r))
                .orElseGet(() -> Filters.gte(VALID_TO_ATTRIBUTE, searchRevision));

        var projection = generateProjection(searchConfigService, selectExpression.getFhirResource(), selectExpression.getElements());

        var documents = collection.find(addSinceParam(selectExpression, bson))
                .projection(projection)
                .sort(Sorts.ascending(ID_ATTRIBUTE))
                .limit(pageSize + 1);

        return CloseableWrapper.<MongoCursor<Document>>builder()
                .content(documents::iterator)
                .build();
    }


    /**
     * Surround a mongodb request with revision condition (_validFrom/_validTo)
     *
     * @param searchRevision the search revision date
     * @param query          the query
     * @return the query with revision filters
     */
    public static Bson wrapQueryWithRevisionDate(long searchRevision, Bson query) {
        if (query != null) {
            return Filters.and(
                    Filters.lt(VALID_FROM_ATTRIBUTE, searchRevision),
                    Filters.gte(VALID_TO_ATTRIBUTE, searchRevision),
                    query);
        } else {
            return Filters.and(
                    Filters.lt(VALID_FROM_ATTRIBUTE, searchRevision),
                    Filters.gte(VALID_TO_ATTRIBUTE, searchRevision)
            );
        }
    }


    /**
     * Extract references to include
     *
     * @param searchConfigService   the search config
     * @param type                  the type of the resource
     * @param selectExpression      the select expression
     * @param includesTypeReference the list to fill with references
     * @param doc                   the document from the db
     */
    public static void extractIncludeReferences(SearchConfigService searchConfigService, String type, SelectExpression<Bson> selectExpression, Map<String, Set<String>> includesTypeReference, Document doc) {
        for (var inclusion : selectExpression.getIncludes()) {
            var config = searchConfigService.getSearchConfigByResourceAndParamName(type, inclusion.getName());
            if (config.isEmpty()) {
                throw new BadConfigurationException("Search not supported on path: " + type + "." + inclusion.getName());
            }
            var as = (List<String>) doc.get(config.get().getIndexName() + "-reference");

            if (as != null) {
                as.stream().filter(Objects::nonNull).forEach(a -> {
                    var partsA = a.split("/");
                    if (!includesTypeReference.containsKey(partsA[0])) {
                        includesTypeReference.put(partsA[0], new HashSet<>());
                    }
                    includesTypeReference.get(partsA[0]).add(a);
                });
            }
        }
    }


    /**
     * Add the "_since" filter if it is set in the select expression
     *
     * @param selectExpression the select expression
     * @param bson             the current bson request
     * @return the new bson
     */
    public static Bson addSinceParam(SelectExpression<Bson> selectExpression, Bson bson) {
        if (selectExpression.getSince() != null) {
            return Filters.and(
                    Filters.gte(LAST_WRITE_DATE, selectExpression.getSince().getTime()),
                    bson
            );
        }
        return bson;
    }


    /**
     * Try to optimize the query.
     * If Fhir resources have _has queries and these resources are joined, we use the join by replacing the has condition by a  parameter
     *
     * @param searchConfigService
     * @param selectExpression
     */
    public static void optimizeQuery(SearchConfigService searchConfigService, SelectExpression<Bson> selectExpression) {
        if (hasAggregation(selectExpression)) {
            var newHasConditions = new ArrayList<HasCondition<Bson>>();
            var joins = searchConfigService.getJoinsByFhirResource(selectExpression.getFhirResource());
            if (joins == null) {
                return;
            }
            optimizeQueryConditions(selectExpression, newHasConditions, joins);
            selectExpression.setHasConditions(newHasConditions);
        }
    }

    private static void optimizeQueryConditions(SelectExpression<Bson> selectExpression, ArrayList<HasCondition<Bson>> newHasConditions, List<JoinPath> joins) {
        for (var cond : selectExpression.getHasConditions()) {
            var found = false;
            for (var join : joins) {
                var joinedResource = cond.getFhirPath().getResource(); // Device
                var joinedPath = cond.getFhirPath().getPath(); // organization
                if (join.getResource().equals(joinedResource) && join.getPath().equals(joinedPath)) {
                    // replace the has condition by a parameter:
                    for (var exp : cond.getExpressions()) {
                        translateExpression(selectExpression.getFhirResource(), joinedResource, joinedPath, exp);
                        selectExpression.getExpression().addExpression(exp);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                newHasConditions.add(cond);
            }
        }
    }

    private static void translateExpression(String mainResource, String joinedResource, String joinedPath, Expression<Bson> exp) {
        if (exp instanceof ContainerExpression) {
            for (var sub : ((ContainerExpression<Bson>) exp).getExpressions()) {
                translateExpression(mainResource, joinedResource, joinedPath, sub);
            }
        } else if (exp instanceof ElementExpression elementExpression) {
            var path = elementExpression.getFhirPath();
            var newPath = FhirSearchPath.builder().resource(mainResource).path("links." + joinedResource + "." + path.getPath()).build();
            elementExpression.setFhirPath(newPath);

        }

    }


    private static boolean hasAggregation(SelectExpression<Bson> selectExpression) {
        return !selectExpression.getHasConditions().isEmpty();

    }

    private static Document generateProjection(SearchConfigService searchConfigService, String fhirResourceName, Set<String> elements) {
        if (elements != null && !elements.isEmpty()) {
            Document document = new Document();

            // INTERN FIELDS OR REQUIRED
            //We get always the intern attributes mongo, indexes and resourceType, id and meta of the resource, because we can need it to use some attributes for correct jsonconstruction (example: t_id for fullUrl)
            Arrays.asList(ID_ATTRIBUTE, "_hash", REVISION_ATTRIBUTE, VALID_FROM_ATTRIBUTE, VALID_TO_ATTRIBUTE, LAST_WRITE_DATE, "fhir.resourceType", "fhir.id", "fhir.meta").forEach(a -> document.append(a, 1));

            // INDEX FIELDS
            //TODO t_status-i for device is not getting in response because we are based on indexes and in this case the status is saved as string for every resource even if index is in token. We have to see this later when we will do RASS-1461
            //We have as well to get the indexes because is used for other purposes (ex: include)
            searchConfigService.getIndexesByFhirResource(fhirResourceName).forEach(i -> document.append(i, 1));
            //t_profile is only applied in save but there is not index created except for HealthcareService
            document.append("t_profile", 1);

            // COMPULSORY AND MODIFIERS FIELDS, fields that are compusory (cardinality min=1) or modifier (?!) in interop profile have to be included
            searchConfigService.getAllByFhirResource(fhirResourceName)
                    .stream().filter(s -> s.getIsCompulsoryOrModifierElementsParam() != null && s.getIsCompulsoryOrModifierElementsParam())
                    .forEach(i -> document.append("fhir.".concat(i.getName()), 1));

            // ELEMENTS FIELDS (from search param _elements)
            elements.forEach(e -> document.append("fhir.".concat(e), 1));

            return document;
        }
        return null;
    }
}
