/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;
import org.openlca.ui.dnd.TextDropComponent;

/**
 * Abstract form page for model components
 * 
 * @author Sebastian Greve
 * 
 */
public abstract class ModelEditorPage extends FormPage {

	private IDatabase database;
	private ScrolledForm form;

	public ModelEditorPage(ModelEditor editor, String id, String title) {
		super(editor, id, title);
		this.database = editor.getDatabase();
	}

	protected TextDropComponent createDropComponent(Composite parent,
			FormToolkit toolkit, String labelText,
			IModelComponent initialObject,
			Class<? extends IModelComponent> objectClass, boolean necessary) {
		NavigationRoot root = null;
		Navigator navigator = (Navigator) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(Navigator.ID);
		if (navigator != null) {
			root = navigator.getRoot();
		}
		return UIFactory.createDropComponent(parent, labelText, toolkit,
				initialObject, objectClass, necessary, database, root);
	}

	protected void updateFormTitle() {
		if (form != null) {
			form.getForm().setText(
					getFormTitle() + " (" + database.getName() + ")");
		}
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		// configure form
		form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		toolkit.decorateFormHeading(form.getForm());
		form.setText(getFormTitle() + " (" + database.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ 

		IToolBarManager toolBar = form.getToolBarManager();
		RefreshAction action = new RefreshAction();
		toolBar.add(action);
		toolBar.update(true);

		Composite body = UI.formBody(getForm(), toolkit);

		createContents(body, toolkit);
		setData();
		initListeners();
		body.setFocus();
		form.reflow(true);
	}

	protected abstract void createContents(Composite body, FormToolkit toolkit);

	protected IDatabase getDatabase() {
		return database;
	}

	protected abstract String getFormTitle();

	protected void initListeners() {
	}

	protected abstract void setData();

	public ScrolledForm getForm() {
		return form;
	}

	private class RefreshAction extends Action {

		public RefreshAction() {
			setText(Messages.Reload);
			setImageDescriptor(ImageType.REFRESH_ICON.getDescriptor());
		}

		@Override
		public void run() {
			OpenEditorAction action = new OpenEditorAction();
			action.setModelComponent(database,
					((ModelEditor) getEditor()).getModelComponent());
			action.run(true);
		}
	}

}