/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hidapi;

import java.io.IOException;

import net.java.games.input.AbstractController;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.Rumbler;
import net.java.games.input.osx.OSXRumbler;
import net.java.games.input.usb.HidController;
import net.java.games.input.usb.HidRumbler;
import purejavahidapi.HidDevice;
import vavi.awt.joystick.hid4java.Hid4JavaInputEvent;


/**
 * HidapiController.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-03 nsano initial version <br>
 */
public class HidapiController extends AbstractController implements HidController {

    /** */
    private final HidDevice device;

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
    }

    @Override
    public int getVendorId() {
        return device.getHidDeviceInfo().getVendorId();
    }

    @Override
    public int getProductId() {
        return device.getHidDeviceInfo().getProductId();
    }

    @Override
    public void open() throws IOException {
        super.open();

        device.open();
        device.setInputReportListener((source, Id, data, len) -> {
            fireOnInput(new HidapiInputEvent(HidapiController.this, getComponents(), data));
        });
    }

    @Override
    public void close() throws IOException {
        device.close();
        super.close();
    }

    @Override
    public Type getType() {
        return Type.GAMEPAD;
    }

    @Override
    public void output(Report report) throws IOException {
        report.cascadeTo(getRumblers());

        int reportId = ((HidReport) report).getReportId();
        byte[] data = ((HidReport) report).getData();
        for (Rumbler rumbler : getRumblers()) {
            ((HidRumbler) rumbler).fill(data);
        }
        int r = device.setOutputReport((byte) reportId, data, data.length);
        if (r == -1) {
            throw new IOException("write returns -1");
        }
    }
}
