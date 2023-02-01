# Recherches consistantes

Lors de recherches paginées, nous cherchons à avoir une consistance de la donnée tout au long de la pagination.

C'est-à-dire que si un client de l'api récupère la première page, il pourra récupérer les ressources des pages suivantes
dans l'état dans lequel elles étaient
lors de la première requête.

L'enjeu ici est d'avoir une donnée consistante, même si la source est modifiée (insertion ou suppression d'objets).

## Implémentation technique

Pour réaliser la consistance des données, la technique utilisée se base sur l'utilisation d'une date de validité des
versions de la donnée.

### 1er étape : datage de la validité des données

Lorsqu'une donnée est insérée, elle à une date de validité qui démarre à l'instant de l'insertion et une date de fin de
validité qui se termine très lointaine (2100).
Lorsqu'une donnée est mise à jour, la nouvelle version est insérée avec la même règle que pour la création et l'ancienne
donnée se voit mise à jour avec
une date de fin de validité à l'instant T.

Les objets json auront tous une date de début de validité et une date de fin de validité.

Voici un exemple avec une mise à jour de Practitioner (vue mongodb):

On remarque dans les json ci-dessous, après mise à jour, que dans la base de données il y a désormais 2 versions du même
objet.
On remarque egualement que les bornes (_validTo, _validFrom) de validité des 2 objets sont disjointes et se touchent.

_Première version de l'objet:_

```json
{
  "fhir": {
    ...
  },
  "_revision": 1,
  "_validTo": {
    "$numberLong": "1658094931206"
  },
  "_validFrom": {
    "$numberLong": "1658094894120"
  },
  "t_fid": "PractitionerRole/prarole-981",
  "t_id": "prarole-981",
  "t_id-value": "prarole-981",
  "t_profile": "https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-PractitionerRole",
  ...
}
```

_Seconde version de l'objet:_

```json
{
  "fhir": {
    ...
  },
  "_hash": -1944926147,
  "_revision": 2,
  "_validTo": {
    "$numberLong": "4099676400000"
  },
  "_validFrom": {
    "$numberLong": "1658094931206"
  },
  "t_fid": "PractitionerRole/prarole-981",
  "t_id": "prarole-981",
  "t_id-value": "prarole-981",
  "t_profile": "https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-PractitionerRole",
  ...
}
```

### 2eme étape : ajout de paramètres lors de la recherche

Lors d'une recherche mongodb, nous ajoutons pour toutes les requêtes de lecture des conditions liés à l'heure de
validité de la ressource :

Exemple de code ajouté pour limiter la requête sur les ressources valides à un instant T

```java
Filters.and(
        Filters.lt("_validFrom",searchRevision),
        Filters.gte("_validTo",searchRevision),
        query);
```

### 3eme étape : conservation de la date de la réception de la première page dans les requêtes paginées

Ce système nécessite un contexte de recherche qui conserve la date de reception de la première page.
C'est grâce à cette date que nous pourrons effecturer toutes les futurs requêtes (page 2, 3...) avec la même date de
validité.

Pour ce faire, nous ajoutons la date de requête dans les liens "next" qui permettent d'accéder aux pages suivantes:

```json
{
  "resourceType": "Bundle",
  "id": "0043b900-9e1e-41b6-9cbe-c8005d320480",
  "meta": {
    "lastUpdated": "2022-07-18T00:17:21.065+02:00"
  },
  "type": "searchset",
  "total": 10000,
  "link": [
    {
      "relation": "next",
      "url": "http://server/fhir/v1?_getpages=b34e1d28-3247-46f7-91c5-b967427c03b0&_pageId=1dY074e0BDvtV6JqD7sbtwrcdADJQk81WeoGvQHDjaeUqqPlef18R52BvgLzdsL5bkhxOnkl7PzOQ6sjiEAe0m84gFdyF5g6F7hZQIXR0083qSHsBSHiawcUNlwm-3bw4IdEk4i3H4xilP-2lUIFtqInuWrtS3nytwQMvw1U1bRc3ZG1lBs2tIlvrNVNw2rhDyHxD0Kvv9_rOx6MCMRFh9tjZPsI7nvWgQhkzhRVZt2h1Xfa_XnmmFLnUmgDuHY%3D&_format=json&_pretty=true&_bundletype=searchset"
    }
  ],
  ...
```

Ci-dessus, le lien "link>url" contient la date de requête qui sera donc propagée sur les prochaines requêtes.

A noter que l'url contient la une fois déchiffré la valeur :

`50_10000_b34e1d28-3247-46f7-91c5-b967427c03b0_1658096388539_Organization_searchRevision=1658096388553,lastId=62d4841b269c580bb67c5c8f_6|Organization$50$null$0%7C$$`

Avec le champs `searchRevision=1658096388553` qui est la date de la requête initiale.

### 4eme étape : nétoyage des anciennes données

Quand les données ne sont plus considérées comme valide, alors elle sont supprimé grâce à un job.
Le job tourne toutes les heures et va effacer les données qui ont une date de fin de validité plus vielles qu'un
paramètre de configuration `afas.fhir.max-revision-duration`.

```java
@Scheduled(fixedDelay = 3600000)
public void cleanOldRevision(){
        mongoDbFhirService.deleteOldRevisions(new Date().getTime()-validityMs);
        }
```

## Avantages/Inconvénients de la technique

Cette technique à pour inconvénient de stocker plusieurs fois la donnée le temps autorisé pour la pagination (les
données sont purgées au bout de X heures).
Sur un système très actif en mise à jour, cela ne sera pas performant.
Si les mises à jour ne sont pas trop fréquentes, alors le système sera performant car il permet de garder une
consistance avec une requête relativement simple.

## Tuning

Cette partie ne concerne que les systèmes qui ont des mises à jour relativement fréquentes.

La durée de validité des révisions de ressource se définie grâce au paramètre `afas.fhir.max-revision-duration`.
Le choix de la durée va dépendre du volume de donnée et du temps que les clients vont prendre pour effectuer une requête
paginée complète.

Il vous faut donc déterminer le temps maximum que prendrait un client à récupérer toutes les données d'une collection et
placer cette valeur en `afas.fhir.max-revision-duration`.

Plus cette valeur est faible, moins il y aura de révisions stockées dans la base (et donc plus performant sera le
système).

Si vous cherchez à déterminer cette valeur, considérez les requêtes sans paramètre qui sont en général les plus longues.
Pensez aussi que les clients peuvent
avoir un traitement entre l'appel de chaque page. 







