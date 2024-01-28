/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Paths;

import net.java.games.input.usb.parser.HidParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-09 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class Test1 {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "mid")
    String mid;
    @Property(name = "pid")
    String pid;

    int vendorId;
    int productId;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);

            vendorId = Integer.decode(mid);
            productId = Integer.decode(pid);
        }
    }

    @Test
    @Disabled("cannot parse")
    @DisplayName("PureJavaHidApi")
    void test4() throws Exception {
        HidDeviceInfo deviceInfo = PureJavaHidApi.enumerateDevices().stream()
                .filter(d -> d.getVendorId() == 0x54c && d.getProductId() == 0x9cc)
                .findFirst().get();
        HidDevice device = PureJavaHidApi.openDevice(deviceInfo);
        device.open();

Debug.printf("device '%s' ----", device.getHidDeviceInfo().getProductString());
        byte[] data = new byte[132];
        int len = device.getFeatureReport(2, data, data.length);
Debug.printf("getFeatureReport: len: %d", len);
        if (len > 0) {
Debug.printf("getFeatureReport:%n%s", StringUtil.getDump(data, len));
            HidParser hidParser = new HidParser();
            hidParser.parse(data, len);
        }
    }
}
