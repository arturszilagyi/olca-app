/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.actions;

import org.eclipse.jface.action.IAction;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;

/**
 * Interface for actions that want to export model components
 * 
 * @author Sebastian Greve
 * 
 */
public interface IExportAction extends IAction {

	/**
	 * Setter of the component that should be exported
	 * 
	 * @param component
	 *            The component to export
	 */
	void setComponent(final IModelComponent component);

	/**
	 * Setter of the database
	 * 
	 * @param database
	 *            The database to access the model components information
	 */
	void setDatabase(final IDatabase database);

}