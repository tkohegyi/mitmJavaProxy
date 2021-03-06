// ========================================================================
// Copyright (c) 1999 Jason Gilbert
// $Id: PKCS12Import.java,v 1.4 2005/08/24 07:12:14 gregwilkins Exp $
// ========================================================================


package net.lightbody.bmp.proxy.jetty.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * This class can be used to import a key/certificate pair from a pkcs12 file
 * into a regular JKS format keystore for use with jetty and other java based
 * SSL applications, etc.
 * <PRE>
 * usage: java PKCS12Import {pkcs12file} [newjksfile]
 * </PRE>
 * <p>
 * If you don't supply newjksfile, newstore.jks will be used.  This can be an
 * existing JKS keystore.
 * <p>
 * Upon execution, you will be prompted for the password for the pkcs12 keystore
 * as well as the password for the jdk file.  After execution you should have a
 * JKS keystore file that contains the private key and certificate that were in
 * the pkcs12
 * <p>
 * You can generate a pkcs12 file from PEM encoded certificate and key files
 * using the following openssl command:
 * <PRE>
 * openssl pkcs12 -export -out keystore.pkcs12 -in www.crt -inkey www.key
 * </PRE>
 * then run:
 * <PRE>
 * java PKCS12Import keystore.pkcs12 keytore.jks
 * </PRE>
 *
 * @author Jason Gilbert &lt;jason@doozer.com&gt;
 */
public class PKCS12Import {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: java PKCS12Import {pkcs12file} [newjksfile]");
            System.exit(1);
        }

        File fileIn = new File(args[0]);
        File fileOut;
        if (args.length > 1) {
            fileOut = new File(args[1]);
        } else {
            fileOut = new File("newstore.jks");
        }

        if (!fileIn.canRead()) {
            System.err.println("Unable to access input keystore: " + fileIn.getPath());
            System.exit(2);
        }

        if (fileOut.exists() && !fileOut.canWrite()) {
            System.err.println("Output file is not writable: " + fileOut.getPath());
            System.exit(2);
        }

        KeyStore kspkcs12 = KeyStore.getInstance("pkcs12");
        KeyStore ksjks = KeyStore.getInstance("jks");

        LineNumberReader in = new LineNumberReader(new InputStreamReader(System.in));
        System.out.print("Enter input keystore passphrase: ");
        char[] inphrase = in.readLine().toCharArray();
        System.out.print("Enter output keystore passphrase: ");
        char[] outphrase = in.readLine().toCharArray();

        kspkcs12.load(new FileInputStream(fileIn), inphrase);

        ksjks.load((fileOut.exists()) ? new FileInputStream(fileOut) : null, outphrase);

        Enumeration eAliases = kspkcs12.aliases();
        int n = 0;
        while (eAliases.hasMoreElements()) {
            String strAlias = (String) eAliases.nextElement();
            System.err.println("Alias " + n++ + ": " + strAlias);

            if (kspkcs12.isKeyEntry(strAlias)) {
                System.err.println("Adding key for alias " + strAlias);
                Key key = kspkcs12.getKey(strAlias, inphrase);

                Certificate[] chain = kspkcs12.getCertificateChain(strAlias);

                ksjks.setKeyEntry(strAlias, key, outphrase, chain);
            }
        }

        OutputStream out = new FileOutputStream(fileOut);
        ksjks.store(out, outphrase);
        out.close();
    }

}

