/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.editors.graphical.command;

import org.eclipse.gef.commands.Command;
import org.openlca.app.Messages;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

public class ReconnectLinkCommand extends Command {

	private ConnectionLink link;
	private ConnectionLink oldLink;
	private ProcessNode sourceNode;
	private ProcessNode targetNode;

	ReconnectLinkCommand() {

	}

	@Override
	public boolean canExecute() {
		if (sourceNode == null)
			return false;
		if (targetNode == null)
			return false;
		if (oldLink == null)
			return false;
		return true;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void execute() {
		oldLink.unlink();
		ProductSystemNode systemNode = sourceNode.getParent();
		systemNode.getProductSystem().getProcessLinks()
				.remove(oldLink.getProcessLink());
		ProcessLink processLink = new ProcessLink();
		processLink.setRecipientId(targetNode.getProcess().getId());
		processLink.setProviderId(sourceNode.getProcess().getId());
		processLink.setFlowId(oldLink.getProcessLink().getFlowId());
		systemNode.getProductSystem().getProcessLinks().add(processLink);
		link = new ConnectionLink();
		link.setSourceNode(sourceNode);
		link.setTargetNode(targetNode);
		link.setProcessLink(processLink);
		link.link();
	}

	@Override
	public String getLabel() {
		return Messages.Systems_ProcessLinkReconnectCommand_Text;
	}

	@Override
	public void redo() {
		link.link();
		oldLink.unlink();
		ProductSystem system = sourceNode.getParent().getProductSystem();
		system.getProcessLinks().remove(oldLink.getProcessLink());
		system.getProcessLinks().add(link.getProcessLink());
	}

	@Override
	public void undo() {
		ProductSystem system = sourceNode.getParent().getProductSystem();
		system.getProcessLinks().remove(link.getProcessLink());
		system.getProcessLinks().add(oldLink.getProcessLink());
		link.unlink();
		oldLink.link();
	}

	void setLink(ConnectionLink oldLink) {
		this.oldLink = oldLink;
	}

	void setSourceNode(ProcessNode sourceNode) {
		this.sourceNode = sourceNode;
	}

	void setTargetNode(ProcessNode targetNode) {
		this.targetNode = targetNode;
	}

}