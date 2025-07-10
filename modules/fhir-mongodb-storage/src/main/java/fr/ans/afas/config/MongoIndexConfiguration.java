/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbStringExpression;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import fr.ans.afas.utils.TenantUtil;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.util.Set;

/**
 * Service that create Mongodb index on startup
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoIndexConfiguration {

    /**
     * The database name
     */
    @Value("${afas.mongodb.dbname}")
    String dbName;

    /**
     * The mongodb client
     */
    @Inject
    MongoClient mongoClient;

    /**
     * The search configuration
     */
    @Inject
    SearchConfigService searchConfigService;

    @Inject
    ServerSearchConfig serverSearchConfig;

    @Inject
    MongoMultiTenantService mongoMultiTenantService;

    /**
     * Initialization of the application. Setup indexes
     */
    @PostConstruct
    public void createIndexes() {
        serverSearchConfig.getConfigs().forEach((k, v) -> {
            TenantUtil.setCurrentTenant(k);
            for (var resourceSearchConfig : searchConfigService.getResources()) {
                Set<String> indexes = searchConfigService.getIndexesByFhirResource(resourceSearchConfig);
                var collection = mongoMultiTenantService.getCollection(resourceSearchConfig);
                // indexes:
                indexes.forEach(index -> collection.createIndex(new Document(index, 1)));
                // joins:
                createJoins(resourceSearchConfig, collection);
            }
        });
    }

    private void createJoins(String resourceSearchConfig, MongoCollection<Document> collection) {
        var joins = searchConfigService.getJoinsByFhirResource(resourceSearchConfig);
        if (joins != null) {
            for (var j : joins) {
                for (var config : searchConfigService.getAllByFhirResource(j.getResource())) {
                    if (config.isIndexInSubRequest()) {
                        writeIndex(collection, config, "links." + j.getResource() + ".");
                    }
                }
            }
        }
    }

    /**
     * Write an index for a search config. This method create specifics indexes for each type.
     *
     * @param col    the database collection
     * @param config the configuration
     * @param prefix the prefix of the index
     * @see SearchParamConfig
     */
    //TODO Utiliser pour les joins seulement, voir si on peut utiliser directement searchConfigService.getIndexesByFhirResource(resourceSearchConfig) lorsqu'on abordera le _has
    private void writeIndex(MongoCollection<Document> col, SearchParamConfig config, String prefix) {
        switch (config.getSearchType()) {
            case StorageConstants.INDEX_TYPE_TOKEN:
                col.createIndex(new Document(prefix + config.getIndexName() + StorageConstants.SYSTEM_SUFFIX, 1));
                col.createIndex(new Document(prefix + config.getIndexName() + StorageConstants.VALUE_SUFFIX, 1));
                col.createIndex(new Document(prefix + config.getIndexName() + StorageConstants.SYSVAL_SUFFIX, 1));
                break;
            case StorageConstants.INDEX_TYPE_REFERENCE:
                col.createIndex(new Document(prefix + config.getIndexName() + StorageConstants.REFERENCE_SUFFIX, 1));
                col.createIndex(new Document(prefix + config.getIndexName() + StorageConstants.TYPE_SUFFIX, 1));
                col.createIndex(new Document(prefix + config.getIndexName() + StorageConstants.ID_SUFFIX, 1));
                break;
            case StorageConstants.INDEX_TYPE_STRING:
            default:
                col.createIndex(new Document(prefix + config.getIndexName(), 1));
                col.createIndex(new Document(prefix + config.getIndexName() + MongoDbStringExpression.INSENSITIVE_SUFFIX, 1));
                break;
        }
    }
}
