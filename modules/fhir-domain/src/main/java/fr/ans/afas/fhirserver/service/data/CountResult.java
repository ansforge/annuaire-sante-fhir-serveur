/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.service.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Store the result of a count
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@AllArgsConstructor
@Builder
@Getter
public class CountResult {

    @Nullable
    Long total;

    @Override
    public String toString() {
        return "CountResult{" +
                "total=" + total +
                '}';
    }
}
