# Pagination (MongoDb)

## Implémentation

Le système de pagination a été pensé pour être performant sur un volume de données > 10 millions. Cela fait partie des
priorités de l'api qui permet entre autre à des systèmes tiers de se synchroniser.

Pour réaliser cela nous utilisons une mécanique de "lastId".

Toutes les requêtes sont triés par `_id`. Pour chaque requête, nous stockons le dernier id récupéré de la requête
courante. Lorsque l'utilisateur effectue un appel à la page suivante, nous ajoutons une condition "WHERE" à la requête
mongodb qui va aller récupérer les ids supérieurs au dernier id récupéré.

Prenons par exemple le résultat de recherche suivant:

```json
{
  "resourceType": "Bundle",
  "total": 10000,
  "link": [
    {
      "relation": "next",
      "url": "http://server/fhir/v1?_getpages=b34e1d28-3247-46f7-91c5-b967427c03b0&_pageId=1dY074e0BDvtV6JqD7sbtwrcdADJQk81WeoGvQHDjaeUqqPlef18R52BvgLzdsL5bkhxOnkl7PzOQ6sjiEAe0m84gFdyF5g6F7hZQIXR0083qSHsBSHiawcUNlwm-3bw4IdEk4i3H4xilP-2lUIFtqInuWrtS3nytwQMvw1U1bRc3ZG1lBs2tIlvrNVNw2rhDyHxD0Kvv9_rOx6MCMRFh9tjZPsI7nvWgQhkzhRVZt2h1Xfa_XnmmFLnUmgDuHY%3D&_format=json&_pretty=true&_bundletype=searchset"
    }
  ],
  ...
```

Ci-dessus, le lien "link>url" décodé à pour valeur :
`50_10000_b34e1d28-3247-46f7-91c5-b967427c03b0_1658096388539_Organization_searchRevision=1658096388553,lastId=62d4841b269c580bb67c5c8f_6|Organization$50$null$0%7C$$`

Dans cette valeur, nous retrouvons le paramètre "lastId=62d4841b269c580bb67c5c8f" qui contient l'id du dernier paramètre
récupéré.

Lors de l'appel à la page 2, nous ajouterons ce paramètre à la requête MongoDB :

```
Filters.gt("_id", new ObjectId("62d4841b269c580bb67c5c8f")),
```

Cela permet d'effectuer une pagination sans réelle perte de performance.

**Note sur le total:** Le système (au 2022/07/18) affiche le nombre total des ressources dans les résultats de recherche
de la première page. Cela va causer un scan complet de la base de données. La première requête sera donc couteuse non
pas à cause de la complexité de la requête, mais à cause du count. Le total est ensuite caché dans le contexte de la
requête.

## Inconvénients

A date l'implémentation en supporte pas de tri autre que celui défini par les ids. Cette limitation technique n'est pas
ferme.


