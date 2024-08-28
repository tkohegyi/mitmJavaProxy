package net.lightbody.bmp.proxy.selenium;

import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Methods for creating certificates.
 * <p>
 * ***************************************************************************************
 * Copyright (c) 2007, Information Security Partners, LLC
 * All rights reserved.
 * <p>
 * In a special exception, Selenium/OpenQA is allowed to use this code under the Apache License 2.0.
 *
 * @author Brad Hill
 */
public class CertificateCreator {

    /**
     * The default key generation algorithm for this package is RSA.
     */
    public static final String KEYGEN_ALGO = "RSA";
    /**
     * The default sign algorithm for this package is SHA1 with RSA.
     */
    public static final String SIGN_ALGO = "SHA256withRSA";  //was SHA1withRSA --hmm maybe SHA256WithRSAEncryption  ???
    /**
     * X.509 OID for Subject Key Identifier Extension - Replaced when duplicating a cert.
     */
    public static final String OID_SUBJECT_KEY_IDENTIFIER = "2.5.29.14";
    /**
     * X.509 OID for Subject Authority Key Identifier - Replaced when duplicating a cert.
     */
    public static final String OID_AUTHORITY_KEY_IDENTIFIER = "2.5.29.35";
    /**
     * X.509 OID for Issuer Alternative Name - Omitted when duplicating a cert by default.
     */
    public static final String OID_ISSUER_ALTERNATIVE_NAME = "2.5.29.8";
    /**
     * X.509 OID for Issuer Alternative Name 2 - Omitted when duplicating a cert by default.
     */
    public static final String OID_ISSUER_ALTERNATIVE_NAME_2 = "2.5.29.18";
    /**
     * X.509 OID for Certificate Revocation List Distribution Point - Omitted when duplicating a cert by default.
     */
    public static final String OID_CRL_DISTRIBUTION_POINT = "2.5.28.31";
    /**
     * X.509 OID for Authority Information Access - Omitted when duplicating a cert by default.
     */
    public static final String OID_AUTHORITY_INFO_ACCESS = "1.3.6.1.5.5.7.1.1";
    /**
     * X.509 OID for Additional CA Issuers for AIA - Omitted when duplicating a cert by default.
     */
    public static final String OID_ID_AD_CAISSUERS = "1.3.6.1.5.5.7.48.2";
    private static final HashSet<String> clientCertOidsNeverToCopy = new HashSet<>();
    private static final HashSet<String> clientCertDefaultOidsNotToCopy = new HashSet<>();

    static {
        clientCertOidsNeverToCopy.add(OID_SUBJECT_KEY_IDENTIFIER);
        clientCertOidsNeverToCopy.add(OID_AUTHORITY_KEY_IDENTIFIER);

        clientCertDefaultOidsNotToCopy.add(OID_ISSUER_ALTERNATIVE_NAME);
        clientCertDefaultOidsNotToCopy.add(OID_ISSUER_ALTERNATIVE_NAME_2);
        clientCertDefaultOidsNotToCopy.add(OID_CRL_DISTRIBUTION_POINT);
        clientCertDefaultOidsNotToCopy.add(OID_AUTHORITY_INFO_ACCESS);
    }

    /**
     * This method creates an X509v3 certificate based on an an existing certificate.
     * It attempts to create as faithful a copy of the existing certificate as possible
     * by duplicating all certificate extensions.
     * <p>
     * If you are testing an application that makes use of additional certificate
     * extensions (e.g. logotype, S/MIME capabilities) this method will preserve those
     * fields.
     * <p>
     * You may optionally include a set of OIDs not to copy from the original certificate.
     * The most common reason to do this would be to remove fields that would cause inconsistency,
     * such as Authority Info Access or Issuer Alternative Name where these are not defined for
     * the MITM authority certificate.
     * <p>
     * OIDs 2.5.29.14 : Subject Key Identifier and 2.5.29.35 : Authority Key Identifier,
     * are never copied, but generated directly based on the input keys and certificates.
     * <p>
     * You may also optionally include maps of custom extensions which will be added to or replace
     * extensions with the same OID on the original certificate for the the MITM certificate.
     * <p>
     * FUTURE WORK: JDK 1.5 is very strict in parsing extensions.  In particular, known extensions
     * that include URIs must parse to valid URIs (including URL encoding all non-valid URI characters)
     * or the extension will be rejected and not available to copy to the MITM certificate.  Will need
     * to directly extract these as ASN.1 fields and re-insert (hopefully BouncyCastle will handle them)
     *
     * @param originalCert           The original certificate to duplicate.
     * @param newPubKey              The new public key for the MITM certificate.
     * @param caCert                 The certificate of the signing authority fot the MITM certificate.
     * @param caPrivateKey           The private key of the signing authority.
     * @param extensionOidsNotToCopy An optional list of certificate extension OIDs not to copy to the MITM certificate.
     * @return The new MITM certificate.
     * @throws SignatureException
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public static X509Certificate mitmDuplicateCertificate(final X509Certificate originalCert,
                                                           final PublicKey newPubKey,
                                                           final X509Certificate caCert,
                                                           final PrivateKey caPrivateKey,
                                                           Set<String> extensionOidsNotToCopy)
            throws SignatureException, InvalidKeyException, CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, CertIOException, OperatorCreationException {
        if (extensionOidsNotToCopy == null) {
            extensionOidsNotToCopy = new HashSet<>();
        }

        // copy other extensions:
        Set<String> critExts = originalCert.getCriticalExtensionOIDs();

        // get extensions returns null, not an empty set!
        if (critExts != null) {
            throw new RuntimeException("Ups has critical extensions..."); //TODO
        }
        Set<String> nonCritExs = originalCert.getNonCriticalExtensionOIDs();

        if (nonCritExs != null) {
            throw new RuntimeException("Ups has non-critical extensions..."); //TODO
        }

        X509v3CertificateBuilder x509v3CertificateBuilder = new JcaX509v3CertificateBuilder(
                caCert.getSubjectX500Principal(),
                originalCert.getSerialNumber(),
                originalCert.getNotBefore(),
                originalCert.getNotAfter(),
                originalCert.getSubjectX500Principal(),
                newPubKey
        );
        x509v3CertificateBuilder.addExtension(
                X509Extensions.AuthorityKeyIdentifier,
                false,
                new AuthorityKeyIdentifierStructure(caCert.getPublicKey()));

        /* TODO
		x509v3CertificateBuilder.addExtension(
				X509Extensions.SubjectKeyIdentifier,
				false,
				new SubjectKeyIdentifierStructure(newPubKey));
        */

        //ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPrivateKey);
        ContentSigner signer = new JcaContentSignerBuilder(CertificateCreator.SIGN_ALGO).build(caPrivateKey); // needs to be the same as the signing cert, not the copied cert
        X509CertificateHolder certHolder = x509v3CertificateBuilder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
        cert.verify(newPubKey);
        return cert;
    }

    /**
     * Convenience method for the most common case of certificate duplication.
     * <p>
     * This method will not add any custom extensions and won't copy the extensions 2.5.29.8 : Issuer Alternative Name,
     * 2.5.29.18 : Issuer Alternative Name 2, 2.5.29.31 : CRL Distribution Point or 1.3.6.1.5.5.7.1.1 : Authority Info Access, if they are present.
     *
     * @param originalCert
     * @param newPubKey
     * @param caCert
     * @param caPrivateKey
     * @return
     * @throws SignatureException
     * @throws InvalidKeyException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public static X509Certificate mitmDuplicateCertificate(final X509Certificate originalCert,
                                                           final PublicKey newPubKey,
                                                           final X509Certificate caCert,
                                                           final PrivateKey caPrivateKey)
            throws SignatureException, InvalidKeyException, CertificateException, NoSuchAlgorithmException, NoSuchProviderException,
            CertIOException, OperatorCreationException {
        return mitmDuplicateCertificate(originalCert, newPubKey, caCert, caPrivateKey, clientCertDefaultOidsNotToCopy);
    }

    public static X509Certificate generateStdSSLServerCertificate(
            final PublicKey newPubKey,
            final X509Certificate caCert,
            final PrivateKey caPrivateKey,
            final String subject)
            throws InvalidKeyException, CertificateException, CertIOException, OperatorCreationException {

        X500Principal x500Principal = new X500Principal(subject);
        X509v3CertificateBuilder x509v3CertificateBuilder = new JcaX509v3CertificateBuilder(
                caCert.getSubjectX500Principal(),
                // This is not a secure serial number generator, (duh!) but it's good enough for our purposes.
                new BigInteger(Long.toString(System.currentTimeMillis())),
                new Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24 * 30 * 12),
                new Date(System.currentTimeMillis() + 30L * 60 * 60 * 24 * 30 * 12),
                x500Principal,
                newPubKey
        );
        x509v3CertificateBuilder.addExtension(
                X509Extensions.BasicConstraints,
                true,
                new BasicConstraints(false));
        x509v3CertificateBuilder.addExtension(
                X509Extensions.AuthorityKeyIdentifier,
                false,
                new AuthorityKeyIdentifierStructure(caCert.getPublicKey()));

        GeneralNames subjectAltName = new GeneralNames(new GeneralName(GeneralName.dNSName, getCn(x500Principal)));
        x509v3CertificateBuilder.addExtension(X509Extensions.SubjectAlternativeName, false, subjectAltName);

        /*
                //TODO
//		x509v3CertificateBuilder.addExtension(
//				X509Extensions.SubjectKeyIdentifier,
//				false,
//				new SubjectKeyIdentifierStructure(newPubKey));

        //TODO
//		DEREncodableVector typicalSSLServerExtendedKeyUsages = new DEREncodableVector();

//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.serverAuth));
//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.clientAuth));
//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.netscapeServerGatedCrypto));
//		typicalSSLServerExtendedKeyUsages.add(new DERObjectIdentifier(ExtendedKeyUsageConstants.msServerGatedCrypto));

//		x509v3CertificateBuilder.addExtension(
//				X509Extensions.ExtendedKeyUsage,
//				false,
//				new DERSequence(typicalSSLServerExtendedKeyUsages));

//  Disabled by default.  Left in comments in case this is desired.
//
//		x509v3CertificateBuilder.addExtension(
//				X509Extensions.AuthorityInfoAccess,
//				false,
//				new AuthorityInformationAccess(new DERObjectIdentifier(OID_ID_AD_CAISSUERS),
//						new GeneralName(GeneralName.uniformResourceIdentifier, "http://" + subject + "/aia")));

//		x509v3CertificateBuilder.addExtension(
//				X509Extensions.CRLDistributionPoints,
//				false,
//				new CRLDistPoint(new DistributionPoint[] {}));
         */
//        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(caPrivateKey);
        ContentSigner signer = new JcaContentSignerBuilder(CertificateCreator.SIGN_ALGO).build(caPrivateKey);
        X509CertificateHolder certHolder = x509v3CertificateBuilder.build(signer);
        X509Certificate newCert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
        //newCert.verify(newPubKey, "BC"); //- seems this throws ex always.... ????
        return newCert;
    }

    private static String getCn(X500Principal x500Principal) {
        X500Name x500Name = new X500Name(x500Principal.getName());
        RDN[] rdns = x500Name.getRDNs(BCStyle.CN);
        List<String> names = new ArrayList<>();
        for (RDN rdn : rdns) {
            String name = IETFUtils.valueToString(rdn.getFirst().getValue());
            names.add(name);
        }
        return names.getFirst();
    }

}
