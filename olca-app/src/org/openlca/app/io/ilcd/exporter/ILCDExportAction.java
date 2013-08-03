package org.openlca.app.io.ilcd.exporter;

import java.util.Collections;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action for the export of a model component to ILCD.
 */
public class ILCDExportAction extends Action {

	private Logger log = LoggerFactory.getLogger(getClass());
	private BaseDescriptor descriptor;

	@Override
	public ImageDescriptor getImageDescriptor() {
		return ImageType.ILCD_ICON.getDescriptor();
	}

	@Override
	public String getText() {
		return Messages.ILCDExportActionText;
	}

	@Override
	public void run() {
		if (descriptor == null) {
			log.error("Component or database is null");
			return;
		}
		ILCDExportWizard wizard = new ILCDExportWizard(
				descriptor.getModelType());
		wizard.setComponents(Collections.singletonList(descriptor));
		WizardDialog dialog = new WizardDialog(UI.shell(), wizard);
		dialog.open();

	}

	public void setDescriptor(BaseDescriptor descriptor) {
		this.descriptor = descriptor;
	}

}
