package opcuaconnector.impl;

import java.util.Optional;
import static java.util.Objects.requireNonNull;

import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.BrowseResponseStatusCode;
import opcuaconnector.proxies.DataValueStatusCode;
import opcuaconnector.proxies.ENUM_StatusCode;
import opcuaconnector.proxies.MessageMonitoredItemStatusCode;
import opcuaconnector.proxies.MonitoredItemStatusCode;
import opcuaconnector.proxies.WriteNodeStatusCode;

public class MxStatusCodeFactory {

	private final StatusCodeType statusCodeType;
	private final IContext context;
	private final StatusCode opcStatusCode;

	public MxStatusCodeFactory(StatusCodeType statusCodeType, StatusCode opcStatusCode, IContext context) {
		requireNonNull(statusCodeType, "The Status code type cannot be empty");
		requireNonNull(opcStatusCode, "Cannot create status code factory without status code from opc server");
		requireNonNull(context, "Cannot create Status Code factory without context");

		this.statusCodeType = statusCodeType;
		this.opcStatusCode = opcStatusCode;
		this.context = context;
	}

	public opcuaconnector.proxies.StatusCode getStatusCode() {
		opcuaconnector.proxies.StatusCode mxStatusCode;
		switch (this.statusCodeType) {
		case BROWSE:
			mxStatusCode = new BrowseResponseStatusCode(context);
			break;
		case WRITE_NODE_RESPONSE:
			mxStatusCode = new WriteNodeStatusCode(context);
			break;
		case DATAVALUE:
			mxStatusCode = new DataValueStatusCode(context);
			break;
		case MONITOREDITEMSTATUSCODE:
			mxStatusCode = new MonitoredItemStatusCode(context);
			break;
		case MESSAGE_MONITORED_ITEM_STATUSCODE:
			mxStatusCode = new MessageMonitoredItemStatusCode(context);
			break;
		default:
			throw new IllegalArgumentException("This specialization of Status code is not yet implemented.");
		}
		mxStatusCode.setStatus(toMxStatus(opcStatusCode));
		mxStatusCode.set_Value(opcStatusCode.getValue());;
		mxStatusCode.setDescription(toMxDescription(opcStatusCode));
		mxStatusCode.setIsSecurityError(opcStatusCode.isSecurityError());
		return mxStatusCode;
	}

	private ENUM_StatusCode toMxStatus(StatusCode opcStatusCode) {
		if (opcStatusCode.isGood()) {
			return ENUM_StatusCode.GOOD;
		}
		if (opcStatusCode.isBad()) {
			return ENUM_StatusCode.BAD;
		}
		if (opcStatusCode.isUncertain()) {
			return ENUM_StatusCode.UNCERTAIN;
		}
		return null;
	}

	private String toMxDescription(StatusCode opcStatusCode) {
		Optional<String[]> resultList = StatusCodes.lookup(opcStatusCode.getValue());
		return resultList.isPresent() ? resultList.get()[0] : null;
	}

	public enum StatusCodeType {
		BROWSE, WRITE_NODE_RESPONSE, DATAVALUE, MONITOREDITEMSTATUSCODE,
		MODIFYMONITOREDITEMSTATUSCODE, MESSAGE_MONITORED_ITEM_STATUSCODE
	}
}
