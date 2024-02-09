package example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import net.java.games.input.plugin.DualShock4PluginBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;
import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class Example3 {

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

    volatile static boolean deviceOpen = false;

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {

        while (true) {
            HidDeviceInfo devInfo = null;
            if (!deviceOpen) {
                Debug.println("scanning");
                List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
                for (HidDeviceInfo info : devList) {
                    if (info.getVendorId() == (short) vendorId && info.getProductId() == (short) productId) {
                        devInfo = info;
                        break;
                    }
                }
                if (devInfo == null) {
                    Debug.println("device not found");
                    Thread.sleep(1000);
                } else {
                    Debug.println("device found");
                    if (true) {
                        deviceOpen = true;

                        HidDevice dev = PureJavaHidApi.openDevice(devInfo);
                        dev.open();

                        dev.setDeviceRemovalListener(source -> {
                            Debug.println("device removed");
                            deviceOpen = false;
                        });

                        dev.setInputReportListener((source, Id, data, len) -> {
                            DualShock4PluginBase.display(data, System.out);
                        });

                        Thread.sleep(2000);
                    }
                }
            }
        }
    }
}
