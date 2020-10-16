package net.coding.demon.lucene.tests;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.MMapDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class DocValuesTest {
    public static final String DOC_SCORE_FIELD = "custom_score";
    public static final String TITLE_FIELD = "title";
    IndexWriter indexWriter;
    Path indexLocation;

    @BeforeEach
    // create the indexer
    public void setup() throws IOException {
        indexLocation = Files.createTempDirectory(null);
        MMapDirectory mMapDirectory = new MMapDirectory(indexLocation);

        IndexWriterConfig iwc = new IndexWriterConfig();
        // add debug print stream for getting indexer logs -- output is too verbose
//        iwc.setInfoStream(new PrintStreamInfoStream(System.out));
        indexWriter = new IndexWriter(mMapDirectory, iwc);
    }

    @Test
    public void testStoreAndFetchDocValues() throws IOException {
//        FieldType fieldType = new FieldType();
//        DocValuesType docValueType = DocValuesType.NUMERIC;
//        fieldType.setDocValuesType(docValueType);

        indexDoc("new movie", 1);
        indexDoc("new movie part2", 3);
        indexDoc("new movie part3", 2);
        indexWriter.commit();

        DirectoryReader indexReader = DirectoryReader.open(indexWriter);

        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        final TermQuery termQuery = new TermQuery(new Term("title", "movie"));
        final TopDocs topDocs = indexSearcher.search(termQuery, 10);

        System.out.println("topDocs.scoreDocs: " + Arrays.toString(topDocs.scoreDocs));
        System.out.println("topDocs.totalHits: " + topDocs.totalHits);
        printDocs(indexSearcher, topDocs);

        // Using custom sorting on the doc values field.
        final TopFieldDocs topDocsSorted = indexSearcher.search(termQuery, 10, new Sort(new SortField(DOC_SCORE_FIELD, SortField.Type.INT)));
        System.out.println("topDocsSorted.scoreDocs: " + Arrays.toString(topDocsSorted.scoreDocs));
        System.out.println("topDocsSorted.totalHits: " + topDocsSorted.totalHits);
        printDocs(indexSearcher, topDocsSorted);

        System.out.println("Iterate through doc values.");
        interateDocValues(indexReader);
        System.out.println("Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));

        for ( int i = 0 ; i < 10 ; i++) {
            indexDoc("Movie " + i, i);
        }
        indexWriter.commit();

        indexReader = DirectoryReader.openIfChanged(indexReader);

        System.out.println("Iterate through doc values after adding more docs");
        interateDocValues(indexReader);
        System.out.println("Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));

        indexReader.close();
    }

    private void interateDocValues(IndexReader indexReader) throws IOException {
        // iterate through the doc values
        for (LeafReaderContext context : indexReader.leaves()) {
            System.out.println("Starting new leaf reader");
            LeafReader atomicReader = context.reader();
            final NumericDocValues numericDocValues = DocValues.getNumeric(atomicReader, DOC_SCORE_FIELD);
            while (numericDocValues.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
                System.out.println("Doc value: " + numericDocValues.longValue());
            }
        }
    }

    private void indexDoc(String s, int i) throws IOException {
        Document doc = new Document();
        TextField title = new TextField(TITLE_FIELD, s, Field.Store.YES);
        NumericDocValuesField score = new NumericDocValuesField(DOC_SCORE_FIELD, i);
        doc.add(title);
        doc.add(score);
        indexWriter.addDocument(doc);
    }

    private void printDocs(IndexSearcher indexSearcher, TopDocs topDocsSorted) throws IOException {
        System.out.println("Printing topDocs : ");
        for (ScoreDoc scoreDoc : topDocsSorted.scoreDocs) {
            System.out.println(indexSearcher.doc(scoreDoc.doc));
        }
    }

    @AfterEach
    public void tearDown() throws IOException {
        indexWriter.close();
    }
}
