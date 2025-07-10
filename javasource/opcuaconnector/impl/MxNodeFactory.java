package opcuaconnector.impl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import static java.util.Objects.requireNonNull;

import org.eclipse.milo.opcua.sdk.client.nodes.UaDataTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaReferenceTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaVariableTypeNode;
import org.eclipse.milo.opcua.sdk.client.nodes.UaViewNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;

import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.DataTypeNode;
import opcuaconnector.proxies.MethodNode;
import opcuaconnector.proxies.Node;
import opcuaconnector.proxies.ObjectNode;
import opcuaconnector.proxies.ObjectTypeNode;
import opcuaconnector.proxies.ReferenceTypeNode;
import opcuaconnector.proxies.VariableNode;
import opcuaconnector.proxies.VariableTypeNode;
import opcuaconnector.proxies.ViewNode;

public class MxNodeFactory {
	private final UaNode opcNode;
	private final IContext context;
	private final MxLogger LOGGER = new MxLogger(MxNodeFactory.class);

	public MxNodeFactory(UaNode opcNode, IContext context) {
		requireNonNull(opcNode, "Cannot create a node object from an empty UaNode object.");
		requireNonNull(context, "Cannot create a node object without context.");

		this.opcNode = opcNode;
		this.context = context;
	}

	/**
	 * Creates the specialization of a node object with all details by checking the
	 * node class of the opcNode object
	 * 
	 * @return a Mendix object that is a copy of the input opc node.
	 */
	public Node getMxNode() {
		Node mxNode;
		switch (opcNode.getNodeClass()) {
		case DataType:
			mxNode = createMxDataTypeNode((UaDataTypeNode) opcNode);
			break;
		case Method:
			mxNode = createMxMethodNode((UaMethodNode) opcNode);
			break;
		case Object:
			mxNode = createMxObjectNode((UaObjectNode) opcNode);
			break;
		case ObjectType:
			mxNode = createMxObjectTypeNode((UaObjectTypeNode) opcNode);
			break;
		case ReferenceType:
			mxNode = createMxReferenceTypeNode((UaReferenceTypeNode) opcNode);
			break;
		case Variable:
			mxNode = createMxVariableNode((UaVariableNode) opcNode);
			break;
		case VariableType:
			mxNode = createMxVariableTypeNode((UaVariableTypeNode) opcNode);
			break;
		case View:
			mxNode = createMxViewNode((UaViewNode) opcNode);
			break;
		default:
			LOGGER.error(
					"Failed to create a Node object from Opc Node, because the Node Class is not implemented. NodeID: "
							+ opcNode.getNodeId().toParseableString() + " node class: "
							+ opcNode.getNodeClass().toString());
			throw new IllegalArgumentException("could not parse opcNode to Mendix Node as data type:"
					+ opcNode.getNodeClass() + " is not supported");
		}
		mxNode.setNodeId(opcNode.getNodeId().toParseableString());
		mxNode.setDisplayName(opcNode.getDisplayName().getText());
		mxNode.setBrowseName(opcNode.getBrowseName().toParseableString());
		mxNode.setNodeClass(ENUMNodeClass.toMxNodeClass(opcNode.getNodeClass()));
		return mxNode;
	}

	private DataTypeNode createMxDataTypeNode(UaDataTypeNode opcDataTypeNode) {
		DataTypeNode mxDataTypeNode = new DataTypeNode(context);
		mxDataTypeNode.setIsAbstract(opcDataTypeNode.getIsAbstract());
		return mxDataTypeNode;
	}

	private MethodNode createMxMethodNode(UaMethodNode opcNode) {
		MethodNode mxMethodNode = new MethodNode(context);
		mxMethodNode.setIsExecutable(opcNode.isExecutable());
		mxMethodNode.setIsUserExecutable(opcNode.isUserExecutable());
		return mxMethodNode;
	}

	private ObjectNode createMxObjectNode(UaObjectNode opcNode) {
		ObjectNode mxObjectNode = new ObjectNode(context);
		mxObjectNode.setIcon(getBase64Icon(opcNode));
		mxObjectNode
				.setEventNotifier(opcNode.getEventNotifier() != null ? opcNode.getEventNotifier().intValue() : null);
		return mxObjectNode;
	}

	private ObjectTypeNode createMxObjectTypeNode(UaObjectTypeNode opcNode) {
		ObjectTypeNode mxObjectTypeNode = new ObjectTypeNode(context);
		mxObjectTypeNode.setIsAbstract(opcNode.getIsAbstract());
		return mxObjectTypeNode;
	}

	private ReferenceTypeNode createMxReferenceTypeNode(UaReferenceTypeNode opcNode) {
		ReferenceTypeNode mxReferenceTypeNode = new ReferenceTypeNode(context);
		mxReferenceTypeNode.setIsAbstract(opcNode.getIsAbstract());
		mxReferenceTypeNode.setIsSymmetric(opcNode.getSymmetric());
		mxReferenceTypeNode
				.setInverseName(opcNode.getInverseName() != null ? opcNode.getInverseName().getText() : null);
		return mxReferenceTypeNode;
	}

	private VariableNode createMxVariableNode(UaVariableNode opcNode) {
		VariableNode mxVariableNode = new VariableNode(context);
		mxVariableNode.setAccessLevel(opcNode.getAccessLevel().intValue());
		mxVariableNode.setArrayDimensions(Arrays.toString(opcNode.getArrayDimensions()));
		mxVariableNode
				.setDataTypeNodeId(opcNode.getDataType() != null ? opcNode.getDataType().toParseableString() : null);
		mxVariableNode.setIsHistorizing(opcNode.getHistorizing());
		mxVariableNode.setMinimumSamplingInterval(opcNode.getMinimumSamplingInterval() != null ? BigDecimal
				.valueOf(opcNode.getMinimumSamplingInterval()) : null);
		mxVariableNode.setUserAccessLevel(
				opcNode.getUserAccessLevel() != null ? opcNode.getUserAccessLevel().intValue() : null);
		mxVariableNode.setValueRank(opcNode.getValueRank());
		return mxVariableNode;
	}

	private VariableTypeNode createMxVariableTypeNode(UaVariableTypeNode opcNode) {
		VariableTypeNode mxVariableTypeNode = new VariableTypeNode(context);
		mxVariableTypeNode.setIsAbstract(opcNode.getIsAbstract());
		mxVariableTypeNode
				.setDataTypeNodeId(opcNode.getDataType() != null ? opcNode.getDataType().toParseableString() : null);
		mxVariableTypeNode.setValueRank(opcNode.getValueRank());
		mxVariableTypeNode.setArrayDimensions(
				opcNode.getArrayDimensions() != null ? Arrays.toString(opcNode.getArrayDimensions()) : null);
		return mxVariableTypeNode;
	}

	private ViewNode createMxViewNode(UaViewNode opcNode) {
		ViewNode mxViewNode = new ViewNode(context);
		mxViewNode.setContainsNoLoops(opcNode.getContainsNoLoops());
		mxViewNode.setEventNotifier(opcNode.getEventNotifier() != null ? opcNode.getEventNotifier().intValue() : null);
		return mxViewNode;
	}

	/**
	 * Returns a Base64 encoded icon. Note that this method logs a debug message if
	 * the icon is empty. This is because the client cannot determine the difference
	 * between an empty icon and being not able to retrieve the icon
	 * 
	 * @param opcObjectNode Object node
	 * @return a base 64 encoded icon
	 */
	private String getBase64Icon(UaObjectNode opcObjectNode) {
		try {
			ByteString icon = opcObjectNode.getIcon().get();
			return icon.isNotNull() ? Base64.getEncoder().encodeToString(icon.bytes()) : null;
		} catch (ExecutionException e) {
			LOGGER.debug("Cannot get icon because " + e.getMessage() + ". Returning null.");
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Cannot get icon because the action was interrupted. The error was " + e.getMessage()
					+ ". Returning null.");
			return null;
		}
	}
}
