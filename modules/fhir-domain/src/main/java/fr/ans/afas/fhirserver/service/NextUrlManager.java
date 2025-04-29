/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service;


import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;

import java.util.Optional;

/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface NextUrlManager<T> {


    /**
     * Find a stored paging data
     *
     * @param id the id of the paging data to find
     * @return the found pagindata or empty
     * @throws BadLinkException if the id can't be processed
     */
    Optional<PagingData<T>> find(String id) throws BadLinkException;

    /**
     * Store data
     *
     * @param pagingData
     * @return
     */
    String store(PagingData<T> pagingData);

    /**
     * Clean stored urls older than a date
     *
     * @param timestamp the timestamp in ms
     */
    void cleanOldPagingData(long timestamp);

}
