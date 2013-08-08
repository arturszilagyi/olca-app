package org.openlca.ui.viewer;

import java.util.List;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.math.IResultData;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.resources.ImageType;
import org.openlca.util.Strings;

public class ImpactCategoryViewer extends
		AbstractComboViewer<ImpactCategoryDescriptor> {

	private static final String[] COLUMN_HEADERS = new String[] { "Name",
			"Unit" };
	private static final int[] COLUMN_BOUNDS_PERCENTAGES = new int[] { 80, 20 };

	public ImpactCategoryViewer(Composite parent) {
		super(parent);
		setInput(new ImpactCategoryDescriptor[0]);
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ImpactCategoryLabelProvider();
	}

	@Override
	protected ViewerSorter getSorter() {
		return new ImpactCategorySorter();
	}

	@Override
	protected int[] getColumnBoundsPercentages() {
		return COLUMN_BOUNDS_PERCENTAGES;
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	public void setInput(ImpactMethodDescriptor impactMethodDescriptor) {
		List<ImpactCategoryDescriptor> categories = impactMethodDescriptor
				.getImpactCategories();
		setInput(categories.toArray(new ImpactCategoryDescriptor[categories
				.size()]));
	}

	public void setInput(IResultData result) {
		setInput(result.getImpactCategories());
	}

	private class ImpactCategoryLabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return ImageType.LCIA_CATEGORY_ICON.createImage();
			}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			ImpactCategoryDescriptor category = (ImpactCategoryDescriptor) element;

			switch (columnIndex) {
			case 0:
				return category.getDisplayName();
			case 1:
				return category.getReferenceUnit();
			}
			return "";
		}

	}

	private class ImpactCategorySorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof ImpactCategoryDescriptor) || e1 == null) {
				if (e2 != null)
					return -1;
				return 0;
			}
			if (!(e2 instanceof ImpactCategoryDescriptor) || e2 == null)
				return 1;
			ImpactCategoryDescriptor category1 = (ImpactCategoryDescriptor) e1;
			ImpactCategoryDescriptor category2 = (ImpactCategoryDescriptor) e2;
			int compare = Strings.compare(category1.getName(),
					category2.getName());
			if (compare != 0)
				return compare;
			return Strings.compare(category1.getReferenceUnit(),
					category2.getReferenceUnit());
		}

	}

}