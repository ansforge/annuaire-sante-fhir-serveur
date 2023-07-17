/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbStringExpression;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

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
    SearchConfig searchConfig;

    /**
     * Initialization of the application. Setup indexes
     */
    @PostConstruct
    public void createIndexes() {
        for (var resourceSearchConfig : searchConfig.getResources()) {
            var collection = getCollection(resourceSearchConfig);
            createGenericIndexes(collection);
            for (var config : searchConfig.getAllByFhirResource(resourceSearchConfig)) {
                if (config.isIndex()) {
                    writeIndex(collection, config, "");
                }
            }

            // joins:
            createJoins(resourceSearchConfig, collection);
        }
    }

    private void createJoins(String resourceSearchConfig, MongoCollection<Document> collection) {
        var joins = searchConfig.getJoinsByFhirResource(resourceSearchConfig);
        if (joins != null) {
            for (var j : joins) {
                for (var config : searchConfig.getAllByFhirResource(j.getResource())) {
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

    /**
     * Create generic indexes used in each FHIR resource like metadata, ids...
     *
     * @param col the database collection
     */
    private void createGenericIndexes(MongoCollection<Document> col) {
        col.createIndex(new Document(StorageConstants.INDEX_T_FID, 1));
        col.createIndex(new Document(StorageConstants.INDEX_T_ID, 1));
        col.createIndex(new Document(StorageConstants.INDEX_T_LASTUPDATED, 1));
        col.createIndex(new Document(StorageConstants.INDEX_T_LASTUPDATED_SECOND, 1));
        col.createIndex(new Document(StorageConstants.INDEX_T_LASTUPDATED_MINUTE, 1));
        col.createIndex(new Document(StorageConstants.INDEX_T_LASTUPDATED_DATE, 1));
        col.createIndex(new Document(StorageConstants.INDEX_T_LASTUPDATED_MONTH, 1));
        col.createIndex(new Document(StorageConstants.INDEX_T_LASTUPDATED_YEAR, 1));
    }

    /**
     * Get the mongo db collection for a type
     *
     * @param type the FHIR type
     * @return the mongodb collection found
     */
    MongoCollection<Document> getCollection(String type) {
        var mongoDatabase = mongoClient.getDatabase(dbName);
        return mongoDatabase.getCollection(type);
    }
}
