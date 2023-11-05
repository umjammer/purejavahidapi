/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hidapi;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

import net.java.games.input.AbstractController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.Rumbler;
import net.java.games.input.osx.plugin.DualShock4Plugin;
import purejavahidapi.HidDevice;
import vavi.util.Debug;


/**
 * HidapiController.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-03 nsano initial version <br>
 */
public class HidapiController extends AbstractController {

    /** */
    private final HidDevice device;

    BlockingDeque<byte[]> reports = new LinkedBlockingDeque<>();

    /**
     * Protected constructor for a controller containing the specified
     * axes, child controllers, and rumblers
     *
     * @param device     the controller
     * @param components components for the controller
     * @param children   child controllers for the controller
     * @param rumblers   rumblers for the controller
     */
    protected HidapiController(HidDevice device, Component[] components, Controller[] children, Rumbler[] rumblers) {
        super(device.getHidDeviceInfo().getPath(), components, children, rumblers);
        this.device = device;

        device.setInputReportListener((source, Id, data, len) -> {
            reports.offer(data);
        });
    }

    @Override
    public Type getType() {
        return Type.GAMEPAD;
    }

    @Override
    protected boolean getNextDeviceEvent(Event event) throws IOException {
Debug.println("getNextDeviceEvent: here");

        DualShock4Plugin.display(data);

        event.set(null, 0, 0);
        return true;
    }

    /** */
    private byte[] data;

    @Override
    protected void pollDevice() throws IOException {
Debug.println("pollDevice: here");
        if (reports.peek() != null) {
            try {
                this.data = reports.take();
            } catch (InterruptedException e) {
Debug.printStackTrace(Level.FINE, e);
            }
        }
    }
}
