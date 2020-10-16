package net.coding.demon.lucene.tests;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.PrintStreamInfoStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.StringTokenizer;

import static net.coding.demon.lucene.tests.TestUtils.catFiles;

public class SimpleTextCodecTest {

    IndexWriter indexWriter;
    Path indexLocation;

    @BeforeEach
    // create the indexer
    public void setup() throws IOException {
        indexLocation = Files.createTempDirectory(null);
        MMapDirectory mMapDirectory = new MMapDirectory(indexLocation);

        IndexWriterConfig iwc = new IndexWriterConfig();
        // lucene-codec jar required for this.
        final SimpleTextCodec simpleTextCodec = new SimpleTextCodec();
        iwc.setCodec(simpleTextCodec);
        // Add DEBUG print stream for getting indexer logs -- output is too verbose
        // iwc.setInfoStream(new PrintStreamInfoStream(System.out));
        indexWriter = new IndexWriter(mMapDirectory, iwc);
    }

    @Test
    public void simpleTextCodecTest() throws IOException {
        System.out.println("Before indexDoc : Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));
        indexDoc("Add DEBUG print stream for getting indexer logs -- output is too verbose.\n");
        System.out.println("After indexDoc : Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));
        indexWriter.commit();
        System.out.println("After index commit : Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));
        catFiles(indexLocation);
    }

    private void indexDoc(String contents) throws IOException {
        int noOfLines = StringUtils.countMatches(contents, '\n');
        int noOfSentences = StringUtils.countMatches(contents, '.');
        int noOfCharacters = contents.length();
        int noOfWords = new StringTokenizer(contents).countTokens();

        System.out.println("noOfWords :" + noOfWords + ", noOfCharacters : " + noOfCharacters + ", noOfSentences : " + noOfSentences + ", noOfLines : " + noOfLines);
        Document doc = new Document();
        doc.add(new TextField("contents",contents, Field.Store.NO));
        doc.add(new IntPoint("noOfLines",noOfLines));
        // Will Standard Analyzer index anything in this?
        doc.add(new TextField("noOfSentences", Integer.toString(noOfSentences), Field.Store.YES));
        doc.add(new NumericDocValuesField("noOfCharacters", noOfCharacters));
        doc.add(new BinaryDocValuesField("noOfWords",new BytesRef(Integer.toString(noOfWords))));

        indexWriter.addDocument(doc);
    }
}
