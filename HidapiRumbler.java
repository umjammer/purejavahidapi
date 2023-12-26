/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hidapi;

import net.java.games.input.Component;
import net.java.games.input.usb.HidRumbler;


/**
 * Hid4JavaRumbler.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-12-25 nsano initial version <br>
 */
public class HidapiRumbler implements HidRumbler {

    private final Component.Identifier identifier;
    private float value;

    private final int offset;

    public HidapiRumbler(Component.Identifier identifier, int offset) {
        this.identifier = identifier;
        this.offset = offset;
    }

    @Override
    public void setValue(float value) {
        this.value = value;
    }

    public int getValue() {
        return (int) value;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String getOutputName() {
        return identifier.getName();
    }

    @Override
    public Component.Identifier getOutputIdentifier() {
        return identifier;
    }

    @Override
    public void fill(byte[] data) {
        data[offset] = (byte) value;
    }
}
