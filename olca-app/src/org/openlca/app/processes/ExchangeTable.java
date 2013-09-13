package org.openlca.app.processes;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Event;
import org.openlca.app.Messages;
import org.openlca.app.components.IModelDropHandler;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CategoryPath;
import org.openlca.app.util.Error;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.FlowDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;

import com.google.common.eventbus.Subscribe;

class ExchangeTable {

	private final boolean forInputs;
	private boolean showFormulas = true;
	private final ProcessEditor editor;
	private Process process;

	private final String FLOW = Messages.Flow;
	private final String CATEGORY = Messages.Category;
	private final String FLOW_PROPERTY = Messages.FlowProperty;
	private final String UNIT = Messages.Unit;
	private final String AMOUNT = Messages.Amount;
	private TableViewer viewer;

	public static void forInputs(Section section, FormToolkit toolkit,
			ProcessEditor editor) {
		ExchangeTable table = new ExchangeTable(true, editor);
		table.render(section, toolkit);
	}

	public static void forOutputs(Section section, FormToolkit toolkit,
			ProcessEditor editor) {
		ExchangeTable table = new ExchangeTable(false, editor);
		table.render(section, toolkit);
	}

	private ExchangeTable(boolean forInputs, ProcessEditor editor) {
		this.forInputs = forInputs;
		this.editor = editor;
		this.process = editor.getModel();
		editor.getEventBus().register(this);
	}

	@Subscribe
	public void handleEvaluation(Event event) {
		if (event.match(editor.FORMULAS_EVALUATED))
			viewer.refresh();
	}

	private void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		viewer = Tables.createViewer(composite, getColumns());
		viewer.setLabelProvider(new ExchangeLabelProvider());
		ModifySupport<Exchange> modifySupport = new ModifySupport<>(viewer);
		modifySupport.bind(FLOW_PROPERTY, new FlowPropertyModifier());
		modifySupport.bind(UNIT, new UnitModifier());
		modifySupport.bind(AMOUNT, new AmountModifier());
		Tables.addDropSupport(viewer, new IModelDropHandler() {
			@Override
			public void handleDrop(List<BaseDescriptor> droppedModels) {
				add(droppedModels);
			}
		});
		viewer.addFilter(new Filter());
		bindActions(section, viewer);
		viewer.setInput(process.getExchanges()); // TODO: sort the exchanges
	}

	private void bindActions(Section section, final TableViewer viewer) {
		Action add = Actions.onAdd(new Runnable() {
			public void run() {
				onAdd();
			}
		});
		Action remove = Actions.onRemove(new Runnable() {
			public void run() {
				onRemove();
			}
		});
		Action formulaSwitch = new Action() {
			{
				setImageDescriptor(ImageType.NUMBER_ICON.getDescriptor());
				setText(Messages.ShowValues);
			}

			@Override
			public void run() {
				showFormulas = !showFormulas;
				if (showFormulas) {
					setImageDescriptor(ImageType.NUMBER_ICON.getDescriptor());
					setText(Messages.ValueViewMode);
				} else {
					setImageDescriptor(ImageType.FORMULA_ICON.getDescriptor());
					setText(Messages.FormulaViewMode);
				}
				viewer.refresh();
			}
		};
		Actions.bind(section, add, remove, formulaSwitch);
		Actions.bind(viewer, add, remove);
	}

	private String[] getColumns() {
		if (forInputs)
			return new String[] { FLOW, CATEGORY, FLOW_PROPERTY, UNIT, AMOUNT };
		else
			return new String[] { FLOW, CATEGORY, FLOW_PROPERTY, UNIT, AMOUNT };
	}

	private void onAdd() {
		BaseDescriptor[] descriptors = ObjectDialog.multiSelect(ModelType.FLOW);
		add(Arrays.asList(descriptors));
	}

	private void onRemove() {
		List<Exchange> selection = Viewers.getAllSelected(viewer);
		for (Exchange exchange : selection)
			process.getExchanges().remove(exchange);
		viewer.setInput(process.getExchanges());
		fireChange();
	}

	private void add(List<BaseDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		for (BaseDescriptor descriptor : descriptors) {
			if (!(descriptor instanceof FlowDescriptor))
				continue;
			Exchange exchange = new Exchange();
			FlowDao flowDao = new FlowDao(Database.get());
			Flow flow = flowDao.getForId(descriptor.getId());
			exchange.setFlow(flow);
			exchange.setFlowPropertyFactor(flow.getReferenceFactor());
			exchange.setUnit(flow.getReferenceFactor().getFlowProperty()
					.getUnitGroup().getReferenceUnit());
			exchange.setAmountValue(1.0);
			exchange.setInput(forInputs);
			process.getExchanges().add(exchange);
		}
		fireChange();
		viewer.setInput(process.getExchanges());
	}

	private void fireChange() {
		editor.setDirty(true);
	}

	private class ExchangeLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;

			if (columnIndex == 0)
				switch (exchange.getFlow().getFlowType()) {
				case ELEMENTARY_FLOW:
					return ImageType.FLOW_SUBSTANCE.get();
				case PRODUCT_FLOW:
					return ImageType.FLOW_PRODUCT.get();
				case WASTE_FLOW:
					return ImageType.FLOW_WASTE.get();
				}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			switch (columnIndex) {
			case 0:
				return exchange.getFlow().getName();
			case 1:
				return CategoryPath.getShort(exchange.getFlow().getCategory());
			case 2:
				return exchange.getFlowPropertyFactor().getFlowProperty()
						.getName();
			case 3:
				return exchange.getUnit().getName();
			case 4:
				return getAmountText(exchange);
			case 5:
				if (exchange.getDefaultProviderId() != 0)
					return Long.toString(exchange.getDefaultProviderId());
				else
					return "-";
			}
			return null;
		}

		private String getAmountText(Exchange exchange) {
			if (!showFormulas || exchange.getAmountFormula() == null)
				return Double.toString(exchange.getAmountValue());
			else
				return exchange.getAmountFormula();
		}
	}

	private class FlowPropertyModifier extends
			ComboBoxCellModifier<Exchange, FlowPropertyFactor> {

		@Override
		protected FlowPropertyFactor[] getItems(Exchange element) {
			List<FlowPropertyFactor> factors = element.getFlow()
					.getFlowPropertyFactors();
			return factors.toArray(new FlowPropertyFactor[factors.size()]);
		}

		@Override
		protected FlowPropertyFactor getItem(Exchange element) {
			return element.getFlowPropertyFactor();
		}

		@Override
		protected String getText(FlowPropertyFactor value) {
			return value.getFlowProperty().getName();
		}

		@Override
		protected void setItem(Exchange element, FlowPropertyFactor item) {
			if (!Objects.equals(element.getFlowPropertyFactor(), item)) {
				element.setFlowPropertyFactor(item);
				fireChange();
			}
		}

	}

	private class UnitModifier extends ComboBoxCellModifier<Exchange, Unit> {

		@Override
		protected Unit[] getItems(Exchange element) {
			List<Unit> units = element.getFlowPropertyFactor()
					.getFlowProperty().getUnitGroup().getUnits();
			return units.toArray(new Unit[units.size()]);
		}

		@Override
		protected Unit getItem(Exchange element) {
			return element.getUnit();
		}

		@Override
		protected String getText(Unit value) {
			return value.getName();
		}

		@Override
		protected void setItem(Exchange element, Unit item) {
			if (!Objects.equals(element.getUnit(), item)) {
				element.setUnit(item);
				fireChange();
			}
		}

	}

	private class AmountModifier extends TextCellModifier<Exchange> {

		@Override
		protected String getText(Exchange element) {
			if (element.getAmountFormula() == null)
				return Double.toString(element.getAmountValue());
			return element.getAmountFormula();
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			try {
				double value = Double.parseDouble(text);
				exchange.setAmountFormula(null);
				exchange.setAmountValue(value);
				fireChange();
			} catch (NumberFormatException e) {
				try {
					double val = editor.eval(text);
					exchange.setAmountFormula(text);
					exchange.setAmountValue(val);
					fireChange();
				} catch (Exception ex) {
					Error.showBox("Invalid formula", text
							+ " is an invalid formula");
				}
			}
		}
	}

	private class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof Exchange))
				return false;
			Exchange exchange = (Exchange) element;
			return exchange.isInput() == forInputs;
		}
	}

}