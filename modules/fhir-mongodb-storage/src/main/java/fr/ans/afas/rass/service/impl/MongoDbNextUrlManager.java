/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;

/**
 * Manage next urls. Store and retrieve them from the database (or the url)
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class MongoDbNextUrlManager implements NextUrlManager<Bson> {


    public static final String MONGO_COLLECTION_NAME = "NextPages";

    public static final String FIRST_CHAR_IN_DB = "d";
    public static final String FIRST_CHAR_IN_URL = "u";


    private static final String CREATED_AT_ATTRIBUTE = "ca";

    /**
     * The db name
     */
    protected final String dbName;

    protected final MongoMultiTenantService mongoMultiTenantService;
    /**
     * The expression serializer
     */
    final ExpressionSerializer<Bson> expressionSerializer;
    /**
     * The url encrypter
     */
    final SerializeUrlEncrypter serializeUrlEncrypter;
    protected int maxNextUrlLength;


    /**
     * Create the service
     *
     * @param mongoMultiTenantService the mongoMultiTenantService
     * @param maxNextUrlLength        the max size of the next url (paging data).
     * @param expressionSerializer    The expression serializer
     * @param serializeUrlEncrypter   The url encrypter
     */
    public MongoDbNextUrlManager(MongoMultiTenantService mongoMultiTenantService, int maxNextUrlLength, ExpressionSerializer<Bson> expressionSerializer, SerializeUrlEncrypter serializeUrlEncrypter, String dbName) {
        this.mongoMultiTenantService = mongoMultiTenantService;
        this.maxNextUrlLength = maxNextUrlLength;
        this.expressionSerializer = expressionSerializer;
        this.serializeUrlEncrypter = serializeUrlEncrypter;
        this.dbName = dbName;
    }

    @Override
    public Optional<PagingData<Bson>> find(String id) throws BadLinkException {

        String theSearchId;

        var typeId = id.substring(0, 1);
        var realId = id.substring(1);

        if (FIRST_CHAR_IN_URL.equals(typeId)) {
            theSearchId = serializeUrlEncrypter.decrypt(realId);
        } else if (FIRST_CHAR_IN_DB.equals(typeId)) {
            theSearchId = this.findFromDb(realId);
            if (theSearchId == null) {
                throw new BadLinkException("The url can't be processed. Maybe it is too old or corrupted.");
            }
        } else {
            throw new BadLinkException("The url can't be processed. Maybe it is too old or corrupted.");
        }

        var parts = theSearchId.split("_", 8);

        Long total = Long.parseLong(parts[1]);
        if (total < 0) {
            total = null;
        }

        var pageSize = Integer.parseInt(parts[0]);
        var size = CountResult.builder().total(total).build();
        var timestamp = Long.parseLong(parts[2]);
        var type = parts[3];

        var lastId = parts[4];
        var uuid = parts[5];
        var elements = !parts[6].isEmpty() ? Arrays.stream(parts[6].substring(1, parts[6].length() - 1).split(","))
                .map(String::trim).collect(Collectors.toSet()) : new HashSet<String>();
        var exp = parts[7];

        var selectExpression = (SelectExpression<Bson>) expressionSerializer.deserialize(exp);

        return Optional.of(PagingData.<Bson>builder()
                .uuid(uuid)
                .pageSize(pageSize)
                .size(size)
                .selectExpression(selectExpression)
                .lastId(lastId)
                .timestamp(timestamp)
                .type(type)
                .elements(elements)
                .build());
    }

    @Override
    public String store(PagingData<Bson> pagingData) {
        var serialized = pagingData.getPageSize() +
                "_" +
                (pagingData.getSize().getTotal() != null ? pagingData.getSize().getTotal().longValue() : "-1") +
                "_" +
                pagingData.getTimestamp() +
                "_" +
                pagingData.getType() +
                "_" +
                pagingData.getLastId() +
                "_" +
                pagingData.getUuid() +
                "_" +
                //This can be true only for V2, because V1 (Hapi) is not using this field elements
                (pagingData.getElements() != null && !pagingData.getElements().isEmpty() ? pagingData.getElements() : "") +
                "_" +
                pagingData.getSelectExpression().serialize(expressionSerializer);

        if (serialized.length() > maxNextUrlLength) {
            this.storeInDb(pagingData.getUuid(), pagingData.getTimestamp(), serialized);
            return FIRST_CHAR_IN_DB + pagingData.getUuid();
        } else {
            return FIRST_CHAR_IN_URL + serializeUrlEncrypter.encrypt(serialized);
        }
    }

    /**
     * delete paging data older than a date
     *
     * @param timestamp the utc timestamp in ms
     */
    @Override
    public void cleanOldPagingData(long timestamp) {
        var validTo = Filters.lt(CREATED_AT_ATTRIBUTE, timestamp);
        var r = getCollection().deleteMany(validTo);
        r.toString();
    }


    /**
     * Store next url data
     *
     * @param id                   the id
     * @param timestamp            the date of the stored nexturl
     * @param pagingDataSerialized the value (as a serialized string)
     */
    protected void storeInDb(String id, long timestamp, String pagingDataSerialized) {
        var col = getCollection();
        col.insertOne(new Document()
                .append("_id", new ObjectId())
                .append("id", id)
                .append(CREATED_AT_ATTRIBUTE, timestamp)
                .append("value", pagingDataSerialized));
    }


    /**
     * Find and get the value of a next url persisted in database
     *
     * @param id the id of the "nexturl" element
     * @return the value
     */
    protected String findFromDb(String id) {
        var col = getCollection();
        var doc = col.find(eq("id", id)).first();
        if (doc == null) {
            return null;
        } else {
            return doc.getString("value");
        }
    }


    /**
     * Get the mongodb collection that store next pages
     *
     * @return the collection
     */
    protected MongoCollection<Document> getCollection() {
        return mongoMultiTenantService.getCollection(MONGO_COLLECTION_NAME);
    }


    public void setMaxNextUrlLength(int maxNextUrlLength) {
        this.maxNextUrlLength = maxNextUrlLength;
    }
}
