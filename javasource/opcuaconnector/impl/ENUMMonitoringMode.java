package opcuaconnector.impl;

import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;

public class ENUMMonitoringMode {
	private ENUMMonitoringMode() {
		throw new IllegalStateException("Utility class");
	}
	
	public static opcuaconnector.proxies.ENUM_MonitoringMode getMxENUM(MonitoringMode opcMonitoringMode) {
		if (opcMonitoringMode == null) {
			return null;
		}
		switch (opcMonitoringMode) {
		case Disabled:
			return opcuaconnector.proxies.ENUM_MonitoringMode.DISABLED;
		case Reporting:
			return opcuaconnector.proxies.ENUM_MonitoringMode.REPORTING;
		case Sampling:
			return opcuaconnector.proxies.ENUM_MonitoringMode.SAMPLING;
		default:
			return null;
		}
	}
	
	public static MonitoringMode getOpcENUM(opcuaconnector.proxies.ENUM_MonitoringMode mxMonitoringMode) {
		if (mxMonitoringMode == null) {
			return null;
		}
		switch (mxMonitoringMode) {
		case DISABLED:
			return MonitoringMode.Disabled;
		case REPORTING:
			return MonitoringMode.Reporting;
		case SAMPLING:
			return MonitoringMode.Sampling;
		default:
			return null;
		}
	}
	
}
