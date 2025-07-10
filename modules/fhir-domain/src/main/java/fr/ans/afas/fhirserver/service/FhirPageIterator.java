/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service;

import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhirserver.search.data.SearchContext;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public interface FhirPageIterator extends Iterator<FhirBundleBuilder.BundleEntry>, AutoCloseable {

    SearchContext searchContext();

    Map<String, Set<String>> getIncludesTypeReference();

    void clearIncludesTypeReference();

    Set<String> getRevIncludeIds();

    void clearRevIncludeIds();

    Set<String> getElements();

    boolean hasNextPage();
}
