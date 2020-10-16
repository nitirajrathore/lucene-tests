package net.coding.demon.lucene.tests;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.store.MMapDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SimpleIndexSearchTest {
    IndexWriter indexWriter;
    Path indexLocation;

    @BeforeEach
    // create the indexer
    public void setup() throws IOException {
        // * @deprecated This class uses inefficient synchronization and is discouraged
        // * in favor of {@link MMapDirectory}. It will be removed in future versions
        // * of Lucene.
        // Directory directory = new RAMDirectory();
        // A straightforward implementation of FSDirectory using java.io.RandomAccessFile. However, this class has poor concurrent performance (multiple threads will bottleneck) as it synchronizes when multiple threads read from the same file.
        // Directory directory = new RAFDirectory();
        // An FSDirectory implementation that uses java.nio's FileChannel's positional read, which allows multiple threads to read from the same file without synchronizing.
        // Directory directory = new NIOFSDirectory();
        indexLocation = Files.createTempDirectory(null);
        MMapDirectory mMapDirectory = new MMapDirectory(indexLocation);

        IndexWriterConfig iwc = new IndexWriterConfig();
        indexWriter = new IndexWriter(mMapDirectory, iwc);
    }

    // Test simple indexing and then searching
    @Test
    public void simpleIndexAndSearchTest() throws IOException {
        // create document
        Document document = new Document();
        String titleId = "1";
        String title = "lucene test";
        String description = "This is simple doc";
        // StringField are indexed as it is without analysing
        document.add(new StringField("titleId", titleId, Field.Store.YES));
        // NOTE StringField are not analyzed so using TextField
        document.add(new TextField("title", title, Field.Store.YES));
        document.add(new TextField("description", description, Field.Store.NO));

        // index document
        indexWriter.addDocument(document);

        // Should open / openIfChanged after writing to index. otherwise you can read only till what is present.
        IndexReader indexReader = DirectoryReader.open(indexWriter);

        // get numDocs from writer
        assertEquals(1, indexWriter.getDocStats().numDocs);
        assertEquals(1, indexWriter.getDocStats().maxDoc);
        // get numDocs from reader
        assertEquals(1, indexReader.numDocs());
        assertEquals(1, indexReader.maxDoc());

        // create searcher from index reader
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        // create a query to read all documents
        final MatchAllDocsQuery matchAllDocsQuery = new MatchAllDocsQuery();

        // search and get results in TopDocs
        final TopDocs topDocs = indexSearcher.search(matchAllDocsQuery, 10);

        // totalHits contains the estimate of results
        System.out.println("topDocs.totalHits : " + topDocs.totalHits);
        System.out.println("topDocs.scoreDocs : " + Arrays.toString(topDocs.scoreDocs));

        assertEquals(1, topDocs.totalHits.value); // one search result was returned
        assertEquals(TotalHits.Relation.EQUAL_TO, topDocs.totalHits.relation); // exactly one doc was returned

        assertEquals(1, topDocs.scoreDocs.length); // exactly one document returned
        System.out.println("topDocs.scoreDocs[0] : " + topDocs.scoreDocs[0]);

        // list the files created in the index folder
        System.out.println("Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));

        final long commitPoint = indexWriter.commit();

        System.out.println("commitPoint : " + commitPoint);

        // this should have created a segments file
        System.out.println("Files created in the index folder after index commit : " + Arrays.toString(indexLocation.toFile().list()));

        // this should return -1 as I just committed above
        final long anotherCommitPoint = indexWriter.commit();
        assertEquals(-1, anotherCommitPoint);

        // score doc contains the doc id and score
        final int docId = topDocs.scoreDocs[0].doc;
        // fetch the document and read contents
        final Document resultDoc = indexReader.document(docId);

        System.out.println("resultDoc : " + resultDoc);
        // returns the title as it was stored
        assertEquals(title, resultDoc.get("title"));
        // description was not stored.
        assertNull(resultDoc.get("description"));


        // delete the doc
        indexWriter.deleteAll();
        // maxDoc will be 0, but cannot trust the value of numDocs
        assertEquals(0, indexWriter.getDocStats().maxDoc);
        System.out.println("indexWriter.getDocStats().numDocs : " + indexWriter.getDocStats().numDocs);

        // as the reader is on older view
        assertEquals(1, indexReader.numDocs());
//        assertEquals(1, indexReader.numDeletedDocs());

        IndexReader indexReader2 = DirectoryReader.open(indexWriter);

        // new reader knows that the doc was deleted.
        assertEquals(0, indexReader2.numDocs());

        indexWriter.commit();

        // after commit even the numDocs of writer can be trusted to return correct number of docs
        assertEquals(0, indexWriter.getDocStats().numDocs);

        // old indexReader still sees the snapshot of older index with one doc
        assertEquals(1, indexReader.numDocs());
        // I dont' know why this numDeletedDocs() is present as the indexReader.deleteDocuments() method is removed.
        assertEquals(0, indexReader.numDeletedDocs());

        // close reader and then dont use it again.
        indexReader.close();
    }

    @AfterEach
    public void tearDown() throws IOException {
        indexWriter.close();
    }
}
