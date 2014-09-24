package de.hpi.isg.metadata_store.domain.targets.impl;

import de.hpi.isg.metadata_store.domain.Location;
import de.hpi.isg.metadata_store.domain.common.Observer;
import de.hpi.isg.metadata_store.domain.impl.AbstractTarget;
import de.hpi.isg.metadata_store.domain.targets.Column;

/**
 * The default implementation of the {@link Column}.
 *
 */

public class DefaultColumn extends AbstractTarget implements Column {

    public static Column buildAndRegister(Observer observer, int id, String name, Location location) {
	final DefaultColumn newColumn = new DefaultColumn(observer, id, name, location);
	newColumn.notifyObserver();
	return newColumn;
    }

    public static Column buildAndRegister(Observer observer, String name, Location location) {
	final DefaultColumn newColumn = new DefaultColumn(observer, -1, name, location);
	newColumn.notifyObserver();
	return newColumn;
    }

    private static final long serialVersionUID = 6715373932483124301L;

    private DefaultColumn(Observer observer, int id, String name, Location location) {
	super(observer, id, name, location);
    }

    @Override
    public String toString() {
	return "Column [getLocation()=" + this.getLocation() + ", getId()=" + this.getId() + ", getName()="
		+ this.getName() + "]";
    }
}
