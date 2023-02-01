/*
 * (c) Copyright 1998-2022, ANS. All rights reserved.
 */

package fr.ans.afas.fhirserver.search;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

/**
 * A search path
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@Getter
public class FhirSearchPath {

    /**
     * The FHIR resource
     */
    @NonNull
    final String resource;
    /**
     * The search path on the resource
     */
    @NonNull
    final String path;

    /**
     * Construct a FhirSearchPath
     *
     * @param resource The FHIR resource
     * @param path     The search path on the resource
     */
    @Builder
    public FhirSearchPath(@NonNull String resource, @NonNull String path) {
        this.resource = resource;
        this.path = path;
    }

    @Override
    public String toString() {
        return "FhirSearchPath{" +
                "resource='" + resource + '\'' +
                ", path='" + path + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FhirSearchPath that = (FhirSearchPath) o;
        return resource.equals(that.resource) && path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, path);
    }
}
