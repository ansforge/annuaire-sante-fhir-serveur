# Start dev

Ce guide vous permet de créer votre propre serveur FHIR.

## Création du projet Maven

Le serveur se base sur les librairies Spring Boot et Hapi.

Pour démarrer, créer un projet spring boot avec le strater spring : [stater spring boot](https://start.spring.io/)


## Intégration du module afas 

Nous proposons l'utilisation d'un starter spring boot. Cela signifie que si vous ajoutiez la dépendance maven, votre
serveur est prêt à fonctionner.

``` xml
<dependencies>
     ... spring dependencies ... 
     <dependency>
        <groupId>fr.ans</groupId>
        <artifactId>afas-server-starter</artifactId>
        <version>${afas.version}</version>
    </dependency>
</dependencies>
```

afas.version doit être remplacée par la version souhaitée de la librairie. 


## Créer une première ressource

La configuration d'une ressource se fait en 2 étapes :
- La première étape consiste à configurer le moteur de stockage 
- et la seconde à configurer Hapi. 

Voir [add-parameter](add-parameter.md) pour plus de détails

## Configurer la servlet FHIR

De manière optionnelle, il est possible de configurer la servlet FHIR fournie par Hapi. 


Pour ce faire, vous devez étendre la classe `fr.ans.afas.AfasServerConfigurerAdapter` puis 
vous pourrez configurer la servlet avec la méthode "void configureHapiServlet(FhirServlet fhirServlet)".

Voici un exemple qui change le nom du serveur : 

``` java

@SpringBootApplication
public class FhirServerApplication extends AfasServerConfigurerAdapter {

    /**
     * Launch the service
     *
     * @param args command line args (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(FhirServerApplication.class, args);
    }


    /**
     * Configure the servlet
     *
     * @param fhirServlet the servlet to configure
     */
    @Override
    public void configureHapiServlet(FhirServlet fhirServlet) {
        fhirServlet.setServerName("My FHIR server");
    }
}
```

Il s'agit de la servlet Hapi.
