/*
 * Copyright (c) 2014, Kustaa Nyholm / SpareTimeLabs
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution.
 *
 * Neither the name of the Kustaa Nyholm or SpareTimeLabs nor the names of its
 * contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package purejavahidapi.windows;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.win32.Cfgmgr32;
import com.sun.jna.ptr.IntByReference;
import purejavahidapi.shared.Backend;

import static com.sun.jna.platform.win32.Cfgmgr32.CR_SUCCESS;
import static com.sun.jna.platform.win32.Kernel32.ERROR_INSUFFICIENT_BUFFER;
import static com.sun.jna.platform.win32.Kernel32.ERROR_INVALID_DATA;
import static com.sun.jna.platform.win32.Kernel32.ERROR_INVALID_USER_BUFFER;
import static com.sun.jna.platform.win32.Kernel32.ERROR_NO_MORE_ITEMS;
import static com.sun.jna.platform.win32.Kernel32.FILE_FLAG_OVERLAPPED;
import static com.sun.jna.platform.win32.Kernel32.FILE_SHARE_READ;
import static com.sun.jna.platform.win32.Kernel32.FILE_SHARE_WRITE;
import static com.sun.jna.platform.win32.Kernel32.GENERIC_READ;
import static com.sun.jna.platform.win32.Kernel32.GENERIC_WRITE;
import static com.sun.jna.platform.win32.Kernel32.HANDLE;
import static com.sun.jna.platform.win32.Kernel32.INSTANCE;
import static com.sun.jna.platform.win32.Kernel32.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.Kernel32.OPEN_EXISTING;
import static purejavahidapi.windows.HidLibrary.HIDD_ATTRIBUTES;
import static purejavahidapi.windows.HidLibrary.HidD_GetAttributes;
import static purejavahidapi.windows.SetupApiLibrary.DIGCF_DEVICEINTERFACE;
import static purejavahidapi.windows.SetupApiLibrary.DIGCF_PRESENT;
import static purejavahidapi.windows.SetupApiLibrary.GUID;
import static purejavahidapi.windows.SetupApiLibrary.HDEVINFO;
import static purejavahidapi.windows.SetupApiLibrary.SPDRP_CLASS;
import static purejavahidapi.windows.SetupApiLibrary.SPDRP_DRIVER;
import static purejavahidapi.windows.SetupApiLibrary.SP_DEVICE_INTERFACE_DATA;
import static purejavahidapi.windows.SetupApiLibrary.SP_DEVICE_INTERFACE_DETAIL_DATA_A;
import static purejavahidapi.windows.SetupApiLibrary.SP_DEVINFO_DATA;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiDestroyDeviceInfoList;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiEnumDeviceInfo;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiEnumDeviceInterfaces;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiGetClassDevs;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiGetDeviceInstanceId;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiGetDeviceInterfaceDetail;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiGetDeviceRegistryProperty;


public class WindowsBackend extends Backend {

    @Override
    public void init() {
        new DeviceRemovalHandler(this);
    }

    @Override
    public void cleanup() {

    }

    static public void reportLastError() {
        int rc = Native.getLastError();
        if (rc != 0) {
            StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
            String message = String.format("GetLastError() == %d at %s:%d\n", rc, ste.getFileName(), ste.getLineNumber());
            throw new RuntimeException(message);
        }
    }

    interface EnumerateCallback {

    }

    private boolean isSamePhysicalDevice(String devId1, String devId2) {
        String[] p1 = devId1.split("\\\\");
        String[] p2 = devId2.split("\\\\");
        if (p1 == null || p2 == null)
            return false;
        if (p1.length < 3 || p2.length < 3)
            return false;
        if (!p1[0].equals(p2[0]))
            return false;
        String[] p11 = p1[1].split("&");
        String[] p21 = p2[1].split("&");
        if (p11 == null || p21 == null)
            return false;
        if (p11.length < 3 || p21.length < 3)
            return false;
        if (!p11[0].equals(p21[0]))
            return false;
        if (!p11[1].equals(p21[1]))
            return false;
        String[] p12 = p1[2].split("&");
        String[] p22 = p2[2].split("&");
        if (p12 == null || p22 == null)
            return false;
        if (p12.length < 4 || p22.length < 4)
            return false;
        for (int i = 0; i < 3; i++)
            if (!p12[i].equals(p22[i]))
                return false;
        return true;
    }

    @Override
    public List<purejavahidapi.HidDeviceInfo> enumerateDevices() {
        try {
            List<purejavahidapi.HidDeviceInfo> list = new LinkedList<>();

            GUID InterfaceClassGuid = new GUID(0x4d1e55b2, 0xf16f, 0x11cf, 0x88, 0xcb, 0x00, 0x11, 0x11, 0x00, 0x00, 0x30);

            SP_DEVINFO_DATA devinfo_data = new SP_DEVINFO_DATA();
            SP_DEVICE_INTERFACE_DATA device_interface_data = new SP_DEVICE_INTERFACE_DATA();
            SP_DEVICE_INTERFACE_DETAIL_DATA_A device_interface_detail_data = null;
            HDEVINFO device_info_set;
            // Initialize the Windows objects.
            devinfo_data.cbSize = devinfo_data.size();
            device_interface_data.cbSize = device_interface_data.size();

            // Get information for all the devices belonging to the HID class.
            device_info_set = SetupDiGetClassDevs(InterfaceClassGuid, null, null, DIGCF_PRESENT | DIGCF_DEVICEINTERFACE);

            // Iterate over each device in the HID class

            for (int deviceIndex = 0; ; deviceIndex++) {
                HANDLE devHandle;

                if (!SetupDiEnumDeviceInterfaces(device_info_set, null, InterfaceClassGuid, deviceIndex, device_interface_data)) {
                    if (INSTANCE.GetLastError() == ERROR_NO_MORE_ITEMS)
                        break;
                    reportLastError();
                }

                int[] required_size = {0};
                if (!SetupDiGetDeviceInterfaceDetail(device_info_set, device_interface_data, null, 0, required_size, null)) {
                    if (INSTANCE.GetLastError() != ERROR_INSUFFICIENT_BUFFER)
                        reportLastError();
                }

                // get the device path
                int[] cbSize = {8, 6, 5}; // horrible hack here, because it is not easy to know what cbSize is we try them
                // all
                for (int j : cbSize) {
                    device_interface_detail_data = new SP_DEVICE_INTERFACE_DETAIL_DATA_A(j, required_size[0]);
                    if (SetupDiGetDeviceInterfaceDetail(device_info_set, device_interface_data, device_interface_detail_data, required_size[0], null, null))
                        break; // ok, we guessed right so we can move on
                    device_interface_detail_data = null;
                    if (INSTANCE.GetLastError() == ERROR_INVALID_USER_BUFFER)
                        continue; // guessed from, try again with next value
                    reportLastError(); // something else went wrong, report it
                }
                if (device_interface_detail_data == null)
                    continue; // this should never happen, but let's ignore it if it happens so that the
                // enumeration will not totally fail

                // Make sure this device is of Setup Class "HIDClass" and has a driver bound to
                // it.
                char[] driverNameChars = new char[256];
                if (!SetupDiEnumDeviceInfo(device_info_set, deviceIndex, devinfo_data)) {
                    reportLastError();
                    continue;
                }

                if (!SetupDiGetDeviceRegistryProperty(device_info_set, devinfo_data, SPDRP_CLASS, null, driverNameChars, driverNameChars.length, null)) {
                    reportLastError();
                    continue;
                }

                int driverNameLen = 0;
                while (driverNameChars[driverNameLen++] != 0)
                    ;
                String drivername = new String(driverNameChars, 0, driverNameLen - 1);
                if ("HIDClass".equals(drivername)) {
                    if (!SetupDiGetDeviceRegistryProperty(device_info_set, devinfo_data, SPDRP_DRIVER, null, driverNameChars, driverNameChars.length, null)) {// ok, found a driver
                        if (INSTANCE.GetLastError() != ERROR_INVALID_DATA) // Invalid data is legitime from code point of view, maybe the device does not
                            // have this property or the device is faulty
                            reportLastError();
                        continue;
                    }
                    char[] deviceIdChars = new char[256];
                    int[] deviceIdLen = {0};
                    if (!SetupDiGetDeviceInstanceId(device_info_set, devinfo_data, deviceIdChars, deviceIdChars.length, deviceIdLen))
                        reportLastError();

                    String deviceId = new String(deviceIdChars);
                    int parent = devinfo_data.DevInst;
                    IntByReference rParent = new IntByReference();

                    while (Cfgmgr32.INSTANCE.CM_Get_Parent(rParent, parent, 0) == 0) {
                        parent = rParent.getValue();

                        IntByReference iParentIdLen = new IntByReference(0);
                        if (Cfgmgr32.INSTANCE.CM_Get_Device_ID_Size(iParentIdLen, parent, 0) != CR_SUCCESS)
                            reportLastError();
                        int parentIdLen = iParentIdLen.getValue() + 1;

                        Memory mIdChars = new Memory(parentIdLen * 2L);
                        if (Cfgmgr32.INSTANCE.CM_Get_Device_ID(parent, mIdChars, parentIdLen, 0) != CR_SUCCESS)
                            reportLastError();
                        String parentId = mIdChars.getString(0);

                        if (parentId.startsWith("USB\\")) {
                            deviceId = parentId;
                            break;
                        }
                    }

                    String path = new String(device_interface_detail_data.DevicePath);
                    // path += DEVICE_ID_SEPARATOR + deviceId;
                    // FIXME, need to figure out how to smugle device ID to the actual device ... or
                    // how
                    // recreate as above when opening the device
                    devHandle = openDeviceHandle(path, true);
                    if (devHandle == INVALID_HANDLE_VALUE)
                        break;

                    HIDD_ATTRIBUTES attrib = new HIDD_ATTRIBUTES();
                    attrib.Size = new NativeLong(attrib.size());
                    HidD_GetAttributes(devHandle, attrib);

                    purejavahidapi.HidDeviceInfo info = null;

                    for (purejavahidapi.HidDeviceInfo t : list) {
                        if (isSamePhysicalDevice(t.getDeviceId(), deviceId)) {
                            info = t;
                        }
                    }

                    if (info == null) {
                        info = new HidDeviceInfo(devHandle, attrib);
                        list.add(info);
                    }

                    ((HidDeviceInfo) info).addDevicePath(deviceId, path);

                    INSTANCE.CloseHandle(devHandle);
                }

            }

            SetupDiDestroyDeviceInfoList(device_info_set);
//			for (purejavahidapi.HidDeviceInfo t : list)
//				System.out.println(t);
//			System.out.println();
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public purejavahidapi.HidDevice openDevice(purejavahidapi.HidDeviceInfo deviceInfo) throws IOException {
        return new HidDevice(deviceInfo, this);
    }

    /* package */
    static HANDLE openDeviceHandle(String path, boolean enumerate) {

        HANDLE handle;
        int desired_access = (enumerate) ? 0 : (GENERIC_WRITE | GENERIC_READ);
        int share_mode = FILE_SHARE_READ | FILE_SHARE_WRITE;

        handle = INSTANCE.CreateFile(path, desired_access, share_mode, null, OPEN_EXISTING, FILE_FLAG_OVERLAPPED, null);

        return handle;
    }

}
