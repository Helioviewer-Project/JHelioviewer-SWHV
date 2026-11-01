package org.helioviewer.jhv.plugins.swek;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;

import org.helioviewer.jhv.astronomy.GOESLevel;
import org.helioviewer.jhv.event.SWEK;
import org.helioviewer.jhv.event.SWEKSession;
import org.helioviewer.jhv.event.SWEKSupplier;
import org.helioviewer.jhv.ui.Panel;
import org.helioviewer.jhv.ui.Screen;

public final class FilterScreen implements Screen {

    private final Panel buffer = new Panel();

    private static final int APPLY = -2;
    private static final int ENABLED = 0;
    private static final int VALUE = 1;
    private static final int CLASS_COLUMNS = 8;
    private final IdentityHashMap<SWEKSupplier, FilterState> states = new IdentityHashMap<>();
    private FilterState state = new FilterState();
    private SWEKSession session;
    private SWEKSupplier supplier;
    private Runnable listener = () -> {};

    private final HashMap<Integer, Filter> filtersByOwner = new HashMap<>();
    private int nextOwner = 1;

    private Filter filter(SWEK.Parameter parameter, SWEK.Operand operand) {
        Filter filter = new Filter(nextOwner++, state, parameter, operand);
        filtersByOwner.put(filter.owner, filter);
        return filter;
    }

    // Swing keeps one filter dialog per supplier, including unapplied edits and the Apply button state.
    private static final class FilterState {
        final List<Filter> filters = new ArrayList<>();
        boolean dirty = true;
    }

    private static final class Filter {
        final int owner;
        final FilterState state;
        final SWEK.Parameter parameter;
        final SWEK.Operand operand;
        final boolean flare;
        final double min;
        final double max;
        boolean enabled;
        double value;
        String text;

        Filter(int _owner, FilterState _state, SWEK.Parameter _parameter, SWEK.Operand _operand) {
            owner = _owner;
            state = _state;
            parameter = _parameter;
            operand = _operand;
            SWEK.ParameterFilter specification = parameter.filter();
            flare = specification.isFlare();
            min = flare ? displayedValue(specification.min()) : specification.min();
            max = flare ? displayedValue(specification.max()) : specification.max();
            value = parameter.filter().startValue();
            text = flare ? GOESLevel.getStringValue(Math.clamp(value, min, max)) : "";
            if (flare)
                value = GOESLevel.getFloatValue(text);
        }
    }

    private static double displayedValue(double value) {
        return GOESLevel.getFloatValue(GOESLevel.getStringValue(value));
    }

    public void show(SWEKSession _session, SWEKSupplier _supplier) {
        if (session != _session) {
            clear();
            session = _session;
        }
        supplier = _supplier;
        state = states.get(supplier);
        if (state == null) {
            state = new FilterState();
            states.put(supplier, state);
            for (SWEK.Parameter parameter : supplier.getParameterList()) {
                if (parameter.filter() == null)
                    continue;
                for (SWEK.Operand operand : parameter.filter().operands())
                    state.filters.add(filter(parameter, operand));
            }
        }
        listener.run();
    }

    public void clear() {
        states.clear();
        filtersByOwner.clear();
        state = new FilterState();
        supplier = null;
        session = null;
    }

    @Override
    public void listen(Runnable _listener) {
        listener = _listener;
    }

    @Override
    public Panel build() {
        Panel p = buffer.reset().grid(1).equalRows().identity(1);
        for (int i = 0; i < state.filters.size(); i++) {
            Filter filter = state.filters.get(i);
            SWEK.ParameterFilter specification = filter.parameter.filter();
            p.owner(filter.owner).grid(3).equalCells(true).check(ENABLED, "", filter.enabled)
                    .label(filter.parameter.displayName() + ' ' + filter.operand.representation).enabled(filter.enabled);
            if (filter.flare) {
                p.textSpinner(VALUE, filter.text, CLASS_COLUMNS).enabled(filter.enabled);
            } else {
                BigDecimal min = new BigDecimal(specification.min(), new MathContext(12)).stripTrailingZeros();
                BigDecimal max = new BigDecimal(specification.max(), new MathContext(12)).stripTrailingZeros();
                int decimals = min.scale() > 0 && max.scale() > 0 ? Math.max(min.scale(), max.scale()) : 0;
                p.numberSpinner(VALUE, filter.value, specification.min(), specification.max(), specification.stepSize())
                        .format(decimals, 1, "").enabled(filter.enabled);
            }
            p.gridEnd().endRow();
        }
        p.owner(0).button(APPLY, "Apply").commitEditors().enabled(state.dirty).gridEnd();
        return p;
    }

    @Override
    public void command(int owner, int item, Panel.Command command) {
        if (item == VALUE && (command == Panel.Command.DECREASE || command == Panel.Command.INCREASE)) {
            Filter filter = filtersByOwner.get(owner);
            if (filter == null || !filter.flare) return;
            SWEK.ParameterFilter specification = filter.parameter.filter();
            double current = filter.value;
            double exponent = Math.log10(current);
            boolean increase = command == Panel.Command.INCREASE;
            int power = (int) (increase && exponent == (int) exponent ? exponent : exponent - 1);
            double step = specification.stepSize() * Math.pow(10, power);
            setText(owner, VALUE, GOESLevel.getStringValue(Math.clamp(current + (increase ? step : -step), filter.min, filter.max)));
            return;
        }
        if (item == APPLY && supplier != null) {
            List<SWEK.Param> applied = new ArrayList<>();
            for (Filter filter : state.filters) {
                if (filter.enabled) {
                    applied.add(new SWEK.Param(filter.parameter.name(), filter.value, filter.operand));
                }
            }
            session.setFilters(supplier, applied);
            state.dirty = false;
            listener.run();
            return;
        }
        if (item != APPLY)
            throw new IllegalArgumentException("SWEK filter item " + item);
    }

    @Override
    public void checked(int owner, int item, boolean value) {
        if (item != ENABLED)
            throw new IllegalArgumentException("SWEK filter checkbox " + item);
        Filter filter = filtersByOwner.get(owner);
        if (filter != null) {
            filter.enabled = value;
            filter.state.dirty = true;
            listener.run();
        }
    }

    @Override
    public void number(int owner, int item, double value) {
        if (item != VALUE)
            throw new IllegalArgumentException("SWEK filter number " + item);
        Filter filter = filtersByOwner.get(owner);
        if (filter == null) return;
        if (!filter.flare && Double.isFinite(value))
            filter.value = Math.clamp(value, filter.min, filter.max);
        filter.state.dirty = true;
        listener.run();
    }

    @Override
    public void setText(int owner, int item, String text) {
        if (item != VALUE)
            throw new IllegalArgumentException("SWEK filter text " + item);
        Filter filter = filtersByOwner.get(owner);
        if (filter == null || !filter.flare)
            return;
        try {
            double value = GOESLevel.getFloatValue(text);
            SWEK.ParameterFilter specification = filter.parameter.filter();
            if (value < filter.min || value > filter.max)
                throw new IllegalArgumentException("GOES class must be between " + GOESLevel.getStringValue(specification.min()) + " and " + GOESLevel.getStringValue(specification.max()));
            filter.value = value;
            filter.text = text;
            filter.state.dirty = true;
            listener.run();
        } catch (IllegalArgumentException e) {
            // Reject the edit and retain the last accepted value.
            filter.text = GOESLevel.getStringValue(filter.value);
        }
    }

}
