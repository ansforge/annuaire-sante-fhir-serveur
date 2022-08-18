# Backup & Restore

Toutes les données de l'application sont stockées dans la base de données (hormis les livrables et configuration).

Pour sauvegarder l'application, il suffit de sauvegarder la base de données

Le backup et la restauration de mongodb se font généralement avec les
outils [mongodump / mongorestore](https://www.mongodb.com/docs/database-tools/mongodump/).

Référez-vous à la documentation qui prendra en compte votre système (monoinstance, cluster...).

# Export Data only

Vous pouvez aussi choisir d'exporter les données uniquement par exemple a des fins de copie de certaines entités fhirs
seulement.

Pour ce faire on préfèrera la commande mongoexport.

```
mongoexport –db [dbname] --collection [FhirResource] --out [FhirResource].json
```

Si la base de données à des login/mot de passe, il est possible d’utiliser les paramètres --username et --password.

Exemple si vous avez 5 ressources :

```bash
mongoexport --db afas --collection Organization --out Organization.json
mongoexport --db afas --collection Device --out Device.json
mongoexport --db afas --collection Practitioner --out Practitioner.json
mongoexport --db afas --collection HealthcareService --out HealthcareService.json
mongoexport --db afas --collection PractitionerRole --out PractitionerRole.json
```

Plus d’informations sur
mongoexport : [https://www.mongodb.com/docs/database-tools/mongoexport/](https://www.mongodb.com/docs/database-tools/mongoexport/)

# Restore Data only

## Si vous avez des données

Si vous avez des données et que vous souhaitez une restauration complète, il faut supprimer les données de mongodb déjà
présente.

Le plus simple pour supprimer une collection est la commande suivante :

```bash
mongosh mongodb://username:password@127.0.0.1/dbname
db.Organization.remove({})
db.Device.remove({})
... Continuez pour chacune des ressources FHIR
```

Vous pouvez aussi utiliser des outils graphiques comme par exemple Compass

## Insertion des données du backup

Il est possible d’utiliser l’outil de mongodb : mongoimport.

La commande est la suivante :

```bash
mongoimport –db [dbname] --collection [FhirResource] --file [FhirResource].json
```

Si la base de données à des login/mot de passe, il est possible d’utiliser les paramètres --username et --password.

Exemple si vous avez 5 ressources :

```bash
mongoimport --db afas --collection Organization --file Organization.json
mongoimport --db afas --collection Device --file Device.json
mongoimport --db afas --collection Practitioner --file Practitioner.json
mongoimport --db afas --collection HealthcareService --file HealthcareService.json
mongoimport --db afas --collection PractitionerRole --file PractitionerRole.json
```

Plus d’informations sur
mongoimport : [https://docs.mongodb.com/database-tools/mongoimport/](https://docs.mongodb.com/database-tools/mongoimport/)


