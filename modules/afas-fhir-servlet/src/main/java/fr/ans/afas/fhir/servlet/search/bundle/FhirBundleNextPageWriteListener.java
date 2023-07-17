/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.data.PagingData;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirBundleNextPageWriteListener<T> extends AbstractFhirBundleWriteListener<T> {
    public FhirBundleNextPageWriteListener(FhirStoreService<T> fhirStoreService, NextUrlManager<T> nextUrlManager, ServletOutputStream sos, AsyncContext c, PagingData<T> pagingData, String serverUrl) {
        super(fhirStoreService, serverUrl, sos, c, nextUrlManager, pagingData.getSelectExpression(), fhirStoreService.iterate(
                SearchContext.builder().firstId(pagingData.getLastId()).total(pagingData.getSize().getTotal()).revision(pagingData.getTimestamp()).build(),
                pagingData.getSelectExpression()
        ));
    }
}
