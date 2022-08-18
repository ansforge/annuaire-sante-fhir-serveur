# Storage (MongoDB)

## Contexte

Iris Dp stock les données dans une base de données MongoDb.
Les données sont stockées au format FHIR comme elles sont recues avec en complément des champs techniques tel que les
indexes.

Le service est un service qui a été pensé pour offrir des performances meilleures en lecture qu’en écriture. Pour avoir
de bonnes performances en lecture le stockage est basé sur 2 concepts :

* Utilisation d’indexes MongoDB généralisée : la plupart des champs recherchables sont indexés
* Pré formatage de données à l’insertion : les champs recherchables de type non string sont décomposés en champs
  techniques orientés pour les requêtes
  L’opération de pré-formatage va engendrer pour chaque champs une série de champs technique proposant certains
  combinatoires couramment recherchés afin d’accélérer la lecture.

## Structure générale

Chaque ressource Fhir est stockée dans une collection mongdb. La collection mongodb d'une ressource porte le nom de la
ressource. Par exemple les ressources "Device" seront stockés dans la collection "Device"
"Organization" dans "Organization"...

Les collections sont automatiquement créées par le serveur (il doit disposer des droits de création).

## Structure de stockage de chaque ressource

Les ressources sont stockées en jsonb (mongodb).

Les ressources sont stockées en 2 parties. D’une part sous le champ “fhir” il y a la ressource brute FHIR. Celle qui
sera retournée à l’utilisateur. Et d’autre part, il y aura les champs techniques utilisés pour les recherches.

```

{
  “fhir": { … resource FHIR à retourner aux clients de l’api ... }
  “t_fid”: “Organization/1”,
  “t_id”: “1”,
  “t_lastupdated” : 102839323,
  ….. other fields … 
}
```

La nomenclature est la suivante :

| Champs               | Description                                                                                                            |
|----------------------|------------------------------------------------------------------------------------------------------------------------|
| fhir                 | Entité FHIR                                                                                                            |
| t_fid                | Identifiant complet de la ressource FHIR sous le format <Nom de ressource>/<identifiant>. Exemple : “Organization/1”   |
| t_id                 | Identifiant de la ressource. Exemple “1”.                                                                              |
| t_lastupdated        | Date de dernière mise à jour FHIR avec une précision à la milliseconde.                                                |
| t_lastupdated-second | Date de dernière mise à jour FHIR avec une précision à la seconde (les ms sont tronquées)                              |
| t_lastupdated-minute | Date de dernière mise à jour FHIR avec une précision en minute (les secondes sont tronquées)                           |
| t_lastupdated-date   | Date de dernière mise à jour FHIR avec une précision en jour (les heures, minutes sont tronquées)                      |
| t_lastupdated-month  | Date de dernière mise à jour FHIR avec une précision en mois (les heures sont tronquées)                               |                                                                                                 |
| t_lastupdated-year   | Date de dernière mise à jour FHIR avec une précision en année (les mois sont tronquées)                                |                                                                                            |
| t_*                     | Les champs techniques pour les champs spécifiques aux objets FHIR. Ces champs sont utilisés pour la recherche.         |                                                                                                                                                                                      |

Les recherches ne s’effectuent jamais sur les ressources FHIR brute. Dans chaque recherche, un champ technique est
utilisé.

## Nomenclature de stockage des indexes des objets

### Type CodeableConcept

| Champs                | Description                                                                    |
|-----------------------|--------------------------------------------------------------------------------|
| t_<index-name>-value  | Champ code du codeable concept                                                 |
| t_<index-name>-system | Champ système du codeable concept                                              |
| t_<index-name>-sysval | Champs système + code du codeable concept concaténés par un caractère '&#x7c;' |

### Type Identifier

| Champs                | Description                                                                       |
|-----------------------|-----------------------------------------------------------------------------------|
| t_<index-name>-value  | Champ value du Identifier                                                         |
| t_<index-name>-system | Champ système du Identifier                                                       |
| t_<index-name>-sysval | Champs système + value du Identifier concept concaténés par un caractère '&#x7c;' |

### Type Boolean

Le champ est traité comme un CodeableConcept

| Champs                | Description                                                                                                    |
|-----------------------|----------------------------------------------------------------------------------------------------------------|
| t_<index-name>-value  | La valeur du boolean                                                                                           |
| t_<index-name>-system | Toujours la valeur “http://hl7.org/fhir/ValueSet/special-values”                                               |
| t_<index-name>-sysval | Toujours la valeur http://hl7.org/fhir/ValueSet/special-values + le séparateur '&#x7c;' + la valeur du boolean |

### Type String

| Champs         | Description            |
|----------------|------------------------|
| t_<index-name> | La chaine de caractère |

### Type String multi-lignes

| Champs          | Description                                   |
|-----------------|-----------------------------------------------|
| t_<index-name>  | Un tableau contenant les chaines de caractère |

### Type Reference

Champs Description
t_<index-name>-reference Définir
t_<index-name>-type Définir

### Type Reference multiple

Champs Description
t_<index-name>-reference Définir
t_<index-name>-type Définir

### Type Adresse

Champs Description
t_<index-name>-city Définir
t_<index-name>-country Définir

t_<index-name>-postalcode Définir
t_<index-name>-state Définir
t_<index-name>-use Définir
t_<index-name>    Définir

_Exemple:_

Prenons l’exemple d’un champ FHIR de type CodeableConcept. Dans ce cas, la structure FHIR est du type :

```yaml
{
  "resourceType": "Organization",
  "type": [
    {
      "coding": [
        {
          "system": "http://terminology.hl7.org/CodeSystem/organization-type",
          "code": "dept",
          "display": "Hospital Department"
        }
      ]
    }
  ]
}
```

Dans ce cas, la norme prévoit 3 types majeurs de recherche : 1 sur le system, 1 sur le code et un sur le système et le
code.

Une recherche mongodb pure sur ces types d’éléments peut se révéler complexe et couteuse en fonction des cas (code +
système multiples, imbrication profonde...).

Dans ce cas, le système de stockage va créer des champs techniques pour adresser les cas de recherche en précalculant
les différents types de champs pour la recherche.

Sur notre exemple de CodeableConcept, le système de stockage va créer 3 champs techniques :

* t_type-system : pour une recherche sur le system
* t_type-value: pour une recherche sur le code
* t_type-sysval : pour une recherche sur la combinatoire des 2

Dans ce cas la ressource complète dans la base sera :

```yaml
{
  “fhir”: {
    "resourceType": "Organization",
    “id”: “1”,
    "type": [
      {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/organization-type",
            "code": "dept",
            "display": "Hospital Department"
          }
        ]
      }
    ]
  },
  “t_fid”: “Organization/1”,
  “t_id”: “1”,
  <metadata indexes>,
  “t_type-system": http://terminology.hl7.org/CodeSystem/organization-type,
  “t_type-value": “dept”,
  “t_type-sysval": “http://terminology.hl7.org/CodeSystem/organization-type|dept”
}
```

Par la suite, les requêtes se feront sur les champs techniques “t_*” et les données retournées aux clients de l’api
proviendront de la propriété “fhir”.

## Indexes

Au démarrage du serveur, un système permet de créer tous les indexes définis dans
la [configuration](configuration-system.md).

Le serveur n'est pas disponible pendant la création des indexes. Si le volume de donnée est important, cela peut prendre
plusieurs minutes.

NOTE: Si vous effectuez un mongodump/mongorestore à chaud, vous perdrez les indexes. Dans ce cas, il est recommandé de
stopper le serveur lors des restaurations de données afin de recréer les indexes.

