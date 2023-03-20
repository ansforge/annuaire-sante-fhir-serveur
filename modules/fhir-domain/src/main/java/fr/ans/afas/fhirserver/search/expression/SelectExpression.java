/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search.expression;


import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.param.*;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.data.TotalMode;
import fr.ans.afas.fhirserver.search.exception.BadParametersException;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import lombok.Getter;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * The expression
     */
    final ContainerExpression<T> expression;
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
     * The sort field
     */
    String order;

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
    public SelectExpression<T> fromFhirParams(FhirSearchPath path, StringAndListParam stringAndListParam) {
        if (stringAndListParam != null) {
            for (var orParams : stringAndListParam.getValuesAsQueryTokens()) {
                OrExpression<T> orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
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
    public SelectExpression<T> fromFhirParams(List<FhirSearchPath> paths, StringAndListParam stringAndListParam) {
        if (stringAndListParam != null) {
            var orExpression = expressionFactory.newOrExpression();
            for (var path : paths) {
                for (var orParams : stringAndListParam.getValuesAsQueryTokens()) {
                    var orExpressionInternal = expressionFactory.newOrExpression();
                    for (var param : orParams.getValuesAsQueryTokens()) {
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
    public SelectExpression<T> fromFhirParams(FhirSearchPath path, TokenAndListParam tokenAndListParam) {
        if (tokenAndListParam != null) {
            for (var orParams : tokenAndListParam.getValuesAsQueryTokens()) {
                var orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
                    orExpression.or(expressionFactory.newTokenExpression(path, param.getSystem(), param.getValue()));
                }
                this.expression.addExpression(orExpression);
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
    public SelectExpression<T> fromFhirParams(FhirSearchPath path, ReferenceAndListParam theEndpoint) {
        if (theEndpoint != null) {
            for (var orParams : theEndpoint.getValuesAsQueryTokens()) {
                var orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
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


    public SelectExpression<T> fromFhirParams(FhirSearchPath path, UriAndListParam theSearchForProfile) {
        if (theSearchForProfile != null) {
            for (var orParams : theSearchForProfile.getValuesAsQueryTokens()) {
                OrExpression<T> orExpression = expressionFactory.newOrExpression();
                for (var param : orParams.getValuesAsQueryTokens()) {
                    orExpression.or(expressionFactory.newStringExpression(path, param.getValue(), StringExpression.Operator.EXACT));
                }
                this.expression.addExpression(orExpression);
            }
        }
        return this;
    }


    public SelectExpression<T> fromFhirParams(Set<Include> theInclude) {
        if (theInclude != null) {
            for (var include : theInclude) {
                this.includes.add(expressionFactory.newIncludeExpression(include.getParamType(), include.getParamName()));
            }
        }
        return this;
    }

    public SelectExpression<T> fromFhirParamsRevInclude(Set<Include> theInclude) {
        if (theInclude != null) {
            for (var include : theInclude) {
                this.revincludes.add(expressionFactory.newIncludeExpression(include.getParamType(), include.getParamName()));
            }
        }
        return this;
    }


    /**
     * Set the order by of the expression
     *
     * @param order the order field
     * @return the current expression
     */
    public SelectExpression<T> orderBy(String order) {
        this.order = order;
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

    public String serialize(ExpressionSerializer expressionSerializer) {
        return expressionSerializer.serialize(this);
    }

    @Override
    public Expression<T> deserialize(ExpressionSerializer expressionDeserializer) {
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
    public void setTotalMode(String totalMode) {
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
                    throw new BadParametersException("Bad value for the _total parameter. Allowed values are : accurate, none, estimate");
            }
        }
    }


}
