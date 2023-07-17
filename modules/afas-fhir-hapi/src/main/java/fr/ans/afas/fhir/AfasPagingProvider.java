/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhir;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.fhirserver.search.expression.serialization.SerializeUrlEncrypter;
import fr.ans.afas.fhirserver.service.FhirStoreService;
import fr.ans.afas.fhirserver.service.NextUrlManager;
import fr.ans.afas.fhirserver.service.exception.BadLinkException;
import fr.ans.afas.fhirserver.service.exception.BadRequestException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;


/**
 * The paging provider. This paging provider use url to store the request and context params.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class AfasPagingProvider<T> implements IPagingProvider {

    /**
     * The expression factory
     */
    @Inject
    ExpressionFactory<T> expressionFactory;

    /**
     * The store service
     */
    @Inject
    FhirStoreService<T> fhirStoreService;

    /**
     * The expression serializer
     */
    @Inject
    ExpressionSerializer<T> expressionSerializer;

    /**
     * The url encrypter
     */
    @Inject
    SerializeUrlEncrypter serializeUrlEncrypter;

    /**
     * A service that store paging
     */
    @Inject
    NextUrlManager<T> nextUrlManager;

    @Override
    public int getDefaultPageSize() {
        return 50;
    }

    @Override
    public int getMaximumPageSize() {
        return 2000;
    }

    @Override
    public boolean canStoreSearchResults() {
        return true;
    }


    @Override
    public IBundleProvider retrieveResultList(@Nullable RequestDetails theRequestDetails, @NotNull String theSearchIdEncrypted) {
        return null;
    }

    @Override
    public IBundleProvider retrieveResultList(@Nullable RequestDetails theRequestDetails, @NotNull String theSearchIdEncoded, String thePageId) {
        try {
            var parts = thePageId.split("_", 2);
            if (parts.length != 2) {
                throw new InvalidRequestException("Your request is not valid.");
            }
            return new AfasBundleProvider<>(fhirStoreService, nextUrlManager, parts[1], parts[0]);
        } catch (BadLinkException | BadRequestException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    @Override
    public String storeResultList(@Nullable RequestDetails theRequestDetails, IBundleProvider theList) {
        return null;
    }
}
