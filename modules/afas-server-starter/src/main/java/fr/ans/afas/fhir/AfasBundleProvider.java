/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhir;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirPage;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.InstantType;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Hapi Fhir bundle provider implementation.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AfasBundleProvider<T> implements IBundleProvider {

    /**
     * The default size of pages
     */
    final int pageSize;
    /**
     * The current size
     */
    final int size;
    /**
     * The uniq id of the bundle
     */
    final String uuid;
    /**
     * The date of the bundle
     */
    final IPrimitiveType<Date> published;
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
    Map<String, Object> context = new HashMap<>();
    /**
     * The expression serializer
     */
    ExpressionSerializer<T> expressionSerializer;
    /**
     * The url encrypter
     */
    SerializeUrlEncrypter serializeUrlEncrypter;

    /**
     * Construct a bundle provider
     *
     * @param fhirStoreService      the fhir store service
     * @param selectExpression      the Fhir expression of the bundle
     * @param serializeUrlEncrypter the serializer for urls
     */
    public AfasBundleProvider(FhirStoreService<T> fhirStoreService, ExpressionSerializer<T> expressionSerializer, SelectExpression<T> selectExpression, SerializeUrlEncrypter serializeUrlEncrypter) {
        this.fhirStoreService = fhirStoreService;
        this.type = selectExpression.getFhirResource();
        this.uuid = UUID.randomUUID().toString();
        published = InstantType.now();
        this.selectExpression = selectExpression;
        this.size = (int) this.fhirStoreService.count(this.type, this.selectExpression);
        this.pageSize = selectExpression.getCount();
        this.expressionSerializer = expressionSerializer;
        this.serializeUrlEncrypter = serializeUrlEncrypter;
        // query: verify that the search is here (not duplicated)
        page = this.fhirStoreService.search(this.type, this.pageSize, this.context, this.selectExpression);
        context = page.getContext();
    }

    /**
     * Construct a bundle provider
     *
     * @param pageSize              the page size
     * @param size                  the size
     * @param uuid                  the uuid
     * @param published             the publication date of the bundle
     * @param fhirStoreService      the store service
     * @param type                  the type of the resource
     * @param selectExpression      the select expression
     * @param context               the context or the provider
     * @param expressionSerializer  the expression serializer
     * @param serializeUrlEncrypter the serializer for urls
     */
    public AfasBundleProvider(int pageSize, int size, String uuid, IPrimitiveType<Date> published, FhirStoreService<T> fhirStoreService,
                              String type, SelectExpression<T> selectExpression, Map<String, Object> context,
                              ExpressionSerializer<T> expressionSerializer, SerializeUrlEncrypter serializeUrlEncrypter) {
        this.pageSize = pageSize;
        this.size = size;
        this.uuid = uuid;
        this.published = published;
        this.fhirStoreService = fhirStoreService;
        this.type = type;
        this.selectExpression = selectExpression;
        this.expressionSerializer = expressionSerializer;
        this.serializeUrlEncrypter = serializeUrlEncrypter;
        this.context = context;
        // query:
        page = this.fhirStoreService.search(this.type, this.pageSize, this.context, this.selectExpression);
        this.context = page.getContext();
    }


    /**
     * Get the  date of the bundle
     *
     * @return the date of the bundle
     */
    @Override
    public IPrimitiveType<Date> getPublished() {
        return published;
    }

    /**
     * Get resources for a specific range
     *
     * @param i  the from
     * @param to the to
     * @return the list of resources of the range
     */
    @Override
    public List<IBaseResource> getResources(int i, int to) {

        return page.getPage();
    }

    /**
     * Get the uuid of the bundle
     *
     * @return the uuid of the bundle
     */
    @Nullable
    @Override
    public String getUuid() {
        return uuid;
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
        return size;
    }


    /**
     * Serialize the bundle provider
     *
     * @return the serialized bundle provider
     */
    public String serialize() {
        String contextAsString = context.keySet().stream()
                .map(key -> key + "=" + context.get(key))
                .collect(Collectors.joining(",", "", ""));
        var serialized = new StringBuilder();
        serialized.append(pageSize);
        serialized.append("_");
        serialized.append(size);
        serialized.append("_");
        serialized.append(uuid);
        serialized.append("_");
        serialized.append(published.getValue().getTime());
        serialized.append("_");
        serialized.append(type);
        serialized.append("_");
        serialized.append(contextAsString);
        serialized.append("_");
        serialized.append(this.selectExpression.serialize(expressionSerializer));

        return serializeUrlEncrypter.encrypt(serialized.toString());
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
     * Get the page id for hapi. We store in the page id the serialized query.
     *
     * @return the serialized query.
     */
    @Override
    public String getNextPageId() {
        if (this.page.isHasNext()) {
            return this.serialize();
        }
        // we have reached the end of the paging
        return null;
    }


}
