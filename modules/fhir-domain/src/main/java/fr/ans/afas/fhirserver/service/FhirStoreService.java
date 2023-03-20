/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service;


import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.exception.TooManyElementToDeleteException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DomainResource;

import java.util.Collection;
import java.util.List;

/**
 * A service that can persist a search FHIR resources.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public interface FhirStoreService<T> {

    /**
     * Store a collection of fhir entities
     *
     * @param fhirResource        fhir entities to store
     * @param overrideLastUpdated if true, will set the last updated date to the server date
     * @return the list of created/updated ids
     */
    List<IIdType> store(Collection<? extends DomainResource> fhirResource, boolean overrideLastUpdated);

    /**
     * Search fhir resources.
     *
     * @param searchContext    the search context
     * @param selectExpression the query expression
     * @return the found resources
     */
    FhirPage search(SearchContext searchContext, SelectExpression<T> selectExpression);

    /**
     * Count resources for a specific search
     *
     * @param type             the type of resource to search
     * @param selectExpression the query expression
     * @return the resource count
     */
    CountResult count(String type, SelectExpression<T> selectExpression);

    /**
     * Find the resource by id
     *
     * @param type  the FHIR resource type
     * @param theId the id of the resource to get
     * @return the resource
     */
    IBaseResource findById(String type, IIdType theId);

    /**
     * Delete all resources from the database
     */
    void deleteAll();

    /**
     * Delete elements not stored since a date. If the last storage date of an element is older than the param, the method delete the element
     *
     * @param timestamp the date in ms to compare
     */
    void deleteElementsNotStoredSince(long timestamp) throws TooManyElementToDeleteException;

    /**
     * Delete an element
     *
     * @param type  the type of the element
     * @param theId the id of the element
     * @return true if an element was deleted
     */
    boolean delete(String type, IIdType theId);
}
