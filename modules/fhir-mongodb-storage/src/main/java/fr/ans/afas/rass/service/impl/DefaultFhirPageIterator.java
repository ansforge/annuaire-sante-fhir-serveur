/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service.impl;

import com.mongodb.client.MongoCursor;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;

public class DefaultFhirPageIterator implements FhirPageIterator {

    private final MongoCursor<Document> cursor;
    private final SelectExpression<Bson> selectExpression;
    private final Long[] total;
    private final long searchRevision;
    private final Set<String> elements;
    boolean hasNextPage;
    /**
     * The search config
     */
    SearchConfigService searchConfigService;
    /**
     * current index
     */
    private int index;

    private final Map<String, Set<String>> includesTypeReference;
    private final Set<String> revIncludeIds;
    private String lastId;

    public DefaultFhirPageIterator(SearchConfigService searchConfigService, MongoCursor<Document> cursor, SelectExpression<Bson> selectExpression, Long[] total, long searchRevision, Set<String> elements) {
        this.searchConfigService = searchConfigService;
        this.cursor = cursor;
        this.selectExpression = selectExpression;
        this.total = total;
        this.searchRevision = searchRevision;
        this.elements = elements;
        index = 0;
        includesTypeReference = new HashMap<>();
        revIncludeIds = new LinkedHashSet<>();
        lastId = "";
    }

    @Override
    public boolean hasNext() {
        return cursor.hasNext() && index < selectExpression.getCount();
    }


    @Override
    public SearchContext searchContext() {
        return SearchContext.builder()
                .total(total[0])
                .firstId(lastId)
                .revision(searchRevision)
                .build();
    }

    @Override
    public FhirBundleBuilder.BundleEntry next() {
        index++;
        var doc = cursor.next();
        // inclusion:
        MongoQueryUtils.extractIncludeReferences(searchConfigService, selectExpression.getFhirResource(), selectExpression, includesTypeReference, doc);
        // end inclusion
        // revinclude
        if (!selectExpression.getRevincludes().isEmpty()) {
            revIncludeIds.add(doc.getString("t_fid"));
        }
        // end revinclude
        lastId = ((ObjectId) doc.get(MongoQueryUtils.ID_ATTRIBUTE)).toString();

        hasNextPage = cursor.hasNext();

        // Add tag if _elements search parameter used
        if(elements != null && !elements.isEmpty()) {
            addMetaTag(doc);
        }

        return new FhirBundleBuilder.BundleEntry(selectExpression.getFhirResource(), doc.getString("t_id"), ((Document) doc.get("fhir")).toJson());
    }

    @Override
    public void close() {
        cursor.close();
    }

    public Map<String, Set<String>> getIncludesTypeReference() {
        return includesTypeReference;
    }

    public Set<String> getRevIncludeIds() {
        return revIncludeIds;
    }

    public void clearIncludesTypeReference() {
        includesTypeReference.clear();
    }

    public void clearRevIncludeIds() {
        revIncludeIds.clear();
    }

    @Override
    public Set<String> getElements() {
        return elements;
    }

    @Override
    public boolean hasNextPage() {
        return hasNextPage;
    }

    private void addMetaTag(Document doc) {
        Document meta = (Document) ((Document) doc.get("fhir")).get("meta");
        Document tag = new Document();
        tag.append("system", "http://terminology.hl7.org/CodeSystem/v3-ObservationValue");
        tag.append("code", "SUBSETTED");
        tag.append("display", "Resource encoded in summary mode");
        meta.append("tag", List.of(tag));
    }
}