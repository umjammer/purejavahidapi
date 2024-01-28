/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.games.input.purejavahidapi.spi;

import java.util.Arrays;

import org.junit.jupiter.api.Test;


/**
 * HidapiEnvironmentPluginTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-10-03 nsano initial version <br>
 */
class HidapiEnvironmentPluginTest {

    @Test
    void test1() throws Exception {
        HidapiEnvironmentPlugin environment = new HidapiEnvironmentPlugin();
        Arrays.stream(environment.getControllers()).forEach(System.err::println);
    }
}