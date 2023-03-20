/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service.json;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Lists;
import fr.ans.afas.fhirserver.search.config.SearchConfig;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An automatic serializer that can serialize fhir objects with a {@link SearchConfig}
 * <p>
 * This serializer use the config path to find objects in the fhir resource. The path definition follow the Spel spring language
 * (https://docs.spring.io/spring-framework/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html)
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class GenericSerializer extends FhirBaseResourceSerializer<DomainResource> {


    /**
     * The search configuration
     */
    SearchConfig searchConfig;

    ExpressionParser expressionParser = new SpelExpressionParser();

    @Autowired
    public GenericSerializer(SearchConfig searchConfig, FhirContext fhirContext) {
        super(fhirContext);
        this.searchConfig = searchConfig;

    }

    @Override
    public void serialize(DomainResource value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        var configs = searchConfig.getAllByFhirResource(value.fhirType());
        if (configs == null) {
            if (logger.isErrorEnabled()) {
                logger.error("Can't write {} resource. If you want to support this type, considère adding a specific configuration for this type.", value.fhirType());
            }

            throw new BadConfigurationException("Can't write " + value.fhirType() + " resource. Resource not supported");
        }
        gen.writeStartObject();
        super.writeFhirResource(value, gen, provider);

        var internalIndexes = Set.of("_lastUpdated", "_id");

        for (var config : configs) {
            // we don't process internal indexes, they are already processed:
            if (!internalIndexes.contains(config.getName())) {
                var extracts = new ArrayList<Object>();
                // serialize paths:
                for (var path : config.getResourcePaths()) {
                    var stringPath = path.getPath();
                    extracts.addAll(extractValues(value, stringPath));
                }

                // each indexed path must have the same type:
                var types = extracts.stream().map(Object::getClass).collect(Collectors.toSet());
                if (types.size() > 1) {
                    throw new BadConfigurationException("Error during the serialization. This is an error in the configuration. When you use multiple path for a field, each path must point to a FHIR property of the same type.");
                }

                if (types.isEmpty()) {
                    // nothing to serialize:
                    continue;
                }

                var theType = types.stream().iterator().next();
                writeValue(value, gen, config, extracts, theType);
            }

        }
        gen.writeEndObject();
    }

    private void writeValue(DomainResource value, JsonGenerator gen, SearchParamConfig config, ArrayList<Object> extracts, Class<?> theType) throws IOException {
        if (theType.equals(String.class)) {
            if (config.getSearchType().equals("token")) {
                this.writeMultiString(gen, extracts.stream().map(e -> new StringType((String) e)).collect(Collectors.toList()), config.getIndexName() + "-value");
            } else {
                this.writeMultiString(gen, extracts.stream().map(e -> new StringType((String) e)).collect(Collectors.toList()), config.getIndexName());
            }
        } else if (theType.equals(StringType.class)) {
            this.writeMultiString(gen, extracts.stream().map(StringType.class::cast).collect(Collectors.toList()), config.getIndexName());
        } else if (theType.equals(Identifier.class)) {
            this.writeIdentifiers(gen, extracts.stream().map(Identifier.class::cast).collect(Collectors.toList()), config.getIndexName());
        } else if (theType.equals(Reference.class)) {
            this.writeMultiReferences(gen, extracts.stream().map(Reference.class::cast).collect(Collectors.toList()), config.getIndexName());
        } else if (theType.equals(CodeableConcept.class)) {
            this.writeCodeableConcepts(gen, extracts.stream().map(CodeableConcept.class::cast).collect(Collectors.toList()), config.getIndexName());
        } else if (theType.equals(Boolean.class)) {
            this.writeBooleansInProperty(gen, extracts.stream().map(e -> new BooleanType((Boolean) e)).collect(Collectors.toList()), config.getIndexName());
        } else if (theType.equals(CanonicalType.class)) {
            this.writeMultiString(gen, extracts.stream().map(e -> new StringType(((CanonicalType) e).getValue())).collect(Collectors.toList()), config.getIndexName());
        } else if (theType.equals(HumanName.class)) {
            this.writeHumanNames(gen, extracts.stream().map(HumanName.class::cast).collect(Collectors.toList()), config.getIndexName());
        } else {
            throw new BadConfigurationException("Error during the serialization of the field: " + config.getName() + " for the resource " + value.fhirType() + ". Type of the field " + theType + " not supported. Please refer to the documentation to see supported types.");
        }
    }


    protected Collection<Object> extractValues(Object value, String stringPath) {
        if (StringUtils.hasLength(stringPath)) {
            var pathParts = stringPath.split("\\|");
            return extractValuesInternal(List.of(value), new ArrayDeque<>(Lists.newArrayList(pathParts)));
        } else {
            logger.warn("Bad configuration during the extraction of a field. The path must not be empty or null.");
        }
        return List.of();
    }

    /**
     * Extract the value for one level
     *
     * @param values
     * @param stringPath
     * @return
     */
    protected Collection<Object> extractValuesInternal(Collection<Object> values, Deque<String> stringPath) {
        var expression = expressionParser.parseExpression(stringPath.getFirst());

        var results = new ArrayList<>();
        for (var eachValue : values) {
            var context = new StandardEvaluationContext(eachValue);
            var result = expression.getValue(context);
            if (result == null) {
                continue;
            }
            if (result instanceof Collection) {
                // array
                results.addAll((Collection) result);
            } else {
                // single item
                results.add(result);
            }
        }
        stringPath.removeFirst();

        if (stringPath.isEmpty()) {
            return results;
        } else {
            return extractValuesInternal(results, stringPath);
        }
    }


    @Override
    public Class getClassFor() {
        return DomainResource.class;
    }


}
