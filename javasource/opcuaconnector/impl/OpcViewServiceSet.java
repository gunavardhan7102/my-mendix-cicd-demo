package opcuaconnector.impl;

import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;

import com.mendix.core.CoreException;

public class OpcViewServiceSet {
	private final MxLogger LOGGER = new MxLogger(OpcViewServiceSet.class);

	private final OpcUaClient opcClient;

	public OpcViewServiceSet(OpcUaClient opcClient) throws CoreException {
		requireNonNull(opcClient, "The client cannot be empty");
		this.opcClient = opcClient;
	}

	public BrowseResult miloBrowse(BrowseDescription opcBrowseDescription) throws CoreException {
		try {
			LOGGER.debug("Initiating Browse request for Browse description: " + opcBrowseDescription);
			BrowseResult opcBrowseResult = opcClient.browse(opcBrowseDescription).get();
			LOGGER.debug("Received response from opc server. Response is: " + opcBrowseResult);
			return opcBrowseResult;
		} catch (ExecutionException e) {
			LOGGER.error("An error occured while browsing the opc server. The request was: "
					+ opcBrowseDescription.toString() + ". The client was:" + opcClient.toString() + ". The error was "
					+ e.getMessage());
			throw new CoreException(e);
		} catch (InterruptedException e) {
			LOGGER.error(
					"An error occured while browsing the opc server, because the thread was interrupted. The request was: "
							+ opcBrowseDescription.toString() + ". The client was:" + opcClient.toString()
							+ ". The error was " + e.getMessage());
			Thread.currentThread().interrupt();
			throw new CoreException(e);
		}
	}
}
