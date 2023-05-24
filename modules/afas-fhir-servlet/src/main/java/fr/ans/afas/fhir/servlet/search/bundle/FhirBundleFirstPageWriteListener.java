/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirBundleFirstPageWriteListener<T> extends FhirBundleWriteListener<T> {


    public FhirBundleFirstPageWriteListener(FhirStoreService<T> fhirStoreService, ExpressionFactory<T> expressionFactory, SearchConfig searchConfig, NextUrlManager<T> nextUrlManager, ServletOutputStream sos, AsyncContext c, SelectExpression<T> selectExpression, String serverUrl) {
        super(fhirStoreService, expressionFactory, searchConfig, nextUrlManager, sos, c, selectExpression, serverUrl);
        fhirPageIterator = fhirStoreService.iterate(null, selectExpression);
    }

}
