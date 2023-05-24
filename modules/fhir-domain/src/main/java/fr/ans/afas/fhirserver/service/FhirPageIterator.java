/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service;

import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhirserver.search.data.SearchContext;

import java.util.Iterator;

public interface FhirPageIterator extends Iterator<FhirBundleBuilder.BundleEntry> {

    SearchContext searchContext();
}
