/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.rass.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Contains an id, a fhir resource (hapi) and the document (mongo) of the fhir resource
 * Used by the {@link MongoDbFhirService} during the storage process
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Builder
@Getter
@Setter
class IdResourceDocument {

    /**
     * The id
     */
    String id;
    /**
     * The resource
     */
    IBaseResource resource;
    /**
     * The mongodb document to insert
     */
    Document newDocument;
    /**
     * The old mongodb document if its an update
     */
    Document oldDocument;

}
