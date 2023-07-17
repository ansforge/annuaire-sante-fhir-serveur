/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPage;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.CountResult;
import fr.ans.afas.fhirserver.service.data.PagingData;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.InstantType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Hapi Fhir bundle provider implementation.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AfasBundleProvider<T> implements IBundleProvider {

    /**
     * A service that store paging
     */
    protected final NextUrlManager<T> nextUrlManager;
    /**
     * The id of the next url
     */
    protected final String nextUrlId;
    /**
     * The default size of pages
     */
    final int pageSize;
    /**
     * The current size
     */
    final CountResult size;
    /**
     * The uniq id of the bundle
     */
    final String uuid;
    /**
     * Service to access data
     */
    final FhirStoreService<T> fhirStoreService;
    /**
     * The type of the bundle
     */
    final String type;
    /**
     * The expression that have create the bundle
     */
    final SelectExpression<T> selectExpression;
    /**
     * The current page
     */
    final FhirPage page;
    /**
     * A context used to store data of the request
     */
    SearchContext context;


    /**
     * Construct a bundle provider. This constructor is used for the first page
     *
     * @param fhirStoreService the fhir store service
     * @param selectExpression the Fhir expression of the bundle
     * @param nextUrlManager   the service that store paging information
     */
    public AfasBundleProvider(FhirStoreService<T> fhirStoreService, SelectExpression<T> selectExpression, NextUrlManager<T> nextUrlManager) {
        this.fhirStoreService = fhirStoreService;
        this.nextUrlManager = nextUrlManager;
        this.type = selectExpression.getFhirResource();
        this.uuid = UUID.randomUUID().toString();
        this.selectExpression = selectExpression;
        this.size = this.fhirStoreService.count(this.selectExpression);
        this.pageSize = selectExpression.getCount();
        // query: verify that the search is here (not duplicated)
        page = this.fhirStoreService.search(this.context, this.selectExpression);
        context = page.getContext();

        if (page.isHasNext()) {
            this.nextUrlId = nextUrlManager.store(PagingData.<T>builder()
                    .pageSize(pageSize)
                    .size(size)
                    .type(type)
                    .selectExpression(selectExpression)
                    .uuid(uuid)
                    .timestamp(this.context.getRevision())
                    .lastId(this.context.getFirstId())
                    .build());
        } else {
            this.nextUrlId = null;
        }
    }

    /**
     * Construct a bundle provider. This constructor is used for next pages.
     *
     * @param fhirStoreService the store service
     * @param nextUrlManager   the service that store paging information
     * @param thePageId        id of the current paging
     * @param theLastId        id of the first element to get.
     */
    public AfasBundleProvider(FhirStoreService<T> fhirStoreService, NextUrlManager<T> nextUrlManager, String thePageId, String theLastId) throws BadLinkException {

        this.nextUrlManager = nextUrlManager;
        this.fhirStoreService = fhirStoreService;

        var link = nextUrlManager.find(thePageId);
        if (link.isEmpty()) {
            throw new BadLinkException("Error in the link");
        }
        var pagingData = link.get();

        this.pageSize = pagingData.getPageSize();
        this.size = pagingData.getSize();
        this.uuid = pagingData.getUuid();
        this.type = pagingData.getType();
        this.context = SearchContext.builder()
                .firstId(theLastId)
                .revision(pagingData.getTimestamp())
                .build();
        this.selectExpression = pagingData.getSelectExpression();


        // query:
        page = this.fhirStoreService.search(this.context, this.selectExpression);
        this.context = page.getContext();

        if (page.isHasNext()) {
            this.nextUrlId = thePageId;
        } else {
            this.nextUrlId = null;
        }
    }

    /**
     * Get the  date of the bundle
     *
     * @return the date of the bundle
     */
    @Override
    public IPrimitiveType<Date> getPublished() {
        var cal = new GregorianCalendar();
        cal.setTimeInMillis(this.context.getRevision());
        return new InstantType(cal);
    }

    /**
     * Get resources for a specific range
     *
     * @param i  the from
     * @param to the to
     * @return the list of resources of the range
     */
    @NotNull
    @Override
    public List<IBaseResource> getResources(int i, int to) {
        return page.getPage().stream().collect(Collectors.toList());
    }

    /**
     * Get the serialized request or the id of the bundle
     *
     * @return the serialized request or the id of the bundle
     */
    @Nullable
    @Override
    public String getUuid() {
        return this.uuid;
    }

    /**
     * Get the preferred page size for the bundle
     *
     * @return the preferred size for the bundle
     */
    @Override
    public Integer preferredPageSize() {
        return pageSize;
    }

    /**
     * Get the size of the bundle
     *
     * @return the size of the bundle
     */
    @Nullable
    @Override
    public Integer size() {
        return size.getTotal() == null ? null : size.getTotal().intValue();
    }

    /**
     * Get the current page id.
     * Tell hapi to use pageId. Hapi will not use this, but we have to return something.
     *
     * @return a string that is not null
     */
    @Override
    public String getCurrentPageId() {
        return "-";
    }

    /**
     * Get the id of the next element to get
     *
     * @return the id of the next element to get
     */
    @Override
    public String getNextPageId() {
        if (this.page.isHasNext()) {
            return this.context.getFirstId() + '_' + this.nextUrlId;
        } else {
            return null;
        }
    }

}
