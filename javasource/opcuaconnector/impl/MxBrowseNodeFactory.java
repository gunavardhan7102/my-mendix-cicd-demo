package opcuaconnector.impl;

import org.eclipse.milo.opcua.stack.core.types.structured.ReferenceDescription;

import com.mendix.systemwideinterfaces.core.IContext;

import opcuaconnector.proxies.BrowseNode;

public class MxBrowseNodeFactory {
	private final ReferenceDescription opcReferenceDescription;
	private final IContext context;

	public MxBrowseNodeFactory(ReferenceDescription opcReferenceDescription, IContext context) {
		this.opcReferenceDescription = opcReferenceDescription;
		this.context = context;
	}
	
	public BrowseNode getBrowseNode() {
		BrowseNode mxBrowseNode = new BrowseNode(context);
		mxBrowseNode.setDisplayName(opcReferenceDescription.getDisplayName().getText());
		mxBrowseNode.setNodeClass(ENUMNodeClass.toMxNodeClass(opcReferenceDescription.getNodeClass()));
		mxBrowseNode.setNodeId(opcReferenceDescription.getNodeId().toParseableString());
		mxBrowseNode.setBrowseName(opcReferenceDescription.getBrowseName().getName());
		return mxBrowseNode;
	}
}