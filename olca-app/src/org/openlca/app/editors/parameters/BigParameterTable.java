package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Info;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a simple editor that contains a table with all parameters of the
 * database (global and local).
 */
public class BigParameterTable extends SimpleFormEditor {

	public static void show() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		String id = "BigParameterTable";
		Editors.open(
				new SimpleEditorInput(id, id, M.Parameters), id);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		private final List<Param> params = new ArrayList<>();
		private TableViewer table;
		private Text filter;

		public Page() {
			super(BigParameterTable.this,
					"BigParameterTable", M.Parameters);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, M.Parameters);
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);

			Composite filterComp = UI.formComposite(body, tk);
			UI.gridData(filterComp, true, false);
			filter = UI.formText(filterComp, tk, M.Filter);
			filter.setToolTipText("#Type 'error' to filter "
					+ "evaluation errors.");
			filter.addModifyListener(e -> {
				String t = filter.getText();
				if (Strings.nullOrEmpty(t)) {
					table.setInput(params);
				} else {
					List<Param> filtered = params.stream()
							.filter(p -> p.matches(t))
							.collect(Collectors.toList());
					table.setInput(filtered);
				}
			});

			table = Tables.createViewer(
					body, M.Name, "#Parameter scope",
					M.Value, M.Description);
			double w = 1.0 / 4.0;
			Tables.bindColumnWidths(table, w, w, w, w);
			table.setLabelProvider(new Label());
			// Actions: open, usage, edit value

			bindActions();
			mform.reflow(true);
			App.runWithProgress(
					"Loading parameters ...",
					this::initParams,
					() -> table.setInput(params));
		}

		private void bindActions() {
			Action onOpen = Actions.onOpen(() -> {
				Param p = Viewers.getFirstSelected(table);
				if (p == null)
					return;
				if (p.scope() == ParameterScope.GLOBAL) {
					App.openEditor(p.parameter);
				} else if (p.owner != null) {
					App.openEditor(p.owner);
				}
			});
			Action onUsage = Actions.create(
					M.Usage, Icon.LINK.descriptor(), () -> {
						Param p = Viewers.getFirstSelected(table);
						if (p == null)
							return;
						ParameterUsagePage.show(p.parameter.getName());
					});
			Action onEvaluate = Actions.create(
					"#Evaluate formulas", Icon.RUN.descriptor(), () -> {
						App.runWithProgress("#Evaluate formulas ...",
								this::evaluateFormulas, () -> {
									table.setInput(params);
									filter.setText("");
								});
					});
			Action onEdit = Actions.create(M.Edit,
					Icon.EDIT.descriptor(), this::onEdit);

			Actions.bind(table, onOpen, onUsage, onEvaluate, onEdit);
			Tables.onDoubleClick(table, e -> onOpen.run());
		}

		private void initParams() {
			IDatabase db = Database.get();
			Map<Long, ProcessDescriptor> processes = new ProcessDao(db)
					.getDescriptors().stream()
					.collect(Collectors.toMap(d -> d.id, d -> d));
			Map<Long, ImpactMethodDescriptor> methods = new ImpactMethodDao(db)
					.getDescriptors().stream()
					.collect(Collectors.toMap(d -> d.id, d -> d));
			Map<Long, Long> owners = new HashMap<>();
			try {
				String sql = "select id, f_owner from tbl_parameters";
				NativeSql.on(db).query(sql, r -> {
					owners.put(r.getLong(1), r.getLong(2));
					return true;
				});
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("Failed to query parameter onwers", e);
			}

			new ParameterDao(db).getAll().forEach(pr -> {
				Param p = new Param();
				p.parameter = pr;
				params.add(p);
				if (pr.scope == ParameterScope.GLOBAL)
					return;
				p.ownerID = owners.get(pr.getId());
				if (p.ownerID == null)
					return;
				if (pr.scope == ParameterScope.PROCESS) {
					p.owner = processes.get(p.ownerID);
				} else if (pr.scope == ParameterScope.IMPACT_METHOD) {
					p.owner = methods.get(p.ownerID);
				}
			});

			Collections.sort(params);
		}

		private void evaluateFormulas() {
			FormulaInterpreter fi = buildInterpreter();
			for (Param param : params) {
				Parameter p = param.parameter;
				if (p.isInputParameter) {
					param.evalError = false;
					continue;
				}
				Scope scope = param.ownerID == null
						? fi.getGlobalScope()
						: fi.getScope(param.ownerID);
				try {
					p.value = scope.eval(p.formula);
					param.evalError = false;
				} catch (Exception e) {
					param.evalError = true;
				}
			}
		}

		/**
		 * Bind the parameter values and formulas to the respective scopes of a formula
		 * interpreter.
		 */
		private FormulaInterpreter buildInterpreter() {
			FormulaInterpreter fi = new FormulaInterpreter();
			for (Param param : params) {
				Scope scope = null;
				if (param.ownerID == null) {
					scope = fi.getGlobalScope();
				} else {
					scope = fi.getScope(param.ownerID);
					if (scope == null) {
						scope = fi.createScope(param.ownerID);
					}
				}
				Parameter p = param.parameter;
				if (p.isInputParameter) {
					scope.bind(p.getName(), Double.toString(p.value));
				} else {
					scope.bind(p.getName(), p.formula);
				}
			}
			return fi;
		}

		private void onEdit() {
			Param param = Viewers.getFirstSelected(table);
			if (param == null || param.parameter == null)
				return;
			Parameter p = param.parameter;

			// build dialog with validation
			InputDialog dialog = null;
			FormulaInterpreter fi = null;
			if (p.isInputParameter) {
				dialog = new InputDialog(UI.shell(),
						"#Edit value", "Set a new parameter value",
						Double.toString(p.value), s -> {
							try {
								Double.parseDouble(s);
								return null;
							} catch (Exception e) {
								return s + " " + M.IsNotValidNumber;
							}
						});
			} else {
				fi = buildInterpreter();
				Scope scope = param.ownerID == null
						? fi.getGlobalScope()
						: fi.getScope(param.ownerID);
				dialog = new InputDialog(UI.shell(),
						"#Edit formula", "Set a new parameter formula",
						p.formula, s -> {
							try {
								scope.eval(s);
								return null;
							} catch (Exception e) {
								return s + " " + M.IsInvalidFormula;
							}
						});
			}

			// parse the value from the dialog
			if (dialog.open() != Window.OK)
				return;
			String val = dialog.getValue();
			if (p.isInputParameter) {
				try {
					p.value = Double.parseDouble(val);
					param.evalError = false;
				} catch (Exception e) {
					param.evalError = true;
				}
			} else {
				try {
					p.formula = val;
					Scope scope = param.ownerID == null
							? fi.getGlobalScope()
							: fi.getScope(param.ownerID);
					p.value = scope.eval(val);
					param.evalError = false;
				} catch (Exception e) {
					param.evalError = true;
				}
			}

			// update the parameter in the database
			ParameterDao dao = new ParameterDao(
					Database.get());
			param.parameter = dao.update(p);
			table.refresh();
		}
	}

	/** Stores a parameter object and its owner. */
	private class Param implements Comparable<Param> {

		/**
		 * We have the owner ID as a separate field because a parameter could have a
		 * link to an owner that does not exist anymore in the database (it is an error
		 * but such things seem to happen).
		 */
		Long ownerID;

		/** If null, it is a global parameter. */
		CategorizedDescriptor owner;

		Parameter parameter;

		boolean evalError;

		@Override
		public int compareTo(Param other) {
			int c = Strings.compare(
					this.parameter.getName(),
					other.parameter.getName());
			if (c != 0)
				return c;

			if (this.owner == null && other.owner == null)
				return 0;
			if (this.owner == null)
				return -1;
			if (other.owner == null)
				return 1;

			return Strings.compare(
					Labels.getDisplayName(this.owner),
					Labels.getDisplayName(other.owner));
		}

		boolean matches(String filter) {
			if (filter == null)
				return true;
			if (parameter.getName() == null)
				return false;
			String f = filter.trim().toLowerCase();
			if (evalError && f.equals("error"))
				return true;
			String n = parameter.getName().toLowerCase();
			return n.contains(f);
		}

		ParameterScope scope() {
			return parameter.scope == null
					? ParameterScope.GLOBAL
					: parameter.scope;
		}

	}

	private class Label extends LabelProvider
			implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 1 || !(obj instanceof Param))
				return null;
			Param p = (Param) obj;
			switch (p.scope()) {
			case GLOBAL:
				return Images.get(ModelType.PARAMETER);
			case IMPACT_METHOD:
				return Images.get(ModelType.IMPACT_METHOD);
			case PROCESS:
				return Images.get(ModelType.PROCESS);
			default:
				return null;
			}
		}

		@Override
		public Color getBackground(Object obj, int col) {
			return null;
		}

		@Override
		public Color getForeground(Object obj, int col) {
			if (!(obj instanceof Param))
				return null;
			Param param = (Param) obj;
			if (col == 1 &&
					param.scope() != ParameterScope.GLOBAL
					&& param.owner == null)
				return Colors.systemColor(SWT.COLOR_RED);
			if (col == 2 && param.evalError)
				return Colors.systemColor(SWT.COLOR_RED);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Param))
				return null;
			Param param = (Param) obj;
			if (param.parameter == null)
				return " - ";
			Parameter p = param.parameter;
			switch (col) {
			case 0:
				return p.getName();
			case 1:
				if (param.scope() == ParameterScope.GLOBAL)
					return M.GlobalParameter;
				if (param.owner == null)
					return "!! missing !!";
				return Labels.getDisplayName(param.owner);
			case 2:
				return p.isInputParameter
						? Double.toString(p.value)
						: param.evalError
								? "!! error !! " + p.formula
								: p.formula + " = " + Numbers.format(p.value);
			case 3:
				return p.getDescription();
			default:
				return null;
			}
		}
	}

}
