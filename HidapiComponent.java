/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hidapi;

import net.java.games.input.AbstractComponent;
import net.java.games.input.WrappedComponent;
import net.java.games.input.usb.HidComponent;
import vavi.hid.parser.Field;
import vavi.hid.parser.HidParser.Feature;

import static vavi.hid.parser.HidParser.Feature.RELATIVE;


/**
 * HidapiComponent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-01-17 nsano initial version <br>
 */
public class HidapiComponent extends AbstractComponent implements HidComponent, WrappedComponent<Field> {

    private final Field field;

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
     * @param offset bits
     * @param size bit length
     */
    public HidapiComponent(String name, Identifier id, int offset, int size) {
        super(name, id);
        this.field = new Field(offset, size);
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
