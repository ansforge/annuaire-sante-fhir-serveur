/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.*;

public class MockedFhirPageIterator implements FhirPageIterator {

    final Iterator<? extends IBaseResource> iterator;
    final long total;
    final String fhirResource;
    final Long revision;
    final FhirContext ctx = FhirContext.forR4();
    final IParser parser = ctx.newJsonParser();
    int lastIdInt = 0;
    String lastId = "";
    int index = 0;
    int count = 0;

    boolean hasNextPage = false;

    public MockedFhirPageIterator(String fhirResource, List<? extends IBaseResource> iterator, SearchContext sc, int count) {
        var first = sc != null ? sc.getFirstId() : "0";
        var firstNumber = Integer.parseInt(first);
        this.iterator = iterator.subList(firstNumber, iterator.size()).iterator();
        this.total = iterator.size();
        this.fhirResource = fhirResource;
        this.revision = System.currentTimeMillis();
        this.count = count;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext() && index < count;
    }

    @Override
    public SearchContext searchContext() {
        return SearchContext.builder()
                .total(total)
                .firstId(lastId)
                .revision(revision)
                .build();
    }

    @Override
    public FhirBundleBuilder.BundleEntry next() {
        if (!iterator.hasNext()) {
            throw new NoSuchElementException();
        }
        var doc = iterator.next();
        lastId = String.valueOf(++lastIdInt);
        this.hasNextPage = iterator.hasNext();
        index++;
        return new FhirBundleBuilder.BundleEntry(fhirResource, "id", parser.encodeResourceToString(doc));
    }

    @Override
    public void close() {

    }

    @Override
    public Map<String, Set<String>> getIncludesTypeReference() {
        return null;
    }

    @Override
    public void clearIncludesTypeReference() {
    }

    @Override
    public Set<String> getRevIncludeIds() {
        return new HashSet<>();
    }

    @Override
    public void clearRevIncludeIds() {
    }

    @Override
    public boolean hasNextPage() {
        return this.hasNextPage;
    }
}
