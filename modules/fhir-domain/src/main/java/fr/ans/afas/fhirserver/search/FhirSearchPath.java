/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.fhirserver.search;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

/**
 * A search path
 *
 * Exemple pour une recherche chaînée :
 * - resource: le type de la ressource (ex: "PractitionerRole")
 * - path: le champ de référence de la ressource (ex: "practitioner")
 * - chain: le critère sur la ressource référencée (ex: "family")
 *
 * Cela permet par exemple de représenter "practitioner.family".
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
     * Optionnel : partie de la chaîne (ex: "family")
     */
    final String chain;

    /**
     * Construct a FhirSearchPath
     *
     * @param resource The FHIR resource
     * @param path     The search path on the resource
     * @param chain    The chained search part (optional), e.g. field of the referenced resource
     */
    @Builder
    public FhirSearchPath(@NonNull String resource, @NonNull String path, String chain) {
        this.resource = resource;
        this.path = path;
        this.chain = chain;
    }

    @Override
    public String toString() {
        return "FhirSearchPath{" +
                "resource='" + resource + '\'' +
                ", path='" + path + '\'' +
                (chain != null ? ", chain='" + chain + '\'' : "") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FhirSearchPath)) return false;
        FhirSearchPath that = (FhirSearchPath) o;
        return resource.equals(that.resource) &&
                path.equals(that.path) &&
                Objects.equals(chain, that.chain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, path, chain);
    }
}
