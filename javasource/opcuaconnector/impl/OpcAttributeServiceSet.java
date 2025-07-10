package opcuaconnector.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExpandedNodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.XmlElement;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.ULong;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;

import com.mendix.core.CoreException;

import opcuaconnector.proxies.ENUM_DefaultVariantType;
import opcuaconnector.proxies.WriteNodeWriteValue;

public class OpcAttributeServiceSet {
	// OPC-UA spec 4. 5.10 Attirbute service set
	private static final MxLogger LOGGER = new MxLogger(OpcAttributeServiceSet.class);

	private OpcUaClient opcClient;

	public OpcAttributeServiceSet(OpcUaClient opcClient) {
		this.opcClient = opcClient;
	}

	/**
	 * Gets a complete node object from the server that does include all properties.
	 * Used in get node details
	 * 
	 * @param opcNodeId parsed opc node ID object
	 * @return complete node object
	 * @throws CoreException whenever the object cannot be found in the namespace
	 */
	public UaNode getOpcUaNode(NodeId opcNodeId) throws CoreException {
		try {
			LOGGER.debug("Requesting node details for node with node ID " + opcNodeId.toParseableString());
			UaNode opcUaNode = opcClient.getAddressSpace().getNode(opcNodeId);
			LOGGER.debug("Received a ua node object with node ID " + opcNodeId.toParseableString() + " and browse name "
					+ opcUaNode.getBrowseName().getName());
			return opcUaNode;

		} catch (UaException e) {
			LOGGER.error("Cannot get UaNode for node " + opcNodeId.toString() + " because " + e.getMessage());
			throw new CoreException("Cannot get node details from node " + opcNodeId.toParseableString());
		}
	}

	/**
	 * Reads attributes via read value ids from the server.
	 * 
	 * @param opcMaxAge             default 0, means read latest value by best
	 *                              effort. Non-negative
	 * @param opcTimestampsToReturn which timestamps will be returned
	 * @param opcReadValueIdList    which attributes need to be returned
	 * @return dataValues that have been read
	 * @throws CoreException whenever the read action returns an error
	 */
	public ReadResponse miloRead(double opcMaxAge, TimestampsToReturn opcTimestampsToReturn,
			List<ReadValueId> opcReadValueIdList) throws CoreException {
		try {
			LOGGER.debug("Requesting values through read operation for " + opcReadValueIdList.size()
					+ " read value IDs with Max age " + opcMaxAge + " and timestamps to return "
					+ opcTimestampsToReturn.toString());
			ReadResponse opcReadResponse = opcClient.read(opcMaxAge, opcTimestampsToReturn, opcReadValueIdList).get();
			LOGGER.debug("Received " + opcReadResponse.getResults().length + " results for read request");
			return opcReadResponse;
		} catch (InterruptedException e) {
			LOGGER.error(
					"Cannot read from opc server, because the action was interrupted. The error was " + e.getMessage());
			Thread.currentThread().interrupt();
			throw new CoreException(e);
		} catch (ExecutionException e) {
			LOGGER.error("Cannot read from opc server. The error was " + e.getMessage());
			throw new CoreException(e);
		}
	}

	/**
	 * Since it we need to convert our Mendix object to a dataValue to write and
	 * this can be all types of specialization of the dataValue (in particular
	 * dataValue contains Variant which can be initialized with ANY type object. We
	 * need to support the default types and want to make it user friendly, we left
	 * the type OPTIONAL and do a read latest value option, if no type is provided
	 * by the user.
	 * 
	 * @param mxWriteNodeList Mendix objects with instructions what should be
	 *                        written to the server
	 * @return Response whether the action was a success for each value
	 * @throws CoreException Whenever the write action throws an error, or the input
	 *                       cannot be parsed.
	 */
	public WriteResponse miloWriteMxImplementation(List<WriteNodeWriteValue> mxWriteNodeWriteValueList)
			throws CoreException {
		List<WriteValue> writeValueList = new ArrayList<>();
		for (WriteNodeWriteValue mxWriteNodeWriteValue : mxWriteNodeWriteValueList) {
			writeValueList.add(new WriteValue(new OpcNode(mxWriteNodeWriteValue.getNodeID()).getOpcNodeId(),
					ENUMAttributeId.getOpcENUM(mxWriteNodeWriteValue.getAttributeId()).uid(), null,
					createOpcDataValue(mxWriteNodeWriteValue)));
		}
		return miloWrite(writeValueList);
	}

	/**
	 * Parses the Mendix Objects to a dataValue object, if no variant type is
	 * provided, tries to read the latest value from the server, to determine the
	 * type.
	 * 
	 * @param mxWriteNode instructions to write
	 * @return Opc DataValue object that will be used to write
	 * @throws CoreException if the conversion is unsuccessful
	 */
	private DataValue createOpcDataValue(WriteNodeWriteValue mxWriteNodeWriteValue) throws CoreException {
		ENUM_DefaultVariantType variantType = mxWriteNodeWriteValue.getVariantType() != null
				? mxWriteNodeWriteValue.getVariantType()
				: readDefaultVariantType(mxWriteNodeWriteValue);
		String payload = mxWriteNodeWriteValue.getPayload();
		Object input = null;
		try {
			switch (variantType) {
			case _BOOLEAN:
				// Default behavior of Boolean.valueof is to return false if it cannot parse it,
				// which is not what we want.
				if (!payload.equalsIgnoreCase("true") && !payload.equalsIgnoreCase("false")) {
					throw new IllegalArgumentException("Cannot convert input string " + payload + " to type Boolean");
				}
				input = Boolean.valueOf(payload);
				break;
			case SBYTE:
				input = Byte.valueOf(payload);
				break;
			case _BYTE:
				input = UByte.valueOf(payload);
				break;
			case INT16:
				input = Short.valueOf(payload);
				break;
			case UINT16:
				input = UShort.valueOf(payload);
				break;
			case INT32:
				input = Integer.valueOf(payload);
				break;
			case UINT32:
				input = UInteger.valueOf(payload);
				break;
			case INT64:
				input = Long.valueOf(payload);
				break;
			case UINT64:
				input = ULong.valueOf(payload);
				break;
			case _FLOAT:
				input = Float.valueOf(payload);
				break;
			case _DOUBLE:
				input = Double.valueOf(payload);
				break;
			case _STRING:
				input = payload;
				break;
			case DATETIME:
				input = new DateTime(
						Date.from(LocalDateTime.parse(payload).toInstant(ZoneOffset.ofHoursMinutes(0, 0))));
				break;
			case _GUID:
				input = UUID.fromString(payload);
				break;
			case NODEID:
				input = new OpcNode(payload).getOpcNodeId();
				break;
			case LOCALIZEDTEXT:
				input = new LocalizedText(payload);
				break;
			case BYTESTRING:
				input = new ByteString(payload.getBytes());
				break;
			case XMLELEMENT:
				input = new XmlElement(payload);
				break;
			case STATUSCODE:
				input = new StatusCode(Long.parseLong(payload));
				break;
			case EXPANDEDNODEID:
			case QUALIFIEDNAME:
			case EXTENSIONOBJECT:
			case DATAVALUE:
			case VARIANT:
			case DIAGNOSTICINFO:
				throw new IllegalArgumentException(
						"Default variant type " + variantType + " is currently not yet supported.");
			default:
				throw new IllegalArgumentException("Default variant type " + variantType + " is not supported.");
			}
		} catch (Exception e) {
			LOGGER.error("Cannot convert " + payload + " to type " + variantType + " because " + e.getMessage());
			throw new CoreException("An error occured while converting " + payload + " to type " + variantType);
		}
		return mxWriteNodeWriteValue.get_IsWritingDataValueOnly() ? DataValue.valueOnly(new Variant(input))
				: new DataValue(new Variant(input));
	}

	/**
	 * Reads the latest value for the request value. then tries to parse it to a
	 * default value type
	 * 
	 * @param mxWriteNode instructions to write
	 * @return Mendix Default Variant type enumeration to know how to create a
	 *         variant object
	 * @throws CoreException whenever the read value is not a default variant type
	 */
	private ENUM_DefaultVariantType readDefaultVariantType(WriteNodeWriteValue mxWriteNodeWriteValue)
			throws CoreException {
		LOGGER.warn("Write node type is empty. Therefore reading the latest value to get the type");
		ReadResponse response = miloRead(0, TimestampsToReturn.Neither,
				Collections.singletonList(new ReadValueId(new OpcNode(mxWriteNodeWriteValue.getNodeID()).getOpcNodeId(),
						ENUMAttributeId.getOpcENUM(mxWriteNodeWriteValue.getAttributeId()).uid(), null, null)));
		try {
			Optional<ExpandedNodeId> dataType = response.getResults()[0].getValue().getDataType();
			if (dataType.isPresent()) {
				Object identifier = dataType.get().getIdentifier();
				if (identifier instanceof UInteger) {
					// Note that OPC default variant types are one-indexed, Mendix enumeration are
					// zero-indexed. therefore the -1 difference.
					Optional<ENUM_DefaultVariantType> enumVal = Arrays.asList(ENUM_DefaultVariantType.values()).stream()
							.filter(val -> val.ordinal() == ((UInteger) identifier).intValue() - 1).findFirst();
					if (enumVal.isPresent()) {
						return enumVal.get();
					}
				}
				LOGGER.debug("dataType identifier is an integer: " + (identifier instanceof UInteger)
						+ " and does not match a default variant type. Identifier is " + identifier.toString());
			}
		} catch (Exception e) {
			LOGGER.error("Cannot match the latest value to a default variant type for node "
					+ mxWriteNodeWriteValue.getNodeID() + " and attribute ID "
					+ mxWriteNodeWriteValue.getAttributeId().getCaption()
					+ " in order to write the payload to this node. The read value was "
					+ response.getResults()[0].getValue() + ". The error was " + e.getMessage());
			throw new CoreException(
					"Cannot read the latest value to determine the type of the value to write to the server");
		}
		throw new IllegalArgumentException("Cannot read the type of the latest value from the server for node ID "
				+ mxWriteNodeWriteValue.getNodeID() + " and attribute ID "
				+ mxWriteNodeWriteValue.getAttributeId().getCaption());
	}

	/**
	 * Writes attributes via write values to the server.
	 * 
	 * @param opcWriteValueList List of instructions to write
	 * @return Response object with statuses
	 * @throws CoreException Whenever the write action throws an error
	 */
	private WriteResponse miloWrite(List<WriteValue> opcWriteValueList) throws CoreException {
		try {
			LOGGER.debug("Requesting to write values through write operation for " + opcWriteValueList.size()
					+ " write values");
			WriteResponse opcWriteResponse = opcClient.write(opcWriteValueList).get();
			LOGGER.debug("Received " + opcWriteResponse.getResults().length + " write values from write operations");
			return opcWriteResponse;
		} catch (InterruptedException e) {
			LOGGER.error(
					"Cannot write to opc server, because the action was interrupted. The error was " + e.getMessage());
			Thread.currentThread().interrupt();
			throw new CoreException(e);
		} catch (ExecutionException e) {
			LOGGER.error("Cannot write to opc server. The error was " + e.getMessage());
			throw new CoreException(e);
		}
	}
}
