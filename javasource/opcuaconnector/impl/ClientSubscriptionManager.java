package opcuaconnector.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import opcuaconnector.proxies.MonitoredItem;

public class ClientSubscriptionManager {
	private ConcurrentHashMap<UInteger, MonitoredItemManager> monitoredItemManagerList = new ConcurrentHashMap<>(); 
	
	public void addMonitoredItem(UaSubscription opcSubscription, UaMonitoredItem opcMonitoredItem) {
		MonitoredItemManager monitoredItemManager = getOrCreateMonitoredItemManager(opcSubscription);
		monitoredItemManager.addMonitoredItem(opcMonitoredItem);
	}

	public void removeMonitoredItem(UaSubscription opcSubscription, MonitoredItem mxMonitoredItem) {
		MonitoredItemManager monitoredItemManager = getOrCreateMonitoredItemManager(opcSubscription);
		monitoredItemManager.removeMonitoredItem(mxMonitoredItem);
	}

	public List<MonitoredItemManager> getAllSubscriptions(){
		return new ArrayList<>(monitoredItemManagerList.values());
	}
	
	public void removeSubscription(UInteger subscriptionId) {
		monitoredItemManagerList.remove(subscriptionId);
	}

	public MonitoredItemManager getOrCreateMonitoredItemManager(UaSubscription opcSubscription) {
		if (monitoredItemManagerList.getOrDefault(opcSubscription.getSubscriptionId(), null) != null) {
			return monitoredItemManagerList.get(opcSubscription.getSubscriptionId());
		}
		MonitoredItemManager newMonitoredItemManager = new MonitoredItemManager(opcSubscription);
		monitoredItemManagerList.put(opcSubscription.getSubscriptionId(), newMonitoredItemManager);
		return newMonitoredItemManager;
	}
	
	public MonitoredItemManager getMonitoredItemManager(UInteger subscriptionID) {
		Optional<MonitoredItemManager> optionalMonitoredItemManager = getAllSubscriptions().stream()
				.filter(t -> t.getOpcSubscription().getSubscriptionId().equals(subscriptionID))
				.findFirst();
		if (!optionalMonitoredItemManager.isPresent()) {
			throw new NullPointerException("Subscription with ID " + subscriptionID
					+ " is not initialized and can therefore not be used");
		}
		return optionalMonitoredItemManager.get();
	}

	public class MonitoredItemManager {
		private UaSubscription opcSubscription;
		private ConcurrentHashMap<UInteger, UaMonitoredItem> opcMonitoredItemList = new ConcurrentHashMap<>();

		private MonitoredItemManager(UaSubscription opcSubscription) {
			this.opcSubscription = opcSubscription;
		}
		
		private void addMonitoredItem(UaMonitoredItem opcMonitoredItem) {
			if (opcMonitoredItemList.getOrDefault(opcMonitoredItem.getMonitoredItemId(), null) == null) {
				opcMonitoredItemList.put(opcMonitoredItem.getMonitoredItemId(), opcMonitoredItem);
			}
		}

		private void removeMonitoredItem(MonitoredItem mxMonitoredItem) {
			if (opcMonitoredItemList.getOrDefault(uint(mxMonitoredItem.get_MonitoredItemID()), null) != null) {
				opcMonitoredItemList.remove(uint(mxMonitoredItem.get_MonitoredItemID()));
			}
		}
		
		public UaSubscription getOpcSubscription() {
			return opcSubscription;
		}
		
		public List<UaMonitoredItem> getOpcMonitoredItems(){
			return new ArrayList<>(opcMonitoredItemList.values());
		}
	}

}
