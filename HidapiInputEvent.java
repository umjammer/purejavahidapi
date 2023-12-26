/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hidapi;

import java.util.Deque;
import java.util.LinkedList;

import net.java.games.input.Component;
import net.java.games.input.Event;
import net.java.games.input.InputEvent;


/**
 * HidapiInputEvent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-12-22 nsano initial version <br>
 */
public class HidapiInputEvent extends InputEvent {

    /** */
    private Deque deque = new LinkedList();

    public HidapiInputEvent(Object source, Component[] components, byte[] data) {
        super(source);

        deque.clear();
        for (Component component : components) {
            // data -> deaue
            Object datumn = null; // <- component <- data
            deque.offer(datumn);
        }
    }

    @Override
    public boolean getNextEvent(Event event) {
        if (!deque.isEmpty()) {
            Object datumn = deque.poll();
            // datumn -> event
            return true;
        } else {
            return false;
        }
    }
}
