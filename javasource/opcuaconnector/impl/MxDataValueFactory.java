package opcuaconnector.impl;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;

import com.google.gson.Gson;
import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.impl.MxStatusCodeFactory.StatusCodeType;
import opcuaconnector.proxies.DataValueStatusCode;
import opcuaconnector.proxies.MessageDataValue;
import opcuaconnector.proxies.ReadNodeResponseResults;

public class MxDataValueFactory {
	private final DataValue opcDataValue;
	private final DataValueType dataValueType;
	private final IContext context;

	public MxDataValueFactory(DataValue opcDataValue, DataValueType dataValueType, IContext context) {
		this.opcDataValue = opcDataValue;
		this.dataValueType = dataValueType;
		this.context = context;
	}

	public opcuaconnector.proxies.DataValue getDataValue() {
		opcuaconnector.proxies.DataValue mxDataValue;
		switch (dataValueType) {
		case READ_RESPONSE:
			mxDataValue = new ReadNodeResponseResults(context);
			break;
		case MESSAGE_MONITORED_ITEM:
			mxDataValue = new MessageDataValue(context);
			break;
		default:
			throw new IllegalArgumentException();
		}
		mxDataValue.setValue(new Gson().toJson(opcDataValue.getValue()));
		mxDataValue.setServerTimestamp(
				opcDataValue.getServerTime().isNull() ? null : opcDataValue.getServerTime().getJavaDate());
		mxDataValue.setSeverPicoSeconds(
				opcDataValue.getServerPicoseconds() != null ? opcDataValue.getServerPicoseconds().intValue() : null);
		mxDataValue.setSourceTimestamp(
				opcDataValue.getSourceTime().isNull() ? null : opcDataValue.getSourceTime().getJavaDate());
		mxDataValue.setSourcePicoSeconds(
				opcDataValue.getSourcePicoseconds() != null ? opcDataValue.getSourcePicoseconds().intValue() : null);

		mxDataValue.setDataValue_DataValueStatusCode(
				(DataValueStatusCode) new MxStatusCodeFactory(StatusCodeType.DATAVALUE, opcDataValue.getStatusCode(),
						context).getStatusCode());
		return mxDataValue;
	}

	public enum DataValueType {
		READ_RESPONSE, MESSAGE_MONITORED_ITEM
	}

}
