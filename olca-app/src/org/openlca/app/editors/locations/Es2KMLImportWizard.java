package org.openlca.app.editors.locations;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.MsgBox;
import org.openlca.app.wizards.io.FileImportPage;
import org.openlca.core.model.ModelType;
import org.openlca.io.ecospold2.input.KMLImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Es2KMLImportWizard extends Wizard implements IImportWizard {

	public static final String ID = "wizard.import.kmz";
	private FileImportPage fileImportPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		addPage(fileImportPage = new FileImportPage("xml"));
	}

	@Override
	public boolean performFinish() {
		try {
			getContainer().run(true, false, monitor -> {
				monitor.beginTask(M.ImportingXMLData,
						IProgressMonitor.UNKNOWN);
				File file = fileImportPage.getFiles()[0];
				boolean wasValidFile = new KMLImport(file,
						Database.get()).run();
				if (!wasValidFile)
					MsgBox.info(M.CouldNotFindKMLData);
				monitor.done();
			});
			Navigator.refresh(Navigator.findElement(ModelType.LOCATION));
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to import KMZ", e);
		}
		return true;
	}
}