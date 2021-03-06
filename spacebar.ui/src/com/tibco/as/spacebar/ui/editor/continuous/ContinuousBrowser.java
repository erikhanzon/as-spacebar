package com.tibco.as.spacebar.ui.editor.continuous;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import org.eclipse.nebula.widgets.nattable.blink.BlinkLayer;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.IColumnPropertyAccessor;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsDataProvider;
import org.eclipse.nebula.widgets.nattable.extension.glazedlists.GlazedListsEventLayer;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.swt.widgets.Display;

import ca.odell.glazedlists.EventList;

import com.tibco.as.space.Metaspace;
import com.tibco.as.spacebar.ui.editor.AbstractBrowser;
import com.tibco.as.spacebar.ui.editor.AbstractConfiguration;
import com.tibco.as.spacebar.ui.editor.StringPropertyAccessor;

public class ContinuousBrowser extends AbstractBrowser<ObservableTuple> {

	private boolean scrollLocked;

	@Override
	protected AbstractConfiguration getConfiguration() {
		return new BlinkConfiguration();
	}

	private void revealAsync(final ObservableTuple tuple) {
		if (tuple == null) {
			return;
		}
		if (scrollLocked) {
			return;
		}
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {
				if (isDisposed()) {
					return;
				}
				getViewportLayer().moveRowPositionIntoViewport(
						getEventList().indexOf(tuple));
			}
		});
	}

	public void setScrollLocked(boolean checked) {
		this.scrollLocked = checked;
	}

	@Override
	protected IUniqueIndexLayer getLayer(
			GlazedListsEventLayer<ObservableTuple> glazedListsEventLayer,
			GlazedListsDataProvider<ObservableTuple> bodyDataProvider,
			IConfigRegistry registry) {
		return new BlinkLayer<ObservableTuple>(glazedListsEventLayer,
				bodyDataProvider, new TupleRowIdAccessor(),
				getPropertyAccessor(), registry, false,
				Executors.newSingleThreadScheduledExecutor());
	}

	private ObservableTuple update(EventList<ObservableTuple> list,
			ObservableTuple tuple, int index) {
		ObservableTuple existing = list.get(index);
		for (String field : getNonKeyFieldNames()) {
			existing.put(field, tuple.get(field));
		}
		return existing;
	}

	@Override
	public void write(ObservableTuple tuple) {
		if (isDisposed()) {
			return;
		}
		EventList<ObservableTuple> eventList = getEventList();
		int index = eventList.indexOf(tuple);
		if (index == -1) {
			super.write(tuple);
			revealAsync(tuple);
		} else {
			revealAsync(update(eventList, tuple, index));
		}
	}

	@Override
	public void write(List<ObservableTuple> tuples) {
		if (isDisposed()) {
			return;
		}
		List<ObservableTuple> adds = new ArrayList<ObservableTuple>();
		EventList<ObservableTuple> list = getEventList();
		ObservableTuple reveal = null;
		for (ObservableTuple tuple : tuples) {
			int index = list.indexOf(tuple);
			if (index == -1) {
				adds.add(tuple);
				reveal = tuple;
			} else {
				reveal = update(list, tuple, index);
			}
		}
		super.write(adds);
		if (reveal != null) {
			revealAsync(reveal);
		}
	}

	@Override
	protected IColumnPropertyAccessor<ObservableTuple> getPropertyAccessor() {
		return new StringPropertyAccessor<ObservableTuple>(getFieldNames());
	}

	@Override
	protected Exporter getExporter(Metaspace metaspace) {
		return new Exporter(metaspace, getPropertyChangeListener());
	}

}
