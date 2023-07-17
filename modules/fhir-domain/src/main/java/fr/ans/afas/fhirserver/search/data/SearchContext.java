/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.data;


import lombok.Builder;
import lombok.Getter;

/**
 * Hold information on the search
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
@Builder
public class SearchContext {

    /**
     * The revision of the search
     */
    private final long revision;

    /**
     * id of the first element to get (during the paging process, this field is used to know where the page have to start).
     */
    private final String firstId;

    /***
     * The number of element found for this query
     */
    private final Long total;
}
