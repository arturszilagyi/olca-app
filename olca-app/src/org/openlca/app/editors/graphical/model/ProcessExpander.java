package org.openlca.app.editors.graphical.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.gef.commands.Command;
import org.openlca.app.editors.graphical.command.ExpansionCommand;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.app.rcp.images.Icon;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;

class ProcessExpander extends ImageFigure {

	private ProcessNode node;
	private Side side;
	private boolean expanded;
	// isCollapsing is used to prevent endless recursion in collapse()
	private boolean isCollapsing;

	ProcessExpander(ProcessNode node, Side side) {
		this.node = node;
		this.side = side;
		setImage(Icon.PLUS.get());
		setVisible(shouldBeVisible());
		addMouseListener(new ExpansionListener());
	}

	boolean shouldBeVisible() {
		ProductSystemNode sysNode = node.parent();
		MutableProcessLinkSearchMap linkSearch = sysNode.linkSearch;
		long processId = node.process.id;
		for (ProcessLink link : linkSearch.getLinks(processId)) {
			FlowType type = sysNode.flows.type(link.flowId);
			boolean isProvider = link.providerId == processId;
			if (side == Side.INPUT) {
				if (type == FlowType.PRODUCT_FLOW && !isProvider)
					return true;
				if (type == FlowType.WASTE_FLOW && isProvider)
					return true;
			} else if (side == Side.OUTPUT) {
				if (type == FlowType.PRODUCT_FLOW && isProvider)
					return true;
				if (type == FlowType.WASTE_FLOW && !isProvider)
					return true;
			}
		}
		return false;
	}

	void expand() {
		createNecessaryNodes();
		setImage(Icon.MINUS.get());
		expanded = true;

		// set expanded nodes visible
		List<ProcessNode> nodes = new ArrayList<>();
		for (Link link : node.links) {
			ProcessNode match = getMatchingNode(link);
			if (match == null || nodes.contains(match))
				continue;
			match.setVisible(true);
			nodes.add(match);
		}
		// then the links of the nodes because
		// there visibility depends on the
		// visibility of the nodes
		for (ProcessNode n : nodes) {
			for (Link link : n.links) {
				link.updateVisibilty();
			}
		}
	}

	private void createNecessaryNodes() {
		ProductSystemNode sysNode = node.parent();
		long processID = node.process.id;
		List<ProcessLink> links = sysNode.linkSearch.getLinks(processID);
		for (ProcessLink pLink : links) {
			FlowType type = sysNode.flows.type(pLink.flowId);
			if (type == null || type == FlowType.ELEMENTARY_FLOW)
				continue;
			boolean isProvider = processID == pLink.providerId;
			long otherID = isProvider ? pLink.processId : pLink.providerId;
			ProcessNode outNode;
			ProcessNode inNode;
			if (isInputNode(type, pLink, isProvider)) {
				inNode = this.node;
				outNode = node(otherID, sysNode);
			} else if (isOutputNode(type, pLink, isProvider)) {
				outNode = this.node;
				inNode = node(otherID, sysNode);
			} else {
				continue;
			}
			Link link = new Link();
			link.outputNode = outNode;
			link.inputNode = inNode;
			link.processLink = pLink;
			link.link();
		}
	}

	private boolean isInputNode(FlowType type, ProcessLink link,
			boolean isProvider) {
		if (side != Side.INPUT)
			return false;
		if (isProvider && type == FlowType.WASTE_FLOW)
			return true; // waste input
		if (!isProvider && type == FlowType.PRODUCT_FLOW)
			return true; // product input
		return false;
	}

	private boolean isOutputNode(FlowType type, ProcessLink link,
			boolean isProvider) {
		if (side != Side.OUTPUT)
			return false;
		if (isProvider && type == FlowType.PRODUCT_FLOW)
			return true; // product output
		if (!isProvider && type == FlowType.WASTE_FLOW)
			return true; // waste output
		return false;
	}

	private ProcessNode node(long processID, ProductSystemNode sysNode) {
		ProcessNode node = sysNode.getProcessNode(processID);
		if (node != null)
			return node;
		node = ProcessNode.create(processID);
		sysNode.add(node);
		return node;
	}

	void collapse(ProcessNode initialNode) {
		if (isCollapsing)
			return;
		isCollapsing = true;
		Link[] links = node.links.toArray(
				new Link[node.links.size()]);
		for (Link link : links) {
			ProcessNode thisNode = side == Side.INPUT ? link.inputNode : link.outputNode;
			ProcessNode otherNode = side == Side.INPUT ? link.outputNode : link.inputNode;
			if (!thisNode.equals(node))
				continue;
			link.unlink();
			otherNode.collapseLeft(initialNode);
			otherNode.collapseRight(initialNode);
			if (otherNode.equals(initialNode))
				continue;
			if (!otherNode.links.isEmpty())
				continue;
			node.parent().remove(otherNode);
		}
		setImage(Icon.PLUS.get());
		isCollapsing = false;
		expanded = false;
	}

	private ProcessNode getMatchingNode(Link link) {
		ProcessNode source = link.outputNode;
		ProcessNode target = link.inputNode;
		if (side == Side.INPUT)
			if (target.equals(node))
				if (!source.equals(node))
					return source;
		if (side == Side.OUTPUT)
			if (source.equals(node))
				if (!target.equals(node))
					return target;
		return null;
	}

	void refresh() {
		setVisible(shouldBeVisible());
		if (expanded)
			setImage(Icon.MINUS.get());
		else
			setImage(Icon.PLUS.get());
	}

	boolean isExpanded() {
		return expanded;
	}

	void setExpanded(boolean value) {
		expanded = value;
	}

	enum Side {
		INPUT, OUTPUT;
	}

	private class ExpansionListener implements MouseListener {

		@Override
		public void mouseDoubleClicked(MouseEvent me) {
		}

		@Override
		public void mousePressed(MouseEvent me) {
			Command command = getCommand();
			node.parent().editor.getCommandStack().execute(command);
		}

		private Command getCommand() {
			if (side == Side.INPUT) {
				return expanded
						? ExpansionCommand.collapseLeft(node)
						: ExpansionCommand.expandLeft(node);
			} else {
				return expanded
						? ExpansionCommand.collapseRight(node)
						: ExpansionCommand.expandRight(node);
			}
		}

		@Override
		public void mouseReleased(MouseEvent me) {
		}
	}

}
