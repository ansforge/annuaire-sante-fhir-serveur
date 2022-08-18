# Search context

Lors d'une recherche paginée, nous allons devoir conserver le contexte initiale de la requête. C'est ce contexte qui va
nous permettre de récupérer les pages suivantes.

Comme le système est stateless, le contexte est stocké dans l'url.

## Fonctionnement FHIR d'une recherche paginée

En FHIR, lors d'une recherche paginée la requête à la page suivante s'effectue avec un lien HATEAOS sous le chemin : "
link>next".

exemple:

```json
{
  "resourceType": "Bundle",
  "total": 10000,
  "link": [
    {
    "relation": "next",
    "url": "http://server/fhir/v1?_getpages=b34e1d28-3247-46f7-91c5-b967427c03b0&_pageId=1dY074e0BDvtV6JqD7sbtwrcdADJQk81WeoGvQHDjaeUqqPlef18R52BvgLzdsL5bkhxOnkl7PzOQ6sjiEAe0m84gFdyF5g6F7hZQIXR0083qSHsBSHiawcUNlwm-3bw4IdEk4i3H4xilP-2lUIFtqInuWrtS3nytwQMvw1U1bRc3ZG1lBs2tIlvrNVNw2rhDyHxD0Kvv9_rOx6MCMRFh9tjZPsI7nvWgQhkzhRVZt2h1Xfa_XnmmFLnUmgDuHY%3D&_format=json&_pretty=true&_bundletype=searchset"
  } ]
```

Pour obtenir la page suivante, il suffit d'appeler le lien contenu dans "link>next".

Le lien est un lien technique, qui ne devrait pas être modifié par le client de l'api.

## Implémentation technique

Le serveur est stateless, il a donc été choisi de stocker tout le contexte de la recherche (paramètres, page
courante...) dans l'url du lien "link>next".

Pour ce faire nous sérialisons tout le contexte dans une chaine de caractère que nous ajoutons à chaque url "link>next".

### Serialization

La sérialisation se fait dans la classe `fr.ans.afas.fhir.AfasBundleProvider` (IBundleProvider de Hapi).

```java
public String serialize(){
        String contextAsString=context.keySet().stream()
        .map(key->key+"="+context.get(key))
        .collect(Collectors.joining(",","",""));
        var serialized=new StringBuilder();
        serialized.append(pageSize);
        serialized.append("_");
        serialized.append(size);
        serialized.append("_");
        serialized.append(uuid);
        serialized.append("_");
        serialized.append(published.getValue().getTime());
        serialized.append("_");
        serialized.append(type);
        serialized.append("_");
        serialized.append(contextAsString);
        serialized.append("_");
        serialized.append(this.selectExpression.serialize(expressionSerializer));

        return serializeUrlEncrypter.encrypt(serialized.toString());
        }
```

Les parties génériques du contexte  (qui ne sont pas liés à la recherche) sont sérialisé sous forme d'une string  :
searchId, pageSize (_count)...

Pour la sérialisation de la requête FHIR en elle même, nous utilisons la classe "
fr.ans.afas.mdbexpression.domain.fhir.serialization.MongoDbExpressionSerializer" qui va transformer la requête en
string:
`serialized.append(this.selectExpression.serialize(expressionSerializer));`

Enfin nous chiffrons la chaine de caractère avec AES afin de limiter les manipulations non maitrisées des paramètres.
La classe pour faire cela est : "
fr.ans.afas.fhirserver.search.expression.serialization.DefaultSerializeUrlEncrypter" `serializeUrlEncrypter.encrypt(serialized.toString())`

### Deserialization

La deserialization se fait depuis le gestionaire de pagination Hapi  (
IPagingProvider) : `fr.ans.afas.fhir.AfasPagingProvider`
Cette méthode est appelée à chaque fois qu'une page profonde est appelée (>2).

```java
@Override
public IBundleProvider retrieveResultList(@Nullable RequestDetails theRequestDetails,@NotNull String theSearchIdEncoded,String thePageId){
        var theSearchId=serializeUrlEncrypter.decrypt(thePageId);
        var parts=theSearchId.split("_",7);
        var pageSize=parts[0];
        var size=parts[1];
        var uuid=parts[2];
        var timestamp=parts[3];
        var type=parts[4];
        var contextAsString=parts[5];
        var exp=parts[6];

        var context=new HashMap<String, Object>();
        var keyVals=contextAsString.split(",");
        for(var keyVal:keyVals){
        var p=keyVal.split("=");
        context.put(p[0],p[1]);
        }

        var expression=expressionDeserializer.deserialize(exp);
        return new AfasBundleProvider<>(Integer.parseInt(pageSize),Integer.parseInt(size),uuid,new DateDt(new Date(Long.parseLong(timestamp))),fhirStoreService,type,(SelectExpression)expression,context,expressionSerializer,serializeUrlEncrypter);
        }
```

La méthode `var theSearchId = serializeUrlEncrypter.decrypt(thePageId);` permet de déchiffrer le paramètre (qui est
chiffré avec AES).

Ensuite, le bundle récupère les parties génériques du contexte (qui ne sont pas liés à la recherche) : searchId,
pageSize (_count)...

Enfin nous utiliserons l'objet ExpressionDeserializer pour désérialiser la requête en elle
même `expressionDeserializer.deserialize(exp)`.

## Limitations du système

Le système ne supporte pas l'affichage du lien HATEAOS "prev".

