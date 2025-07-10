package opcuaconnector.impl;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription.ItemCreationCallback;

import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;

import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IDataType;

import opcuaconnector.impl.ClientManager.Client;
import opcuaconnector.impl.ClientSubscriptionManager.MonitoredItemManager;
import opcuaconnector.impl.MxDataValueFactory.DataValueType;
import opcuaconnector.impl.MxReadValueIdFactory.ReadValueIdType;
import opcuaconnector.impl.MxStatusCodeFactory.StatusCodeType;
import opcuaconnector.proxies.MessageDataValue;
import opcuaconnector.proxies.MessageMonitoredItem;
import opcuaconnector.proxies.MessageMonitoredItemReadValueId;
import opcuaconnector.proxies.MessageMonitoredItemStatusCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OpcMonitoredItemServiceSet {
	private static final MxLogger LOGGER = new MxLogger(OpcMonitoredItemServiceSet.class);

	private Client client;
	private UaSubscription opcSubscription;
	private final IContext context;

	public OpcMonitoredItemServiceSet(Client client, UInteger subscriptionId, IContext context) throws CoreException {
		// Make sure the subscriptionID is initialized by the client by checking whether
		// it exists in the subscription Manager.
		MonitoredItemManager monitoredItemManager = client.getClientSubscriptionManager().getAllSubscriptions().stream()
				.filter(manager -> subscriptionId != null
						&& manager.getOpcSubscription().getSubscriptionId().equals(subscriptionId))
				.findFirst().orElse(null);
		if (monitoredItemManager == null) {
			throw new CoreException("Subscription with ID: " + subscriptionId + " is not initialized.");
		}
		this.client = client;
		this.opcSubscription = monitoredItemManager.getOpcSubscription();
		this.context = context;
	}

	public UaSubscription getOpcSubscription() {
		return this.opcSubscription;
	}

	public List<UaMonitoredItem> opcCreateMonitoredItems(
			List<MonitoredItemCreateRequest> opcMonitoredItemCreateRequestList, String microflowToCall)
			throws CoreException {
		try {
			validateMicroflowName(microflowToCall);
			LOGGER.debug("Requesting creation of " + opcMonitoredItemCreateRequestList.size()
					+ " monitored items for subscription" + opcSubscription.getSubscriptionId());
			List<UaMonitoredItem> monitoredItemResponse = opcSubscription.createMonitoredItems(TimestampsToReturn.Both,
					opcMonitoredItemCreateRequestList, new ItemCreationCallback() {

						@Override
						public void onItemCreated(UaMonitoredItem opcMonitoredItem, int clientHandle) {
							opcMonitoredItem.setValueConsumer(createValueConsumer(opcMonitoredItem, microflowToCall));
							LOGGER.info("Initialized value consumer for monitored item for node ID "
									+ opcMonitoredItem.getReadValueId().getNodeId().toParseableString()
									+ " for attribute id " + opcMonitoredItem.getReadValueId().getAttributeId());
							LOGGER.debug("Initialized value consumer for monitored item for node ID "
									+ opcMonitoredItem.getReadValueId().getNodeId().toParseableString()
									+ " for attribute id " + opcMonitoredItem.getReadValueId().getAttributeId()
									+ ". The client handle is " + clientHandle + ". The microflow to call is "
									+ microflowToCall);
						}
					}).get();
			LOGGER.debug("Succesfully received " + monitoredItemResponse.size() + " monitored items for subscription "
					+ opcSubscription.getSubscriptionId());
			return monitoredItemResponse;
		} catch (InterruptedException e) {
			LOGGER.error("Cannot create monitored item for subscription " + opcSubscription.getSubscriptionId()
					+ " because the thread was occupied. The error is " + e);
			Thread.currentThread().interrupt();
			throw new CoreException(e);
		} catch (ExecutionException e) {
			LOGGER.error("Cannot create monitored item for subscription " + opcSubscription.getSubscriptionId()
					+ ". The error is " + e);
			LOGGER.error(e.getCause());
			throw new CoreException(e);
		}
	}

	public Consumer<DataValue> createValueConsumer(UaMonitoredItem opcMonitoredItem, String mxMicroflowName) {
		return new Consumer<DataValue>() {

			@Override
			public void accept(DataValue opcDataValue) {
				IContext sysContext = null;
				try {
					sysContext = Core.createSystemContext();
					sysContext.startTransaction();
					Map<String, IDataType> inputParams = Core.getInputParameters(mxMicroflowName);

					Entry<String, IDataType> dataValueInput = findInputByEntityName(inputParams,
							MessageDataValue.entityName);
					opcuaconnector.proxies.DataValue mxDataValue = dataValueInput != null
							? new MxDataValueFactory(opcDataValue, DataValueType.MESSAGE_MONITORED_ITEM, context)
									.getDataValue()
							: null;
					String mxDataValueKey = dataValueInput != null ? dataValueInput.getKey() : null;

					Entry<String, IDataType> messageMonitoredItemInput = findInputByEntityName(inputParams,
							MessageMonitoredItem.entityName);
					MessageMonitoredItem mxMessageMonitoredItem = messageMonitoredItemInput != null
							? createMxMessageMonitoredItem(opcMonitoredItem)
							: null;
					String mxMessageMonitoredItemKey = messageMonitoredItemInput != null
							? messageMonitoredItemInput.getKey()
							: null;
					
					Entry<String, IDataType> messageMonitoredItemReadvalueIdInput = findInputByEntityName(inputParams,
							MessageMonitoredItemReadValueId.entityName);
					MessageMonitoredItemReadValueId mxMessageMonitoredItemReadValueId = messageMonitoredItemReadvalueIdInput != null
							? (MessageMonitoredItemReadValueId) new MxReadValueIdFactory(
									opcMonitoredItem.getReadValueId(), ReadValueIdType.MESSAGE, context)
									.getReadValueId()
							: null;
					String mxMessageMonitoredItemReadValueIdKey = messageMonitoredItemReadvalueIdInput != null
							? messageMonitoredItemReadvalueIdInput.getKey()
							: null;

					Core.microflowCall(mxMicroflowName)
							.withParam(mxDataValueKey, mxDataValue != null ? mxDataValue.getMendixObject() : null)
							.withParam(mxMessageMonitoredItemKey,
									mxMessageMonitoredItemKey != null ? mxMessageMonitoredItem.getMendixObject() : null)
							.withParam(mxMessageMonitoredItemReadValueIdKey,
									mxMessageMonitoredItemReadValueId != null
											? mxMessageMonitoredItemReadValueId.getMendixObject()
											: null)
							.execute(sysContext);
					
					sysContext.endTransaction();
				} catch (Exception e) {
					if (sysContext != null)	{
						sysContext.rollbackTransaction();
					}
					LOGGER.error("An error occured while receiving a message for monitored item "
							+ opcMonitoredItem.getMonitoredItemId().longValue() + "." + System.lineSeparator()
							+ "Error message :" + e.getMessage() + System.lineSeparator() + "Full error :" + e);

					// Should not throw an error here as this action is happening in the background.
				}
			}
		};
	}

	private Entry<String, IDataType> findInputByEntityName(Map<String, IDataType> inputParams, String entityName) {
		return inputParams.entrySet().stream()
				.filter(input -> !input.getValue().isList() && entityName.equals(input.getValue().getObjectType()))
				.findFirst().orElse(null);
	}

	private void validateMicroflowName(String mxMicroflowName) {
		Optional<String> existingMfName = Core.getMicroflowNames().stream().filter(name -> name.equals(mxMicroflowName))
				.findFirst();
		if (!existingMfName.isPresent()) {
			throw new NullPointerException(
					"Microflow with name " + mxMicroflowName + " does not exist and can therefore not be used");
		}

	}

	private MessageMonitoredItem createMxMessageMonitoredItem(UaMonitoredItem opcMonitoredItem) {
		MessageMonitoredItem mxMessageMonitoredItem = new MessageMonitoredItem(context);
		mxMessageMonitoredItem.setConfigurationName(client.getConfigurationName());
		mxMessageMonitoredItem.setSubscriptionID(opcSubscription.getSubscriptionId().longValue());
		mxMessageMonitoredItem.setClientHandle(opcMonitoredItem.getClientHandle().longValue());
		mxMessageMonitoredItem.setDiscardOldest(opcMonitoredItem.getDiscardOldest());
		mxMessageMonitoredItem.setMonitoredItemID(
				opcMonitoredItem.getMonitoredItemId() != null ? opcMonitoredItem.getMonitoredItemId().longValue()
						: null);
		mxMessageMonitoredItem.setMonitoringMode(ENUMMonitoringMode.getMxENUM(opcMonitoredItem.getMonitoringMode()));
		mxMessageMonitoredItem.setRequestedQueueSize(
				opcMonitoredItem.getRequestedQueueSize() != null ? opcMonitoredItem.getRequestedQueueSize().longValue()
						: null);
		mxMessageMonitoredItem
				.setRequestedSamplingInterval(BigDecimal.valueOf(opcMonitoredItem.getRequestedSamplingInterval()));
		mxMessageMonitoredItem.setRevisedQueueSize(opcMonitoredItem.getRevisedQueueSize().longValue());
		mxMessageMonitoredItem
				.setRevisedSamplingInterval(BigDecimal.valueOf(opcMonitoredItem.getRevisedSamplingInterval()));
		mxMessageMonitoredItem.setTimestamps(ENUMTimestampsToReturn.getMxENUM(opcMonitoredItem.getTimestamps()));
		mxMessageMonitoredItem.setMessageMonitoredItem_MessageMonitoredItemStatusCode(
				(MessageMonitoredItemStatusCode) new MxStatusCodeFactory(
						StatusCodeType.MESSAGE_MONITORED_ITEM_STATUSCODE, opcMonitoredItem.getStatusCode(), context)
						.getStatusCode());
		mxMessageMonitoredItem.setMessageMonitoredItem_MessageMonitoredItemReadValueId(
				(MessageMonitoredItemReadValueId) new MxReadValueIdFactory(opcMonitoredItem.getReadValueId(),
						ReadValueIdType.MESSAGE, context).getReadValueId());
		return mxMessageMonitoredItem;
	}

	public List<StatusCode> opcDeleteMonitoredItems(List<UaMonitoredItem> opcMonitoredItemsToDelete)
			throws CoreException {
		try {
			LOGGER.debug("Requesting deletion of monitored items with monitored item IDs: "
					+ opcMonitoredItemsToDelete.stream().map(UaMonitoredItem::getMonitoredItemId)
							.map(UInteger::toString).collect(Collectors.joining(", ")));
			List<StatusCode> deleteMonitoredItemResponseStatusCodes = opcSubscription
					.deleteMonitoredItems(opcMonitoredItemsToDelete).get();
			LOGGER.debug("Received " + deleteMonitoredItemResponseStatusCodes.size()
					+ " status codes for delete monitored items request. The statusCodes are "
					+ deleteMonitoredItemResponseStatusCodes.stream().map(StatusCode::toString)
							.collect(Collectors.joining(", ")));
			return deleteMonitoredItemResponseStatusCodes;
		} catch (InterruptedException e) {
			LOGGER.error("Cannot delete monitored item for subscription " + opcSubscription.getSubscriptionId()
					+ " because the thread was occupied. The error is " + e);
			Thread.currentThread().interrupt();
			throw new CoreException(e);
		} catch (ExecutionException e) {
			LOGGER.error("Cannot delete monitored item for subscription " + opcSubscription.getSubscriptionId()
					+ ". The error is " + e);
			throw new CoreException(e);
		}
	}
}