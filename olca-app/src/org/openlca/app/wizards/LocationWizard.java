package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Location;

public class LocationWizard extends AbstractWizard<Location> {

	@Override
	protected BaseDao<Location> createDao() {
		return Database.createDao(Location.class);
	}

	@Override
	protected String getTitle() {
		return Messages.NewLocation;
	}

	@Override
	protected AbstractWizardPage<Location> createPage() {
		return new LocationWizardPage();
	}

	private class LocationWizardPage extends AbstractWizardPage<Location> {

		public LocationWizardPage() {
			super("LocationWizardPage");
			setTitle(Messages.NewLocation);
			setMessage(Messages.CreatesANewLocation);
			// TODO change icon
			setImageDescriptor(ImageType.NEW_WIZ_ACTOR.getDescriptor());
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
		}

		@Override
		public Location createModel() {
			Location location = new Location();
			location.setRefId(UUID.randomUUID().toString());
			location.setName(getModelName());
			location.setDescription(getModelDescription());
			return location;
		}

	}

}