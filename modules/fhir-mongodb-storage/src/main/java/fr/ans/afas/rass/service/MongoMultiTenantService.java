/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import fr.ans.afas.fhirserver.search.config.domain.Tenant;
import fr.ans.afas.fhirserver.service.DefaultMultiTenantService;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


public class MongoMultiTenantService {

    @Value("${afas.mongodb.dbname}")
    String defaultDbName;

    @Autowired
    private MongoClient mongoClient;

    @Autowired
    private DefaultMultiTenantService multiTenantService;

    public MongoCollection<Document> getCollection(String resourceType) {
        var tenant = multiTenantService.getTenant();

        var mongoDatabase = getDatabase(tenant);
        return mongoDatabase.getCollection(getCollectionName(tenant, resourceType));
    }

    private MongoDatabase getDatabase(Tenant tenant) {
        if (StringUtils.isNotBlank(tenant.getDbname())) {
            return mongoClient.getDatabase(tenant.getDbname());
        } else {
            return mongoClient.getDatabase(defaultDbName);
        }
    }


    /**
     * Get the name of the collection based on the tenant and the fhir resource type.
     *
     * @param tenant       the tenant
     * @param resourceType the fhir resource type (Device, Patient...)
     * @return The collection name that store the resource.
     */
    public String getCollectionName(Tenant tenant, String resourceType) {
        return resourceType + tenant.getSuffixCollection();
    }

    /**
     * Get the name of the collection based on the current tenant with the fhir resource type.
     *
     * @param resourceType the fhir resource type (Device, Patient...)
     * @return The collection name that store the resource.
     */
    public String getCollectionName(String resourceType) {
        var tenant = multiTenantService.getTenant();
        return getCollectionName(tenant, resourceType);
    }


}
