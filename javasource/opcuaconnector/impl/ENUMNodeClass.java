package opcuaconnector.impl;

import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

public class ENUMNodeClass {
	private ENUMNodeClass() {
		throw new IllegalStateException("Utility class");
	}

	public static opcuaconnector.proxies.ENUM_NodeClass toMxNodeClass(NodeClass opcNodeClass) {
		if (opcNodeClass == null) {
			return null;
		}
		switch (opcNodeClass) {
		case DataType: {
			return opcuaconnector.proxies.ENUM_NodeClass.DATATYPE;
		}
		case Method: {
			return opcuaconnector.proxies.ENUM_NodeClass.METHOD;
		}
		case Object: {
			return opcuaconnector.proxies.ENUM_NodeClass._OBJECT;
		}
		case ObjectType: {
			return opcuaconnector.proxies.ENUM_NodeClass.OBJECTTYPE;
		}
		case ReferenceType: {
			return opcuaconnector.proxies.ENUM_NodeClass.REFERENCETYPE;
		}
		case Variable: {
			return opcuaconnector.proxies.ENUM_NodeClass.VARIABLE;
		}
		case VariableType: {
			return opcuaconnector.proxies.ENUM_NodeClass.VARIABLETYPE;
		}
		case View: {
			return opcuaconnector.proxies.ENUM_NodeClass.VIEW;
		}
		default: {
			return null;
		}
		}
	}
}
