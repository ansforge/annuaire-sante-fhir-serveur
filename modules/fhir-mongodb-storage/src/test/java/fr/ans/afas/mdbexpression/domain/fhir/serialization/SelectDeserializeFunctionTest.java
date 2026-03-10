package fr.ans.afas.mdbexpression.domain.fhir.serialization;

import fr.ans.afas.exception.SerializationException;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.expression.*;
import fr.ans.afas.fhirserver.search.expression.serialization.ExpressionSerializer;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelectDeserializeFunctionTest {

    private SelectDeserializeFunction selectDeserializeFunction;
    private SearchConfigService mockSearchConfigService;
    private ExpressionFactory<Bson> mockExpressionFactory;
    private ExpressionSerializer<Bson> mockExpressionSerializer;

    @BeforeEach
    void setUp() {
        selectDeserializeFunction = new SelectDeserializeFunction();
        mockSearchConfigService = mock(SearchConfigService.class);
        mockExpressionFactory = mock(ExpressionFactory.class);
        mockExpressionSerializer = mock(ExpressionSerializer.class);
    }

    @Test
    void testProcessValidInput() {
        String validInput = "ResourceType$10$expression$include:field1|field2$revinclude:field3|field4$has";

        // Créez des instances réelles des classes nécessaires
        ContainerExpression<Bson> realContainerExpression = new ContainerExpression<>() {
            @Override
            public Bson interpreter(ExpressionContext expressionContext) {
                return null;
            }

            @Override
            public String serialize(ExpressionSerializer<Bson> expressionSerializer) {
                return "";
            }

            @Override
            public Expression<Bson> deserialize(ExpressionSerializer<Bson> expressionDeserializer) {
                return null;
            }

            @Override
            public ContainerExpression<Bson> addExpression(Expression<Bson> expression) {
                return null;
            }

            @Override
            public List<Expression<Bson>> getExpressions() {
                return List.of();
            }
        };
        HasCondition<Bson> realHasCondition = new HasCondition<>(null);

        // Configurez les retours des méthodes mockées
        when(mockExpressionSerializer.deserialize("expression")).thenReturn(realContainerExpression);
        when(mockExpressionSerializer.deserialize("has")).thenReturn(realHasCondition);

        Expression<Bson> result = selectDeserializeFunction.process(
                mockSearchConfigService,
                mockExpressionFactory,
                mockExpressionSerializer,
                validInput
        );

        assertNotNull(result);
        assertTrue(result instanceof SelectExpression);
    }
    @Test
    void testProcessEmptyInput() {
        String emptyInput = "";

        SerializationException exception = assertThrows(SerializationException.class, () -> {
            selectDeserializeFunction.process(
                    mockSearchConfigService,
                    mockExpressionFactory,
                    mockExpressionSerializer,
                    emptyInput
            );
        });

        assertEquals("Error during the Select deserialization. 6 parameters wanted. 1 found. Params: " + emptyInput, exception.getMessage());
    }

    @Test
    void testProcessInvalidInput() {
        String invalidInput = "ResourceType$10$expression$include$revinclude";

        SerializationException exception = assertThrows(SerializationException.class, () -> {
            selectDeserializeFunction.process(
                    mockSearchConfigService,
                    mockExpressionFactory,
                    mockExpressionSerializer,
                    invalidInput
            );
        });

        assertEquals("Error during the Select deserialization. 6 parameters wanted. 5 found. Params: " + invalidInput, exception.getMessage());
    }


    @Test
    void testDeserializeIncludeMalformedInput() {
        String malformedInclude = "field1|field2|field3"; // Nombre impair de parties

        // Configurez les mocks pour retourner des objets valides
        ContainerExpression<Bson> mockContainerExpression = mock(ContainerExpression.class);
        HasCondition<Bson> mockHasCondition = mock(HasCondition.class);

        when(mockExpressionSerializer.deserialize("expression")).thenReturn(mockContainerExpression);
        when(mockExpressionSerializer.deserialize("has")).thenReturn(mockHasCondition);

        SerializationException exception = assertThrows(SerializationException.class, () -> {
            selectDeserializeFunction.process(
                    mockSearchConfigService,
                    mockExpressionFactory,
                    mockExpressionSerializer,
                    "ResourceType$10$expression$" + malformedInclude + "$revinclude$has"
            );
        });

        assertEquals("Error during the Select deserialization. (Rev)include not well formatted.", exception.getMessage());
    }
}