package opcuaconnector.impl;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

public class CertificateUtils {
	private static MxLogger LOGGER = new MxLogger(CertificateUtils.class);

	private CertificateUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static PrivateKey getPrivateKey(opcuaconnector.proxies.PrivateKey mxPrivateKey, IContext context)
			throws CoreException {
		try {
			// Read KeyPair contents
			String privateKeyContents = new String(
					IOUtils.toByteArray(Core.getFileDocumentContent(context, mxPrivateKey.getMendixObject())));
			PEMParser pemParser = new PEMParser(new StringReader(privateKeyContents));
			Object keyPair = pemParser.readObject();
			pemParser.close();

			// Decipher KeyPair Object
			PrivateKeyInfo keyInfo;
			if (keyPair instanceof PEMEncryptedKeyPair) { // Encrypted PKCS1
				PEMDecryptorProvider pKCS1decryptionProvider = new JcePEMDecryptorProviderBuilder()
						.setProvider(new BouncyCastleProvider()).build(mxPrivateKey.getPassword().toCharArray());
				keyInfo = ((PEMEncryptedKeyPair) keyPair).decryptKeyPair(pKCS1decryptionProvider).getPrivateKeyInfo();
			} else if (keyPair instanceof PKCS8EncryptedPrivateKeyInfo) { // Encrypted PKCS8
				InputDecryptorProvider pKCS8DecryptionProvider = new JcePKCSPBEInputDecryptorProviderBuilder()
						.setProvider(new BouncyCastleProvider()).build(mxPrivateKey.getPassword().toCharArray());
				keyInfo = ((PKCS8EncryptedPrivateKeyInfo) keyPair).decryptPrivateKeyInfo(pKCS8DecryptionProvider);
			} else {
				LOGGER.error(
						"Private key format is not supported. The format is " + keyPair.getClass().getCanonicalName()
								+ " while only Encrypted PKCS1 and Encrypted PKCS8 are supported.");
				throw new CoreException("Private key format is not supported");
			}
			return new JcaPEMKeyConverter().getPrivateKey(keyInfo);
		} catch (Exception e) {
			LOGGER.error("Cannot create private key from provided document because " + e.getMessage());
			throw new CoreException(
					"Cannot create Private key from file because the Private key is not in a valid format. See Log messages for more information "
							+ e.getMessage());
		} // Note that we keep this message explicitly vague because these results are
			// hardly to debug. Expect Encrypted PEM-formatted Private key file in either PKCS1 or PKCS8.
	}

	public static X509Certificate getX509Certificate(IMendixObject mxObject, IContext context) throws CoreException {
		try {
			return (X509Certificate) CertificateFactory.getInstance("X.509")
					.generateCertificate(Core.getFileDocumentContent(context, mxObject));
		} catch (Exception e) {
			LOGGER.error("Cannot create X509 certificate from provided file because " + e.getMessage());
			throw new CoreException(
					"Cannot create X509 certificate from file because the provided certificate is not in a valid format. See Log messages for more information.");
		} // Note that we keep this message explicitly vague because these results are
			// hardly to debug. Expect CER/CRT formatted certificate.
	}
}
