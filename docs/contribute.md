# Environnement de développement

Ce guide vous permet de contribuer au développement de l'application.

## Pré-requis

* Maven 3
* git
* docker/docker-compose (ou mongodb)
* java 11 (openjdk)

## Démarrage pour le développeur

### Mongodb

Le projet permet de démarrer les développements rapidement avec docker. Un fichier docker-compose permet de créer un
mongodb déjà configuré.

Lancement du docker : `docker-compose -f docker/dev/docker-compose.yml up`

Si vous ne disposez pas de docker, installez un mongodb et utilisez le.

### Build

Pour construire le projet, utilisez la commande : `mvn clean package`.

Vous pouvez ensuite lancer le main "RassFhirServerApplication" dans le module "modules-as/fhir-server".

Les paramètres de l'application se trouvent dans un fichier application.yaml situé
`<Root>/modules-as/fhir-server/src/main/resources/application.yml`

Il est aussi possible d'utiliser un fichier de
développement `<Root>/modules-as/fhir-server/src/main/resources/application-local.yml` en
spécifiant à spring que le profil actif est "local" (-Dspring.profiles.active=local).

Le principal paramètre à changer pour fonctionner est la connection string d'accès à mongodb `afas.mongodb.uri`.

Exemple complet si vous n'utilisez pas d'IDE (penser à remplacer la version du projet pour le fichier jar).

```
mvn clean package
cd modules-as/fhir-server/target
java -Duser.timezone=UTC -jar fhir-server-0.1.2-SNAPSHOT.jar --spring.config.location=C:\Users\guill\fhir-server-base\modules\fhir-server\target\application-local.yml
```

### IntelliJ

1) ouvrir le projet maven
2) modifier le fichier `<Root>/modules-as/fhir-server/src/main/resources/application-local.yml` et changer la
   propriété `afas.mongodb.uri` pour correspondre à votre base de données mongodb.
3) Aller dans Run/Debug, Configurer et ajouter une configuration spring boot avec comme
   classe : `modules-as/fhir-server/src/main/java/fr/ans/afas/fhirserver/RassFhirServerApplication.java`
4) Metter le profile actif à "local": ![Exemple de configuration de lancement](assets/images/launch-ide.png)

### Mode démo

Une fois le serveur lancé avec les paramètres par défaut, le service FHIR sera accessible à cette url : http://localhost:8080/fhir/v1/

Quelques urls utiles:

* CapabilityStatement: http://localhost:8080/fhir/v1/metadata?_format=json
* Lister les ressources Organization : http://localhost:8080/fhir/v1/Organization?_format=json&_pretty=true
* Lister les ressources Device : http://localhost:8080/fhir/v1/Device?_format=json&_pretty=true

## Build & packaging

### Commandes utiles

Pour tout construire : `mvn clean package -Duser.timezone=UTC`

Pour lancer uniquement les tests unitaires : `mvn test -Duser.timezone=UTC`

Pourl lancer les tests au complet : `mvn verify -Duser.timezone=UTC`

Pour tout construire, tester et installer les artifacts : `mvn clean package install -Duser.timezone=UTC`

### Jar

Pour déployer l'application, il est possible d'utiliser maven qui va créer un fichier jar. Le fichier contiendra l'api
FHIR à démarrer.

Avec la commande `mvn clean package`, vous obtiendrez un fichier jar sous le dossier module-as/fhir-server/target.

Ce fichier pourra être utilisé pour le déploiement de l'api.

Attention, si vous n'avez pas docker, vous devez sauter les tests utilisant
docker: `mvn clean install -DskipIntegrationTest -Duser.timezone=UTC`

Le système utilise spring boot (vous pouvez vous référer à
la [documentation de Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/index.html) pour plus
d'informations).

### Docker

Pour les déploiements docker, nous utilisons le plugin maven jib com.google.cloud.tools:jib-maven-plugin (vous trouvrez plus d'informations sur la [page officielle](https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin)
du plugin.

Après avoir installé la version courante (`mvn install`) puis dans le dossier: `modules-as/fhir-server`

Compilation et build de l'image docker locale: `mvn compile jib:dockerBuild`

Déploiement du conteneur vers le registre: `mvn compile jib:build`

Puis le docker sera déployé sous le nom positionné dans le fichier : `modules-as/fhir-server/pom.xml` :

```xml

<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.2.1</version>
    <configuration>
        <to>
            <image>ans/as-fhir-server[TODO Name of the repo and version]</image>
        </to>
    </configuration>
</plugin>
```

Une fois l'image créée, vous pouvez la lancer avec une commande de ce type:

`docker run -p 8085:8080 -e SPRING_CONFIG_LOCATION="/opt/props/" -e JAVA_TOOL_OPTIONS="-Dspring.profiles.active=local" -e MONGO_CONNECTION_STRING="mongodb://root:root@host.docker.internal:27017/?socketTimeoutMS=360000&connectTimeoutMS=360000" --mount type=bind,source=D:/dev/ans/irisdp-api-fhir-back/modules-as/fhir-server/src/main/resources/,target=/opt/props/ ans/as-fhir-server`

Cela lance un conteneur avec 3 variables d'environnement :

* SPRING_CONFIG_LOCATION="/opt/props/": Permet de dire à spring où chercher la configuration
* JAVA_TOOL_OPTIONS="-Dspring.profiles.active=local": Permet de spécifier le profil spring à "local"
* MONGO_CONNECTION_STRING="mongodb://root:root@host.docker.internal:
  27017/?socketTimeoutMS=360000&connectTimeoutMS=360000": Permet de spécifier l'url du serveur mongodb (connection
  string). On note que l'url utilisée ici est spéciale car nous sommes dans un conteneur.

Et cela monte un dossier depuis notre dossier de propriétés vers /opt/props.

Avec cette commande vous devriez pouvoir accéder au CapabilityStatement
avec : `http://localhost:8085/fhir/v1/metadata?_format=json`

