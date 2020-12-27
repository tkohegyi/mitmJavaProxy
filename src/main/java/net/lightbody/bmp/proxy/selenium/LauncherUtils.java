package net.lightbody.bmp.proxy.selenium;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;

import java.io.File;

public class LauncherUtils {
    public static void copyDirectory(File source, File dest) {
        Project p = new Project();
        Copy c = new Copy();
        c.setProject(p);
        c.setTodir(dest);
        FileSet fs = new FileSet();
        fs.setDir(source);
        c.addFileset(fs);
        c.execute();
    }

    /**
     * Delete a directory and all subdirectories
     */
    public static void recursivelyDeleteDir(File customProfileDir) {
        if (customProfileDir == null || !customProfileDir.exists()) {
            return;
        }
        Delete delete = new Delete();
        delete.setProject(new Project());
        delete.setDir(customProfileDir);
        delete.setFailOnError(true);
        delete.execute();
    }
}
