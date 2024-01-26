/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hidapi;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.InputEvent;
import vavi.awt.joystick.hid4java.Hid4JavaComponent;


/**
 * HidapiInputEvent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-12-22 nsano initial version <br>
 */
public class HidapiInputEvent extends InputEvent {

    /** which value is changed only */
    private final Deque<HidapiComponent> deque = new LinkedList<>();

    /** the time when got an event */
    private final long time;

    /** source */
    private final byte[] data;

    /** for debug */
    public byte[] getData() {
        return data;
    }

    /** */
    public HidapiInputEvent(Object source, Component[] components, byte[] data) {
        super(source);
        this.data = data;
        this.time = System.nanoTime();

        boolean fillAll = Boolean.parseBoolean(System.getProperty("net.java.games.input.InputEvent.fillAll", "false"));

        deque.clear();
        for (HidapiComponent component : Arrays.stream(components).map(HidapiComponent.class::cast).toArray(HidapiComponent[]::new)) {
            if (fillAll || component.isValueChanged(data)) {
                component.setValue(data);
                deque.offer(component);
            }
        }
    }

    @Override
    public boolean getNextEvent(Event event) {
        if (!deque.isEmpty()) {
            HidapiComponent component = deque.poll();
            event.set(component, component.getValue(), time);
            return true;
        } else {
            return false;
        }
    }
}
