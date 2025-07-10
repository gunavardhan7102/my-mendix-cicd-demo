package opcuaconnector.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;

import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.util.DigestUtil;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.ServerCertificate;

public class ClientCertificateValidator {
	static MxLogger LOGGER = new MxLogger(ClientCertificateValidator.class);
	static ClientCertificateValidator instance;
	private DefaultTrustListManager trustListManager;
	private DefaultClientCertificateValidator certificateValidator;

	private ClientCertificateValidator() throws IOException {
			Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "client", "security");
			Files.createDirectories(securityTempDir);
			if (!Files.exists(securityTempDir)) {
				return;
			}
			File pkiDir = securityTempDir.resolve("pki").toFile();
			this.trustListManager = new DefaultTrustListManager(pkiDir);
			this.certificateValidator = new DefaultClientCertificateValidator(trustListManager);
		}

	public static ClientCertificateValidator getInstance() throws CoreException {
		if (instance == null) {
			try {
				instance = new ClientCertificateValidator();
			}
			catch (IOException e) {
				throw new CoreException("Cannot instantiate a Client Certificate Validator because " + e.getMessage());
			}
		}
		return instance;
	}

	public DefaultClientCertificateValidator getDefaultClientCertificateValidator() {
		return this.certificateValidator;
	}

	/**
	 * Adds the certificate to the trusted folder
	 * 
	 * @param mxServerCertificate certificate to trust
	 * @param context
	 * @throws CoreException whenever no certificate can be created from the
	 *                       FileDocument
	 */
	public void trustCertificate(ServerCertificate mxServerCertificate, IContext context) throws CoreException {
		X509Certificate certifcate = CertificateUtils.getX509Certificate(mxServerCertificate.getMendixObject(),
				context);
		trustListManager.addTrustedCertificate(certifcate);
	}

	/**
	 * This method removes the files from the trusted folder, as a result the
	 * certificate will not be trusted anymore. Adding the file to the Rejected
	 * folder has no impact on the behaviour and is therefore omitted.
	 * 
	 * @param mxServerCertificate Certificate from the server
	 * @param context
	 * @throws CoreException Whenever the certificate cannot be read
	 */
	public void untrustCertifcate(ServerCertificate mxServerCertificate, IContext context) throws CoreException {
		try {
			X509Certificate certificate = CertificateUtils.getX509Certificate(mxServerCertificate.getMendixObject(),
					context);
			ByteString thumbprint = ByteString.of(DigestUtil.sha1(certificate.getEncoded()));
			trustListManager.removeTrustedCertificate(thumbprint);
		} catch (CertificateEncodingException e) {
			throw new CoreException("Cannot remove certificate from trusted list because " + e.getMessage());
		}
	}
	
	
	/**
	 * This method removes all files from the trusted folder, as a result
	 * no certificate will be trusted anymore. Adding the file to the Rejected
	 * folder has no impact on the behaviour and is therefore omitted.
	 * 
	 */
	public void untrustAllCertificates() {
		trustListManager.getTrustedCertificates().forEach(certificate -> 
		{
			try {
				ByteString thumbprint = ByteString.of(DigestUtil.sha1(certificate.getEncoded()));
				trustListManager.removeTrustedCertificate(thumbprint);
				LOGGER.info("Removed certificate from trusted store");
			}
			catch (Exception e) {
				LOGGER.error("Cannot remove certificate with public key " + certificate.getPublicKey().toString() + " because " + e.getMessage());
			}
		});
	}
	
	/**
	 * This method takes the certificates from the Mendix keystore and adds them to the trusted folder.
	 * As a result all servers for which their their public certificate is in the mendix keystore, will be accepted.
	 * 
	 */
	public void trustMendixModelCertificates() throws IOException {
		 List <InputStream> CaCertificates = Core.getConfiguration().getCACertificates();
		 Iterator <InputStream> caIterator = CaCertificates.iterator();
		 while(caIterator.hasNext() ) {
			 try {
				 this.trustListManager.addTrustedCertificate((X509Certificate)CertificateFactory.getInstance("X.509")
						 									.generateCertificate(caIterator.next()));
				 LOGGER.info("Added a CA certificate from Keystore to OPCUA trusted list");
			 } catch (CertificateException e) {
				 LOGGER.error("Cannot add CA certificate from Keystore to OPC-UA trusted list. The error is " + e.getMessage());
			 }
		 }
	 }

}
