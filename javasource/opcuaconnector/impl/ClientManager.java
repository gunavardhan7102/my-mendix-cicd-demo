package opcuaconnector.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;

import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.ServerConfiguration;

public class ClientManager {
	private static final ClientManager SingletonClientManager = new ClientManager();
	private static final MxLogger LOGGER = new MxLogger(ClientManager.class);
	private final ConcurrentHashMap<String, Client> clientCache;

	private ClientManager() {
		this.clientCache = new ConcurrentHashMap<>();
	}

	public static ClientManager getInstance() {
		return SingletonClientManager;
	}
	
	private static String generateHashMapId(ServerConfiguration mxServerConfiguration) {
		return mxServerConfiguration.getMendixObject().getId().toString();
		//return mxServerConfiguration.getEndpointURL() + mxServerConfiguration.getMessageSecurityMode().getCaption();
	}

	/**
	 * Class to manage subscriptions and monitored items along the UaClient objects.
	 * The Monitored items are not stored on the client and would therefore be lost
	 * in the context of how Mendix works together with Java. Hence we need to keep
	 * track of these ourselves.
	 *
	 */
	public class Client {
		OpcUaClient opcClient;
		String configurationName;
		ClientSubscriptionManager clientSubscriptionManager;

		private Client(OpcUaClient opcClient, ServerConfiguration mxServerConfiguration) {
			this.opcClient = opcClient;
			this.configurationName = mxServerConfiguration.getConfigurationName();
			this.clientSubscriptionManager = new ClientSubscriptionManager();
		}

		public OpcUaClient getUaClient() {
			return opcClient;
		}

		public ClientSubscriptionManager getClientSubscriptionManager() {
			return clientSubscriptionManager;
		}
		
		public String getConfigurationName() {
			return configurationName;
		}
	}
	
	/**
	 * To ensure we reuse our client objects and maintain our connection with the server, 
	 * @param mxServerConfiguration
	 * @param context
	 * @return
	 * @throws CoreException
	 */
	public Client getOrCreateClient(ServerConfiguration mxServerConfiguration, IContext context) throws CoreException {
		String serverConfigurationID = generateHashMapId(mxServerConfiguration);
		if (clientCache.containsKey(serverConfigurationID)) {
			return clientCache.get(serverConfigurationID);
		}
		Client newOpcClient = new Client(new ClientBuilder(mxServerConfiguration, context).build(), mxServerConfiguration);
		miloConnect(newOpcClient.getUaClient());
		clientCache.put(serverConfigurationID, newOpcClient);
		return newOpcClient;
	}

	/**
	 * Connects the client to the server to initiate the connection. This action
	 * should only be called once as the session timeout is refreshed on each
	 * connection to the server.
	 * 
	 * @param opcClient: fully configured client object
	 * @return A connected client, that can be used in further actions
	 * @throws CoreException whenever Client.connect throws an error, we return the
	 *                       error in a CoreException.
	 */
	private UaClient miloConnect(OpcUaClient opcClient) throws CoreException {
		try {
			LOGGER.debug("Connecting client to server with endpoint URL "
					+ getEndpointURL(opcClient));
			UaClient client = opcClient.connect().get();
			LOGGER.debug("Succesfully connected to server with endpoint URL "
					+ getEndpointURL(opcClient));
			return client;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Cannot connect the client with endpoint URL "
					+ getEndpointURL(opcClient)
					+ "because the thread was interrupted. The error is " + e);
			throw new CoreException(e);
		} catch (ExecutionException e) {
			try {
				LOGGER.debug("Client could not connect. Calling disconnect in order to prevent further retries.");
				miloDisconnect(opcClient);
			} catch (CoreException er) {}
			LOGGER.error("Cannot connect the client with endpoint URL "
					+ getEndpointURL(opcClient) + " because " + e);
			throw new CoreException(e);
		}
	}
	
	private UaClient miloDisconnect(OpcUaClient opcClient) throws CoreException {
		try {
			LOGGER.debug("Disconnecting client from servier with endpoint URL "
					+ getEndpointURL(opcClient));
			UaClient client = opcClient.disconnect().get();
			LOGGER.debug("Successfully disconnected from server with endpoint URL "
					+ getEndpointURL(opcClient));
			return client;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Cannot disconnect the client with endpoint URL "
					+ getEndpointURL(opcClient)
					+ "because the thread was interrupted. The error is " + e);
			throw new CoreException(e);
		} catch (ExecutionException e) {
			LOGGER.error("Cannot disconnect the client with endpoint URL "
					+ getEndpointURL(opcClient) + " because " + e);
			throw new CoreException(e);
		}
	}
	
	private String getEndpointURL(UaClient opcClient) {
		return opcClient.getConfig().getEndpoint().getEndpointUrl();
	}

	public void deleteClient(ServerConfiguration mxServerConfiguration) {
		String serverConfigurationID = generateHashMapId(mxServerConfiguration);
		if (clientCache.isEmpty() || clientCache.containsKey(serverConfigurationID) == false) {
			LOGGER.debug(
					"Cannot delete client from memory as the client with id: " + serverConfigurationID + "as it is not present in the cache.");
			return;
		}
		OpcUaClient opcClient = clientCache.get(serverConfigurationID).getUaClient();
		deleteClientFromHashMap(serverConfigurationID);
		try {
			miloDisconnect(opcClient);
		} catch (CoreException e) {	
			//Suppress error
		}
	}
	

	public void deleteAllClients() {
		clientCache.forEach((severConfigurationID, client) -> deleteClientFromHashMap(severConfigurationID));
	}

	private void deleteClientFromHashMap(String key) {
		clientCache.remove(key);
		LOGGER.info("Removed client with id: " + key);
	}
}
