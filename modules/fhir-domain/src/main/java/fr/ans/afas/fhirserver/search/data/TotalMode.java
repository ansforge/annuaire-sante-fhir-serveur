/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search.data;

/**
 * Calculation mode for the total of bundles
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public enum TotalMode {
    /**
     * Try to calculate the total if it's not too slow
     */
    BEST_EFFORT,
    /**
     * Don't calculate the total
     */
    NONE,
    /**
     * Always calculate the total
     */
    ALWAYS
}
