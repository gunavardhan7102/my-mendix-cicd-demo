package opcuaconnector.impl;

import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;

import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.MessageMonitoredItemReadValueId;
import opcuaconnector.proxies.ReadNodeResponseReadValueId;

public class MxReadValueIdFactory {
	private final ReadValueId opcReadValueId;
	private final ReadValueIdType readValueIdType;
	private final IContext context;

	public MxReadValueIdFactory(ReadValueId opcReadValueId, ReadValueIdType readValueIdType, IContext context) {
		this.opcReadValueId = opcReadValueId;
		this.readValueIdType = readValueIdType;
		this.context = context;
	}

	public opcuaconnector.proxies.ReadValueId getReadValueId() {
		opcuaconnector.proxies.ReadValueId readValueId;
		switch (readValueIdType) {
		case MESSAGE:
			readValueId = new MessageMonitoredItemReadValueId(context);
			break;
		case READNODERESPONSE:
			readValueId = new ReadNodeResponseReadValueId(context);
			break;
		default:
			throw new IllegalArgumentException("Cannot instantiate non specialized Read value Id object.");
		}
		readValueId.setAttributeId(ENUMAttributeId.getMxENUM(opcReadValueId.getAttributeId().intValue()));
		readValueId.setNodeID(opcReadValueId.getNodeId().toParseableString());
		readValueId.setNumericRange(opcReadValueId.getIndexRange());
		return readValueId;
	}

	public enum ReadValueIdType {
		MESSAGE,
		READNODERESPONSE
	}
}
