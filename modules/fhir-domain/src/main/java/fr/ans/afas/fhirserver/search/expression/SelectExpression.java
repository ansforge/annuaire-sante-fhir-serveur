/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;


import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.exception.BadDataFormatException;
import fr.ans.afas.exception.BadSelectExpression;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.data.TotalMode;
import fr.ans.afas.fhirserver.search.exception.BadParametersException;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import fr.ans.afas.validation.DataValidationUtils;
import lombok.Getter;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * A FHIR select expression. Contains all information used to query the database.
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class SelectExpression<T> implements Expression<T> {


    /**
     * The default size of pages
     */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * The maximum size of a select expression. Hardcoded, juste to ensure that there is no abuse.
     */
    private static final int MAX_SELECT_COUNT = 5000;
    /**
     * The fhir resource of the select
     */
    final String fhirResource;
    /**
     * The expression Factory to use (depends on the implementation)
     */
    final ExpressionFactory<T> expressionFactory;
    /**
     * All includes to add to the request (FHIR _include)
     */
    final Set<IncludeExpression<T>> includes = new HashSet<>();
    /**
     * All revincludes to add to the request (FHIR _revinclude)
     */
    final Set<IncludeExpression<T>> revincludes = new HashSet<>();
    /**
     * The expression
     */
    private final ContainerExpression<T> expression;
    private final List<HasCondition<T>> hasConditions = new ArrayList<>();
    /**
     * Item per pages
     */
    Integer count = DEFAULT_PAGE_SIZE;
    /**
     * Mode of the count calculation
     */
    TotalMode totalMode = TotalMode.BEST_EFFORT;
    /**
     * The "_since" parameter to limit the request on object that have a modification date after this value.
     * If null, the parameter is not used
     */
    Date since;

    /**
     * Construct a SelectExpression.
     * Use the expression factory to choose the implementation.
     *
     * @param fhirResource      the Fhir resource where to search
     * @param expressionFactory the expression factory
     */
    public SelectExpression(@NotNull String fhirResource, @NotNull ExpressionFactory<T> expressionFactory) {
        this(fhirResource, expressionFactory, expressionFactory.newAndExpression());
    }

    /**
     * Construct a SelectExpression.
     * Use the expression factory to choose the implementation.
     *
     * @param fhirResource      the Fhir resource where to search
     * @param expressionFactory the expression factory
     * @param expression        the base expression
     */
    public SelectExpression(@NotNull String fhirResource, @NotNull ExpressionFactory<T> expressionFactory, ContainerExpression<T> expression) {
        Assert.notNull(expression, "Expression can not be null");
        this.expression = expression;
        this.fhirResource = fhirResource;
        this.expressionFactory = expressionFactory;
    }

    /**
     * Convert a Hapi parameter to a Select expression for StringAndListParam
     *
     * @param path               the path of the parameter
     * @param stringAndListParam the parameter to convert
     * @return the converted select expression
     */
    public SelectExpression<T> fromFhirParams(FhirSearchPath path, StringAndListParam stringAndListParam) throws BadDataFormatException {
        if (stringAndListParam != null) {
            for (var orParams : stringAndListParam.getValuesAsQueryTokens()) {
                OrExpression<T> orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
                    DataValidationUtils.validateTokenParameter(param.getValue());
                    orExpression.or(expressionFactory.newStringExpression(path, param.getValue(), resolveOperator(param)));
                }
                this.expression.addExpression(orExpression);
            }
        }
        return this;
    }

    /**
     * Convert a Hapi parameter to a Select expression for StringAndListParam. This will search with a OR operator against all parameters
     *
     * @param paths              list of paths where to search
     * @param stringAndListParam the parameter to convert
     * @return the converted select expression
     */
    public SelectExpression<T> fromFhirParams(List<FhirSearchPath> paths, StringAndListParam stringAndListParam) throws BadDataFormatException {
        if (stringAndListParam != null) {
            var orExpression = expressionFactory.newOrExpression();
            for (var path : paths) {
                for (var orParams : stringAndListParam.getValuesAsQueryTokens()) {
                    var orExpressionInternal = expressionFactory.newOrExpression();
                    for (var param : orParams.getValuesAsQueryTokens()) {
                        DataValidationUtils.validateTokenParameter(param.getValue());
                        orExpressionInternal.or(expressionFactory.newStringExpression(path, param.getValue(), resolveOperator(param)));
                    }
                    orExpression.addExpression(orExpressionInternal);
                }
            }
            this.expression.addExpression(orExpression);
        }
        return this;
    }

    /**
     * Convert a Hapi parameter to a Select expression for TokenAndListParam
     *
     * @param path              the path of the parameter
     * @param tokenAndListParam the parameter to convert
     * @return the converted select expression
     */
    public SelectExpression<T> fromFhirParams(FhirSearchPath path, TokenAndListParam tokenAndListParam) throws BadDataFormatException {
        if (tokenAndListParam != null) {
            for (var orParams : tokenAndListParam.getValuesAsQueryTokens()) {
                var orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
                    DataValidationUtils.validateTokenParameter(param.getSystem());
                    DataValidationUtils.validateTokenParameter(param.getValue());
                    orExpression.or(expressionFactory.newTokenExpression(path, param.getSystem(), param.getValue()));
                }
                this.expression.addExpression(orExpression);
            }
        }
        return this;
    }

    public SelectExpression<T> fromFhirParams(HasAndListParam hasParams) {
        if (hasParams != null) {
            for (var orParams : hasParams.getValuesAsQueryTokens()) {
                if (!orParams.getValuesAsQueryTokens().isEmpty()) {
                    var firstHasParam = orParams.getValuesAsQueryTokens().get(0);
                    var paramPath = FhirSearchPath.builder().resource(firstHasParam.getTargetResourceType()).path(firstHasParam.getParameterName()).build();
                    var linkPath = FhirSearchPath.builder().resource(firstHasParam.getTargetResourceType()).path(firstHasParam.getReferenceFieldName()).build();
                    var values = new ArrayList<String>();
                    for (var hasParam : orParams.getValuesAsQueryTokens()) {
                        values.add(hasParam.getParameterValue());
                    }
                    var hasCondition = expressionFactory.newHasExpression(linkPath, paramPath, values);
                    this.addHasCondition(hasCondition);
                }
            }
        }
        return this;
    }

    /**
     * Convert a Hapi parameter to a Select expression for ReferenceAndListParam
     *
     * @param path        the path of the parameter
     * @param theEndpoint the parameter to convert
     * @return the converted select expression
     */
    public SelectExpression<T> fromFhirParams(FhirSearchPath path, ReferenceAndListParam theEndpoint) throws BadDataFormatException {
        if (theEndpoint != null) {
            for (var orParams : theEndpoint.getValuesAsQueryTokens()) {
                var orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
                    DataValidationUtils.validateTokenParameter(param.getValue());
                    orExpression.or(expressionFactory.newReferenceExpression(path, param.getValue()));
                }
                this.expression.addExpression(orExpression);
            }
        }
        return this;
    }

    /**
     * Convert a Hapi parameter to a Select expression for DateRangeParam
     *
     * @param path           the path of the parameter
     * @param dateRangeParam the parameter to convert
     * @return the converted select expression
     */
    public SelectExpression<T> fromFhirParams(FhirSearchPath path, DateRangeParam dateRangeParam) {
        if (dateRangeParam != null) {
            for (var orParams : dateRangeParam.getValuesAsQueryTokens()) {
                var orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
                    orExpression.or(expressionFactory.newDateRangeExpression(path, param.getValue(), param.getPrecision(), param.getPrefix()));
                }
                this.expression.addExpression(orExpression);
            }
        }
        return this;
    }

    /**
     * Convert a Hapi parameter to a Select expression for multiple DateParam. The parameters will be applied with a logical OR.
     *
     * @param path       the path of the parameter
     * @param dateParams parameters
     */
    public void orFromFhirParams(FhirSearchPath path, List<DateParam> dateParams) {
        if (dateParams != null) {
            var orExpression = expressionFactory.newOrExpression();
            for (var param : dateParams) {
                orExpression.or(expressionFactory.newDateRangeExpression(path, param.getValue(), param.getPrecision(), param.getPrefix()));
            }
            this.expression.addExpression(orExpression);
        }
    }

    public SelectExpression<T> fromFhirParams(FhirSearchPath path, UriAndListParam theSearchForProfile) throws BadDataFormatException {
        if (theSearchForProfile != null) {
            for (var orParams : theSearchForProfile.getValuesAsQueryTokens()) {
                OrExpression<T> orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
                    DataValidationUtils.validateTokenParameter(param.getValue());
                    orExpression.or(expressionFactory.newStringExpression(path, param.getValue(), StringExpression.Operator.EXACT));
                }
                this.expression.addExpression(orExpression);
            }
        }
        return this;
    }

    public SelectExpression<T> fromFhirParams(Set<Include> theInclude) throws BadDataFormatException {
        if (theInclude != null) {
            for (var include : theInclude) {
                DataValidationUtils.validateIncludeParameter(include.getParamType(), include.getParamName());
                this.includes.add(expressionFactory.newIncludeExpression(include.getParamType(), include.getParamName()));
            }
        }
        return this;
    }

    public SelectExpression<T> fromFhirParamsRevInclude(Set<Include> theInclude) throws BadDataFormatException {
        if (theInclude != null) {
            for (var include : theInclude) {
                DataValidationUtils.validateIncludeParameter(include.getParamType(), include.getParamName());
                this.revincludes.add(expressionFactory.newIncludeExpression(include.getParamType(), include.getParamName()));
            }
        }
        return this;
    }

    /**
     * Get the request
     *
     * @return the request
     */
    public T interpreter() {
        return this.interpreter(new ExpressionContext());
    }

    /**
     * Convert the operator from Hapi
     *
     * @param param the Hapi param
     * @return the operator of the spring expression
     */
    private StringExpression.Operator resolveOperator(StringParam param) {
        StringExpression.Operator operator;
        if (param.isExact()) {
            operator = StringExpression.Operator.EXACT;
        } else if (param.isContains()) {
            operator = StringExpression.Operator.CONTAINS;
        } else {
            operator = StringExpression.Operator.EQUALS;
        }
        return operator;
    }

    @Override
    public T interpreter(ExpressionContext expressionContext) {
        return this.expression.interpreter(expressionContext);
    }

    /**
     * Set the count (page size) of the request
     *
     * @param count the page size of the request
     */
    public void setCount(Integer count) {
        if (count != null) {
            if (count > MAX_SELECT_COUNT) {
                throw new BadParametersException("The Fhir _count parameter is limited to " + MAX_SELECT_COUNT);
            }
            this.count = count;
        }
    }

    /**
     * Set the "_since" parameter to limit the request on object that have a modification date after this value.
     * If null, the parameter is not used
     *
     * @param since the date or null
     */
    public void setSince(Date since) {
        this.since = since;
    }

    public String serialize(ExpressionSerializer<T> expressionSerializer) {
        return expressionSerializer.serialize(this);
    }

    @Override
    public Expression<T> deserialize(ExpressionSerializer<T> expressionDeserializer) {
        return expressionDeserializer.deserialize("");
    }

    /**
     * Set the mode of the count calculation
     *
     * @param totalMode the mode
     */
    public void setTotalMode(TotalMode totalMode) {
        this.totalMode = totalMode;
    }

    /**
     * Set the mode of the count calculation from a string.
     * Allowed values are accurate, none, estimate
     *
     * @param totalMode the mode
     */
    public void setTotalMode(String totalMode) throws BadSelectExpression {
        if (totalMode != null) {
            switch (totalMode) {
                case "accurate":
                case "estimate":
                    this.setTotalMode(TotalMode.ALWAYS);
                    break;
                case "none":
                    this.setTotalMode(TotalMode.NONE);
                    break;
                default:
                    throw new BadSelectExpression("Bad value for the _total parameter. Allowed values are : accurate, none, estimate");
            }
        }
    }

    @Override
    public String toString() {
        return "Select " +
                this.fhirResource +
                this.expression +
                "\tCount:" +
                this.count;
    }

    public void addHasCondition(HasCondition<T> expression) {
        this.hasConditions.add(expression);
    }

    public void setHasConditions(List<HasCondition<T>> expression) {
        this.hasConditions.clear();
        this.hasConditions.addAll(expression);
    }

}
