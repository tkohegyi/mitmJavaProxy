Please see README.md (in Markdown format).

Supportive pages:
- if openssl hangs, use winpty : https://stackoverflow.com/questions/3758167/openssl-command-hangs
- to create self signed CA - https://gist.github.com/fntlnz/cf14feb5a46b2eda428e000157447309



- TODOS
We still have certificate handling issue:

javax.net.ssl|ALL|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|X509Authentication.java:241|No X.509 cert selected for EC
javax.net.ssl|WARNING|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|CertificateMessage.java:1059|Unavailable authentication scheme: ecdsa_secp256r1_sha256
javax.net.ssl|ALL|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|X509Authentication.java:241|No X.509 cert selected for EC
javax.net.ssl|WARNING|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|CertificateMessage.java:1059|Unavailable authentication scheme: ecdsa_secp384r1_sha384
javax.net.ssl|ALL|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|X509Authentication.java:241|No X.509 cert selected for EC
javax.net.ssl|WARNING|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|CertificateMessage.java:1059|Unavailable authentication scheme: ecdsa_secp512r1_sha512
javax.net.ssl|DEBUG|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|SunX509KeyManagerImpl.java:392|matching alias: 127.0.0.1
javax.net.ssl|DEBUG|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|StatusResponseManager.java:763|Staping disabled or is a resumed session
javax.net.ssl|ALL|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|CertStatusExtension.java:1111|Stapling is disabled for this connection
javax.net.ssl|DEBUG|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|SSLExtensions.java:256|Ignore, context unavailable extension: status_request
javax.net.ssl|ALL|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|CertStatusExtension.java:1111|Stapling is disabled for this connection
javax.net.ssl|DEBUG|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|SSLExtensions.java:256|Ignore, context unavailable extension: status_request
javax.net.ssl|DEBUG|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|CertificateMessage.java:998|Produced server Certificate message (
"Certificate": {
  "certificate_request_context": "",
  "certificate_list": [
  {
    "certificate" : {
      "version"            : "v3",
      "serial number"      : "01 76 18 10 8C EA",
      "signature algorithm": "SHA1withRSA",
      "issuer"             : "CN=mitmProxy",
      "not before"         : "2019-12-06 08:31:46.000 CET",
      "not  after"         : "2020-12-11 03:43:46.000 CET",
      "subject"            : "CN=127.0.0.1, OU=Test, O=CyberVillainsCA, L=Seattle, ST=Washington, C=US",
      "subject public key" : "RSA",
      "extensions"         : [
        {
          ObjectId: 2.5.29.35 Criticality=false
          AuthorityKeyIdentifier [
          KeyIdentifier [
          0000: 51 EE 4F 7F DD 54 09 C6   88 DB 48 BE 6B E4 34 90  Q.O..T....H.k.4.
          0010: 51 75 D9 FE                                        Qu..
          ]
          ]
        },
        {
          ObjectId: 2.5.29.19 Criticality=true
          BasicConstraints:[
            CA:false
            PathLen: undefined
          ]
        },
        {
          ObjectId: 2.5.29.37 Criticality=false
          ExtendedKeyUsages [
            serverAuth
            clientAuth
            2.16.840.1.113730.4.1
            1.3.6.1.4.1.311.10.3.3
          ]
        },
        {
          ObjectId: 2.5.29.14 Criticality=false
          SubjectKeyIdentifier [
          KeyIdentifier [
          0000: 92 96 00 89 90 42 C1 66   F2 D5 D9 B3 11 5D E5 27  .....B.f.....].'
          0010: 57 1D 2F D2                                        W./.
          ]
          ]
        }
      ]}
    "extensions": {
      <no extension>
    }
  },
  {
    "certificate" : {
      "version"            : "v3",
      "serial number"      : "72 FD FA 99",
      "signature algorithm": "SHA384withRSA",
      "issuer"             : "CN=mitmProxy",
      "not before"         : "2020-11-28 11:21:38.000 CET",
      "not  after"         : "2120-11-04 11:21:38.000 CET",
      "subject"            : "CN=mitmProxy",
      "subject public key" : "RSA",
      "extensions"         : [
        {
          ObjectId: 2.5.29.14 Criticality=false
          SubjectKeyIdentifier [
          KeyIdentifier [
          0000: 51 EE 4F 7F DD 54 09 C6   88 DB 48 BE 6B E4 34 90  Q.O..T....H.k.4.
          0010: 51 75 D9 FE                                        Qu..
          ]
          ]
        }
      ]}
    "extensions": {
      <no extension>
    }
  },
]
}
)
javax.net.ssl|DEBUG|49|SeleniumProxyHandler$SslRelay1-1|2020-11-30 08:31:47.116 CET|SSLSocketOutputRecord.java:241|WRITE: TLS13 handshake, length = 2284

As the solution is not yet known trying to fallback to BMP original keystore (Cybervillain)
Solution mac come from here:
https://security.stackexchange.com/questions/150078/missing-x509-extensions-with-an-openssl-generated-certificate