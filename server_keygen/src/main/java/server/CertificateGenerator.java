package server;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

public class CertificateGenerator {

    public static X509Certificate generateCertificate(String subjectName, String issuerName,
                                                      KeyPair keyPair, PrivateKey issuerPrivateKey) throws Exception {
        X500Name subject = new X500Name("CN=" + subjectName);
        X500Name issuer = new X500Name("CN=" + issuerName);

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000);

        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
                keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, publicKeyInfo);

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSA");
        return new JcaX509CertificateConverter().getCertificate(
                certBuilder.build(signerBuilder.build(issuerPrivateKey)));
    }
}