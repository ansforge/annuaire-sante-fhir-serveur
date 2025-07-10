/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhir.servlet.search.bundle;

import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.fhirserver.service.data.PagingData;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirBundleNextPageWriteListener<T> extends AbstractFhirBundleWriteListener<T> {
    public FhirBundleNextPageWriteListener(FhirServerContext<T> fhirServerContext, ServletOutputStream sos, AsyncContext c, PagingData<T> pagingData, AfasConfiguration afasConfiguration) {
        super(fhirServerContext, afasConfiguration, sos, c, pagingData.getSelectExpression(), fhirServerContext.getFhirStoreService().iterate(
                SearchContext.builder().firstId(pagingData.getLastId()).total(pagingData.getSize().getTotal()).revision(pagingData.getTimestamp())
                        .elements(pagingData.getElements()).build(),
                pagingData.getSelectExpression()
        ));
    }
}
