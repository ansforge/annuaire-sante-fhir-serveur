/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service;


import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;

import java.util.Optional;

/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface NextUrlManager {


    /**
     * Find a stored paging data
     *
     * @param id the id of the paging data to find
     * @return the found pagindata or empty
     * @throws BadLinkException if the id can't be processed
     */
    Optional<PagingData> find(String id) throws BadLinkException;

    /**
     * Store data
     *
     * @param pagingData
     * @return
     */
    String store(PagingData pagingData);

    /**
     * Clean stored urls older than a date
     *
     * @param timestamp the timestamp in ms
     */
    void cleanOldPagingData(long timestamp);

}
