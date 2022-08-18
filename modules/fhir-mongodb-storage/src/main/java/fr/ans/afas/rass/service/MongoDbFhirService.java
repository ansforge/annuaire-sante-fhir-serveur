/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.Sorts;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.exception.BadRequestException;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPage;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.exception.CantReadFhirResource;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import fr.ans.afas.rass.service.json.FhirBaseResourceDeSerializer;
import fr.ans.afas.rass.service.json.FhirBaseResourceSerializer;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MongoDb implementation of the {@link FhirStoreService} that store fhir data into a mongodb database.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbFhirService implements FhirStoreService<Bson> {

    /**
     * attribute that store the last write date. The date is updated even if the object is not updated
     */
    public static final String LAST_WRITE_DATE = "_lastWriteDate";
    /**
     * Search attribute for the valid from date
     */
    private static final String VALID_FROM_ATTRIBUTE = "_validFrom";
    /**
     * Search attribute for the valid to date
     */
    private static final String VALID_TO_ATTRIBUTE = "_validTo";
    /**
     * Search attribute for the id
     */
    private static final String ID_ATTRIBUTE = "_id";
    /**
     * Search attribute for the revision
     */
    private static final String REVISION_ATTRIBUTE = "_revision";
    /**
     * The revision in the search context
     */
    private static final String SEARCH_CONTEXT_REVISION = "searchRevision";
    /**
     * When the request is not supported
     */
    public static final String CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED = "Can't process the request. Resource type not supported";
    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * Object mapper to (de)serialize objects
     */
    final ObjectMapper om = new ObjectMapper();
    /**
     * The name of the mongodb database
     */
    @Value("${afas.mongodb.dbname}")
    String dbName;


    @Value("${afas.fhir.max-include-size:5000}")
    int maxIncludePageSize;

    /**
     * The fhir context
     */
    FhirContext fhirContext;
    /**
     * The mongodb client
     */
    MongoClient mongoClient;
    /**
     * The search configuration
     */

    SearchConfig searchConfig;

    /**
     * All fhir collections
     */
    Set<String> fhirCollections = new HashSet<>();


    @Autowired
    public MongoDbFhirService(
            List<FhirBaseResourceSerializer> serializers,
            FhirBaseResourceDeSerializer fhirBaseResourceDeSerializer,
            MongoClient mongoClient,
            SearchConfig searchConfig,
            FhirContext fhirContext,
            int maxIncludePageSize,
            String dbName
    ) {
        this.mongoClient = mongoClient;
        this.searchConfig = searchConfig;
        this.fhirContext = fhirContext;
        this.maxIncludePageSize = maxIncludePageSize;
        this.dbName = dbName;

        var module = new SimpleModule();
        for (var serializer : serializers) {
            module.addSerializer(serializer.getClassFor(), serializer);
        }
        module.addDeserializer(DomainResource.class, fhirBaseResourceDeSerializer);
        om.registerModule(module);
        fhirCollections.addAll(searchConfig.getResources());
    }

    /**
     * Suppress metadata: version, lastUpdated from the source (values will be set by this server)
     * Detect if it's an insert or an update by searching against ids of fhir resources
     * If it's an insert, we add the resource
     * If it's an update, we change revisions information of the old item (_validTo) and we set revision information of the new node (_validFrom).
     * When we update a value, we store 2 nodes.
     *
     * @param fhirResources       resources to store
     * @param overrideLastUpdated if true, will set the last updated date to the server date
     * @return the list of created/updated ids
     */
    @Override
    public List<IIdType> store(Collection<? extends DomainResource> fhirResources, boolean overrideLastUpdated) {
        var jsonWriter = om.writer();

        logger.debug("Store a collection of resources in mongodb.");
        if (fhirResources.isEmpty()) {
            logger.debug("No resource to store.");
            return List.of();
        }
        var fhirResourceType = fhirResources.iterator().next().getResourceType().name();
        var mongoDatabase = mongoClient.getDatabase(dbName);
        MongoCollection<Document> collection = mongoDatabase.getCollection(fhirResourceType);
        var now = new Date().getTime();

        var toSave = prepareResourcesToSave(fhirResources, fhirResourceType);
        var toUpdate = new HashMap<String, IdResourceDocument>();
        var toInsert = new HashMap<String, IdResourceDocument>();
        var toFlagAsNotUpdated = new HashMap<String, IdResourceDocument>();

        // find resources that are already present in the database. If they are present, it's an update.
        var updatedDocuments = collection.find(wrapQueryWithRevisionDate(now, Filters.in(StorageConstants.INDEX_T_ID, toSave.keySet())));
        var i = updatedDocuments.iterator();
        while (i.hasNext()) {
            var oldDoc = i.next();
            var id = oldDoc.getString(StorageConstants.INDEX_T_ID);
            var doc = IdResourceDocument.builder()
                    .id(id)
                    .oldDocument(oldDoc)
                    .resource(toSave.get(id).getResource())
                    .build();
            toUpdate.put(id, doc);
        }
        for (var toSaveIdKeyVal : toSave.entrySet()) {
            if (!toUpdate.containsKey(toSaveIdKeyVal.getKey())) {
                toInsert.put(toSaveIdKeyVal.getKey(), toSaveIdKeyVal.getValue());
            }
        }

        // create process:
        for (var idToInsert : toInsert.values()) {
            if (overrideLastUpdated) {
                idToInsert.getResource().getMeta().setLastUpdated(new Date());
            }
            idToInsert.getResource().getMeta().setVersionId("1");
            Document document;
            try {
                document = Document.parse(jsonWriter.writeValueAsString(idToInsert.getResource()));
                document.put(LAST_WRITE_DATE, now);
                idToInsert.setNewDocument(document);
            } catch (JsonProcessingException e) {
                throw new CantWriteFhirResource("Error converting the FHIR resource to a MongoDb Document.", e);
            }
        }

        // update process:
        var toUpdateClone = new HashMap<>(toUpdate);
        for (var idToUpdate : toUpdateClone.values()) {
            if (overrideLastUpdated) {
                idToUpdate.getResource().getMeta().setLastUpdated(new Date());
            }
            idToUpdate.getResource().getMeta().setVersionId("1");
            Document document;
            try {
                document = Document.parse(jsonWriter.writeValueAsString(idToUpdate.getResource()));
                idToUpdate.setNewDocument(document);
            } catch (JsonProcessingException e) {
                throw new CantWriteFhirResource("Error converting the FHIR resource to a MongoDb Document.", e);
            }
            var oldDoc = idToUpdate.getOldDocument();
            // it's an update, verify that the object really change and if it changes, update it:
            var newDoc = idToUpdate.getNewDocument();
            var oldHash = oldDoc.getInteger("_hash");
            var newHash = newDoc.getInteger("_hash");


            newDoc.put(LAST_WRITE_DATE, now);
            oldDoc.put(LAST_WRITE_DATE, now);

            if (!oldHash.equals(newHash)) {
                oldDoc.put(VALID_TO_ATTRIBUTE, now);
                newDoc.put(VALID_FROM_ATTRIBUTE, now);
                // it's a real update:
                newDoc.put(REVISION_ATTRIBUTE, oldDoc.getInteger(REVISION_ATTRIBUTE) + 1);
                var fhir = (Document) newDoc.get("fhir");
                var meta = (Document) fhir.get("meta");
                meta.put("versionId", oldDoc.getInteger(REVISION_ATTRIBUTE) + 1);
            } else {
                // dont update
                toFlagAsNotUpdated.put(idToUpdate.getResource().getIdElement().getIdPart(), toUpdate.remove(idToUpdate.getResource().getIdElement().getIdPart()));
            }
        }


        if (!toInsert.values().isEmpty()) {
            collection.insertMany(toInsert.values().stream().map(IdResourceDocument::getNewDocument).collect(Collectors.toList()));
        } else {
            logger.debug("No item to insert");
        }

        if (!toUpdate.isEmpty()) {
            // update old
            collection.bulkWrite(toUpdate.values().stream().map(e -> new ReplaceOneModel<>(new Document(ID_ATTRIBUTE, e.getOldDocument().get(ID_ATTRIBUTE)), e.getOldDocument())).collect(Collectors.toList()));
            // save new
            collection.insertMany(toUpdate.values().stream().map(IdResourceDocument::getNewDocument).collect(Collectors.toList()));
        }


        if (!toFlagAsNotUpdated.entrySet().isEmpty()) {
            collection.bulkWrite(toFlagAsNotUpdated.values().stream().map(e -> new ReplaceOneModel<>(new Document(ID_ATTRIBUTE, e.getOldDocument().get(ID_ATTRIBUTE)), e.getOldDocument())).collect(Collectors.toList()));
        }

        logger.debug("{} resources stored.", toSave.size());
        return Stream.concat(toInsert.values().stream(), toUpdate.values().stream()).map(a ->
                new IdType(((DomainResource) a.getResource()).getResourceType().name(), a.getNewDocument().getString(StorageConstants.INDEX_T_ID), a.getNewDocument().getString("_version"))
        ).collect(Collectors.toList());
    }

    /**
     * Pre-treatment of the resources to save.
     * Verify that all resources are in the same type (throw an exception if not)
     * Translate the list into a Map with the id as the key.
     * Set the version at 1.
     *
     * @param fhirResources
     * @param fhirResourceType
     * @return the map containing the resources to save
     */
    private Map<String, IdResourceDocument> prepareResourcesToSave(Collection<? extends DomainResource> fhirResources, String fhirResourceType) {
        var toSave = new HashMap<String, IdResourceDocument>();
        for (var resource : fhirResources) {
            if (!fhirResourceType.equals(resource.getResourceType().name())) {
                throw new CantWriteFhirResource("The MongoDbFhirService service only support one type of FHIR type for one call of the method MongoDbFhirService#store. Found " + fhirResourceType + " and " + resource.getResourceType().name());
            }
            // by default the version is 1 (we don't use the source version):
            resource.getMeta().setVersionId("1");
            toSave.put(resource.getIdElement().getIdPart(), IdResourceDocument.builder()
                    .id(resource.getIdElement().getIdPart())
                    .resource(resource)
                    .build());
        }
        return toSave;
    }

    @Override
    public FhirPage search(String type, int pageSize, Map<String, Object> searchContext, SelectExpression<Bson> selectExpression) {

        if (type == null || !fhirCollections.contains(type)) {
            throw new BadRequestException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
        }

        logger.debug("Search fhir resources in mongo with expression {}", selectExpression);

        var collection = getCollection(type);
        MongoCursor<Document> cursor = null;
        var savedLastId = (String) searchContext.get("lastId");
        Long searchRevision = null;

        // The search:
        if (savedLastId != null) { // next page
            searchRevision = Long.parseLong((String) searchContext.get(SEARCH_CONTEXT_REVISION));
            Bson filters = null;
            var expression = selectExpression.interpreter();

            if (expression == null) {
                filters = Filters.and(
                        Filters.lt(VALID_FROM_ATTRIBUTE, searchRevision),
                        Filters.gte(VALID_TO_ATTRIBUTE, searchRevision),
                        Filters.gt(ID_ATTRIBUTE, new ObjectId(savedLastId))
                );
            } else {
                filters = Filters.and(
                        Filters.lt(VALID_FROM_ATTRIBUTE, searchRevision),
                        Filters.gte(VALID_TO_ATTRIBUTE, searchRevision),
                        Filters.gt(ID_ATTRIBUTE, new ObjectId(savedLastId)),
                        expression
                );
            }
            cursor = collection.find(
                            filters
                    )
                    .sort(Sorts.ascending(ID_ATTRIBUTE))
                    .limit(pageSize + 1).iterator();
            searchRevision = Long.parseLong((String) searchContext.get(SEARCH_CONTEXT_REVISION));
        } else { // first page:
            searchRevision = new Date().getTime();
            var request = selectExpression.interpreter();
            if (request != null) {
                cursor = collection.find(
                                Filters.and(
                                        Filters.gte(VALID_TO_ATTRIBUTE, searchRevision),
                                        request
                                )
                        )
                        .sort(Sorts.ascending(ID_ATTRIBUTE))
                        .limit(pageSize + 1).iterator();
            } else {
                cursor = collection.find(Filters.gte(VALID_TO_ATTRIBUTE, searchRevision))
                        .sort(Sorts.ascending(ID_ATTRIBUTE))
                        .limit(pageSize + 1).iterator();
            }


        }


        // The Fetch:
        var lastId = "";
        var hasNext = false;
        var ret = new ArrayList<IBaseResource>();
        var ids = new HashSet<String>();
        var includesTypeReference = new HashMap<String, Set<String>>();
        var total = 0;
        while (cursor.hasNext()) {
            var doc = cursor.next();
            total++;
            // detect if it's the last item:
            if (total > pageSize) {
                hasNext = true;
                break;
            }
            try {
                DomainResource domainResource = om.readerFor(DomainResource.class).readValue(doc.toJson());
                ret.add(domainResource);

                // inclusion:
                extractIncludeReferences(type, selectExpression, includesTypeReference, doc);
                // end inclusion

                // revinclude
                ids.add(type + "/" + domainResource.getIdElement().getIdPart());
                // end revinclude

            } catch (JsonProcessingException e) {
                throw new CantReadFhirResource("Error converting the MongoDb Documents to a FHIR resources during the fetch from Ids.");
            }
            lastId = ((ObjectId) doc.get(ID_ATTRIBUTE)).toString();
        }

        // Include:
        addIncludes(searchRevision, ret, includesTypeReference);
        // Revinclude:
        addRevIncludes(searchRevision, selectExpression, ret, ids);

        return FhirPage.builder().page(ret).hasNext(hasNext).context(Map.of("lastId", lastId, SEARCH_CONTEXT_REVISION, "" + searchRevision)).build();
    }

    /**
     * Extract references to include
     *
     * @param type                  the type of the resource
     * @param selectExpression      the select expression
     * @param includesTypeReference the list to fill with references
     * @param doc                   the document from the db
     */
    private void extractIncludeReferences(String type, SelectExpression<Bson> selectExpression, HashMap<String, Set<String>> includesTypeReference, Document doc) {
        for (var inclusion : selectExpression.getIncludes()) {
            var config = searchConfig.getSearchConfigByResourceAndParamName(type, inclusion.getName());
            if (config.isEmpty()) {
                throw new BadConfigurationException("Search not supported on path: " + type + "." + inclusion.getName());
            }
            var as = (List<String>) doc.get(config.get().getIndexName() + "-reference");

            if(as!=null) {
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
     * Add revinclude to the response
     *
     * @param searchRevision   the revision
     * @param selectExpression the select expression
     * @param ret              the response
     * @param ids              list of id in the initial response. Ids are FHIR Id with resourceId/id
     */
    private void addRevIncludes(long searchRevision, SelectExpression<Bson> selectExpression, ArrayList<IBaseResource> ret, Set<String> ids) {
        for (var inclusion : selectExpression.getRevincludes()) {

            if (inclusion.getType() == null || !fhirCollections.contains(inclusion.getType())) {
                throw new BadRequestException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
            }

            var collectionIncluded = getCollection(inclusion.getType());
            var config = searchConfig.getSearchConfigByPath(FhirSearchPath.builder().resource(inclusion.getType()).path(inclusion.getName()).build());
            if (config.isEmpty()) {
                throw new CantReadFhirResource("Search not supported on path: " + inclusion.getType() + "." + inclusion.getName());
            }
            var inclusionResult = collectionIncluded.find(
                            wrapQueryWithRevisionDate(
                                    searchRevision,
                                    Filters.in(config.get().getIndexName() + "-reference", ids)
                            ))
                    .limit(maxIncludePageSize)
                    .cursor();
            while (inclusionResult.hasNext()) {
                var doc = inclusionResult.next();
                try {
                    ret.add(om.readerFor(DomainResource.class).readValue(doc.toJson()));
                } catch (JsonProcessingException e) {
                    throw new CantReadFhirResource("Error converting the MongoDb Document to a FHIR resource when getting _revincludes");
                }
            }
        }
    }

    /**
     * Add includes to the response
     *
     * @param searchRevision        the revision
     * @param ret                   the response
     * @param includesTypeReference he list of id of elements to includes. Ids are FHIR Id with resourceId/id
     */
    private void addIncludes(long searchRevision, ArrayList<IBaseResource> ret, Map<String, Set<String>> includesTypeReference) {
        for (var resourceTypeAndValue : includesTypeReference.entrySet()) {
            var collectionForInclude = getCollection(resourceTypeAndValue.getKey());
            var inclusionResult = collectionForInclude.find(
                            wrapQueryWithRevisionDate(
                                    searchRevision,
                                    Filters.in(StorageConstants.INDEX_T_FID, resourceTypeAndValue.getValue())))
                    .limit(maxIncludePageSize)
                    .cursor();
            while (inclusionResult.hasNext()) {
                var doc = inclusionResult.next();
                try {
                    ret.add(om.readerFor(DomainResource.class).readValue(doc.toJson()));
                } catch (JsonProcessingException e) {
                    throw new CantReadFhirResource("Error converting the MongoDb Document to a FHIR resource when getting _includes");
                }
            }
        }
    }

    /**
     * Count resources that match a select
     *
     * @param type             the type of resource to search
     * @param selectExpression the query expression
     * @return the count
     */
    public long count(String type, SelectExpression<Bson> selectExpression) {

        if (!fhirCollections.contains(type)) {
            throw new BadRequestException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
        }

        var collection = getCollection(type);
        var query = selectExpression.interpreter();
        var searchRevision = new Date().getTime();
        var wrappedQuery = wrapQueryWithRevisionDate(searchRevision, query);
        return collection.countDocuments(wrappedQuery);
    }

    /**
     * Find a resource by id
     *
     * @param type  the FHIR resource type
     * @param theId the id of the resource to get
     * @return the found resource
     */
    @Override
    public IBaseResource findById(String type, IIdType theId) {

        if (!fhirCollections.contains(type)) {
            throw new BadRequestException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
        }

        var collection = getCollection(type);
        var searchRevision = new Date().getTime();
        var doc = collection.find(
                wrapQueryWithRevisionDate(searchRevision, Filters.eq(StorageConstants.INDEX_T_ID, theId.getIdPart()))
        ).limit(1).first();
        if (doc == null) {
            return null;
        }
        try {
            return om.readerFor(DomainResource.class).readValue(doc.toJson());
        } catch (JsonProcessingException e) {
            throw new CantReadFhirResource("Error reading the resource from the database Type: " + type + ". Id: " + theId);
        }
    }

    /**
     * Delete all resources from the database
     */
    @Override
    public void deleteAll() {
        for (var c : fhirCollections) {
            getCollection(c).deleteMany(new BsonDocument());
        }
    }

    @Override
    public void deleteElementsNotStoredSince(long timestamp) {
        var validTo = Filters.lt(LAST_WRITE_DATE, timestamp);
        for (var c : fhirCollections) {
            getCollection(c).deleteMany(validTo);
        }
    }

    /**
     * Delete elements that have a validTo date before a timestamp (MongoDbFhirService.VALID_TO_ATTRIBUTE / _validTo).
     *
     * @param timestamp the utc timestamp in ms
     */
    public void deleteOldRevisions(long timestamp) {
        var validTo = Filters.lt(VALID_TO_ATTRIBUTE, timestamp);
        for (var c : fhirCollections) {
            getCollection(c).deleteMany(validTo);
        }
    }


    /**
     * Get the mongodb collection by FHIR name
     *
     * @param type the FHIR name of the collection
     * @return the collection
     */
    MongoCollection<Document> getCollection(String type) {
        var mongoDatabase = mongoClient.getDatabase(dbName);
        return mongoDatabase.getCollection(type);
    }


    /**
     * Surround a mongodb request with revision condition (_validFrom/_validTo)
     *
     * @param searchRevision the search revision date
     * @param query          the query
     * @return the query with revision filters
     */
    Bson wrapQueryWithRevisionDate(long searchRevision, Bson query) {
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
}
