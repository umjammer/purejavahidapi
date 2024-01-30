/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.purejavahidapi.spi;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

import net.java.games.input.usb.HidController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import vavi.games.input.purejavahidapi.spi.plugin.DualShock4Plugin;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


/**
 * HidapiEnvironmentPluginTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-03 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class HidapiEnvironmentPluginTest {

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
    void test0() throws Exception {
        HidapiEnvironmentPlugin environment = new HidapiEnvironmentPlugin();
        Arrays.stream(environment.getControllers()).forEach(Debug::println);
    }

    @Test
    @DisplayName("rumbler")
    void test2() throws Exception {
        HidapiEnvironmentPlugin environment = new HidapiEnvironmentPlugin();
        HidapiController controller = environment.getController(vendorId, productId);
        controller.open();

        Random random = new Random();

        DualShock4Plugin.Report5 report = new DualShock4Plugin.Report5();
        report.smallRumble = 20;
        report.bigRumble = 0;
        report.ledRed = random.nextInt(255);
        report.ledGreen = random.nextInt(255);
        report.ledBlue = random.nextInt(255);
        report.flashLed1 = 80;
        report.flashLed2 = 80;
Debug.printf("R: %02x, G: %02x, B: %02x", report.ledRed, report.ledGreen, report.ledBlue);

        controller.output(report);
    }
}