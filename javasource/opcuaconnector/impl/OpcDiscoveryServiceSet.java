package opcuaconnector.impl;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.stack.client.DiscoveryClient;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import com.mendix.core.CoreException;

public class OpcDiscoveryServiceSet {
	private static final MxLogger LOGGER = new MxLogger(OpcDiscoveryServiceSet.class);

	/**
	 * Implements OPC 10000-4 5.4.2 via milo DiscoveryClient.java
	 * 
	 * @param endpointURL Endpoint that allows Clients access to Discovery Services
	 *                    without security
	 * @return The Endpoints supported by a Server and all of the
	 *         configuration information required to establish a SecureChannel and a
	 *         Session, i.e. to connect to a server.
	 * @throws CoreException whenever DiscoveryClient.getEndpoints returns a handled
	 *                       exception
	 */
	public List<EndpointDescription> miloGetEndpoints(String endpointURL) throws CoreException {
		try {
			LOGGER.debug("Requesting endpoints for endpoint url " + endpointURL);
			List<EndpointDescription> opcEndpointDescriptionList = DiscoveryClient.getEndpoints(endpointURL).get();
			LOGGER.debug("Received " + opcEndpointDescriptionList.size() + " endpoint descriptions for endpoint url "
					+ endpointURL);
			return opcEndpointDescriptionList;
		} catch (ExecutionException e) {
			LOGGER.error("Cannot get endpoints for endpoint url " + endpointURL + " because " + e.getMessage());
			throw new CoreException(e);
		} catch (InterruptedException e) {
			LOGGER.error("Cannot get endpoints for endpoint url " + endpointURL
					+ " because the thread was interrupted. Error " + e.getMessage());
			Thread.currentThread().interrupt(); // interrupt the current thread.
			throw new CoreException(e);
		}
	}

	/**
	 * Implements OPC 10000-4 5.4.2 via milo DiscoveryClient.java
	 * 
	 * @param endpointURL Endpoint that allows Clients access to Discovery Services
	 *                    without security
	 * @return The Servers known to a Server or Discovery Server.
	 * @throws CoreException whenever DiscoveryClient.findServers returns a handled
	 *                       exception
	 */
	public List<ApplicationDescription> miloFindServers(String endpointURL) throws CoreException {
		try {
			LOGGER.debug("Requesting servers for endpoint url " + endpointURL);
			List<ApplicationDescription> opcApplicationDescriptions = DiscoveryClient.findServers(endpointURL).get();
			LOGGER.debug("Received " + opcApplicationDescriptions.size() + " servers for endpoint url " + endpointURL);
			return opcApplicationDescriptions;
		} catch (ExecutionException e) {
			LOGGER.error("Cannot find servers for endpoint url " + endpointURL + " because " + e.getMessage());
			throw new CoreException(e);
		} catch (InterruptedException e) {
			LOGGER.error("Cannot find servers for endpoint url " + endpointURL
					+ " because the thread was interrupted. Error " + e.getMessage());
			Thread.currentThread().interrupt(); // interrupt the current thread.
			throw new CoreException(e);
		}
	}
}
