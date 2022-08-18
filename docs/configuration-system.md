# Configuration system

Les ressources du serveur FHIR sont configurées grâce à un fichier de configuration. Ce fichier de configuration
au format yaml permet de déclarer et définir les ressources, paramètres et indexes en base de données.

## Format du fichier

Le format du fichier est le suivant :

```yaml
fhir:
  validationMode: strict
  resources:
    - name: Patient
      profile: http://hl7.org/fhir/StructureDefinition/Patient
      searchParams:
        - name: active
          urlParameter: active
          searchType: token
          description: Whether the patient record is active
          resourcePaths:
            - path: active
          indexName: t_active
        - name: address
          urlParameter: address
          searchType: string
          description: A server defined search that may match any of the string fields in the Address, including line, city, district, state, country, postalCode, and/or text
          resourcePaths:
            - path: address|line
            - path: address|city
            - path: address|district
          indexName: t_address
```

Les ressources sont définies sous le chemin `fhir.resources`.

## Configuration générale


validationMode : non utilisé pour le moment
resources : définition de toutes les ressources

## Ressources

### Définition d'une ressource

Les champs d'une ressource sont les suivants :

| Field name   | Description                                               | Exemple                                                                                                                              |
|--------------|-----------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| name         | The name of the resource. Must be the Official Fhir name. | Organization, Device                                                                                                                 |
| profile      | The FHIR profile of the resource                          | http://hl7.org/fhir/StructureDefinition/Organization, https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-Organization        |
| searchParams | Configuration of fhir search params                       | See the next section                                                                                                                 |

Exemple avec 2 profiles :

```yaml
fhir:
  resources:
    - name: Organization
      profile: https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-Organization
      searchParams: <...>
    - name: Device
      profile: https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-Device
      searchParams: <...>
      <...>
```

### Définition des paramètres d'une ressource

Les champs des paramètres de ressource sont :

| Name          | Description                                                                                                                                             | Exemple                                  |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------|
| name          | The name of the parameter                                                                                                                               | name                                     |
| urlParameter  | The name of the parameter in Fhir search                                                                                                                | name                                     |
| searchType    | The type of the search parameter. Allowed values are date, string, token, reference, quantity, uri                                                      | string                                   |
| description   | The description of the parameter                                                                                                                        | Name of the patient                      |
| resourcePaths | The path of the field that match the resource in the Fhir object. Use a '&vert;' to chain properties. All paths must match a the same type of resource.  | - name&vert;family<br/>- name&vert;given |

Exemple sur l'implémentation de référence "Patient (R4)" et quelques champs:

```yaml
fhir:
  resources:
    - name: Patient
      profile: http://hl7.org/fhir/StructureDefinition/Patient
      searchParams:
        - name: active
          urlParameter: active
          searchType: token
          description: Whether the patient record is active
          resourcePaths:
            - path: active
          indexName: t_active
        - name: address
          urlParameter: address
          searchType: string
          description: A server defined search that may match any of the string fields in the Address, including line, city, district, state, country, postalCode, and/or text
          resourcePaths:
            - path: address|line
            - path: address|city
            - path: address|district
            - path: address|state
            - path: address|country
            - path: address|postalCode
            - path: address|text
          indexName: t_address
        - name: address-city
          urlParameter: address-city
          searchType: string
          description: A city specified in an address
          resourcePaths:
            - path: address.city
          indexName: t_address
```

Les types des champs supportés pour "resourcePaths" sont :

Pour les types primitifs:

* string
* code
* period
* reference

Pour les types complexes:

* Identifier
* CodeableConcept

