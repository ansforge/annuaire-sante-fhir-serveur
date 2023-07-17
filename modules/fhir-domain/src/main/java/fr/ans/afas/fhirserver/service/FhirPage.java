/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service;

import fr.ans.afas.fhirserver.search.data.SearchContext;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.hl7.fhir.r4.model.DomainResource;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * A fhir search result.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Builder
public class FhirPage implements Iterable<DomainResource> {
    /**
     * Content of the fhir page
     */
    @NonNull
    List<DomainResource> page;
    /**
     * Used to store the context of the request like the last id of the query. Can be used for paging or store other metadata.
     */
    @NonNull
    SearchContext context;

    /**
     * If true, there is more elements in next pages
     */
    boolean hasNext;

    @NotNull
    @Override
    public Iterator<DomainResource> iterator() {
        return page.iterator();
    }

    @Override
    public void forEach(Consumer<? super DomainResource> action) {
        this.page.forEach(action);
    }

    @Override
    public Spliterator<DomainResource> spliterator() {
        return this.page.spliterator();
    }
}
