package opcuaconnector.impl;

public class ENUMTimestampsToReturn {
	private ENUMTimestampsToReturn() {
		throw new IllegalStateException("Utility class");
	}

	public static opcuaconnector.proxies.ENUM_TimestampsToReturn getMxENUM(
			org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn opcTimestampsToReturn) {
		if (opcTimestampsToReturn == null) {
			return null;
		}
		switch (opcTimestampsToReturn) {
		case Server:
			return opcuaconnector.proxies.ENUM_TimestampsToReturn.SERVER;
		case Source:
			return opcuaconnector.proxies.ENUM_TimestampsToReturn.SOURCE;
		case Both:
			return opcuaconnector.proxies.ENUM_TimestampsToReturn.BOTH;
		case Neither:
			return opcuaconnector.proxies.ENUM_TimestampsToReturn.NEITHER;
		case Invalid:
			return opcuaconnector.proxies.ENUM_TimestampsToReturn.INVALID;
		default:
			return null;
		}
	}

	public static org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn getOpcENUM(
			opcuaconnector.proxies.ENUM_TimestampsToReturn mxTimestampToReturn) {
		if (mxTimestampToReturn == null) {
			return null;
		}
		switch (mxTimestampToReturn) {
		case SERVER:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn.Server;
		case SOURCE:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn.Source;
		case BOTH:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn.Both;
		case NEITHER:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn.Neither;
		case INVALID:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn.Invalid;
		default:
			return null;
		}
	}

}
