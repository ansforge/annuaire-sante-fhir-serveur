/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service.impl;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.exception.BadParametersException;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class to generate aggregations
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AggregationUtils {

    public static final String MONGO_MATCH = "$match";
    public static final String MONGO_SORT = "$sort";
    public static final String MONGO_LOOKUP = "$lookup";

    protected static final Logger logger = LoggerFactory.getLogger(AggregationUtils.class);

    private AggregationUtils() {
    }

    public static List<Document> generateAggregation(SearchConfigService config, SelectExpression<Bson> selectExpression, Long searchRevision, String lastId, MongoMultiTenantService mongoMultiTenantService) {
        var aggrs = new ArrayList<Document>();
        // the main query:
        var interpreted = selectExpression.interpreter(new ExpressionContext(null));
        // the revision date:
        var wrappedQuery = MongoQueryUtils.addSinceParam(selectExpression, MongoQueryUtils.wrapQueryWithRevisionDate(searchRevision, interpreted));

        // sort and paging:
        aggrs.add(new Document(MONGO_SORT, new Document(MongoQueryUtils.ID_ATTRIBUTE, 1)));
        if (StringUtils.hasLength(lastId)) {
            aggrs.add(new Document(MONGO_MATCH, Filters.gt(MongoQueryUtils.ID_ATTRIBUTE, new ObjectId(lastId))));
        }

        aggrs.add(new Document(MONGO_MATCH, wrappedQuery));

        // _has conditions will be converted into lookup:
        var allHas = mergeAggregations(selectExpression);
        for (var entries : allHas.entrySet()) {

            var fhirPath = entries.getKey();
            var sc = config.getSearchConfigByPath(fhirPath).orElseThrow(() -> new BadConfigurationException("Search not supported on path: " + fhirPath));

            if (!"reference".equals(sc.getSearchType())) {
                throw new BadParametersException("_has is only supported on references");
            }

            var aggregation = new ArrayList<Document>();
            final String subObjName;
            final String collectionName;
            final String localField;
            final String foreignField;

            if (fhirPath.getChain() == null) {
                //_has on a reference
                subObjName = "sub_r_" + fhirPath.getResource() + "_" + sc.getName();
                collectionName = mongoMultiTenantService.getCollectionName(fhirPath.getResource());
                localField = "t_id";
                foreignField = sc.getIndexName() + "-id";
            } else {
                //chain reference
                subObjName = "sub_r_" + sc.getReferenceType() + "_" + fhirPath.getResource();
                collectionName = mongoMultiTenantService.getCollectionName(sc.getReferenceType());
                foreignField = "t_id";
                localField = sc.getIndexName() + "-id";
            }

            aggregation.add(new Document(MONGO_LOOKUP,
                    new Document("from", collectionName)
                            .append("localField", localField)
                            .append("foreignField", foreignField)
                            .append("as", subObjName)
            ));
            for (var ex : entries.getValue()) {
                aggregation.add(new Document(MONGO_MATCH, ex.interpreter(new ExpressionContext(subObjName))));
            }

            aggrs.addAll(aggregation);
        }

        return aggrs;
    }

    /**
     * Utility method to merge multiple aggregations on the same relation
     */
    public static Map<FhirSearchPath, List<Expression<Bson>>> mergeAggregations(SelectExpression<Bson> selectExpression) {
        var aggregations = new HashMap<FhirSearchPath, List<Expression<Bson>>>();
        var allHas = selectExpression.getHasConditions();
        for (var has : allHas) {
            var resource = has.getFhirPath();
            aggregations.computeIfAbsent(resource, k -> new ArrayList<>());
            aggregations.get(resource).addAll(has.getExpressions());
        }
        return aggregations;
    }

}
