package example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import purejavahidapi.HidDevice;
import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;
import vavi.util.ByteUtil;
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
    void test1() throws Exception {

        while (true) {
            HidDeviceInfo devInfo = null;
            if (!deviceOpen) {
                System.out.println("scanning");
                List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
                for (HidDeviceInfo info : devList) {
                    if (info.getVendorId() == (short) vendorId && info.getProductId() == (short) productId) {
                        devInfo = info;
                        break;
                    }
                }
                if (devInfo == null) {
                    System.out.println("device not found");
                    Thread.sleep(1000);
                } else {
                    System.out.println("device found");
                    if (true) {
                        deviceOpen = true;

                        HidDevice dev = PureJavaHidApi.openDevice(devInfo);

                        dev.setDeviceRemovalListener(source -> {
                            System.out.println("device removed");
                            deviceOpen = false;
                        });

                        dev.setInputReportListener((source, Id, data, len) -> {
                            display2(data);
                        });

                        Thread.sleep(2000);
                    }
                }
            }
        }
    }

    static int c;

    static void display2(byte[] d) {
        int l3x = d[0] & 0xff;
        int l3y = d[1] & 0xff;
        int r3x = d[2] & 0xff;
        int r3y = d[3] & 0xff;

        boolean tri	= (d[4] & 0x80) != 0;
        boolean cir	= (d[4] & 0x40) != 0;
        boolean x = (d[4] & 0x20) != 0;
        boolean sqr = (d[4] & 0x10) != 0;
        int dPad = d[4] & 0x0f;

        enum Hat {
            N("↑"), NE("↗"), E("→"), SE("↘"), S("↓"), SW("↙"), W("←"), NW("↖"), Released(" "); final String s; Hat(String s) { this.s = s; }
        }

        boolean r3 = (d[5] & 0x80) != 0;
        boolean l3 = (d[5] & 0x40) != 0;
        boolean opt = (d[5] & 0x20) != 0;
        boolean share = (d[5] & 0x10) != 0;
        boolean r2 = (d[5] & 0x08) != 0;
        boolean l2 = (d[5] & 0x04) != 0;
        boolean r1 = (d[5] & 0x02) != 0;
        boolean l1 = (d[5] & 0x01) != 0;

        int counter = (d[6] >> 2) & 0x3f;
        boolean tPad = (d[6] & 0x02) != 0;
        boolean ps = (d[6] & 0x01) != 0;

        int lTrigger = d[7] & 0xff;
        int rTrigger = d[8] & 0xff;

        int timestump = ByteUtil.readLeShort(d, 9) & 0xffff;
        int temperature = d[11] & 0xff;

        int gyroX = ByteUtil.readLeShort(d, 12) & 0xffff;
        int gyroY = ByteUtil.readLeShort(d, 14) & 0xffff;
        int gyroZ = ByteUtil.readLeShort(d, 16) & 0xffff;

        int accelX = ByteUtil.readLeShort(d, 18) & 0xffff;
        int accelY = ByteUtil.readLeShort(d, 20) & 0xffff;
        int accelZ = ByteUtil.readLeShort(d, 22) & 0xffff;

        boolean extension_detection = (d[29] & 0x01) != 0;
        int battery_info = (d[29] >> 3) & 0x1f;

        System.out.printf("L3 x:%02x y:%02x R3 x:%02x y:%02x (%d)%n", l3x, l3y, r3x, r3y, c++);
        System.out.printf("%3s %3s %3s %3s %5s %2s %s%n", tri ? "▲" : "", cir ? "●" : "", x ? "✖" : "", sqr ? "■" : "", tPad ? "T-PAD" : "", ps ? "PS" : "", Hat.values()[dPad].s);
        System.out.printf("gyro x:%04x y:%04x z:%04x, accel x:%04x y:%04x z:%04x%n%n", gyroX, gyroY, gyroZ, accelX, accelY, accelZ);
    }
}
