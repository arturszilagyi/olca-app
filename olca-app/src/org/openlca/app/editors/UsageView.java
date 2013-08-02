package org.openlca.app.editors;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.html.HtmlPage;
import org.openlca.app.html.IHtmlResource;
import org.openlca.app.plugin.HtmlView;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.usage.IUseSearch;
import org.openlca.core.model.Actor;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.Process;
import org.openlca.core.model.Source;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * View of usages of a model entity in other entities.
 */
public class UsageView extends FormEditor {

	public static String ID = "views.usage";

	private Logger log = LoggerFactory.getLogger(getClass());
	private BaseDescriptor model;
	private IDatabase database;

	@Override
	protected void addPages() {
		try {
			addPage(new Page());
		} catch (Exception e) {
			log.error("Failed to add form page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		if (input instanceof UsageViewInput) {
			UsageViewInput in = (UsageViewInput) input;
			model = in.getDescriptor();
			database = in.getDatabase();
		}
	}

	private class Page extends FormPage implements HtmlPage {

		private Browser browser;

		public Page() {
			super(UsageView.this, "UsageView.Page", Messages.Common_Usage);
		}

		@Override
		public IHtmlResource getResource() {
			return HtmlView.USAGES_VIEW.getResource();
		}

		@Override
		public void onLoaded() {
			log.trace("page completed, set data");
			registerFunction();
			try {
				List<BaseDescriptor> list = runSearch();
				Gson gson = new Gson();
				String json = gson.toJson(list);
				String function = "setData(" + json + ")";
				System.out.println(function);
				browser.evaluate(function);
			} catch (Exception e) {
				log.trace("Failed to load data: where used", e);
			}
		}

		private List<BaseDescriptor> runSearch() {
			if (model == null || model.getModelType() == null)
				return Collections.emptyList();
			switch (model.getModelType()) {
			case ACTOR:
				return IUseSearch.FACTORY.createFor(Actor.class, database)
						.findUses(Descriptors.toActor(model));
			case SOURCE:
				return IUseSearch.FACTORY.createFor(Source.class, database)
						.findUses(Descriptors.toSource(model));
			case UNIT_GROUP:
				return IUseSearch.FACTORY.createFor(UnitGroup.class, database)
						.findUses(Descriptors.toUnitGroup(model));
			case FLOW_PROPERTY:
				return IUseSearch.FACTORY.createFor(FlowProperty.class, database)
						.findUses(Descriptors.toFlowProperty(model));
			case FLOW:
				return IUseSearch.FACTORY.createFor(Flow.class, database)
						.findUses(Descriptors.toFlow(model));
			case PROCESS:
				return IUseSearch.FACTORY.createFor(Process.class, database)
						.findUses(Descriptors.toProcess(model));
			default:
				return Collections.emptyList();
			}
		}

		@Override
		protected void createFormContent(IManagedForm managedForm) {
			if (model == null)
				return;
			ScrolledForm form = UI.formHeader(managedForm,
					Messages.Common_UsageOf + ": " + model.getDisplayName());
			FormToolkit toolkit = managedForm.getToolkit();
			Composite body = UI.formBody(form, toolkit);
			browser = UI.createBrowser(body, this);
			UI.gridData(browser, true, true);
			form.reflow(true);
		}

		private void registerFunction() {
			new BrowserFunction(browser, "openModel") {
				@Override
				public Object function(Object[] arguments) {
					openModel(arguments);
					return null;
				}
			};
		}

		private void openModel(Object[] args) {
			if (args == null || args.length < 1 || args[0] == null) {
				log.error("Could not open model, no Json string in arg[0]");
				return;
			}
			String json = args[0].toString();
			log.trace("open model: json={}", json);
			try {
				Gson gson = new Gson();
				BaseDescriptor descriptor = gson.fromJson(json,
						BaseDescriptor.class);
				App.openEditor(descriptor);
			} catch (Exception e) {
				log.error("Failed to open model from usage page", e);
			}
		}
	}
}