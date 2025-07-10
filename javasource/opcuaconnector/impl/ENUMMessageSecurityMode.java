package opcuaconnector.impl;

import opcuaconnector.proxies.ENUM_MessageSecurityMode;

public class ENUMMessageSecurityMode {
	private ENUMMessageSecurityMode() {
		throw new IllegalStateException("Utility class");
	}
	
	public static ENUM_MessageSecurityMode toMxMessageSecurityMode(
			org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode opcSecurityMode) {
		if (opcSecurityMode == null) {
			return null;
		}
		switch (opcSecurityMode) {
		case None: {
			return ENUM_MessageSecurityMode.NONE;
		}
		case Invalid: {
			return ENUM_MessageSecurityMode.INVALID;
		}
		case Sign: {
			return ENUM_MessageSecurityMode.SIGN;
		}
		case SignAndEncrypt: {
			return ENUM_MessageSecurityMode.SIGNENCRYPT;
		}
		default:
			return null;
		}
	}

	public static org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode fromMxMessageSecurityMode(
			ENUM_MessageSecurityMode mxSecurityMode) {
		if (mxSecurityMode == null) {
			return null;
		}
		switch (mxSecurityMode) {
		case NONE:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode.None;
		case INVALID:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode.Invalid;
		case SIGN:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode.Sign;
		case SIGNENCRYPT:
			return org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode.SignAndEncrypt;
		default:
			return null;
		}
	}
}
