package opcuaconnector.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ApplicationType;

import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.ApplicationDescription;
import opcuaconnector.proxies.DiscoveryURL;
import opcuaconnector.proxies.Server;
import opcuaconnector.proxies.ServerApplicationDescription;

public class MxApplicationDescriptionFactory {
	private final ApplicationDescriptionType applicationDescriptionType;
	private final org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription opcApplicationDescription;
	private final IContext context;

	public MxApplicationDescriptionFactory(ApplicationDescriptionType applicationDescriptionType,
			org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription opcApplicationDescription,
			IContext context) {
		requireNonNull(applicationDescriptionType,
				"Cannot create an application description for empty application description type.");
		requireNonNull(opcApplicationDescription,
				"Cannot create an application description for empty opc application description.");
		requireNonNull(opcApplicationDescription, "Cannot create an application description without context.");

		this.applicationDescriptionType = applicationDescriptionType;
		this.opcApplicationDescription = opcApplicationDescription;
		this.context = context;
	}

	public ApplicationDescription getApplicationDescription() {
		ApplicationDescription mxApplicationDescription;
		switch (applicationDescriptionType) {
		case FIND_SERVER:
			mxApplicationDescription = new ServerApplicationDescription(context);
			break;
		case GET_ENDPOINTS:
			mxApplicationDescription = new Server(context);
			break;
		default:
			throw new IllegalArgumentException(
					"Cannot create an application description for empty application description type.");
		}
		mxApplicationDescription.setApplicationName(opcApplicationDescription.getApplicationName() != null
				? opcApplicationDescription.getApplicationName().getText()
				: null);
		mxApplicationDescription
				.setApplicationType(toMxApplicationType(opcApplicationDescription.getApplicationType()));
		mxApplicationDescription.setApplicationURI(opcApplicationDescription.getApplicationUri());
		mxApplicationDescription.setDiscoveryProfileURI(opcApplicationDescription.getDiscoveryProfileUri());
		mxApplicationDescription.setGatewayServerURI(opcApplicationDescription.getGatewayServerUri());
		mxApplicationDescription.setProductURI(opcApplicationDescription.getProductUri());
		mxApplicationDescription.setApplicationDescription_DiscoveryURL(createMxDiscoveryURLList(
				nullableopcDiscoveryURLArrayTolist(opcApplicationDescription.getDiscoveryUrls())));
		return mxApplicationDescription;
	}

	private List<DiscoveryURL> createMxDiscoveryURLList(List<String> opcDiscoveryURLList) {
	    return opcDiscoveryURLList.stream()
	            .map(this::createMxDiscoveryURL)
	            .collect(Collectors.toList());
	}

	private DiscoveryURL createMxDiscoveryURL(String discoveryURL) {
	    DiscoveryURL mxDiscoveryURL = new DiscoveryURL(context);
	    mxDiscoveryURL.setdiscoveryUrl(discoveryURL);
	    return mxDiscoveryURL;
	}

	private opcuaconnector.proxies.ENUM_ApplicationType toMxApplicationType(ApplicationType opcApplicationType) {
		switch (opcApplicationType) {
		case Client:
			return opcuaconnector.proxies.ENUM_ApplicationType.CLIENT;
		case Server:
			return opcuaconnector.proxies.ENUM_ApplicationType.SERVER;
		case ClientAndServer:
			return opcuaconnector.proxies.ENUM_ApplicationType.CLIENTANDSERVER;
		case DiscoveryServer:
			return opcuaconnector.proxies.ENUM_ApplicationType.DISCOVERYSERVER;
		default:
			return null;
		}
	}

	public enum ApplicationDescriptionType {
		GET_ENDPOINTS, FIND_SERVER
	}

	private List<String> nullableopcDiscoveryURLArrayTolist(@Nullable String[] opcDiscoveryURLlist) {
		return opcDiscoveryURLlist != null ? Arrays.asList(opcDiscoveryURLlist) : new ArrayList<>();
	}
}
