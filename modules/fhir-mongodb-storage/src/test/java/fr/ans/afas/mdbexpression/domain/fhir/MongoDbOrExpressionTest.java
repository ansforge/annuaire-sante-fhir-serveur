package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.expression.Expression;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class MongoDbOrExpressionTest {
    private MongoDbOrExpression orExpression = new MongoDbOrExpression();;
    @Test
    void testInterpreterWithEmptyExpressions() {

        ExpressionContext context = mock(ExpressionContext.class);

        Bson result = orExpression.interpreter(context);

        assertNull(result, "La méthode doit retourner null si la liste des expressions est vide");
    }

    @Test
    void testInterpreterWithSingleExpression() {

        Expression<Bson> mockExpression = mock(Expression.class);
        ExpressionContext context = mock(ExpressionContext.class);

        Bson mockBson = mock(Bson.class);
        when(mockExpression.interpreter(context)).thenReturn(mockBson);

        orExpression.addExpression(mockExpression);

        Bson result = orExpression.interpreter(context);

        assertEquals(mockBson, result, "La méthode doit retourner l'expression unique si elle est seule");
    }

    @Test
    void testInterpreterWithMultipleExpressions() {

        Expression<Bson> mockExpression1 = mock(Expression.class);
        Expression<Bson> mockExpression2 = mock(Expression.class);
        ExpressionContext context = mock(ExpressionContext.class);

        Bson mockBson1 = mock(Bson.class);
        Bson mockBson2 = mock(Bson.class);
        when(mockExpression1.interpreter(context)).thenReturn(mockBson1);
        when(mockExpression2.interpreter(context)).thenReturn(mockBson2);

        orExpression.addExpression(mockExpression1);
        orExpression.addExpression(mockExpression2);

        Bson result = orExpression.interpreter(context);

        assertNotNull(result, "La méthode ne doit pas retourner null si plusieurs expressions sont présentes");
        assertTrue(result instanceof Bson, "Le résultat doit être une instance de Bson");
        // Vérification que Filters.or est utilisé
        assertEquals(Filters.or(List.of(mockBson1, mockBson2)).toBsonDocument(), result.toBsonDocument());
    }

    @Test
    void testSerialize() {


        // Mock du sérialiseur
        ExpressionSerializer<Bson> serializer = mock(ExpressionSerializer.class);
        String expectedSerializedValue = "serializedExpression";
        when(serializer.serialize(orExpression)).thenReturn(expectedSerializedValue);

        // Appel de la méthode serialize
        String result = orExpression.serialize(serializer);

        // Vérifications
        assertNotNull(result, "La méthode serialize ne doit pas retourner null");
        assertEquals(expectedSerializedValue, result, "La méthode serialize doit retourner la valeur attendue");
        verify(serializer, times(1)).serialize(orExpression);
    }

    @Test
    void testDeserialize() {

        ExpressionSerializer<Bson> serializer = mock(ExpressionSerializer.class);

        // Appel de la méthode à tester
        Expression<Bson> result = orExpression.deserialize(serializer);

        // Vérification
        assertNull(result, "La méthode deserialize devrait retourner null.");
    }

}