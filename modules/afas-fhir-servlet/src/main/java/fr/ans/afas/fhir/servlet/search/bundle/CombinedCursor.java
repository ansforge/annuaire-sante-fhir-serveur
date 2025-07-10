/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.domain.FhirBundleBuilder;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A cursor of bundle entry of multiple cursors
 */
public class CombinedCursor implements Iterator<FhirBundleBuilder.BundleEntry> {
    private final Iterator<Iterator<FhirBundleBuilder.BundleEntry>> cursors;
    private Iterator<FhirBundleBuilder.BundleEntry> currentCursor;

    public CombinedCursor(List<Iterator<FhirBundleBuilder.BundleEntry>> cursorList) {
        this.cursors = cursorList.iterator();
        this.currentCursor = cursors.hasNext() ? cursors.next() : null;
    }

    @Override
    public boolean hasNext() {
        while ((currentCursor == null || !currentCursor.hasNext()) && cursors.hasNext()) {
            currentCursor = cursors.next();
        }
        return currentCursor != null && currentCursor.hasNext();
    }

    @Override
    public FhirBundleBuilder.BundleEntry next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return currentCursor.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported.");
    }
}