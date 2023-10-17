package example;

import java.util.List;

import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;


public class Example1 {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        List<HidDeviceInfo> devList = PureJavaHidApi.enumerateDevices();
        for (HidDeviceInfo info : devList) {
            System.out.printf("VID = 0x%04X PID = 0x%04X Manufacturer = %s Product = %s Path = %s\n", //
                    info.getVendorId(), //
                    info.getProductId(), //
                    info.getManufacturerString(), //
                    info.getProductString(), //
                    info.getPath());
        }
    }
}
