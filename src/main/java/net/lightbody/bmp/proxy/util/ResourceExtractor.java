package net.lightbody.bmp.proxy.util;

import net.lightbody.bmp.proxy.selenium.LauncherUtils;
import org.apache.tools.ant.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ResourceExtractor {

    public static File extractResourcePath(String resourcePath, File dest) throws IOException {
        return extractResourcePath(ResourceExtractor.class, resourcePath, dest);
    }

    public static File extractResourcePath(Class cl, String resourcePath, File dest)
            throws IOException {
        boolean alwaysExtract = true;
        URL url = cl.getResource(resourcePath);
        if (url == null) {
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }
        if ("jar".equalsIgnoreCase(url.getProtocol())) {
            throw new IllegalArgumentException("In Wilma we don't extract jars inside the proxy: " + resourcePath);
        } else {
            try {
                File resourceFile = new File(new URI(url.toExternalForm()));
                if (!alwaysExtract) {
                    return resourceFile;
                }
                if (resourceFile.isDirectory()) {
                    LauncherUtils.copyDirectory(resourceFile, dest);
                } else {
                    FileUtils.getFileUtils().copyFile(resourceFile, dest);
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException("Couldn't convert URL to File:" + url, e);
            }
        }
        return dest;
    }

}
