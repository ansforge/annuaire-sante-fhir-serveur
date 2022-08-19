# Deployment

## MongoDb

Lors d'un déploiement avec Mongodb, il faut que l'application dispose des droits de création de Collection, d'indexes et
de données (insert/update/delete).

Si la base de données n'existe pas, l'application va tenter de la créer avec le nom spécifié en configuration. Dans ce
cas, il faudra également le droit de créer des bases de données.

La configuration mongodb par défaut autorise tout cela.

## Configuration

### Paramètres de configuration


| clé                               | description                                                                                                               | exemple                                                                             |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| afas.fhir.next-url-encryption-key | secret qui permet d'encoder les urls next. Doit être secret                                                               | changeme                                                                            |
| afas.mongodb.uri                  | la chaine de connection au mongodb sous le format connection string                                                       | mongodb://root:root@localhost:27017/?socketTimeoutMS=360000&connectTimeoutMS=360000 |
| afas.mongodb.dbname               | le nom de la collection mongodb                                                                                           | fhirdb                                                                              |
| afas.publicUrl                    | l'adresse public du serveur FHIR. doit être la base de l'url à partir de laquelle les utilisateurs utiliserons le service | http://localhost:8080/fhir                                                          |


## Jar deployment

Créer un fichier de configuration ex `/etc/fhir-server/application.yml` et positionner les valeurs de ce fichier avec les votres.

```yaml
afas:
  fhir:
    next-url-encryption-key: change-me
  mongodb:
    uri: ${MONGO_CONNECTION_STRING:mongodb://root:root@localhost:27017/?socketTimeoutMS=360000&connectTimeoutMS=360000}
    dbname: ${DB_NAME:afassample}

  publicUrl: http://localhost:8080/fhir
```

`java -Duser.timezone=UTC -jar fhir-server.jar --spring.config.location=/etc/fhir-server/application.yml`

## Docker deployment

Pour lancer le docker, il suffira d'utiliser la commande docker run :

`docker run [Name of the repo and version]`

Lorsque vous lancez le docker, vous pouvez préciser un fichier de configuration ou bien des variables d'environnement =>
Monter les 2 techniques et faire référence au [deploy](deploy.md)

