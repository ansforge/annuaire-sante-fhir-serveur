# Environnement de développement

Ce guide vous permet de contribuer au développement de l'application.

## Pré-requis

* Maven 3
* git
* docker/docker-compose (ou mongodb)
* java 11 (openjdk)

## Démarrage pour le développeur

### Mongodb

Le système nécessite mongodb comme moteur de stockage. Vous devez l'installer. Il est compatible Mongodb 5 et 6.

### Build

Pour construire le projet, utilisez la commande : `mvn clean package`.

Vous pouvez ensuite lancer le main "RassFhirServerApplication" dans le module "modules-as/fhir-server".

Les paramètres de l'application se trouvent dans un fichier application.yaml situé
`<Root>/modules-as/fhir-server/src/main/resources/application.yml`

Il est aussi possible d'utiliser un fichier de
développement `<Root>/modules-as/fhir-server/src/main/resources/application-local.yml` en spécifiant à spring que le
profil actif est "local" (-Dspring.profiles.active=local).

Le principal paramètre à changer pour fonctionner est la connection string d'accès à mongodb `afas.mongodb.uri`.

Exemple complet si vous n'utilisez pas d'IDE (pensez à remplacer la version du projet pour le fichier jar).

```
mvn clean package
cd modules-as/fhir-server/target
java -Duser.timezone=UTC -jar fhir-server-0.1.2-SNAPSHOT.jar --spring.config.location=C:\Users\guill\fhir-server-base\modules\fhir-server\target\application-local.yml
```

### IntelliJ

1) ouvre le projet maven
2) modifiez le fichier `<Root>/modules-as/fhir-server/src/main/resources/application-local.yml` et changez la
   propriété `afas.mongodb.uri` pour correspondre à votre base de données mongodb.
3) Allez dans Run/Debug, Configure. Ajoutez une configuration spring boot avec comme
   classe : `modules-as/fhir-server/src/main/java/fr/ans/afas/fhirserver/RassFhirServerApplication.java`
4) Mettez le profile actif à "local": ![Exemple de configuration de lancement](assets/images/launch-ide.png){ width=50%
   }

### Mode démo

Une fois le serveur lancé avec les paramètres par défaut, vous avec l'url du serveur FHIR accéssible sur l'
url : http://localhost:8080/fhir/v1/

Quelques urls utiles:

* CapabilityStatement: http://localhost:8080/fhir/v1/metadata?_format=json
* Lister les Organization : http://localhost:8080/fhir/v1/Organization?_format=json&_pretty=true
* Lister les Device : http://localhost:8080/fhir/v1/Device?_format=json&_pretty=true

## Build & packaging

### Commandes utiles

Pour tout construire : `mvn clean package -Duser.timezone=UTC`

Pour lancer uniquement les tests unitaires : `mvn test -Duser.timezone=UTC`

Pourl lancer les tests au complet : `mvn verify -Duser.timezone=UTC`

Pour tout construire, tester et installer les artifacts : `mvn clean package install -Duser.timezone=UTC`

### Jar

Pour déployer l'application il est possible d'utiliser maven qui va créer un fichier jar. Le fichier contiendra l'api
FHIR qui pourra être démarrée.

Avec la commande `mvn clean package` vous obtiendrez un fichier jar sous le dossier module-as/fhir-server/target.

Ce fichier pourra être utilisé pour le déploiement de l'api.

Attention, si vous n'avez pas docker, vous devez skip les tests utilisant
docker: `mvn clean install -DskipIntegrationTest -Duser.timezone=UTC`

Le système utilise spring boot. Se référer a
la [documentation de Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/index.html) pour plus
d'informations.
