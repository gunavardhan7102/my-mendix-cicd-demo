package opcuaconnector.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.milo.opcua.stack.core.types.structured.BrowseResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.BrowseNode;
import opcuaconnector.proxies.BrowseResponse;
import opcuaconnector.proxies.BrowseResponseStatusCode;
import opcuaconnector.proxies.ContinuationPoint;

public class MxBrowseResponseFactory {
	//private static final MxLogger LOGGER = new MxLogger(MxBrowseResponseFactory.class);
	private final BrowseResult opcBrowseResult;
	private final IContext context;

	public MxBrowseResponseFactory(BrowseResult opcBrowseResult,
			IContext context) {
		this.opcBrowseResult = opcBrowseResult;
		this.context = context;
	}
	
	public BrowseResponse getBrowseResponse() {
		BrowseResponse browseResponse = new BrowseResponse(context);
		browseResponse.setBrowseResponse_BrowseResponseStatusCode(createMxBrowseResponseStatusCode(opcBrowseResult));
		browseResponse.setBrowseResponse_ContinuationPoint(createMxContinuationPoint());
		browseResponse.setBrowseResponse_BrowseNode(context, createBrowseNode());
		return browseResponse;
	}

	private BrowseResponseStatusCode createMxBrowseResponseStatusCode(BrowseResult opcBrowseResult) {
		return (BrowseResponseStatusCode) new MxStatusCodeFactory(MxStatusCodeFactory.StatusCodeType.BROWSE,
				opcBrowseResult.getStatusCode(), context).getStatusCode();
	}

	private ContinuationPoint createMxContinuationPoint() {
		ContinuationPoint newContinuationPoint = new ContinuationPoint(context);
		newContinuationPoint.setValue(opcBrowseResult.getContinuationPoint().toString());
		return newContinuationPoint;
	}
	
	private List<BrowseNode> createBrowseNode() {
		List<BrowseNode> browseNodeList = new ArrayList<>();
		for (ReferenceDescription opcReferenceDescription: opcBrowseResult.getReferences()) {
			browseNodeList.add(new MxBrowseNodeFactory(opcReferenceDescription, context).getBrowseNode());
		}
		return browseNodeList;
	}
}
