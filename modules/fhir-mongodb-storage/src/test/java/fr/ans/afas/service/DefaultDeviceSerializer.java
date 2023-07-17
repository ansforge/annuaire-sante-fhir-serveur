/*
 * (c) Copyright 1998-2023, ANS. All rights reserved.
 */

package fr.ans.afas.service;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import fr.ans.afas.rass.service.json.FhirBaseResourceSerializer;
import org.hl7.fhir.r4.model.Device;

import java.io.IOException;

public class DefaultDeviceSerializer extends FhirBaseResourceSerializer<Device> {
    /**
     * Create a serializer
     *
     * @param fhirContext the fhir context
     */
    protected DefaultDeviceSerializer(FhirContext fhirContext) {
        super(fhirContext);
    }

    @Override
    public void serialize(Device value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        super.writeFhirResource(value, gen, provider, false);
        gen.writeEndObject();
    }

    @Override
    public Class<Device> getClassFor() {
        return Device.class;
    }
}
