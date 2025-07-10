package fr.ans.afas.fhir.servlet.search.bundle;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import fr.ans.afas.configuration.AfasConfiguration;
import fr.ans.afas.fhir.servlet.exception.UnknownErrorWritingResponse;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.fhirserver.service.FhirPageIterator;
import fr.ans.afas.fhirserver.service.FhirServerContext;
import fr.ans.afas.fhirserver.service.exception.CantWriteFhirResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletOutputStream;
import java.io.IOException;

class AbstractFhirBundleWriteListenerTest {

    @Mock
    private AfasConfiguration afasConfiguration;

    @Mock
    private ServletOutputStream output;

    @Mock
    private AsyncContext asyncContext;

    @Mock
    private FhirPageIterator fhirPageIterator;


    private AbstractFhirBundleWriteListener<Object> listener;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Crear una clase anónima que extiende AbstractFhirBundleWriteListener
        listener = new AbstractFhirBundleWriteListener<Object>(mock(FhirServerContext.class), afasConfiguration, output, asyncContext, mock(SelectExpression.class), fhirPageIterator) {
            @Override
            public void onWritePossibleInTenant() {
                // Puedes dejar la implementación vacía si no es necesaria
                try {
                    super.onWritePossibleInTenant();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }


    @Test
    void testWriteEntriesWithException() {
        // Simular que fhirPageIterator.hasNext() lanza una excepción
        when(fhirPageIterator.hasNext()).thenThrow(new RuntimeException("Simulated Exception"));

        // Ejecutar el método writeEntries()
        assertThrows(UnknownErrorWritingResponse.class, () -> {
            listener.writeEntries();
        });

        // Verificar que el log del error fue registrado y que se manejó la excepción
        verify(asyncContext, times(1)).complete();  // Asegurarse de que el contexto se complete si se lanza una excepción
    }

    @Test
    void testWriteIteratorEntriesWithException() {
        // Simular que fhirPageIterator.next() lanza una excepción
        when(fhirPageIterator.next()).thenThrow(new RuntimeException("Simulated Exception"));

        // Ejecutar el método writeIteratorEntries()
        assertThrows(CantWriteFhirResource.class, () -> {
            listener.writeIteratorEntries();
        });

        // Verificar que el log del error fue registrado y que la excepción fue lanzada
        verify(asyncContext, times(1)).complete();
    }


}
