/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service.json;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import fr.ans.afas.domain.StorageConstants;
import fr.ans.afas.exception.BadReferenceFormat;
import fr.ans.afas.utils.FhirDateUtils;
import fr.ans.afas.utils.IrisFhirUtils;
import fr.ans.afas.utils.data.ParsedReference;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Serializer to serialize an FHIR Resource.
 * <p>
 * This class serialize the base Fhir object under the "fhir" property. So you can store other data in the root of the json object (for example data used to search in fhir).
 * </p>
 * <p>
 * This class give some utility methods to serialize common Fhir fields.
 * </p>
 *
 * @param <T> the Fhir resource type
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public abstract class FhirBaseResourceSerializer<T> extends JsonSerializer<T> {


    /**
     * Logger
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The fhir context
     */
    final FhirContext fhirContext;

    /**
     * Create a serializer
     *
     * @param fhirContext the fhir context
     */
    protected FhirBaseResourceSerializer(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    /**
     * Serialize a Fhir object to json
     *
     * @param value    the fhir object to serialize
     * @param gen      to write the json
     * @param provider the provider for the serialization
     * @throws IOException if an error occur during the serialization
     */
    @Override
    public abstract void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException;


    /**
     * Write the fhir object inside the "fhir" property
     *
     * @param value    the fhir object
     * @param gen      the json generator
     * @param provider the serializer
     * @throws IOException if an error occur during the storage of the element
     */
    protected void writeFhirResource(DomainResource value, JsonGenerator gen, SerializerProvider provider) throws IOException {

        var fhirResourceAsString = fhirContext.newJsonParser().encodeResourceToString(value);
        gen.writeFieldName("fhir");
        gen.writeRawValue(fhirResourceAsString);

        // revision related fields:
        var clone = fhirContext.newJsonParser().parseResource(fhirResourceAsString);
        clone.getMeta().setLastUpdated(null);
        clone.getMeta().setVersionId(null);
        var cloneString = fhirContext.newJsonParser().encodeResourceToString(clone);
        gen.writeNumberField("_hash", cloneString.hashCode());
        gen.writeNumberField("_revision", 1);
        gen.writeNumberField("_validTo", 4099676400000L);// year 2100
        gen.writeNumberField("_validFrom", new Date().getTime());

        // write some technical fields:
        gen.writeStringField(StorageConstants.INDEX_T_FID, value.getResourceType().name() + "/" + value.getIdElement().getIdPart());
        gen.writeStringField(StorageConstants.INDEX_T_ID, value.getIdElement().getIdPart());
        // We add a value with "-value" because we also allow search by id and in this case it's a token search:
        gen.writeStringField(StorageConstants.INDEX_T_ID + StorageConstants.VALUE_SUFFIX, value.getIdElement().getIdPart());
        if (!value.getMeta().getProfile().isEmpty()) {
            gen.writeStringField(StorageConstants.INDEX_T_PROFILE, value.getMeta().getProfile().get(0).getValue());
        }

        if (value.getMeta().getLastUpdated() != null) {
            // mongodb can't store long, so we use a string to store raw value:
            gen.writeNumberField(StorageConstants.INDEX_T_LASTUPDATED, FhirDateUtils.getTimeInPrecision(value.getMeta().getLastUpdated(), TemporalPrecisionEnum.MILLI));
            gen.writeNumberField(StorageConstants.INDEX_T_LASTUPDATED_SECOND, FhirDateUtils.getTimeInPrecision(value.getMeta().getLastUpdated(), TemporalPrecisionEnum.SECOND));
            gen.writeNumberField(StorageConstants.INDEX_T_LASTUPDATED_MINUTE, FhirDateUtils.getTimeInPrecision(value.getMeta().getLastUpdated(), TemporalPrecisionEnum.MINUTE));
            gen.writeNumberField(StorageConstants.INDEX_T_LASTUPDATED_DATE, FhirDateUtils.getTimeInPrecision(value.getMeta().getLastUpdated(), TemporalPrecisionEnum.DAY));
            gen.writeNumberField(StorageConstants.INDEX_T_LASTUPDATED_MONTH, FhirDateUtils.getTimeInPrecision(value.getMeta().getLastUpdated(), TemporalPrecisionEnum.MONTH));
            gen.writeNumberField(StorageConstants.INDEX_T_LASTUPDATED_YEAR, FhirDateUtils.getTimeInPrecision(value.getMeta().getLastUpdated(), TemporalPrecisionEnum.YEAR));
        }
    }

    /**
     * Write a collection of string into a json array
     *
     * @param gen      the json generator
     * @param v        the string collection
     * @param property the name of the json property
     * @throws IOException if an error occur during the storage of values
     */
    protected void writeMultiString(JsonGenerator gen, Collection<StringType> v, String property) throws IOException {
        if (!v.isEmpty()) {
            gen.writeFieldName(property);
            var array = v.stream().map(PrimitiveType::getValue).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);
        }
    }


    protected void writeBooleansInProperty(JsonGenerator gen, Collection<BooleanType> vs, String property) throws IOException {
        if (!vs.isEmpty()) {
            var cc = new CodeableConcept();
            for (var v : vs) {
                if (!v.isEmpty()) {
                    cc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/special-values").setCode(v.getValueAsString());
                }
            }
            if (cc.hasCoding()) {
                this.writeCodeableConcepts(gen, List.of(cc), property);
            }
        }
    }


    /**
     * Write a fhir identifier. We index value, system and both
     *
     * @param gen         the json generator
     * @param identifiers the identifier
     * @param prefix      the base name of the json property
     * @throws IOException if an error occur writing identifiers
     */
    protected void writeIdentifiers(JsonGenerator gen, Collection<Identifier> identifiers, String prefix) throws IOException {
        if (!identifiers.isEmpty()) {
            gen.writeFieldName(prefix + StorageConstants.SYSTEM_SUFFIX);
            var array = identifiers.stream().map(Identifier::getSystem).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);

            gen.writeFieldName(prefix + StorageConstants.VALUE_SUFFIX);
            array = identifiers.stream().map(Identifier::getValue).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);

            gen.writeFieldName(prefix + StorageConstants.SYSVAL_SUFFIX);
            array = identifiers.stream().map(st -> st.getSystem() + "|" + st.getValue()).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);
        }
    }


    /**
     * Write a fhir codeableconcept. We index code (as value), system and both.
     * <p>
     * Everything is store in arrays to allow multiple values.
     *
     * @param gen              the json generator
     * @param codeableConcepts the codeable concepts
     * @param prefix           the base name of the json property
     * @throws IOException if an error occur writing the codeable concept
     */
    protected void writeCodeableConcepts(JsonGenerator gen, Collection<CodeableConcept> codeableConcepts, String prefix) throws IOException {
        if (!codeableConcepts.isEmpty()) {
            gen.writeFieldName(prefix + StorageConstants.VALUE_SUFFIX);
            var array = codeableConcepts.stream().flatMap(s -> s.getCoding().stream()).map(Coding::getCode).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);

            gen.writeFieldName(prefix + StorageConstants.SYSTEM_SUFFIX);
            array = codeableConcepts.stream().flatMap(s -> s.getCoding().stream()).map(Coding::getSystem).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);

            gen.writeFieldName(prefix + StorageConstants.SYSVAL_SUFFIX);
            array = codeableConcepts.stream().flatMap(s -> s.getCoding().stream()).map(st -> st.getSystem() + "|" + st.getCode()).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);
        }
    }


    /**
     * Write multiple references
     *
     * @param gen        the json generator
     * @param references the identifier
     * @param prefix     the base name of the json property
     * @throws IOException if an error occur writing the reference
     */
    protected void writeMultiReferences(JsonGenerator gen, Collection<Reference> references, String prefix) throws IOException {
        Assert.notNull(prefix, "Prefix is required");
        Assert.notNull(references, "References are required for Fhir org.hl7.fhir.r4.model.Reference. Path: " + prefix);

        if (!references.isEmpty()) {

            var goodReferences = references.stream().filter(r -> !r.isEmpty()).map(reference -> {
                try {
                    return IrisFhirUtils.parseReference(reference.getReference());
                } catch (BadReferenceFormat e) {
                    logger.error("Error persisting a reference (in an array of references) for a FHIR resource. Prefix: {}. The object is stored without the reference.", prefix);
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());

            gen.writeFieldName(prefix + StorageConstants.REFERENCE_SUFFIX);

            var array = goodReferences.stream().map(gr -> gr.getResourceType() + "/" + gr.getResourceId()).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);

            gen.writeFieldName(prefix + StorageConstants.TYPE_SUFFIX);
            array = goodReferences.stream().map(ParsedReference::getResourceType).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);

            gen.writeFieldName(prefix + StorageConstants.ID_SUFFIX);
            array = goodReferences.stream().map(ParsedReference::getResourceId).toArray(String[]::new);
            gen.writeArray(array, 0, array.length);
        }
    }

    /**
     * Write a human name. We index the prefix
     *
     * @param gen        the json generator
     * @param humanNames the human names
     * @param prefix     the base name of the json property
     * @throws IOException if an error occur writing humanNames
     */
    protected void writeHumanNames(JsonGenerator gen, Collection<HumanName> humanNames, String prefix) throws IOException {
        if (!humanNames.isEmpty()) {
            var array = humanNames.stream().map(HumanName::getPrefix).flatMap(p -> p.stream().map(PrimitiveType::getValue)).toArray(String[]::new);
            if (array.length > 0) {
                gen.writeFieldName(prefix + StorageConstants.HUMAN_NAME_PREFIX_SUFFIX);
                gen.writeArray(array, 0, array.length);
            }
            var arraySuffix = humanNames.stream().map(HumanName::getSuffix).flatMap(p -> p.stream().map(PrimitiveType::getValue)).toArray(String[]::new);
            if (array.length > 0) {
                gen.writeFieldName(prefix + StorageConstants.HUMAN_NAME_SUFFIX_SUFFIX);
                gen.writeArray(arraySuffix, 0, arraySuffix.length);
            }
        }
    }

    /**
     * Get the class that the serializer can handle (Organization, Device...)
     *
     * @return the class
     */
    public abstract Class<T> getClassFor();
}