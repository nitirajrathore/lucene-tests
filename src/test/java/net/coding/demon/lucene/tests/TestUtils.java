package net.coding.demon.lucene.tests;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class TestUtils {

    public static void listFiles(Path indexLocation) {
        System.out.println("After IndexWriter. commit() : Files created in the index folder : " + Arrays.toString(indexLocation.toFile().list()));
    }

    public static void catFiles(Path folder) throws IOException {
        for (String filePath : folder.toFile().list()) {
            System.out.println("\n---------- Start Contents of " + filePath + " ----------" );
            System.out.println(FileUtils.readFileToString(folder.resolve(filePath).toFile(), "UTF-8"));
            System.out.println("---------- End Contents of " + filePath + " ----------\n" );
        }
    }
}
