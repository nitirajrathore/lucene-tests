package net.coding.demon.lucene.tests;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.PrintStreamInfoStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class IndexMergeTest {
    IndexWriter indexWriter;
    Path indexLocation;

    @BeforeEach
    // create the indexer
    public void setup() throws IOException {
        indexLocation = Files.createTempDirectory(null);
        MMapDirectory mMapDirectory = new MMapDirectory(indexLocation);

        IndexWriterConfig iwc = new IndexWriterConfig();
        // add debug print stream for getting indexer logs -- output is too verbose
        iwc.setInfoStream(new PrintStreamInfoStream(System.out));
        indexWriter = new IndexWriter(mMapDirectory, iwc);
    }

    // Test simple indexing and then searching
    @Test
    public void simpleIndexAndSearchTest() throws IOException {
        // list the files created in the index folder
        System.out.println("Initial Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));

        // create document
        Document document = new Document();
        String title = "lucene test";
        String description = "This is simple doc";
        document.add(new StringField("title", title, Field.Store.YES));
        document.add(new StringField("description", description, Field.Store.NO));

        // index document
        indexWriter.addDocument(document);

        // list the files created in the index folder
        System.out.println("Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));
        final long commitPoint = indexWriter.commit();
        System.out.println("commitPoint : " + commitPoint);
        // this should have created a segments file
        System.out.println("Files created in the index folder after index commit : " + Arrays.toString(indexLocation.toFile().list()));

        // create document 2
        Document document2 = new Document();
        document.add(new StringField("title", title + "2", Field.Store.YES));
        document.add(new StringField("description", description + "2", Field.Store.NO));

        indexWriter.addDocument(document2);
        System.out.println("Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));

        final long commitPoint2 = indexWriter.commit();
        System.out.println("commitPoint2 : " + commitPoint2);
        // this should have created a segments file
        System.out.println("Files created in the index folder after index commit2 : " + Arrays.toString(indexLocation.toFile().list()));

        final MergePolicy mergePolicy = indexWriter.getConfig().getMergePolicy();
        System.out.println("index merge policy : " + mergePolicy);

        // merge all segments
        indexWriter.forceMerge(1);
        System.out.println("Files in the index folder after force merge : " + Arrays.toString(indexLocation.toFile().list()));

        indexWriter.deleteUnusedFiles();
        System.out.println("Files in the index folder after deleteUnusedFiles : " + Arrays.toString(indexLocation.toFile().list()));

        // TODO : not sure how to confirm if there is exactly one segment left after merging.
    }

    public void tearDown() throws IOException {
        indexWriter.close();
    }
}
