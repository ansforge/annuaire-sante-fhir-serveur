/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.mdbexpression.domain.fhir;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.exception.BadPrecisionException;
import fr.ans.afas.fhirserver.search.expression.DateRangeExpression;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.utils.FhirDateUtils;
import lombok.Getter;
import org.bson.conversions.Bson;

import javax.validation.constraints.NotNull;
import java.util.Date;

import static ca.uhn.fhir.rest.param.ParamPrefixEnum.EQUAL;

/**
 * Implementation of the date range expression for Mongodb
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class MongoDbDateRangeExpression extends DateRangeExpression<Bson> {

    /***
     * The factor for approximation of the date
     */
    public static final double APPROX_FACTOR = 1.1;

    /**
     * Suffix for the database field that store the value in millisecond
     */
    public static final String SUFFIX_MILLI = "";
    /**
     * Suffix for the database field that store the value in second
     */
    public static final String SUFFIX_SECOND = "-second";
    /**
     * Suffix for the database field that store the value in minute
     */
    public static final String SUFFIX_MINUTE = "-minute";
    /**
     * Suffix for the database field that store the value in day
     */
    public static final String SUFFIX_DAY = "-day";
    /**
     * Suffix for the database field that store the value in month
     */
    public static final String SUFFIX_MONTH = "-month";
    /**
     * Suffix for the database field that store the value in year
     */
    public static final String SUFFIX_YEAR = "-year";

    /**
     * The search configuration
     */
    final SearchConfigService searchConfigService;

    /**
     * Constructor
     *
     * @param searchConfigService The search configuration
     * @param fhirPath            The fhir path where to find
     * @param date                The date to search
     * @param precision           The precision
     * @param prefix              How to search (gt, lt...)
     */
    public MongoDbDateRangeExpression(@NotNull SearchConfigService searchConfigService, @NotNull FhirSearchPath fhirPath, @NotNull Date date, @NotNull TemporalPrecisionEnum precision, @NotNull ParamPrefixEnum prefix) {
        super(fhirPath, date, precision, prefix);
        this.searchConfigService = searchConfigService;
    }

    /**
     * Find the mongodb filter based on precision and return a filter
     *
     * @param expressionContext the expression context
     * @return the filter
     */
    @Override
    public Bson interpreter(ExpressionContext expressionContext) {
        var config = searchConfigService.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }

        switch (this.getPrefix() != null ? this.getPrefix() : EQUAL) {
            case GREATERTHAN, STARTS_AFTER:
                return Filters.gt(expressionContext.getPrefix() + config.get().getIndexName() + getIndexSuffixFromPrecision(), getTimeInPrecision(this.date));
            case EQUAL:
                return Filters.eq(expressionContext.getPrefix() + config.get().getIndexName() + getIndexSuffixFromPrecision(), getTimeInPrecision(this.date));
            case GREATERTHAN_OR_EQUALS:
                return Filters.gte(expressionContext.getPrefix() + config.get().getIndexName() + getIndexSuffixFromPrecision(), getTimeInPrecision(this.date));
            case LESSTHAN, ENDS_BEFORE:
                return Filters.lt(expressionContext.getPrefix() + config.get().getIndexName() + getIndexSuffixFromPrecision(), getTimeInPrecision(this.date));
            case LESSTHAN_OR_EQUALS:
                return Filters.lte(expressionContext.getPrefix() + config.get().getIndexName() + getIndexSuffixFromPrecision(), getTimeInPrecision(this.date));
            case NOT_EQUAL:
                return Filters.ne(expressionContext.getPrefix() + config.get().getIndexName() + getIndexSuffixFromPrecision(), getTimeInPrecision(this.date));
            case APPROXIMATE:
                // Approximate don't support precision:
                long baseTime = this.date.getTime();
                long max = (long) (baseTime * APPROX_FACTOR);
                long min = (long) (baseTime / APPROX_FACTOR);
                var minFilter = Filters.gt(expressionContext.getPrefix() + config.get().getIndexName(), min);
                var maxFilter = Filters.lt(expressionContext.getPrefix() + config.get().getIndexName(), max);
                return Filters.and(minFilter, maxFilter);
            default:
                throw new BadConfigurationException("Prefix not supported: " + this.getPrefix());
        }
    }


    /**
     * Get the suffix of the index based on the precision
     *
     * @return the suffix of the index
     */
    private String getIndexSuffixFromPrecision() {
        switch (this.precision) {
            case MILLI:
                return SUFFIX_MILLI;
            case SECOND:
                return SUFFIX_SECOND;
            case MINUTE:
                return SUFFIX_MINUTE;
            case DAY:
                return SUFFIX_DAY;
            case MONTH:
                return SUFFIX_MONTH;
            case YEAR:
                return SUFFIX_YEAR;
            default:
                throw new BadPrecisionException("Unsupported date precision: " + this.precision);
        }
    }

    /**
     * Get the date in the current precision
     *
     * @param date the date
     * @return the date in ms in the current precision
     */
    private long getTimeInPrecision(Date date) {
        return FhirDateUtils.getTimeInPrecision(date, this.precision);
    }


    @Override
    public String serialize(ExpressionSerializer<Bson> expressionSerializer) {
        return expressionSerializer.serialize(this);
    }

    @Override
    public Expression<Bson> deserialize(ExpressionSerializer<Bson> expressionDeserializer) {
        return null;
    }

}
