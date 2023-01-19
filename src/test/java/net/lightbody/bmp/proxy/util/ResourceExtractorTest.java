package net.lightbody.bmp.proxy.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.fail;

public class ResourceExtractorTest {

    @Test
    public void testLoadIllegalFile() throws IOException {
        File root = File.createTempFile("seleniumSslSupport", "127.0.0.1");
        root.delete();
        root.mkdirs();

        try {
            ResourceExtractor.extractResourcePath(getClass(), "/somethingDoesNotExit", root);
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("Resource not found"));
            return;
        }
        fail();
    }

}