/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * A base of a date range expression
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public abstract class DateRangeExpression<T> implements ElementExpression<T> {

    /**
     * The fhir path where to search
     */
    protected final FhirSearchPath fhirPath;
    /**
     * The date
     */
    protected final Date date;
    /**
     * The precision
     */
    protected final TemporalPrecisionEnum precision;

    /**
     * How to query (gt, lt, eq...)
     */
    protected final ParamPrefixEnum prefix;

    /**
     * Constructor
     *
     * @param fhirPath  The fhir path where to find
     * @param date      The date to search
     * @param precision The precision
     * @param prefix    How to search (gt, lt...)
     */
    protected DateRangeExpression(@NotNull FhirSearchPath fhirPath, @NotNull Date date, @NotNull TemporalPrecisionEnum precision, @NotNull ParamPrefixEnum prefix) {
        this.fhirPath = fhirPath;
        this.date = date;
        this.precision = precision;
        this.prefix = prefix;
    }

}
