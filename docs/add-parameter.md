# Procédure d’ajout d’une nouvelle ressource

L'exposition d'une nouvelle ressource FHIR se fait en 2 étapes.

1) Il faut configurer le moteur de recherche grâce à un bean de type "
   fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig"
2) Il faut créer un Provider Hapi qui expose l'api FHIR et le connecter au moteur de recherche Afas

## Configuration

Voici une configuration java :

``` java
 @Bean
 SearchConfig fhirResourceConfig() {
     var config = new HashMap<String, List<SearchParamConfig>>();
     config.put("Device", List.of(
            // some optonal parameters:
             SearchParamConfig.builder().name(Device.SP_DEVICE_NAME).urlParameter(Device.SP_DEVICE_NAME).resourcePaths(List.of(ResourcePathConfig.builder().path("deviceName|name").build())).indexName(StorageConstants.INDEX_DEVICE_NAME).searchType(StorageConstants.INDEX_TYPE_STRING).build(),
     ));
     return new BaseSearchConfigService(config) {
     };
 
```

Voici la configuration yaml :

``` yaml
fhir:
  resources:
    - name: Device
      profile: http://hl7.org/fhir/StructureDefinition/Device
      searchParams:
        # some optional parameters
        - name: identifier
          urlParameter: identifier
          searchType: token
          description: The device identifier
          resourcePaths:
            - path: identifier
          indexName: t_identifier
```

Plus de détail sur la configuration dans la section [configuration-system](configuration-system.md)

## Provider Hapi

La configuration précédente ne fait que la configuration système de stockage et de recherche de afas. Pour exposer l'api
fhir en http, nous utilisons Hapi qui s'occupera de la partie controller.

Créez une classe qui implémente IResourceProvider et optionnellement étends AsBaseResourceProvider (l'extension va
faciliter certaines opérations).

Voici un exemple avec la ressource Device R4:

``` java
@Component
public class DeviceProvider extends AsBaseResourceProvider implements IResourceProvider {

    /**
     * The expression factory
     */
    @Autowired
    ExpressionFactory<?> expressionFactory;

    @Autowired
    ExpressionSerializer expressionSerializer;

    /**
     * The encrypter for urls
     */
    @Autowired
    SerializeUrlEncrypter serializeUrlEncrypter;

    /**
     * Construct the base provider
     * * @param fhirStoreService the service that store fhir resources
     */
    @Autowired
    protected DeviceProvider(FhirStoreService<?> fhirStoreService, ExpressionFactory<?> expressionFactory) {
        super("Device", fhirStoreService);
        this.expressionFactory = expressionFactory;
    }

    /**
    * The search operation
    */
    @Search()
    public IBundleProvider search(@Count Integer theCount,
                                  @Description(shortDefinition = "Recherche sur l'identifiant de l'équipement matériel lourd")
                                  @OptionalParam(name = Device.SP_IDENTIFIER)
                                  TokenAndListParam theIdentifier,
                                  @Description(shortDefinition = "The device name")
                                  @OptionalParam(name = Device.SP_DEVICE_NAME)
                                  StringAndListParam theName) {

        var selectExpression = new SelectExpression<>(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME, expressionFactory);
        selectExpression.setCount(theCount);
        selectExpression.fromFhirParams(FhirSearchPath.builder().resource(FhirServerConstants.DEVICE_FHIR_RESOURCE_NAME).path(Device.SP_IDENTIFIER).build(), theIdentifier);
        return new AfasBundleProvider<>(fhirStoreService, expressionSerializer, selectExpression, serializeUrlEncrypter);
    }

    /**
    * The Fhir resource class
    **/
    @Override
    public Class<Device> getResourceType() {
        return Device.class;
    }
}
```

Si tout ce passe bien, vous devriez avoir en appelant l'url `http://localhost:8080/fhir/v1` une réponse valide (sans
résultats de recherche) :

``` json
{
  "resourceType": "Bundle",
  "id": "229bd4ad-6bee-4874-a065-3403f2fe06e8",
  "meta": {
    "lastUpdated": "2022-08-11T15:14:59.515+02:00"
  },
  "type": "searchset",
  "total": 0,
  "link": [ {
    "relation": "self",
    "url": "http://iris-dp-server:8080/fhir/v1/Organization?_format=json&_pretty=true"
  }],
  "entry": [ {
  }]
 }
```

# Procédure d’ajout d’un nouveau champ de recherche

## Contexte

L’exposition et le parsing des paramètres sont gérés par la librairie HAPI FHIR. Le stockage et le moteur de recherche
sont gérés par l’implémentation spécifique du projet.

Afin d’ajouter un nouveau paramètre de recherche il faut donc intervenir sur ces 2 parties.

## Ajout du paramètre pour l’exposition et le parsing

La configuration des paramètres fhir HAPI se situe dans le projet : « modules-as/fhir-server ».

Il faut ajouter le paramètre sur la méthode annotée avec @Search() de la classe qui implémente
ca.uhn.fhir.rest.server.IResourceProvider pour la ressource FHIR souhaitée.

Par exemple pour Organization il s’agit de la classe com.synodis.fhir_server.provider.RassOrganizationProvider du projet
fhir-server.

Ajoutons un paramètre de type string avec pour nom « demoParam » :

``` java
@Search()
public IBundleProvider search(
        @Description(shortDefinition = "A sample parameter")
        @OptionalParam(name = "demoParam")
                StringAndListParam demoParam,
```

Plus de documentation sur cette partie se retrouve sur le site officiel de Hapi qui est la librairie utilisée :
[Hapi FHir](https://hapifhir.io/hapi-fhir/docs/server_plain/resource_providers.html)

Cela va avoir pour action de déclarer le paramètre dans le CapabilityStatement du serveur « /fhir/metadata?_
format=json&_pretty=true » :

``` json
{
  "resourceType": "CapabilityStatement",
  "id": "d031dee3-c6e0-44b5-ad74-dd9d93461716",
  "name": "RestServer",
  "status": "active",
  "date": "2022-08-11T14:57:05.515+02:00",
  "publisher": "Not provided",
  "kind": "instance",
  "software": {
    "name": "Afas Fhir server",
    "version": "V1-R4"
  },
  "implementation": {
    "description": "Afas data",
    "url": "http://iris-dp-server:8080/fhir/v1"
  },
  "fhirVersion": "4.0.1",
  "format": [ "application/fhir+xml", "xml", "application/fhir+json", "json" ],
  "rest": [ {
    "mode": "server",
    "resource": [ {
      "type": "Organization",
      "profile": "https://annuaire.sante.gouv.fr/fhir/StructureDefinition/AS-Organization",
      "searchParam": [{
        "name": "demoParam",
        "type": "string",
        "documentation": "A sample parameter"
      }
      ...
      
```

Une fois le paramètre récupéré, il faut le transmettre au système de requête. Cela se fait toujours dans la méthode
précédente :

``` java
//… Autres paramètres …

selectExpression.fromFhirParams(FhirSearchPath.builder().resource("Organization").path("demoParam").build(), demoParam); 
//… Autres paramètres …

return new RassBundleProvider<T>(fhirStoreService, selectExpression);
```

La méthode “SelectExpression.fromFhirParams” permet d’ajouter à la requête select le nouveau paramètre.

Le premier paramètre de cette méthode est un FhirSearchPath qui permet de définir la ressource sur laquelle appliquer le
paramètre par rapport à la configuration du moteur de recherche.

Il faudra ajouter une configuration dans le moteur de recherche correspondant à ce paramètre.

## Configuration du moteur de recherche

Il existe 2 possibilités de faire la configuration : Java ou yaml. La finalité est la même.

En java, pour ajouter un paramètre, il faut ajouter le paramètre dans la configuration du bean qui implémente "
fr.ans.afas.fhirserver.search.config.domain.ServerSearchConfig" :

``` java
var listRassOrg = new ArrayList<SearchConfigElement>();
// other params...
params.add(SearchParamConfig.builder().name("demoParam").urlParameter("demoParam").searchType(StorageConstants.INDEX_TYPE_STRING).description("").indexName("t_demoParam").resourcePaths(List.of(ResourcePathConfig.builder().path("demoParam").build())).build());
configs.put("Organization", listRassOrg);
```

En yaml la configuration équivalente est :

``` yaml
fhir:
  validationMode: strict
  resources:
    - name: Organization
      profile: http://hl7.org/fhir/StructureDefinition/Organization
      searchParams:
        # other params...
        - name: demoParam
          urlParameter: demoParam
          searchType: string
          description: A demo parameter
          resourcePaths:
            - path: demoParam
          indexName: t_demoParam
```

Plus de détail sur la configuration dans la section [configuration-system](configuration-system.md)
