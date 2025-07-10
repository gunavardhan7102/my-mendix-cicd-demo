package opcuaconnector.impl;

import opcuaconnector.proxies.ENUM_AttributeId;

public class ENUMAttributeId {
	private ENUMAttributeId() {
		throw new IllegalStateException("Utility class");
	}
	
	public static ENUM_AttributeId getMxENUM(org.eclipse.milo.opcua.stack.core.AttributeId opcAttributeId) {
		if (opcAttributeId == null) {
			return null;
		}
		switch (opcAttributeId) {
		case AccessLevel:
			return ENUM_AttributeId.ACCESSLEVEL;
		case ArrayDimensions:
			return ENUM_AttributeId.ARRAYDIMENSIONS;
		case BrowseName:
			return ENUM_AttributeId.BROWSENAME;
		case ContainsNoLoops:
			return ENUM_AttributeId.CONTAINSNOLOOP;
		case DataType:
			return ENUM_AttributeId.DATATYPE;
		case Description:
			return ENUM_AttributeId.DESCRIPTION;
		case DisplayName:
			return ENUM_AttributeId.DISPLAYNAME;
		case EventNotifier:
			return ENUM_AttributeId.EVENTNOTIFIER;
		case Executable:
			return ENUM_AttributeId.EXECUTABLE;
		case Historizing:
			return ENUM_AttributeId.HISTORIZING;
		case InverseName:
			return ENUM_AttributeId.INVERSENAME;
		case IsAbstract:
			return ENUM_AttributeId.ISABSTRACT;
		case MinimumSamplingInterval:
			return ENUM_AttributeId.MINIMUMSAMPLINGRATE;
		case NodeClass:
			return ENUM_AttributeId.NODECLASS;
		case NodeId:
			return ENUM_AttributeId.NODEID;
		case Symmetric:
			return ENUM_AttributeId.SYMMETRIC;
		case UserAccessLevel:
			return ENUM_AttributeId.USERACCESSLEVEL;
		case UserExecutable:
			return ENUM_AttributeId.USEREXECUTABLE;
		case UserWriteMask:
			return ENUM_AttributeId.USERWRITEMASK;
		case Value:
			return ENUM_AttributeId.VALUE;
		case ValueRank:
			return ENUM_AttributeId.VALUERANK;
		case WriteMask:
			return ENUM_AttributeId.WriteMask;
		default:
			return null;
		}
	}

	public static ENUM_AttributeId getMxENUM(int opcAttributeIdInt) {
		org.eclipse.milo.opcua.stack.core.AttributeId opcAttributeId = org.eclipse.milo.opcua.stack.core.AttributeId
				.from(opcAttributeIdInt).orElse(null);
		return getMxENUM(opcAttributeId);
	}

	public static org.eclipse.milo.opcua.stack.core.AttributeId getOpcENUM(ENUM_AttributeId mxAttributeId) {
		if (mxAttributeId == null) {
			return null;
		}
		switch (mxAttributeId) {
		case ACCESSLEVEL:
			return org.eclipse.milo.opcua.stack.core.AttributeId.AccessLevel;
		case ARRAYDIMENSIONS:
			return org.eclipse.milo.opcua.stack.core.AttributeId.ArrayDimensions;
		case BROWSENAME:
			return org.eclipse.milo.opcua.stack.core.AttributeId.BrowseName;
		case CONTAINSNOLOOP:
			return org.eclipse.milo.opcua.stack.core.AttributeId.ContainsNoLoops;
		case DATATYPE:
			return org.eclipse.milo.opcua.stack.core.AttributeId.DataType;
		case DESCRIPTION:
			return org.eclipse.milo.opcua.stack.core.AttributeId.Description;
		case DISPLAYNAME:
			return org.eclipse.milo.opcua.stack.core.AttributeId.DisplayName;
		case EVENTNOTIFIER:
			return org.eclipse.milo.opcua.stack.core.AttributeId.EventNotifier;
		case EXECUTABLE:
			return org.eclipse.milo.opcua.stack.core.AttributeId.Executable;
		case HISTORIZING:
			return org.eclipse.milo.opcua.stack.core.AttributeId.Historizing;
		case INVERSENAME:
			return org.eclipse.milo.opcua.stack.core.AttributeId.InverseName;
		case ISABSTRACT:
			return org.eclipse.milo.opcua.stack.core.AttributeId.IsAbstract;
		case MINIMUMSAMPLINGRATE:
			return org.eclipse.milo.opcua.stack.core.AttributeId.MinimumSamplingInterval;
		case NODECLASS:
			return org.eclipse.milo.opcua.stack.core.AttributeId.NodeClass;
		case NODEID:
			return org.eclipse.milo.opcua.stack.core.AttributeId.NodeId;
		case SYMMETRIC:
			return org.eclipse.milo.opcua.stack.core.AttributeId.Symmetric;
		case USERACCESSLEVEL:
			return org.eclipse.milo.opcua.stack.core.AttributeId.UserAccessLevel;
		case USEREXECUTABLE:
			return org.eclipse.milo.opcua.stack.core.AttributeId.UserExecutable;
		case USERWRITEMASK:
			return org.eclipse.milo.opcua.stack.core.AttributeId.UserWriteMask;
		case VALUE:
			return org.eclipse.milo.opcua.stack.core.AttributeId.Value;
		case VALUERANK:
			return org.eclipse.milo.opcua.stack.core.AttributeId.ValueRank;
		case WriteMask:
			return org.eclipse.milo.opcua.stack.core.AttributeId.WriteMask;
		default:
			return null;
		}
	}
}
