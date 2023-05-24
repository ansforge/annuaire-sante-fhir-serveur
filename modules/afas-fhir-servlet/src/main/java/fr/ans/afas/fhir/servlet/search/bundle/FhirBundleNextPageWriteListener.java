/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.PagingData;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirBundleNextPageWriteListener<T> extends FhirBundleWriteListener<T> {
    public FhirBundleNextPageWriteListener(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory, SearchConfig searchConfig, NextUrlManager<T> nextUrlManager, ServletOutputStream sos, AsyncContext c, PagingData<T> pagingData, String serverUrl) {
        super(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, sos, c, pagingData.getSelectExpression(), serverUrl);
        fhirPageIterator = fhirStoreService.iterate(
                SearchContext.builder().firstId(pagingData.getLastId()).total(pagingData.getSize().getTotal()).revision(pagingData.getTimestamp()).build(),
                pagingData.getSelectExpression()
        );
    }
}
