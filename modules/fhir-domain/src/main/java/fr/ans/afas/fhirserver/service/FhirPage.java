/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;
import java.util.Map;

/**
 * A fhir search result.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Builder
public class FhirPage {
    /**
     * Content of the fhir page
     */
    @NonNull
    List<IBaseResource> page;
    /**
     * Used to store the context of the request like the last id of the query. Can be used for paging or store other metadata.
     */
    @NonNull
    Map<String, Object> context;

    /**
     * If true, there is more elemnts in next pages
     */
    boolean hasNext;
}
