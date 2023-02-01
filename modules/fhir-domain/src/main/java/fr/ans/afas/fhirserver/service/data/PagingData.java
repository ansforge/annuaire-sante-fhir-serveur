/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service.data;

import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;


/**
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class PagingData {

    /**
     * The type of the bundle
     */
    final String type;
    /**
     * The expression that have create the bundle
     */
    final SelectExpression selectExpression;
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
     * The last id of the paging
     */
    final String lastId;

    /**
     * the timestamp of the call to the first page
     */
    final long timestamp;

    @Builder
    public PagingData(@NonNull String type, @NonNull SelectExpression selectExpression, int pageSize, @NonNull CountResult size, @NonNull String uuid,
                      @NonNull String lastId, long timestamp) {
        this.type = type;
        this.selectExpression = selectExpression;
        this.pageSize = pageSize;
        this.size = size;
        this.uuid = uuid;
        this.lastId = lastId;
        this.timestamp = timestamp;
    }
}
