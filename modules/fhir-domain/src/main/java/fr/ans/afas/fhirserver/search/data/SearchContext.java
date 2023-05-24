/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.data;


import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Hold information on the search
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class SearchContext {

    /**
     * The revision of the search
     */
    long revision;

    /**
     * id of the first element to get (during the paging process, this field is used to know where the page have to start).
     */
    String firstId;

    /***
     * The number of element found for this query
     */
    Long total;

    @Builder
    public SearchContext(long revision, String firstId, @Nullable Long total) {
        this.revision = revision;
        this.firstId = firstId;
        this.total = total;
    }
}
