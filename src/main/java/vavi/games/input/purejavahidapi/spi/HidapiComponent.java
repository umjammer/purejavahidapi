/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.purejavahidapi.spi;

import java.util.function.Function;

import net.java.games.input.AbstractComponent;
import net.java.games.input.WrappedComponent;
import net.java.games.input.usb.HidComponent;
import net.java.games.input.usb.parser.Field;
import net.java.games.input.usb.parser.HidParser.Feature;

import static net.java.games.input.usb.parser.HidParser.Feature.RELATIVE;


/**
 * HidapiComponent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-01-17 nsano initial version <br>
 */
public class HidapiComponent extends AbstractComponent implements HidComponent, WrappedComponent<Field> {

    private final Field field;

    /** special data picker (e.g. for touch x, y) */
    private Function<byte[], Integer> function;

    /**
     * Protected constructor
     *
     * @param name A name for the axis
     * @param field an input report descriptor fragment.
     */
    protected HidapiComponent(String name, Identifier id, Field field) {
        super(name, id);
        this.field = field;
    }

    /**
     * @param offset bits (must be excluded first one byte (8 bits) for report id)
     * @param size bit length
     */
    public HidapiComponent(String name, Identifier id, int offset, int size) {
        this(name, id, offset, size, null);
    }

    /**
     * @param offset bits (must be excluded first one byte (8 bits) for report id)
     * @param size bit length
     */
    public HidapiComponent(String name, Identifier id, int offset, int size, Function<byte[], Integer> function) {
        super(name, id);
        this.field = new Field(offset, size);
        this.function = function;
    }

    @Override
    public boolean isRelative() {
        return field != null && Feature.containsIn(RELATIVE, field.getFeature());
    }

    @Override
    public boolean isValueChanged(byte[] data) {
        return getEventValue() != getValue(data);
    }

    @Override
    public float getValue() {
        return getEventValue();
    }

    /** by hid input report */
    private int getValue(byte[] data) {
        return field.getValue(data);
    }

    @Override
    public void setValue(byte[] data) {
        setEventValue(field.getValue(data));
    }

    @Override
    public Field getWrappedObject() {
        return field;
    }
}
