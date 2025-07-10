/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.exception.ResourceNotFoundException;
import fr.ans.afas.fhirserver.hook.event.*;
import fr.ans.afas.fhirserver.hook.service.HookService;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.IncludeExpression;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPage;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.exception.CantReadFhirResource;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import fr.ans.afas.fhirserver.service.exception.TooManyElementToDeleteException;
import fr.ans.afas.rass.service.impl.DefaultFhirPageIterator;
import fr.ans.afas.rass.service.impl.MongoQueryUtils;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.concurrent.TimeUnit;
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
     * When the request is not supported
     */
    public static final String CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED = "Can't process the request. Resource type not supported. Server knows how to handle: [Device, HealthcareService, Organization, Practitioner, PractitionerRole]";
    public static final float MAX_OLD_REVISION_DELETION_PERCENT = 0.15f;


    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * Object mapper to (de)serialize objects
     */
    final ObjectMapper om = new ObjectMapper();
    /**
     * Service to launch hooks
     */
    final HookService hookService;

    /**
     * Service to access the database
     */
    final MongoMultiTenantService mongoMultiTenantService;
    /**
     * The fhir context
     */
    final FhirContext fhirContext;
    /**
     * The search configuration
     */

    final SearchConfigService searchConfigService;


    @Value("${afas.fhir.max-count-calculation-time:1000}")
    int maxCountCalculationTime;


    public MongoDbFhirService(
            List<FhirBaseResourceSerializer<ResourceAndSubResources>> serializers,
            FhirBaseResourceDeSerializer fhirBaseResourceDeSerializer,
            SearchConfigService searchConfigService,
            FhirContext fhirContext,
            HookService hookService,
            MongoMultiTenantService mongoMultiTenantService
    ) {
        this.searchConfigService = searchConfigService;
        this.fhirContext = fhirContext;
        this.mongoMultiTenantService = mongoMultiTenantService;
        this.hookService = hookService;

        var module = new SimpleModule();
        for (var serializer : serializers) {
            module.addSerializer(serializer.getClassFor(), serializer);
        }
        module.addDeserializer(DomainResource.class, fhirBaseResourceDeSerializer);
        om.registerModule(module);
    }


    @Override
    public List<IIdType> store(Collection<? extends DomainResource> fhirResources, boolean overrideLastUpdated) {
        return this.storeWithDependencies(fhirResources.stream().map(r -> ResourceAndSubResources.builder().resource(r).build()).toList(), overrideLastUpdated, false);
    }

    @Override
    public List<IIdType> store(Collection<? extends DomainResource> fhirResources, boolean overrideLastUpdated, boolean forceUpdate) {
        return this.storeWithDependencies(fhirResources.stream().map(r -> ResourceAndSubResources.builder().resource(r).build()).toList(), overrideLastUpdated, forceUpdate);
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
     * @param forceUpdate         if true force the update
     * @return the list of created/updated ids
     */
    @Override
    public List<IIdType> storeWithDependencies(Collection<ResourceAndSubResources> fhirResources, boolean overrideLastUpdated, boolean forceUpdate) {

        // call hooks:
        hookService.callHook(fhirResources.stream().map(r -> BeforeCreateResourceEvent.builder()
                .resource(r.getResource())
                .build()).collect(Collectors.toList()));

        var jsonWriter = om.writer();

        logger.debug("Store a collection of resources in mongodb.");
        if (fhirResources.isEmpty()) {
            logger.debug("No resource to store.");
            return List.of();
        }
        String resourceType = getResourceTypeByResourceList(fhirResources);
        MongoCollection<Document> collection = getCollection(resourceType);
        var now = new Date().getTime();

        var toSave = prepareResourcesToSave(fhirResources, resourceType);
        var toUpdate = new HashMap<String, IdResourceDocument>();
        var toInsert = new HashMap<String, IdResourceDocument>();

        // find resources that are already present in the database. If they are present, it's an update.
        var updatedDocuments = collection.find(MongoQueryUtils.wrapQueryWithRevisionDate(now, Filters.in(StorageConstants.INDEX_T_ID, toSave.keySet())));
        for (Document oldDoc : updatedDocuments) {
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
        prepareToCreate(overrideLastUpdated, jsonWriter, now, toInsert);

        // update process:
        var toFlagAsNotUpdated = prepareToUpdate(overrideLastUpdated, jsonWriter, now, toUpdate, forceUpdate);


        if (!toInsert.values().isEmpty()) {
            collection.insertMany(toInsert.values().stream().map(IdResourceDocument::getNewDocument).toList());
        } else {
            logger.debug("No item to insert");
        }

        if (!toUpdate.isEmpty()) {
            // update old
            collection.bulkWrite(toUpdate.values().stream().map(e -> new ReplaceOneModel<>(new Document(MongoQueryUtils.ID_ATTRIBUTE, e.getOldDocument().get(MongoQueryUtils.ID_ATTRIBUTE)), e.getOldDocument())).toList());
            // save new
            collection.insertMany(toUpdate.values().stream().map(IdResourceDocument::getNewDocument).toList());
        }


        if (!toFlagAsNotUpdated.entrySet().isEmpty()) {
            collection.bulkWrite(toFlagAsNotUpdated.values().stream().map(e -> new ReplaceOneModel<>(new Document(MongoQueryUtils.ID_ATTRIBUTE, e.getOldDocument().get(MongoQueryUtils.ID_ATTRIBUTE)), e.getOldDocument())).toList());
        }


        // call hooks:
        hookService.callHook(toInsert.values().stream().map(r -> AfterCreateResourceEvent.builder()
                .resource(r.getResource().getResource())
                .build()).collect(Collectors.toList()));
        hookService.callHook(toUpdate.values().stream().map(r -> AfterCreateResourceEvent.builder()
                .resource(r.getResource().getResource())
                .build()).collect(Collectors.toList()));


        logger.debug("{} resources stored.", toSave.size());
        return
                Stream.concat(toInsert.values().stream(), Stream.concat(toFlagAsNotUpdated.values().stream(), toUpdate.values().stream()))
                        .map(a ->
                                new IdType((a.getResource().getResource()).getResourceType().name(), a.getNewDocument().getString(StorageConstants.INDEX_T_ID), a.getNewDocument().getString("_version"))
                        ).collect(Collectors.toList());
    }


    /**
     * Prepare the resources that will be created
     *
     * @param overrideLastUpdated if true, the lastUpdated value will be updated
     * @param jsonWriter          the jsonWriter to write json
     * @param now                 the date of the update
     * @param toInsert            elements to insert
     */
    private void prepareToCreate(boolean overrideLastUpdated, ObjectWriter jsonWriter, long now, HashMap<String, IdResourceDocument> toInsert) {
        for (var idToInsert : toInsert.values()) {
            var resource = idToInsert.getResource().getResource();
            if (overrideLastUpdated) {
                resource.getMeta().setLastUpdated(new Date());
            }
            resource.getMeta().setVersionId("1");
            Document document;
            try {
                document = Document.parse(jsonWriter.writeValueAsString(idToInsert.getResource()));
                document.put(MongoQueryUtils.LAST_WRITE_DATE, now);
                idToInsert.setNewDocument(document);
            } catch (JsonProcessingException e) {
                throw new CantWriteFhirResource("Error converting the FHIR resource to a MongoDb Document.", e);
            }
        }
    }


    /**
     * Prepare the resources that will be updated
     *
     * @param overrideLastUpdated if true, the lastUpdated value will be updated
     * @param jsonWriter          the jsonWriter to write json
     * @param now                 the date of the update
     * @param toUpdate            elements to update
     * @param forceUpdate         if true force the update of elements
     * @return a map tha contains elements not updated (resource id/resource)
     */
    private Map<String, IdResourceDocument> prepareToUpdate(boolean overrideLastUpdated, ObjectWriter jsonWriter, long now, HashMap<String, IdResourceDocument> toUpdate, boolean forceUpdate) {
        var toFlagAsNotUpdated = new HashMap<String, IdResourceDocument>();
        var toUpdateClone = new HashMap<>(toUpdate);
        for (var idToUpdate : toUpdateClone.values()) {
            var resource = idToUpdate.getResource().getResource();
            if (overrideLastUpdated) {
                resource.getMeta().setLastUpdated(new Date());
            }
            resource.getMeta().setVersionId("1");
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


            newDoc.put(MongoQueryUtils.LAST_WRITE_DATE, now);
            oldDoc.put(MongoQueryUtils.LAST_WRITE_DATE, now);

            if (!oldHash.equals(newHash) || forceUpdate) {
                oldDoc.put(MongoQueryUtils.VALID_TO_ATTRIBUTE, now);
                newDoc.put(MongoQueryUtils.VALID_FROM_ATTRIBUTE, now);
                // it's a real update:
                newDoc.put(MongoQueryUtils.REVISION_ATTRIBUTE, oldDoc.getInteger(MongoQueryUtils.REVISION_ATTRIBUTE) + 1);
                var fhir = (Document) newDoc.get("fhir");
                var meta = (Document) fhir.get("meta");
                meta.put("versionId", oldDoc.getInteger(MongoQueryUtils.REVISION_ATTRIBUTE) + 1);
            } else {
                // dont update
                toFlagAsNotUpdated.put(resource.getIdElement().getIdPart(), toUpdate.remove(resource.getIdElement().getIdPart()));
            }
        }
        return toFlagAsNotUpdated;
    }

    /**
     * Pre-treatment of the resources to save.
     * Verify that all resources are in the same type (throw an exception if not)
     * Translate the list into a Map with the id as the key.
     * Set the version at 1.
     *
     * @param fhirResources    fhir resources to store
     * @param fhirResourceType type of fhir resources
     * @return the map containing the resources to save
     */
    private Map<String, IdResourceDocument> prepareResourcesToSave(Collection<ResourceAndSubResources> fhirResources, String fhirResourceType) {
        var toSave = new HashMap<String, IdResourceDocument>();
        for (var r : fhirResources) {
            var resource = r.getResource();
            if (!fhirResourceType.equals(resource.getResourceType().name())) {
                throw new CantWriteFhirResource("The MongoDbFhirService service only support one type of FHIR type for one call of the method MongoDbFhirService#store. Found " + fhirResourceType + " and " + resource.getResourceType().name());
            }
            // by default the version is 1 (we don't use the source version):
            resource.getMeta().setVersionId("1");
            toSave.put(resource.getIdElement().getIdPart(), IdResourceDocument.builder()
                    .id(resource.getIdElement().getIdPart())
                    .resource(r)
                    .build());
        }
        return toSave;
    }

    @Override
    public FhirPage search(SearchContext searchContext, SelectExpression<Bson> selectExpression) {

        // call hooks:
        hookService.callHook(new BeforeSearchEvent(searchContext, selectExpression));

        if (!searchConfigService.getResources().contains(selectExpression.getFhirResource())) {
            throw new ResourceNotFoundException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
        }

        if (searchContext == null) {
            searchContext = SearchContext.builder().build();
        }

        logger.debug("Search fhir resources in mongo with expression {}", selectExpression);

        var collection = getCollection(selectExpression.getFhirResource());
        CloseableWrapper<MongoCursor<Document>> cursorWrapper;
        var savedLastId = searchContext.getFirstId();
        long searchRevision;

        // The search:
        if (savedLastId != null) { // next page
            cursorWrapper = MongoQueryUtils.searchNextPage(this.searchConfigService, selectExpression.getCount(), searchContext, selectExpression, collection, savedLastId, mongoMultiTenantService);
            searchRevision = searchContext.getRevision();
        } else { // first page:
            searchRevision = new Date().getTime();
            cursorWrapper = MongoQueryUtils.searchFirstPage(this.searchConfigService, selectExpression.getCount(), selectExpression, collection, searchRevision, mongoMultiTenantService);
        }


        try (MongoCursor<Document> cursor = cursorWrapper.content()) {

            // The Fetch:
            var lastId = "";
            var hasNext = false;
            var ret = new ArrayList<DomainResource>();
            var ids = new HashSet<String>();
            var includesTypeReference = new HashMap<String, Set<String>>();
            var total = 0;
            while (cursor.hasNext()) {
                var doc = cursor.next();
                total++;
                // detect if it's the last item:
                if (total > selectExpression.getCount()) {
                    hasNext = true;
                    break;
                }

                var domainResource = (DomainResource) om.readerFor(DomainResource.class).readValue(doc.toJson());
                ret.add(domainResource);

                // inclusion:
                MongoQueryUtils.extractIncludeReferences(searchConfigService, selectExpression.getFhirResource(), selectExpression, includesTypeReference, doc);
                // end inclusion

                // revinclude
                ids.add(selectExpression.getFhirResource() + "/" + domainResource.getIdElement().getIdPart());
                // end revinclude

                lastId = ((ObjectId) doc.get(MongoQueryUtils.ID_ATTRIBUTE)).toString();
            }

            // Include:
            addIncludes(searchRevision, ret, includesTypeReference);
            // Revinclude:
            addRevIncludes(searchRevision, selectExpression, ret, ids);


            // call hooks:
            hookService.callHook(new AfterSearchEvent(searchContext, selectExpression));

            return FhirPage.builder().page(ret).hasNext(hasNext).context(SearchContext.builder()
                    .firstId(lastId)
                    .revision(searchRevision)
                    .build()).build();

        } catch (JsonProcessingException e) {
            throw new CantReadFhirResource("Error converting the MongoDb Documents to a FHIR resources during the fetch from Ids.");
        }

    }


    /**
     * Iterate over the result of a search.
     *
     * @param searchContext    the search context
     * @param selectExpression the query expression
     * @return the iterator
     */
    @Override
    public FhirPageIterator iterate(SearchContext searchContext, SelectExpression<Bson> selectExpression) {

        // call hooks:
        hookService.callHook(new BeforeSearchEvent(searchContext, selectExpression));

        if (!searchConfigService.getResources().contains(selectExpression.getFhirResource())) {
            throw new ResourceNotFoundException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
        }

        if (searchContext == null) {
            var count = this.count(selectExpression);
            searchContext = SearchContext.builder()
                    .total(count.getTotal())
                    .build();
        }

        logger.debug("Search fhir resources in mongo with expression {}", selectExpression);

        var collection = getCollection(selectExpression.getFhirResource());
        CloseableWrapper<MongoCursor<Document>> cursorWrapper;
        var savedLastId = searchContext.getFirstId();
        long searchRevision;
        Set<String> elements;

        final var total = new Long[1];
        // The search:
        if (savedLastId != null) { // next page
            cursorWrapper = MongoQueryUtils.searchNextPage(this.searchConfigService, selectExpression.getCount(), searchContext, selectExpression, collection, savedLastId, mongoMultiTenantService);
            searchRevision = searchContext.getRevision();
            total[0] = searchContext.getTotal();
            elements = searchContext.getElements();
        } else { // first page:
            searchRevision = new Date().getTime();
            total[0] = searchContext.getTotal();
            elements = selectExpression.getElements();
            cursorWrapper = MongoQueryUtils.searchFirstPage(this.searchConfigService, selectExpression.getCount(), selectExpression, collection, searchRevision, mongoMultiTenantService);
        }

        //noinspection resource
        MongoCursor<Document> cursor = cursorWrapper.content();

        return new DefaultFhirPageIterator(searchConfigService, cursor, selectExpression, total, searchRevision, elements);
    }


    /**
     * Add revinclude to the response
     *
     * @param searchRevision   the revision
     * @param selectExpression the select expression
     * @param ret              the response
     * @param ids              list of id in the initial response. Ids are FHIR Id with resourceId/id
     */
    private void addRevIncludes(long searchRevision, SelectExpression<Bson> selectExpression, ArrayList<DomainResource> ret, Set<String> ids) {
        var includes = selectExpression.getRevincludes();
        ret.addAll(findRevIncludesV1(searchRevision, ids, includes));
    }

    /**
     * @deprecated (Used for version HAPI ( V1))
     */
    @Deprecated(forRemoval = true)
    @NotNull
    public List<DomainResource> findRevIncludesV1(long searchRevision, Set<String> ids, Set<IncludeExpression<Bson>> includes) {
        var elements = new ArrayList<DomainResource>();
        for (var inclusion : includes) {
            if (inclusion.getType() == null || !searchConfigService.getResources().contains(inclusion.getType())) {
                throw new ResourceNotFoundException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
            }
            var collectionIncluded = getCollection(inclusion.getType());
            var config = searchConfigService.getSearchConfigByPath(FhirSearchPath.builder().resource(inclusion.getType()).path(inclusion.getName()).build());
            if (config.isEmpty()) {
                throw new CantReadFhirResource("Search not supported on path: " + inclusion.getType() + "." + inclusion.getName());
            }
            FindIterable<Document> inclusionResult = collectionIncluded
                    .find(MongoQueryUtils.wrapQueryWithRevisionDate(
                            searchRevision,
                            Filters.in(config.get().getIndexName() + "-reference", ids)));

            try (MongoCursor<Document> cursor = inclusionResult.cursor()) {
                while (cursor.hasNext()) {
                    var doc = cursor.next();
                    elements.add(om.readerFor(DomainResource.class).readValue(doc.toJson()));
                }
            } catch (JsonProcessingException e) {
                throw new CantReadFhirResource("Error converting the MongoDb Document to a FHIR resource when getting _revincludes");
            }
        }
        return elements;
    }

    @Override
    @NotNull
    public List<FhirBundleBuilder.BundleEntry> findRevIncludes(long searchRevision, Set<String> ids, Set<IncludeExpression<Bson>> includes) {
        List<FhirBundleBuilder.BundleEntry> elements = new ArrayList<>();
        for (var inclusion : includes) {
            if (inclusion.getType() == null || !searchConfigService.getResources().contains(inclusion.getType())) {
                throw new ResourceNotFoundException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
            }
            var collectionIncluded = getCollection(inclusion.getType());
            var config = searchConfigService.getSearchConfigByPath(FhirSearchPath.builder().resource(inclusion.getType()).path(inclusion.getName()).build());
            if (config.isEmpty()) {
                throw new CantReadFhirResource("Search not supported on path: " + inclusion.getType() + "." + inclusion.getName());
            }
            FindIterable<Document> inclusionResult = collectionIncluded
                    .find(MongoQueryUtils.wrapQueryWithRevisionDate(
                            searchRevision,
                            Filters.in(config.get().getIndexName() + "-reference", ids)));

            MongoCursor<Document> cursor = inclusionResult.cursor();
            while (cursor.hasNext()) {
                var doc = cursor.next();
                elements.add(new FhirBundleBuilder.BundleEntry(inclusion.getType(), doc.getString("t_id"), ((Document) doc.get("fhir")).toJson()));
            }
        }
        return elements;
    }

    /**
     * Add includes to the response
     *
     * @param searchRevision        the revision
     * @param ret                   the response
     * @param includesTypeReference list of id of elements to includes. Ids are FHIR Id with resourceId/id
     */
    public void addIncludes(long searchRevision, List<DomainResource> ret, Map<String, Set<String>> includesTypeReference) {
        for (var resourceTypeAndValue : includesTypeReference.entrySet()) {
            var collectionForInclude = getCollection(resourceTypeAndValue.getKey());
            FindIterable<Document> inclusionResult = collectionForInclude
                    .find(MongoQueryUtils.wrapQueryWithRevisionDate(
                            searchRevision,
                            Filters.in(StorageConstants.INDEX_T_FID, resourceTypeAndValue.getValue())));
            try (MongoCursor<Document> cursor = inclusionResult.cursor()) {
                while (cursor.hasNext()) {
                    var doc = cursor.next();
                    ret.add(om.readerFor(DomainResource.class).readValue(doc.toJson()));
                }
            } catch (JsonProcessingException e) {
                throw new CantReadFhirResource("Error converting the MongoDb Document to a FHIR resource when getting _includes");
            }
        }
    }


    /**
     * Find all include resources as a cursor
     *
     * @param searchRevision the revision
     * @param resourceType   type of the fhir resource
     * @param ids            list of id of elements to includes. Ids are FHIR Id with resourceId/id
     * @return cursor to the response elements
     */
    public Iterator<FhirBundleBuilder.BundleEntry> findByIds(long searchRevision, String resourceType, Set<String> ids) {
        var collectionForInclude = getCollection(resourceType);
        var cursor = collectionForInclude.find(
                        MongoQueryUtils.wrapQueryWithRevisionDate(
                                searchRevision,
                                Filters.in(StorageConstants.INDEX_T_FID, ids)))
                .cursor();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return cursor.hasNext();
            }

            @Override
            public FhirBundleBuilder.BundleEntry next() {
                if (!cursor.hasNext()) {
                    throw new NoSuchElementException();
                }
                var document = cursor.next();
                return new FhirBundleBuilder.BundleEntry(resourceType, document.getString("t_id"), ((Document) document.get("fhir")).toJson());
            }
        };
    }

    /**
     * Count resources that match a select
     *
     * @param selectExpression the query expression
     * @return the count
     */
    public CountResult count(SelectExpression<Bson> selectExpression) {

        // call hooks:
        hookService.callHook(new BeforeCountEvent(selectExpression));

        if (!searchConfigService.getResources().contains(selectExpression.getFhirResource())) {
            throw new ResourceNotFoundException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
        }

        var collection = getCollection(selectExpression.getFhirResource());
        CountResult cr;
        // calculate the count with the options of the select expression:
        switch (selectExpression.getTotalMode()) {
            case BEST_EFFORT:
                var c = new CountOptions();
                c.maxTime(maxCountCalculationTime, TimeUnit.MILLISECONDS);
                cr = MongoQueryUtils.count(this.searchConfigService, collection, selectExpression, c, mongoMultiTenantService);
                break;
            case ALWAYS:
                cr = MongoQueryUtils.count(this.searchConfigService, collection, selectExpression, new CountOptions(), mongoMultiTenantService);
                break;
            case NONE:
            default:
                cr = CountResult.builder().total(null).build();
                break;
        }

        // call hooks:
        hookService.callHook(new AfterCountEvent(selectExpression, cr));
        return cr;
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
        // call hooks:
        hookService.callHook(new BeforeFindByIdEvent(type, theId));

        if (!searchConfigService.getResources().contains(type)) {
            throw new ResourceNotFoundException(CAN_T_PROCESS_THE_REQUEST_RESOURCE_TYPE_NOT_SUPPORTED);
        }

        var collection = getCollection(type);
        var searchRevision = new Date().getTime();
        var doc = collection.find(
                MongoQueryUtils.wrapQueryWithRevisionDate(searchRevision, Filters.eq(StorageConstants.INDEX_T_ID, theId.getIdPart()))
        ).limit(1).first();
        if (doc == null) {
            return null;
        }

        IBaseResource foundResource;
        try {
            foundResource = om.readerFor(DomainResource.class).readValue(doc.toJson());
        } catch (JsonProcessingException e) {
            throw new CantReadFhirResource("Error reading the resource from the database Type: " + type + ". Id: " + theId);
        }

        // call hooks:
        hookService.callHook(new AfterFindByIdEvent(type, theId));

        return foundResource;
    }

    /**
     * Delete all resources from the database
     */
    @Override
    public void deleteAll() {
        // call hooks:
        hookService.callHook(new BeforeDeleteAllEvent());

        for (var c : searchConfigService.getResources()) {
            getCollection(c).deleteMany(new BsonDocument());
        }

        // call hooks:
        hookService.callHook(new BeforeDeleteAllEvent());
    }

    @Override
    public void deleteElementsNotStoredSince(long timestamp) throws TooManyElementToDeleteException {
        var validTo = Filters.lt(MongoQueryUtils.LAST_WRITE_DATE, timestamp);
        for (var c : searchConfigService.getResources()) {
            var collection = getCollection(c);
            var toDelete = (double) collection.countDocuments(validTo);
            var total = (double) collection.countDocuments();
            var percent = toDelete / total;
            if (total > 0 && percent > MAX_OLD_REVISION_DELETION_PERCENT) {
                throw new TooManyElementToDeleteException("There is more than " + MAX_OLD_REVISION_DELETION_PERCENT + "% elements to delete. This is probably an error. Percent: " + percent);
            }
            getCollection(c).deleteMany(validTo);
        }
    }

    @Override
    public boolean delete(String type, IIdType theId) {
        // call hooks:
        hookService.callHook(BeforeDeleteEvent.builder().resourceId(theId).build());

        var collection = getCollection(type);
        var result = collection.deleteOne(Filters.eq(StorageConstants.INDEX_T_ID, theId.getIdPart()));

        // call hooks:
        hookService.callHook(AfterDeleteEvent.builder().resourceId(theId).build());

        return result.getDeletedCount() == 1;
    }


    @Override
    public boolean businessDelete(String type, IIdType theId) {
        // call hooks:
        hookService.callHook(BeforeDeleteEvent.builder().resourceId(theId).build());

        var collection = getCollection(type);


        var now = new Date().getTime();
        var update = new Document("$set", new Document(MongoQueryUtils.VALID_TO_ATTRIBUTE, now));
        collection.updateMany(Filters.eq(StorageConstants.INDEX_T_ID, theId.getIdPart()), update);

        // call hooks:
        hookService.callHook(AfterDeleteEvent.builder().resourceId(theId).build());

        return true;
    }


    /**
     * Delete elements that have a validTo date before a timestamp (MongoDbFhirService.VALID_TO_ATTRIBUTE / _validTo).
     *
     * @param timestamp the utc timestamp in ms
     */
    public void deleteOldRevisions(long timestamp) {
        var validTo = Filters.lt(MongoQueryUtils.VALID_TO_ATTRIBUTE, timestamp);
        for (var c : searchConfigService.getResources()) {
            getCollection(c).deleteMany(validTo);
        }
    }

    private String getResourceTypeByResourceList(Collection<ResourceAndSubResources> fhirResources) {
        return fhirResources.iterator().next().getResource().getResourceType().name();
    }

    /**
     * Get the mongodb collection by FHIR name
     *
     * @param resourceType the FHIR name of the collection
     * @return the collection
     */
    private MongoCollection<Document> getCollection(String resourceType) {
        return mongoMultiTenantService.getCollection(resourceType);
    }

    /**
     * Set the value of the max duration time used for the count calculation. This value is in ms.
     *
     * @param maxCountCalculationTime time in ms
     */
    public void setMaxCountCalculationTime(int maxCountCalculationTime) {
        this.maxCountCalculationTime = maxCountCalculationTime;
    }

}
