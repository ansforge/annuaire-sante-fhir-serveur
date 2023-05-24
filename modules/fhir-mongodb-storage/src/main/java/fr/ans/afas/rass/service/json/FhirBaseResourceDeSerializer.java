/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service.json;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hl7.fhir.r4.model.DomainResource;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Read a Fhir resource from the stored json.
 * <p>
 * The Fhir object have to be stored under the "fhir" property.
 * </p>
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
public class FhirBaseResourceDeSerializer extends JsonDeserializer<DomainResource> {

    /**
     * The fhir context
     */
    final FhirContext fhirContext;
    /**
     * A json parser
     */
    final IParser parser;

    /**
     * Create the parser
     *
     * @param fhirContext the fhir context
     */
    @Inject
    public FhirBaseResourceDeSerializer(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
        parser = fhirContext.newJsonParser();
    }

    /**
     * Deserialize a fhir resource
     *
     * @param p   the parser
     * @param ctx the context
     * @return the parsed resource
     * @throws IOException if the resource can't be deserialized
     */
    @Override
    public DomainResource deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        var rawFhir = p.getCodec().readTree(p).get("fhir").toString();
        return (DomainResource) parser.parseResource(rawFhir);
    }
}