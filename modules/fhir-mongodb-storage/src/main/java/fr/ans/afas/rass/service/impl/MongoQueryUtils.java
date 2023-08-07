/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
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
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.JoinPath;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import fr.ans.afas.rass.service.CloseableWrapper;
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
     * @param searchConfig     the search config
     * @param collection       the collection
     * @param selectExpression the select expression
     * @param c                the count option
     * @return the result (the count)
     */
    public static CountResult count(SearchConfig searchConfig, MongoCollection<Document> collection, SelectExpression<Bson> selectExpression, CountOptions c) {
        var searchRevision = new Date().getTime();
        try {
            optimizeQuery(searchConfig, selectExpression);
            if (hasAggregation(selectExpression)) {
                var agg = AggregationUtils.generateAggregation(searchConfig, selectExpression, searchRevision, null);
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
     * @param searchConfig     the search config
     * @param pageSize         the page size
     * @param selectExpression the select expression
     * @param collection       the mongo collection where to search
     * @param searchRevision   the searchRevision
     * @return the mongodb cursor
     */
    @NotNull
    public static CloseableWrapper<MongoCursor<Document>> searchFirstPage(SearchConfig searchConfig, int pageSize, SelectExpression<Bson> selectExpression, MongoCollection<Document> collection, Long searchRevision) {
        optimizeQuery(searchConfig, selectExpression);
        if (hasAggregation(selectExpression)) {
            var documentList = AggregationUtils.generateAggregation(searchConfig, selectExpression, searchRevision, null);
            documentList.add(new Document("$limit", pageSize + 1));
            AggregateIterable<Document> aggregate = collection.aggregate(documentList);

            return CloseableWrapper.<MongoCursor<Document>>builder()
                    .content(aggregate::cursor)
                    .build();
        } else {
            return findFirstPageWithSearch(pageSize, selectExpression, collection, searchRevision);
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
    public static CloseableWrapper<MongoCursor<Document>> searchNextPage(SearchConfig searchConfig, int pageSize, SearchContext searchContext, SelectExpression<Bson> selectExpression, MongoCollection<Document> collection, String savedLastId) {
        var searchRevision = searchContext.getRevision();
        if (hasAggregation(selectExpression)) {
            var documentList = AggregationUtils.generateAggregation(searchConfig, selectExpression, searchRevision, savedLastId);
            documentList.add(new Document("$limit", pageSize + 1));
            AggregateIterable<Document> aggregate = collection.aggregate(documentList);

            return CloseableWrapper.<MongoCursor<Document>>builder()
                    .content(aggregate::cursor)
                    .build();
        }
        return findNextPageWithSearch(pageSize, searchContext, selectExpression, collection, savedLastId);

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
    private static CloseableWrapper<MongoCursor<Document>> findNextPageWithSearch(int pageSize, SearchContext searchContext, SelectExpression<Bson> selectExpression, MongoCollection<Document> collection, String savedLastId) {
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

            FindIterable<Document> documents = collection
                    .find(filters)
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
     * @param pageSize         the page size
     * @param selectExpression the select expression
     * @param collection       the mongo collection where to search
     * @param searchRevision   the searchRevision
     * @return the mongodb cursor
     */
    @NotNull
    private static CloseableWrapper<MongoCursor<Document>> findFirstPageWithSearch(int pageSize,
                                                                                   SelectExpression<Bson> selectExpression,
                                                                                   MongoCollection<Document> collection,
                                                                                   Long searchRevision) {

        var bson = Optional.ofNullable(selectExpression.interpreter())
                .map(r -> Filters.and(Filters.gte(VALID_TO_ATTRIBUTE, searchRevision), r))
                .orElseGet(() -> Filters.gte(VALID_TO_ATTRIBUTE, searchRevision));

        var documents = collection.find(addSinceParam(selectExpression, bson))
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
     * @param searchConfig          the search config
     * @param type                  the type of the resource
     * @param selectExpression      the select expression
     * @param includesTypeReference the list to fill with references
     * @param doc                   the document from the db
     */
    public static void extractIncludeReferences(SearchConfig searchConfig, String type, SelectExpression<Bson> selectExpression, Map<String, Set<String>> includesTypeReference, Document doc) {
        for (var inclusion : selectExpression.getIncludes()) {
            var config = searchConfig.getSearchConfigByResourceAndParamName(type, inclusion.getName());
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
     * @param searchConfig
     * @param selectExpression
     */
    public static void optimizeQuery(SearchConfig searchConfig, SelectExpression<Bson> selectExpression) {
        if (hasAggregation(selectExpression)) {
            var newHasConditions = new ArrayList<HasCondition<Bson>>();
            var joins = searchConfig.getJoinsByFhirResource(selectExpression.getFhirResource());
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
        } else if (exp instanceof ElementExpression) {
            var elementExpression = (ElementExpression) exp;
            var path = elementExpression.getFhirPath();
            var newPath = FhirSearchPath.builder().resource(mainResource).path("links." + joinedResource + "." + path.getPath()).build();
            elementExpression.setFhirPath(newPath);

        }

    }


    private static boolean hasAggregation(SelectExpression<Bson> selectExpression) {
        return !selectExpression.getHasConditions().isEmpty();

    }

}
