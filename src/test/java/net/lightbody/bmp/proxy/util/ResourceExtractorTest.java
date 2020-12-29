package net.lightbody.bmp.proxy.util;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ResourceExtractorTest extends TestCase {

    @Test
    public void testLoadIllegalFile() throws IOException {
        File root = File.createTempFile("seleniumSslSupport", "127.0.0.1");
        root.delete();
        root.mkdirs();

        try {
            ResourceExtractor.extractResourcePath(getClass(), "/somethingDoesNotExit", root);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Resource not found"));
            return;
        }
        fail();
    }

}