package opcuaconnector.impl;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager.SubscriptionListener;
import org.eclipse.milo.opcua.stack.client.security.ClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.util.EndpointUtil;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import static java.util.Objects.requireNonNull;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import opcuaconnector.proxies.AbstractIdentityToken;
import opcuaconnector.proxies.AnonymousIdentityToken;
import opcuaconnector.proxies.CertificateIdentityToken;
import opcuaconnector.proxies.ClientCertificate;
import opcuaconnector.proxies.ClientCertificateHolder;
import opcuaconnector.proxies.ClientCertificatePrivateKey;
import opcuaconnector.proxies.ENUM_MessageSecurityMode;
import opcuaconnector.proxies.ServerConfiguration;
import opcuaconnector.proxies.Subscription;
import opcuaconnector.proxies.UserNameIdentityToken;
import opcuaconnector.proxies.constants.Constants;

public class ClientBuilder {
	private static final MxLogger LOGGER = new MxLogger(ClientBuilder.class);
	private final ServerConfiguration mxServerConfiguration;
	private final IContext context;

	/**
	 * Prepares the construction of a client object, this method also validates all
	 * the input parameters. Use build() to create the OpcUaClient Object.
	 * 
	 * @param mxServerConfiguration Configuration object to create the client
	 * @param context               IContext object to import fileDocuments
	 * @throws CoreException whenever the
	 */
	public ClientBuilder(ServerConfiguration mxServerConfiguration, IContext context) throws CoreException {
		requireNonNull(context, "Context cannot be empty when creating a client.");
		validateServerConfiguration(mxServerConfiguration);

		this.mxServerConfiguration = mxServerConfiguration;
		this.context = context;
	}

	/**
	 * Build the OpcUaClient object.
	 * 
	 * @return Not connected client. This client needs to be connected, to fully
	 *         check whether the client is good.
	 * @throws CoreException Whenever a validation error occurs while creating the
	 *                       client object.
	 */
	public OpcUaClient build() throws CoreException {
		try {
			LOGGER.debug("Initiating build request for client, the object will have endpoint URL "
					+ mxServerConfiguration.getEndpointURL() + ", message security mode "
					+ mxServerConfiguration.getMessageSecurityMode().getCaption() + " and Security profile uri "
					+ mxServerConfiguration.getSecurityPolicyURI());
			OpcUaClient opcClient = OpcUaClient.create(createOpcUaClientConfig());
			opcClient.getSubscriptionManager().addSubscriptionListener(addSubscriptionListener(mxServerConfiguration));
			LOGGER.debug("Created a client for endpoint " + mxServerConfiguration.getEndpointURL());
			return opcClient;
		} catch (UaException e) {
			LOGGER.error("Cannot create Client object, because " + e.getMessage());
			throw new CoreException(e.getMessage());
		}
	}

	private void validateServerConfiguration(ServerConfiguration mxServerConfiguration) throws CoreException {
		requireNonNull(mxServerConfiguration, "Server configuration cannot be empty");

		requireNonNull(mxServerConfiguration.getConfigurationName(), "Server configuration name cannot be empty");
		if (mxServerConfiguration.getConfigurationName().isBlank()) {
			throw new IllegalArgumentException("Server configuration name cannot be blank");
		}
		requireNonNull(Constants.getCONST_ApplicationName(),
				"Application name cannot be empty. Check the constants in the USE_ME folder to adjust this value");
		if (Constants.getCONST_ApplicationName().isBlank()) {
			throw new IllegalArgumentException(
					"Application name cannot be blank. Check the constants in the USE_ME folder to adjust this value");
		}
		requireNonNull(Constants.getCONST_ApplicationURI(),
				"Application URI cannot be empty. Check the constants in the USE_ME folder to adjust this value");
		if (Constants.getCONST_ApplicationURI().isBlank()) {
			throw new IllegalArgumentException(
					"Application URI cannot be blank. Check the constants in the USE_ME folder to adjust this value");
		}
		requireNonNull(mxServerConfiguration.getEndpointURL(), "Endpoint URL cannot be empty");
		if (mxServerConfiguration.getEndpointURL().isBlank()) {
			throw new IllegalArgumentException("Endpoint URL cannot be blank");
		}
		requireNonNull(mxServerConfiguration.getMessageSecurityMode(), "Security mode cannot be empty");
		requireNonNull(mxServerConfiguration.getSessionTimeout(), "Session timeout cannot be empty");
		if (mxServerConfiguration.getSessionTimeout() < 0) {
			throw new IllegalArgumentException("Session timeout cannot be negative");
		}
		requireNonNull(mxServerConfiguration.getRequestTimeout(), "Request timeout cannot be empty");
		if (mxServerConfiguration.getRequestTimeout() < 0) {
			throw new IllegalArgumentException("Request timeout cannot be empty");
		}
		LOGGER.debug("Passed basic validation");
		validateMxIdentityToken(mxServerConfiguration.getServerConfiguration_AbstractIdentityToken());
		LOGGER.debug("Passed Identity Token validation");
		validateMxApplicationCertificate(mxServerConfiguration);
		LOGGER.debug("Passed Client Certificate validation");
	}

	private void validateMxIdentityToken(AbstractIdentityToken mxIdentityToken) throws CoreException {
		requireNonNull(mxIdentityToken, "Abstract Identity token cannot be empty for Server configuration");
		if (mxIdentityToken instanceof AnonymousIdentityToken) {
			return;
		}
		if (mxIdentityToken instanceof UserNameIdentityToken) {
			UserNameIdentityToken mxUsernameIdentityToken = (UserNameIdentityToken) mxIdentityToken;
			requireNonNull(mxUsernameIdentityToken.getUsername(), "User name cannot be empty");
			if (mxUsernameIdentityToken.getUsername().isBlank()) {
				throw new IllegalArgumentException("User name cannot be blank");
			}
			requireNonNull(mxUsernameIdentityToken.getPassword(), "Password cannot be empty");
			if (mxUsernameIdentityToken.getPassword().isBlank()) {
				throw new IllegalArgumentException("Password cannot be blank");
			}
			return;
		}
		if (mxIdentityToken instanceof CertificateIdentityToken) {
			CertificateIdentityToken mxCertificateIdentityToken = (CertificateIdentityToken) mxIdentityToken;
			requireNonNull(mxCertificateIdentityToken.getCertificateIdentityToken_IdentityTokenCertificate(),
					"Identity token certificate cannot be empty");
			if (Boolean.FALSE.equals(mxCertificateIdentityToken.getCertificateIdentityToken_IdentityTokenCertificate()
					.getHasContents())) {
				throw new IllegalArgumentException("Identity token certificate  must have content");
			}
			requireNonNull(mxCertificateIdentityToken.getCertificateIdentityToken_IdentityTokenPrivateKey(),
					"Identity token private key cannot be empty");
			if (Boolean.FALSE.equals(mxCertificateIdentityToken.getCertificateIdentityToken_IdentityTokenPrivateKey()
					.getHasContents())) {
				throw new IllegalArgumentException("Identity token private key must have content");
			}
			requireNonNull(
					mxCertificateIdentityToken.getCertificateIdentityToken_IdentityTokenPrivateKey().getPassword(),
					"Identity token private key password cannot be empty");
			return;
		}
		throw new IllegalArgumentException(
				"Cannot initialize an opcua client from a non-specialized abstract identity token");
	}

	private void validateMxApplicationCertificate(ServerConfiguration mxServerConfiguration) throws CoreException {
		switch (mxServerConfiguration.getMessageSecurityMode()) {
		case NONE:
			return;
		case SIGN:
		case SIGNENCRYPT:
			ClientCertificateHolder mxCertificateHolder = mxServerConfiguration
					.getServerConfiguration_ClientCertificateHolder();
			requireNonNull(mxCertificateHolder,
					"Client certificate holder cannot be empty, when the security mode is Sign or Sign & Encrypt");
			ClientCertificate mxClientCertificate = mxCertificateHolder.getClientCertificateHolder_ClientCertificate();
			requireNonNull(mxClientCertificate,
					"Client certificate cannot be empty, when security mode is Sign or Sign & Encrypt");
			if (Boolean.FALSE.equals(mxClientCertificate.getHasContents())) {
				throw new NullPointerException(
						"Client certificate must have contents, when security mode is Sign or Sign & Encrypt");
			}
			ClientCertificatePrivateKey mxClientCertificatePrivateKey = mxCertificateHolder
					.getClientCertificateHolder_ClientCertificatePrivateKey();
			requireNonNull(mxClientCertificatePrivateKey,
					"Client certificate private key cannot be empty when security mode is Sign or Sign & Encrypt");
			if (Boolean.FALSE.equals(mxClientCertificatePrivateKey.getHasContents())) {
				throw new NullPointerException(
						"Client certificate private key must have content, when security mode is Sign or Sign & Encrypt");
			}
			requireNonNull(mxClientCertificatePrivateKey.getPassword(),
					"client certificate private key password cannot be empty");
			return;
		default:
			throw new IllegalArgumentException(
					"Security mode " + mxServerConfiguration.getMessageSecurityMode() + " is not supported.");
		}
	}

	/**
	 * Core of the OpcUaClient object creation, is to set up the configuration. Here
	 * the Mendix Configuration object is translated into an OpcUa Client
	 * Configuration object.
	 * 
	 * @return OpcUaClientConfig that can be used to create a client object.
	 * @throws CoreException Whenever the endpoint cannot be found, or one of the
	 *                       certificates cannot be deserialized.
	 */
	private OpcUaClientConfig createOpcUaClientConfig() throws CoreException {
		OpcUaClientConfigBuilder opcConfigBuilder = new OpcUaClientConfigBuilder();
		opcConfigBuilder.setApplicationName(new LocalizedText(Constants.getCONST_ApplicationName()))
				.setApplicationUri(Constants.getCONST_ApplicationURI()).setCertificate(buildApplicationCertificate())
				.setCertificateChain(getX509CertificateChain()).setCertificateValidator(buildCertificateValidator())
				.setEndpoint(getMatchingEndpointDescription())
				.setIdentityProvider(
						buildIdentityProvider(mxServerConfiguration.getServerConfiguration_AbstractIdentityToken()))
				.setKeyPair(buildKeyPair())
				.setRequestTimeout(mxServerConfiguration.getRequestTimeout() != null
						? uint(mxServerConfiguration.getRequestTimeout())
						: uint(3000))
				.setSessionTimeout(mxServerConfiguration.getSessionTimeout() != null
						? uint(mxServerConfiguration.getSessionTimeout())
						: uint(120000));

		return opcConfigBuilder.build();
	}

	private ClientCertificateValidator buildCertificateValidator() throws CoreException {
		return mxServerConfiguration.getMessageSecurityMode().equals(ENUM_MessageSecurityMode.NONE) ? null
				: opcuaconnector.impl.ClientCertificateValidator.getInstance().getDefaultClientCertificateValidator();
	}

	private X509Certificate buildApplicationCertificate() throws CoreException {
		switch (mxServerConfiguration.getMessageSecurityMode()) {
		case NONE:
			return null;
		case SIGN:
		case SIGNENCRYPT:
			ClientCertificate mxApplicationCertificate = mxServerConfiguration
					.getServerConfiguration_ClientCertificateHolder().getClientCertificateHolder_ClientCertificate();
			return CertificateUtils.getX509Certificate(mxApplicationCertificate.getMendixObject(), context);
		default:
			throw new CoreException("Security mode is not supported.");
		}
	}

	private IdentityProvider buildIdentityProvider(AbstractIdentityToken mxIdentityToken) throws CoreException {
		if (mxIdentityToken instanceof AnonymousIdentityToken) {
			return new AnonymousProvider();
		}
		if (mxIdentityToken instanceof UserNameIdentityToken) {
			UserNameIdentityToken mxUsernameServerConfiguration = (UserNameIdentityToken) mxIdentityToken;
			return new UsernameProvider(mxUsernameServerConfiguration.getUsername(),
					mxUsernameServerConfiguration.getPassword());
		}
		if (mxIdentityToken instanceof CertificateIdentityToken) {
			CertificateIdentityToken mxCertificateIdentityToken = (CertificateIdentityToken) mxIdentityToken;
			return new X509IdentityProvider(
					CertificateUtils.getX509Certificate(mxCertificateIdentityToken
							.getCertificateIdentityToken_IdentityTokenCertificate().getMendixObject(), context),
					CertificateUtils.getPrivateKey(
							mxCertificateIdentityToken.getCertificateIdentityToken_IdentityTokenPrivateKey(), context));
		}
		throw new IllegalArgumentException("Cannot create opc Identity token from non-specialized Identity token");
	}

	private KeyPair buildKeyPair() throws CoreException {
		switch (mxServerConfiguration.getMessageSecurityMode()) {
		case NONE:
			return null;
		case SIGN:
		case SIGNENCRYPT:
			ClientCertificateHolder mxApplicationCertificateHolder = mxServerConfiguration
					.getServerConfiguration_ClientCertificateHolder();
			PublicKey publicKey = CertificateUtils.getX509Certificate(
					mxApplicationCertificateHolder.getClientCertificateHolder_ClientCertificate().getMendixObject(),
					context).getPublicKey();
			PrivateKey privateKey = CertificateUtils.getPrivateKey(
					mxApplicationCertificateHolder.getClientCertificateHolder_ClientCertificatePrivateKey(), context);
			return new KeyPair(publicKey, privateKey);
		default:
			return null;
		}
	}

	private X509Certificate[] getX509CertificateChain() throws CoreException {
		switch (mxServerConfiguration.getMessageSecurityMode()) {
		case NONE:
			return new X509Certificate[] {};
		case SIGN:
		case SIGNENCRYPT:
			ClientCertificate mxAppCertificate = mxServerConfiguration.getServerConfiguration_ClientCertificateHolder()
					.getClientCertificateHolder_ClientCertificate();
			List<X509Certificate> chain = new ArrayList<>();
			chain.add(CertificateUtils.getX509Certificate(mxAppCertificate.getMendixObject(), context));
			ClientCertificate certPointer = mxAppCertificate;
			while (certPointer.getCertificateChain() != null) {
				certPointer = certPointer.getCertificateChain();
				chain.add(CertificateUtils.getX509Certificate(certPointer.getMendixObject(), context));
			}
			return chain.toArray(new X509Certificate[chain.size()]);
		default:
			return new X509Certificate[] {};
		}
	}
	
	/**
	 * This action returns the EndpointDescription that was selected based on the
	 * values that are set on the configuration object. It does so by using the
	 * EndpointURL, then find matching on Message Security Mode and Security Policy
	 * URI.
	 * 
	 * @return Endpoint that matches the selection on the config object
	 * @throws CoreException Whenever no matching endpoint can be found
	 */
	private EndpointDescription getMatchingEndpointDescription() throws CoreException {
		for (EndpointDescription opcEndpointDescription : new OpcDiscoveryServiceSet()
				.miloGetEndpoints(mxServerConfiguration.getEndpointURL())) {
			if ((opcEndpointDescription.getSecurityMode().equals(
					ENUMMessageSecurityMode.fromMxMessageSecurityMode(mxServerConfiguration.getMessageSecurityMode()))
					&& mxServerConfiguration.getSecurityPolicyURI()
							.equalsIgnoreCase(opcEndpointDescription.getSecurityPolicyUri()))) {
				if(mxServerConfiguration.getIsManualConfiguration()) {
					opcEndpointDescription = overWriteEndPointHost(opcEndpointDescription);
				}
				return opcEndpointDescription;
			}
		}
		throw new CoreException(
				"Cannot find Endpoint description on server with endpoint url " + mxServerConfiguration.getEndpointURL()
						+ " and security " + mxServerConfiguration.getMessageSecurityMode());
	}
	
	
	private EndpointDescription overWriteEndPointHost(EndpointDescription opcEndpointDescription) throws CoreException {
		try {
			String hostName = EndpointUtil.getHost(mxServerConfiguration.getEndpointURL());
			int port = EndpointUtil.getPort(mxServerConfiguration.getEndpointURL());
			LOGGER.debug("Manual overwriting endpoint to " + hostName + " : "+ String.valueOf(port));
			return EndpointUtil.updateUrl(opcEndpointDescription, hostName, port);
		}
		catch (Exception e) {
			LOGGER.error("Cannot manually overwrite hostname and port based on endpoint URL: " + mxServerConfiguration.getEndpointURL() + ". because " + e.getMessage());
			throw new CoreException(e);
		}
	}

	

	/**
	 * Adds all default subscription listeners to the Subscription manager
	 * 
	 * @param Server configuration to backtrace what the origin is of the message
	 * @return a subscription listener to be added to the client.
	 */
	private SubscriptionListener addSubscriptionListener(ServerConfiguration mxServerConfiguration) {
		String subscrionLogNodeName = MxLogger.getLogNode() + " - subscription manager";
		ILogNode subscriptionLOGGER = Core.getLogger(subscrionLogNodeName);
		return new UaSubscriptionManager.SubscriptionListener() {
			@Override
			public void onKeepAlive(UaSubscription subscription, DateTime publishTime) {
				subscriptionLOGGER.debug(String.format("onKeepAlive event for [Server:%s|Subscription:%s]",
						mxServerConfiguration.getConfigurationName(), subscription.getSubscriptionId()));
			}

			@Override
			public void onStatusChanged(UaSubscription subscription, StatusCode status) {
				subscriptionLOGGER
						.info(String.format("onStatusChanged event for [Server:%s|Subscription:%s], status = %s",
								mxServerConfiguration.getConfigurationName(), subscription.getSubscriptionId(),
								status.toString()));
			}

			@Override
			public void onPublishFailure(UaException exception) {
				subscriptionLOGGER.error(
						"onPublishFailure exception on server: " + mxServerConfiguration.getConfigurationName(),
						exception);
			}

			@Override
			public void onNotificationDataLost(UaSubscription subscription) {
				subscriptionLOGGER.warn(String.format("onNotificationDataLost event for [Server:%s|Subscription:%s]",
						mxServerConfiguration.getConfigurationName(), subscription.getSubscriptionId()));
			}

			@Override
			public void onSubscriptionTransferFailed(UaSubscription subscription, StatusCode statusCode) {
				try {
					String microflowToRecreate = "OPCUAConnector.PRIVATE_Subscription_Recreate";
					subscriptionLOGGER.error("An on Subscription Transfer Failed event occured for server "
							+ mxServerConfiguration.getConfigurationName() + " and subscription id "
							+ subscription.getSubscriptionId().toString() + ". The status code is "
							+ statusCode.toString() + ". Recreating the subscription by calling "
							+ microflowToRecreate);
					IMendixObject mxObject = Core
							.createXPathQuery("//" + Subscription.entityName + "["
									+ Subscription.MemberNames.Subscription_ServerConfiguration.toString() + "="
									+ mxServerConfiguration.getMendixObject().getId().toLong() + "]")
							.setAmount(1).execute(context).get(0);
					Core.microflowCall(microflowToRecreate).withParam("Subscription", mxObject).execute(context);
				} catch (Exception e) {
					subscriptionLOGGER.error("Failed to disconnect subscription " + subscription.getSubscriptionId()
							+ " because " + e.getMessage());
				}
			}
		};
	}
}
