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

import java.util.Iterator;
import java.util.List;

public class MockedFhirPageIterator implements FhirPageIterator {

    final Iterator<? extends IBaseResource> iterator;
    final long total;
    final String fhirResource;
    final Long revision;
    int lastIdInt = 0;
    String lastId = "";
    FhirContext ctx = FhirContext.forR4();
    IParser parser = ctx.newJsonParser();

    public MockedFhirPageIterator(String fhirResource, List<? extends IBaseResource> iterator, SearchContext sc) {
        var first = sc != null ? sc.getFirstId() : "0";
        var firstNumber = Integer.parseInt(first);
        this.iterator = iterator.subList(firstNumber, iterator.size()).iterator();
        this.total = iterator.size();
        this.fhirResource = fhirResource;
        this.revision = System.currentTimeMillis();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
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
        var doc = iterator.next();
        lastId = String.valueOf(++lastIdInt);
        return new FhirBundleBuilder.BundleEntry(fhirResource, "id", parser.encodeResourceToString(doc));
    }
}
