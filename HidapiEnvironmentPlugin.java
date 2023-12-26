/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.awt.joystick.hidapi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Rumbler;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;
import vavi.hid.parser.HidParser;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * The purejavahidapi ControllerEnvironment.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 241003 nsano initial version <br>
 */
public final class HidapiEnvironmentPlugin extends ControllerEnvironment {

    /** */
    private final List<HidapiController> controllers;

    /** */
    public HidapiEnvironmentPlugin() {
        controllers = PureJavaHidApi.enumerateDevices().stream().map(this::toHidapiController).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /** @return nullable */
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    private HidapiController toHidapiController(HidDeviceInfo deviceInfo) {
        try {
Debug.println("deviceInfo: " + deviceInfo);
            HidDevice device = PureJavaHidApi.openDevice(deviceInfo);
            if (device == null) {
                return null;
            }

            device.setDeviceRemovalListener(d -> {
Debug.println("device removed");
                Iterator<HidapiController> i = controllers.iterator();
                while (i.hasNext()) {
                    HidapiController c = i.next();
                    if (Objects.equals(c.getName(), d.getHidDeviceInfo().getPath())) {
                        controllers.remove(c);
Debug.printf("@@@@@@@@@@@ remove: %s/%s ... %d%n", d.getHidDeviceInfo().getPath(), controllers.size());
                    }
                }
            });

            // TODO out source filters
            if (!((device.getHidDeviceInfo().getUsagePage()) == 1 &&
                    (device.getHidDeviceInfo().getUsageId()) == 5)) {
                return null;
            }

Debug.printf("device '%s' ----", device.getHidDeviceInfo().getProductString());
            byte[] data = new byte[4096];
            data[0] = 1;
            int len = device.getInputReportDescriptor(data, data.length);
Debug.printf("getInputReportDescriptor: len: %d", len);
            if (len > 0) {
Debug.printf("getInputReportDescriptor:%n%s", StringUtil.getDump(data, len));
                HidParser hidParser = new HidParser();
                hidParser.parse(data, len);
            }

            return new HidapiController(device, new Component[0], new Controller[0], new Rumbler[0]);

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Controller[] getControllers() {
        return controllers.toArray(Controller[]::new);
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}

/* */
