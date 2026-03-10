/**
 * (c) Copyright 1998-2024, ANS. All rights reserved.
 */
package fr.ans.afas.service;


import ca.uhn.fhir.context.FhirContext;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.ExpressionFactory;
import fr.ans.afas.fhirserver.search.expression.HasCondition;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.test.unit.WithMongoTest;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbChainedReferenceExpression;
import fr.ans.afas.rass.service.MongoDbFhirService;
import fr.ans.afas.rass.service.MongoMultiTenantService;
import fr.ans.afas.rass.service.impl.DefaultIndexService;
import fr.ans.afas.rass.service.json.GenericSerializer;
import org.bson.conversions.Bson;
import org.hl7.fhir.r4.model.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;
import java.util.List;


/**
 * Test FHIR _has expressions
 *
 * @author Guillaume Poulériguen
 * @since 1.0.0
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestFhirApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WithMongoTest.PropertyOverrideContextInitializer.class})

public class ChainedReferenceExpressionTestIT {

    @Inject
    MongoDbFhirService mongoDbFhirService;


    /**
     * The expression factory
     */
    @Inject
    ExpressionFactory<Bson> expressionFactory;


    @Inject
    SearchConfigService searchConfigService;

    @Inject
    MongoMultiTenantService multiTenantService;

    @Before
    public void init() {
        mongoDbFhirService.deleteAll();
        this.initData();
    }


    @Test
    public void testChainedReferenceExpression() {
        // On crée une expression Select sur la ressource PractitionerRole
        var selectExpression = new SelectExpression<>("PractitionerRole", expressionFactory);

        // Chemin FHIR : PractitionerRole.practitioner (champ de référence)
        var outerPath = FhirSearchPath.builder()
                .resource("PractitionerRole")
                .path("practitioner")
                .chain("name.family")
                .build();
        // Construction de la condition _has : PractitionerRole.practitioner → Practitioner.name.family = someFamily1
        var hasCondition = new HasCondition<Bson>(outerPath);

        // Création de l'expression de référence chaînée (_chained reference)
        var chainedExpr = new MongoDbChainedReferenceExpression(searchConfigService, outerPath, hasCondition);

        // On ajoute la condition _has dans l'expression Select
        selectExpression.addHasCondition(chainedExpr.getHasCondition());

        // On déclenche l’indexation si elle est requise
        indexJoins();

        // Exécution du comptage des résultats
        var count = mongoDbFhirService.count(selectExpression);
        Assert.assertEquals("Un seul résultat est attendu pour practitioner.name.family = someFamily1", Long.valueOf(3), count.getTotal());

        // Exécution de la recherche
        var results = mongoDbFhirService.search(null, selectExpression);
        Assert.assertEquals("Un seul PractitionerRole attendu", 3, results.getPage().size());

        // Vérification de l'ID retourné
        var result = results.getPage().get(0);
        Assert.assertEquals("L'identifiant doit être 'pr3'", "pr3", result.getIdElement().getIdPart());

    }


    private void initData() {
        // Device:
        var device1 = new Device();
        device1.setId("i1");
        device1.addDeviceName().setName("1234");
        var owner1 = new Reference();
        owner1.setReference("Organization/org1");
        device1.setOwner(owner1);
        this.mongoDbFhirService.store(List.of(device1), false);

        // Organization:
        var org1 = new Organization();
        org1.setId("org1");
        this.mongoDbFhirService.store(List.of(org1), false);
        var org2 = new Organization();
        org2.setId("org2");
        this.mongoDbFhirService.store(List.of(org2), false);


        // Practitioner:
        var practitioner1 = new Practitioner();
        practitioner1.setId("p1");
        practitioner1.addName().setFamily("someFamily1");
        var practitioner2 = new Practitioner();
        practitioner2.setId("p2");
        practitioner2.addName().setFamily("someFamily2");
        var practitioner3 = new Practitioner();
        practitioner3.setId("p3");
        practitioner3.addName().setFamily("someFamily3");
        this.mongoDbFhirService.store(List.of(practitioner1, practitioner2, practitioner3), false);

        // PractitionerRole:
        var practitionerRole1 = new PractitionerRole();
        practitionerRole1.setId("pr1");
        practitionerRole1.addCode().addCoding().setCode("code1");
        var r1 = new Reference();
        r1.setType("Practitioner");
        r1.setId("p1");
        r1.setReference("Practitioner/p1");
        var rO1 = new Reference();
        rO1.setType("Organization");
        rO1.setId("org1");
        rO1.setReference("Organization/org1");
        practitionerRole1.setPractitioner(r1);
        practitionerRole1.setOrganization(rO1);
        var practitionerRole2 = new PractitionerRole();
        practitionerRole2.setId("pr2");
        practitionerRole2.addCode().addCoding().setCode("code2");
        var r2 = new Reference();
        r2.setType("Practitioner");
        r2.setId("p2");
        r2.setReference("Practitioner/p2");
        practitionerRole2.setPractitioner(r2);
        var practitionerRole3 = new PractitionerRole();
        practitionerRole3.setId("pr3");
        practitionerRole3.addCode().addCoding().setCode("code3");
        practitionerRole3.setPractitioner(r1);
        this.mongoDbFhirService.store(List.of(practitionerRole1, practitionerRole2, practitionerRole3), false);

    }

    /**
     * Trigger the join indexation
     */
    private void indexJoins() {
        var genericSerializer = new GenericSerializer(searchConfigService, FhirContext.forR4());
        var defaultIndexService = new DefaultIndexService(mongoDbFhirService, expressionFactory, searchConfigService, genericSerializer);
        defaultIndexService.refreshIndexesSync(1);
    }
}
