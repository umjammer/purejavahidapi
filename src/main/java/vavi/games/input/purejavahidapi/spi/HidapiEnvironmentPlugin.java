/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.purejavahidapi.spi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;

import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.DeviceSupportPlugin;
import net.java.games.input.Rumbler;
import net.java.games.input.usb.HidComponent;
import net.java.games.input.usb.HidControllerEnvironment;
import net.java.games.input.usb.UsageId;
import net.java.games.input.usb.UsagePage;
import net.java.games.input.usb.parser.HidParser;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * The purejavahidapi ControllerEnvironment.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 241003 nsano initial version <br>
 */
public final class HidapiEnvironmentPlugin extends ControllerEnvironment implements HidControllerEnvironment {

    /** */
    private List<HidapiController> controllers;

    /** @return nullable */
    @SuppressWarnings("WhileLoopReplaceableByForEach")
    private HidapiController toHidapiController(HidDeviceInfo deviceInfo) {
        try {
Debug.println(Level.FINER, "deviceInfo: " + deviceInfo);
            HidDevice device = PureJavaHidApi.openDevice(deviceInfo);
            if (device == null) {
                return null;
            }

            // TODO out source filters
            if (!((device.getHidDeviceInfo().getUsagePage()) == 1 &&
                    (device.getHidDeviceInfo().getUsageId()) == 5)) {
                return null;
            }

            List<Component> components = new ArrayList<>();
            List<Controller> children = new ArrayList<>();
            List<Rumbler> rumblers = new ArrayList<>();

Debug.printf(Level.FINE, "device '%s' ----", device.getHidDeviceInfo().getProductString());
            byte[] data = new byte[4096];
            data[0] = 1;
            int len = device.getInputReportDescriptor(data, data.length);
Debug.printf(Level.FINER, "getInputReportDescriptor: len: %d", len);
            if (len <= 0) {
                return null;
            }
Debug.printf(Level.FINER, "getInputReportDescriptor:%n%s", StringUtil.getDump(data, len));
            HidParser parser = new HidParser();
            parser.parse(data, len).enumerateFields().forEach(f -> {
Debug.println(Level.FINER, "UsagePage: " + UsagePage.map(f.getUsagePage()) + ", " + f.getUsageId());
                if (UsagePage.map(f.getUsagePage()) != null) {
                    switch (UsagePage.map(f.getUsagePage())) {
                    case GENERIC_DESKTOP, BUTTON -> {
                        UsagePage usagePage = UsagePage.map(f.getUsagePage());
                        UsageId usageId = usagePage.mapUsage(f.getUsageId());
                        components.add(new HidapiComponent(usageId.toString(), usageId.getIdentifier(), f));
Debug.println(Level.FINER, "add: " + components.get(components.size() - 1));
                    }
                    default -> {
                    }
                    }
                }
            });

            // extra elements by plugin
            for (DeviceSupportPlugin plugin : DeviceSupportPlugin.getPlugins()) {
Debug.println(Level.FINER, "plugin: " + plugin + ", " + plugin.match(device));
                if (plugin.match(device)) {
Debug.println(Level.FINE, "@@@ plugin for extra: " + plugin.getClass().getName());
                    components.addAll(plugin.getExtraComponents(device));
                    children.addAll(plugin.getExtraChildControllers(device));
                    rumblers.addAll(plugin.getExtraRumblers(device));
                }
            }

            //
            device.setDeviceRemovalListener(d -> {
Debug.println(Level.FINE, "device removed");
                Iterator<HidapiController> i = controllers.iterator();
                while (i.hasNext()) {
                    HidapiController c = i.next();
                    if (Objects.equals(c.getName(), d.getHidDeviceInfo().getPath())) {
                        controllers.remove(c);
Debug.printf(Level.FINE, "@@@@@@@@@@@ remove: %s/%s ... %d%n", d.getHidDeviceInfo().getPath(), controllers.size());
                    }
                }
            });

Debug.printf(Level.FINE, "@@@@@@@@@@@ add: %s/%s", device.getHidDeviceInfo().getManufacturerString(), device.getHidDeviceInfo().getProductString());
Debug.printf(Level.FINE, "    components: %d, %s", components.size(), components);
//Debug.printf(Level.FINE, "    children: %d", children.size());
Debug.printf(Level.FINE, "    rumblers: %d, %s", rumblers.size(), rumblers);
            return new HidapiController(device,
                    components.toArray(Component[]::new),
                    children.toArray(Controller[]::new),
                    rumblers.toArray(Rumbler[]::new));

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void enumerate() throws IOException {
        boolean r = isSupported();
Debug.println(Level.FINE, "isSupported: " + r);
        controllers = PureJavaHidApi.enumerateDevices().stream().map(this::toHidapiController).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public HidapiController[] getControllers() {
        if (controllers == null) {
            try {
                enumerate();
            } catch (IOException e) {
Debug.printStackTrace(Level.FINE, e);
                return new HidapiController[0];
            }
        }
        return controllers.toArray(HidapiController[]::new);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    /**
     * @throws NoSuchElementException no matched device of mid and pid
     */
    @Override
    public HidapiController getController(int mid, int pid) {
Debug.println(Level.FINE, "controllers: " + getControllers().length);
        for (HidapiController controller : getControllers()) {
Debug.printf(Level.FINE, "%s: %4x, %4x%n", controller.getName(), controller.getVendorId(), controller.getProductId());
            if (controller.getVendorId() == mid && controller.getProductId() == pid) {
                return controller;
            }
        }
        throw new NoSuchElementException(String.format("no device: mid: %1$d(0x%1$x), pid: %2$d(0x%2$x))", mid, pid));
    }
}

/* */
