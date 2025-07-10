/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.service;


/**
 * Service to manage indexes
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface IndexService {

    /**
     * Refresh index of fhir resources that have been written after a date
     *
     * @param fromDateMs the date in ms
     */
    void refreshIndexes(long fromDateMs);

    void refreshIndexesSync(long fromDate);

    /**
     * Tell if the system is indexing fhir resources (if true, the indexing process is running).
     *
     * @return status of the indexing process true == running
     */
    boolean isRunning();
}
