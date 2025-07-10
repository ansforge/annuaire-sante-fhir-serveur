/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.rass.service.json;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Lists;
import fr.ans.afas.domain.ResourceAndSubResources;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.config.domain.SearchParamConfig;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import org.hl7.fhir.r4.model.*;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An automatic serializer that can serialize fhir objects with a {@link SearchConfigService}
 * <p>
 * This serializer use the config path to find objects in the fhir resource. The path definition follow the Spel spring language
 * (<a href="https://docs.spring.io/spring-framework/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html">...</a>)
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class GenericSerializer extends FhirBaseResourceSerializer<ResourceAndSubResources> {


    /**
     * The search configuration
     */
    final SearchConfigService searchConfigService;

    SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE,
            this.getClass().getClassLoader());

    final ExpressionParser expressionParser = new SpelExpressionParser(config);


    /**
     * Used to cache spring spel expressions (they are compiled)
     */
    Map<String, Expression> cachedExpression = new HashMap<>();


    @Inject
    public GenericSerializer(SearchConfigService searchConfigService, FhirContext fhirContext) {
        super(fhirContext);
        this.searchConfigService = searchConfigService;

    }

    @Override
    public void serialize(ResourceAndSubResources valueAndSubValues, JsonGenerator gen, SerializerProvider provider) throws IOException {
        this.serialize(valueAndSubValues, gen, provider, false);
    }

    public void serialize(ResourceAndSubResources valueAndSubValues, JsonGenerator gen, SerializerProvider provider, boolean onlyIndexes) throws IOException {
        var value = valueAndSubValues.getResource();
        var configs = searchConfigService.getAllByFhirResource(value.fhirType());
        if (configs == null) {
            if (logger.isErrorEnabled()) {
                logger.error("Can't write {} resource. If you want to support this type, considère adding a specific configuration for this type.", value.fhirType());
            }

            throw new BadConfigurationException("Can't write " + value.fhirType() + " resource. Resource not supported");
        }
        gen.writeStartObject();
        super.writeFhirResource(value, gen, provider, onlyIndexes);

        var internalIndexes = Set.of("_lastUpdated", "_id");

        for (var oneConfig : configs) {
            // we don't process internal indexes, they are already processed:
            if (!internalIndexes.contains(oneConfig.getName())) {
                var extracts = new ArrayList<>();
                // serialize paths:
                for (var path : oneConfig.getResourcePaths()) {
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
                writeValue(value, gen, oneConfig, extracts, theType);
            }

        }

        // write links:
        writeLinks(valueAndSubValues, gen, provider);

        gen.writeEndObject();
    }

    private void writeLinks(ResourceAndSubResources valueAndSubValues, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (valueAndSubValues.getSubResources() != null && !valueAndSubValues.getSubResources().isEmpty()) {

            var resourcesToPersist = valueAndSubValues.getSubResources().stream()
                    .collect(Collectors.groupingBy(r -> r.getResourceType().toString()));
            gen.writeFieldName("links");
            gen.writeStartObject();
            for (var subs : resourcesToPersist.entrySet()) {
                gen.writeFieldName(subs.getKey());
                gen.writeStartArray();
                for (var sub : subs.getValue()) {
                    this.serialize(ResourceAndSubResources.builder().resource(sub).build(), gen, provider, true);
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }
    }

    private void writeValue(DomainResource value, JsonGenerator gen, SearchParamConfig config, ArrayList<Object> extracts, Class<?> theType) throws IOException {
        if (theType.equals(String.class)) {
            if (config.getSearchType().equals("token")) {
                this.writeMultiString(gen, extracts.stream().map(e -> new StringType((String) e)).toList(), config.getIndexName() + "-value");
            } else {
                this.writeMultiString(gen, extracts.stream().map(e -> new StringType((String) e)).toList(), config.getIndexName());
            }
        } else if (theType.equals(StringType.class)) {
            this.writeMultiString(gen, extracts.stream().map(StringType.class::cast).toList(), config.getIndexName());
        } else if (theType.equals(CodeType.class)) {
            this.writeCodeTypes(gen, extracts.stream().map(CodeType.class::cast).toList(), config.getIndexName());
        } else if (theType.equals(Identifier.class)) {
            this.writeIdentifiers(gen, extracts.stream().map(Identifier.class::cast).toList(), config.getIndexName());
        } else if (theType.equals(Reference.class)) {
            this.writeMultiReferences(gen, extracts.stream().map(Reference.class::cast).toList(), config.getIndexName());
        } else if (theType.equals(CodeableConcept.class)) {
            this.writeCodeableConcepts(gen, extracts.stream().map(CodeableConcept.class::cast).toList(), config.getIndexName());
        } else if (theType.equals(Boolean.class)) {
            this.writeBooleansInProperty(gen, extracts.stream().map(e -> new BooleanType((Boolean) e)).toList(), config.getIndexName());
        } else if (theType.equals(CanonicalType.class)) {
            this.writeMultiString(gen, extracts.stream().map(e -> new StringType(((CanonicalType) e).getValue())).toList(), config.getIndexName());
        } else if (theType.equals(HumanName.class)) {
            this.writeHumanNames(gen, extracts.stream().map(HumanName.class::cast).toList(), config.getIndexName());
        } else {
            throw new BadConfigurationException("Error during the serialization of the field: " + config.getName() + " for the resource " + value.fhirType() + ". Type of the field " + theType + " not supported. Please refer to the documentation to see supported types.");
        }
    }


    public Collection<Object> extractValues(Object value, String stringPath) {
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
     * @param values     values
     * @param stringPath paths to extract
     * @return extracted values
     */
    protected Collection<Object> extractValuesInternal(Collection<Object> values, Deque<String> stringPath) {
        var elem = values.stream().findAny();
        if (elem.isEmpty()) {
            return List.of();
        }
        var cacheKey = elem.get().getClass() + "_" + stringPath.getFirst();


        cachedExpression.computeIfAbsent(cacheKey, k -> expressionParser.parseExpression(stringPath.getFirst()));
        var expression = cachedExpression.get(cacheKey);

        var results = new ArrayList<>();
        for (var eachValue : values) {
            var context = new StandardEvaluationContext(eachValue);
            var result = expression.getValue(context);
            if (result == null) {
                continue;
            }
            if (result instanceof Collection) {
                // array
                results.addAll((Collection<?>) result);
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
    public Class<ResourceAndSubResources> getClassFor() {
        return ResourceAndSubResources.class;
    }


}
