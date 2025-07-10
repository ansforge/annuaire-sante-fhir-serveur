package fr.ans.afas.rass.service.impl;

import com.mongodb.client.MongoCursor;
import fr.ans.afas.domain.FhirBundleBuilder;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.data.SearchContext;
import fr.ans.afas.fhirserver.search.expression.IncludeExpression;
import fr.ans.afas.fhirserver.search.expression.SelectExpression;
import fr.ans.afas.mdbexpression.domain.fhir.MongoDbIncludeExpression;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultFhirPageIteratorTest {

    @Mock
    private SearchConfigService searchConfigService;

    @Mock
    private MongoCursor<Document> cursor;

    @Mock
    private SelectExpression<Bson> selectExpression;

    private Long[] total;
    private long searchRevision;
    private Set<String> elements;
    private DefaultFhirPageIterator fhirPageIterator;

    @BeforeEach
    public void setUp() {
        total = new Long[]{10L};
        searchRevision = 1L;
        elements = new HashSet<>(Collections.singletonList("element1"));
        fhirPageIterator = new DefaultFhirPageIterator(searchConfigService, cursor, selectExpression, total, searchRevision, elements);
    }

    @Test
    void testHasNext_WhenCursorHasNextAndIndexIsLessThanCount_ReturnsTrue() {
        when(cursor.hasNext()).thenReturn(true);
        when(selectExpression.getCount()).thenReturn(5);

        assertTrue(fhirPageIterator.hasNext());
    }

    @Test
    void testHasNext_WhenCursorDoesNotHaveNext_ReturnsFalse() {
        when(cursor.hasNext()).thenReturn(false);
        assertFalse(fhirPageIterator.hasNext());
    }

    @Test
    void testSearchContext_ReturnsCorrectContext() {
        SearchContext context = fhirPageIterator.searchContext();

        assertEquals(total[0], context.getTotal());
        assertEquals("", context.getFirstId());
        assertEquals(searchRevision, context.getRevision());
    }

    @Test
    void testNext_WhenCursorHasNext_ReturnsBundleEntry() {
        Document document = new Document().append("t_id", "testId")
                .append(MongoQueryUtils.ID_ATTRIBUTE, new ObjectId())
                .append("fhir", new Document("meta", new Document()));
        when(cursor.next()).thenReturn(document);
        when(cursor.hasNext()).thenReturn(true);
        when(selectExpression.getFhirResource()).thenReturn("resource");
        when(selectExpression.getRevincludes()).thenReturn(new HashSet<>());

        FhirBundleBuilder.BundleEntry entry = fhirPageIterator.next();

        assertNotNull(entry);
    }

    @Test
    void testNext_WhenRevincludesArePresent_AddsRevIncludeId() {
        Document document = new Document()
                .append("t_fid", "testId")
                .append(MongoQueryUtils.ID_ATTRIBUTE, new ObjectId())
                .append("fhir", new Document("meta", new Document()));
        Set<IncludeExpression<Bson>> revincludes = new HashSet<>();
        revincludes.add(new MongoDbIncludeExpression(searchConfigService, "type", "name"));
        when(cursor.next()).thenReturn(document);
        when(cursor.hasNext()).thenReturn(true);
        when(selectExpression.getFhirResource()).thenReturn("resource");
        when(selectExpression.getRevincludes()).thenReturn(revincludes);

        fhirPageIterator.next();

        assertTrue(fhirPageIterator.getRevIncludeIds().contains("testId"));
    }

    @Test
    void testClose_ClosesCursor() {
        fhirPageIterator.close();

        verify(cursor).close();
    }

    @Test
    void testHasNextPage_WhenHasNextPageIsTrue_ReturnsTrue() {
        fhirPageIterator.hasNextPage = true;

        assertTrue(fhirPageIterator.hasNextPage());
    }

    @Test
    void testHasNextPage_WhenHasNextPageIsFalse_ReturnsFalse() {
        fhirPageIterator.hasNextPage = false;

        assertFalse(fhirPageIterator.hasNextPage());
    }

    @Test
    void testClearIncludesTypeReference_ClearsMap() {
        fhirPageIterator.clearIncludesTypeReference();
        assertTrue(fhirPageIterator.getIncludesTypeReference().isEmpty());
    }

    @Test
    void testClearRevIncludeIds_ClearsSet() {
        fhirPageIterator.getRevIncludeIds().add("testId");
        fhirPageIterator.clearRevIncludeIds();
        assertTrue(fhirPageIterator.getRevIncludeIds().isEmpty());
    }

    @Test
    void testNext_WhenElementsAreEmpty_DoesNotAddMetaTag() {
        fhirPageIterator = new DefaultFhirPageIterator(searchConfigService, cursor, selectExpression, total, searchRevision, Collections.emptySet());
        Document document = new Document().append("t_id", "testId")
                .append(MongoQueryUtils.ID_ATTRIBUTE, new ObjectId())
                .append("fhir", new Document());

        when(cursor.next()).thenReturn(document);
        when(cursor.hasNext()).thenReturn(true);
        when(selectExpression.getFhirResource()).thenReturn("resource");

        fhirPageIterator.next();

        assertNull(document.get("meta"));
    }
}