package example;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import purejavahidapi.*;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;


@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
public class Example2 {

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
                            System.out.printf("onInputReport: id %d len %d data ", Id, len);
                            for (int i = 0; i < len; i++)
                                System.out.printf("%02X ", data[i]);
                            System.out.println();
                        });

						new Thread(() -> {
                            while (true) {
//								try {
//									Thread.currentThread().sleep(1000);
//									System.out.println();
//									System.out.println("Sending reset");
//									for (int i = 0; i < 10; i++) {
//										byte[] cmd = new byte[64];
//										cmd[0] = (byte) 0xFE;
//										cmd[1] = (byte) 0xED;
//										cmd[2] = (byte) 0xC0;
//										cmd[3] = (byte) 0xDE;
//										System.out.println("SEND");
//										dev.setOutputReport((byte) 0, cmd, cmd.length);
//									}
//									Thread.currentThread().sleep(1000);
//
//									deviceOpen = false;
//									dev.close();
//									break;
//								} catch (InterruptedException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}

                                byte[] data = new byte[132];
                                data[0] = 1;
                                int len = 0;
                                if (((len = dev.getFeatureReport(data, data.length)) >= 0) && true) {
                                    int Id = data[0];
                                    System.out.printf("getFeatureReport: id %d len %d data ", Id, len);
                                    for (byte datum : data) System.out.printf("%02X ", datum);
                                    System.out.println();
                                }
                            }

                        }).start();

						Thread.sleep(2000);
					}
				}
			}
		}
	}
}
