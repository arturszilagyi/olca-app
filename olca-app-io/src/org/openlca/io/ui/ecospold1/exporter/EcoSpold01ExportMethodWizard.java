/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.io.ui.ecospold1.exporter;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.LCIAMethod;
import org.openlca.io.ui.ObjectWrapper;
import org.openlca.io.ui.SelectObjectsExportPage;

/**
 * Extension of {@link EcoSpold01ExportWizard} for LCIA methods
 * 
 * @author Sebastian Greve
 * 
 */
public class EcoSpold01ExportMethodWizard extends EcoSpold01ExportWizard {

	public EcoSpold01ExportMethodWizard() {
		super(SelectObjectsExportPage.METHOD);
	}

	public EcoSpold01ExportMethodWizard(final IDatabase database,
			final LCIAMethod method) {
		super(SelectObjectsExportPage.METHOD);
		List<ObjectWrapper> list = new ArrayList<>();
		list.add(new ObjectWrapper(method, database));
		setModelComponentsToExport(list);
		setSingleExport(true);
	}

}