/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service.impl;

import com.mongodb.client.MongoCursor;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DefaultFhirPageIterator implements FhirPageIterator {

    private final MongoCursor<Document> cursor;
    private final SelectExpression<Bson> selectExpression;
    private final Long[] total;
    private final long searchRevision;
    boolean hasNextPage;
    /**
     * The search config
     */
    SearchConfig searchConfig;
    /**
     * current index
     */
    private int index;

    private Map<String, Set<String>> includesTypeReference;
    private Set<String> revIncludeIds;
    private String lastId;

    public DefaultFhirPageIterator(SearchConfig searchConfig, MongoCursor<Document> cursor, SelectExpression<Bson> selectExpression, Long[] total, long searchRevision) {
        this.searchConfig = searchConfig;
        this.cursor = cursor;
        this.selectExpression = selectExpression;
        this.total = total;
        this.searchRevision = searchRevision;
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
        MongoQueryUtils.extractIncludeReferences(searchConfig, selectExpression.getFhirResource(), selectExpression, includesTypeReference, doc);
        // end inclusion
        // revinclude
        if (!selectExpression.getRevincludes().isEmpty()) {
            revIncludeIds.add(doc.getString("t_fid"));
        }
        // end revinclude
        lastId = ((ObjectId) doc.get(MongoQueryUtils.ID_ATTRIBUTE)).toString();

        hasNextPage = cursor.hasNext();

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
    public boolean hasNextPage() {
        return hasNextPage;
    }
}