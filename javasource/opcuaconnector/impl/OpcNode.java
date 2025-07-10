package opcuaconnector.impl;

import static java.util.Objects.requireNonNull;

import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import com.mendix.core.CoreException;

public class OpcNode {
	private static final MxLogger LOGGER = new MxLogger(OpcNode.class);

	private final NodeId opcNodeId;
	
	public OpcNode(String nodeId) throws CoreException {
		requireNonNull(nodeId, "The node ID cannot be empty");
		if (nodeId.isBlank()) {
			throw new IllegalArgumentException("The node ID cannot be blank");
		}
		try {
			this.opcNodeId = NodeId.parse(nodeId);
		} catch (UaRuntimeException e) {
			LOGGER.error("Cannot parse " + nodeId + " to a valid node ID, because " + e.getMessage());
			throw new CoreException("The node ID " + nodeId + " is not valid. Please check the format.");
		}
	}
	
	public NodeId getOpcNodeId(){
		return opcNodeId;
	}
}
