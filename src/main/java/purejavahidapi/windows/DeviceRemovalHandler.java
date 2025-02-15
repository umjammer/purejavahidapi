/*
 * Copyright (c) 2016, Kustaa Nyholm / SpareTimeLabs
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

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.ATOM;
import com.sun.jna.platform.win32.WinDef.HMODULE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser.HDEVNOTIFY;
import com.sun.jna.platform.win32.WinUser.MSG;
import com.sun.jna.platform.win32.WinUser.WNDCLASSEX;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import com.sun.jna.platform.win32.Wtsapi32;
import purejavahidapi.shared.SyncPoint;

import static com.sun.jna.platform.win32.DBT.DBT_DEVICEREMOVECOMPLETE;
import static com.sun.jna.platform.win32.DBT.DBT_DEVTYP_DEVICEINTERFACE;
import static com.sun.jna.platform.win32.DBT.DEV_BROADCAST_DEVICEINTERFACE;
import static com.sun.jna.platform.win32.DBT.DEV_BROADCAST_HDR;
import static com.sun.jna.platform.win32.DBT.GUID_DEVINTERFACE_USB_DEVICE;
import static com.sun.jna.platform.win32.Kernel32.INSTANCE;
import static com.sun.jna.platform.win32.User32.DEVICE_NOTIFY_WINDOW_HANDLE;
import static com.sun.jna.platform.win32.User32.WS_EX_TOPMOST;
import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinUser.WM_DESTROY;
import static com.sun.jna.platform.win32.WinUser.WM_DEVICECHANGE;
import static com.sun.jna.platform.win32.Wtsapi32.NOTIFY_FOR_THIS_SESSION;
import static purejavahidapi.windows.SetupApiLibrary.HDEVINFO;
import static purejavahidapi.windows.SetupApiLibrary.SP_DEVICE_INTERFACE_DATA;
import static purejavahidapi.windows.SetupApiLibrary.SP_DEVINFO_DATA;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiCreateDeviceInfoList;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiDeleteDeviceInterfaceData;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiDestroyDeviceInfoList;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiEnumDeviceInfo;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiGetDeviceInstanceId;
import static purejavahidapi.windows.SetupApiLibrary.SetupDiOpenDeviceInterface;
import static purejavahidapi.windows.WindowsBackend.reportLastError;


public class DeviceRemovalHandler implements WindowProc {

    private WindowsBackend m_WindowsBackend;
    private SyncPoint m_StartupSync;

    public DeviceRemovalHandler(WindowsBackend windowsBackend) {
        m_WindowsBackend = windowsBackend;
        m_StartupSync = new SyncPoint(2);
        Runnable threadRunnable = () -> {
            String wndClassName = "WindowClass";
            HMODULE hInst = INSTANCE.GetModuleHandle(null);
            if (hInst == null)
                reportLastError();

            WNDCLASSEX wndClassEx = new WNDCLASSEX();
            wndClassEx.hInstance = hInst;
            wndClassEx.lpfnWndProc = DeviceRemovalHandler.this;
            wndClassEx.lpszClassName = wndClassName;

            ATOM wndClassRef = User32.INSTANCE.RegisterClassEx(wndClassEx);
            if (wndClassRef == null)
                reportLastError();

            HWND hWnd = User32.INSTANCE.CreateWindowEx(WS_EX_TOPMOST, "WindowClass", "", 0, 0, 0, 0, 0, null, null, hInst, null);

            if (hWnd == null)
                reportLastError();

            if (!Wtsapi32.INSTANCE.WTSRegisterSessionNotification(hWnd, NOTIFY_FOR_THIS_SESSION))
                reportLastError();

            DEV_BROADCAST_DEVICEINTERFACE notificationFilter = new DEV_BROADCAST_DEVICEINTERFACE();
            notificationFilter.dbcc_size = notificationFilter.size();
            notificationFilter.dbcc_devicetype = DBT_DEVTYP_DEVICEINTERFACE;
            notificationFilter.dbcc_classguid = GUID_DEVINTERFACE_USB_DEVICE;

            HDEVNOTIFY hDevNotify = User32.INSTANCE.RegisterDeviceNotification(hWnd, notificationFilter, DEVICE_NOTIFY_WINDOW_HANDLE);
            if (hDevNotify == null)
                reportLastError();

            m_StartupSync.waitAndSync();

            MSG msg = new MSG();
            while (User32.INSTANCE.GetMessage(msg, hWnd, 0, 0) != 0) {
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }

            if (!User32.INSTANCE.UnregisterDeviceNotification(hDevNotify))
                reportLastError();

            if (!Wtsapi32.INSTANCE.WTSUnRegisterSessionNotification(hWnd))
                reportLastError();

            if (!User32.INSTANCE.UnregisterClass(wndClassRef.toString() /* "WindowClass" */, hInst))
                reportLastError();

            if (!User32.INSTANCE.DestroyWindow(hWnd))
                reportLastError();
        };
        Thread thread = new Thread(threadRunnable, this.getClass().getSimpleName());
        thread.setDaemon(true);
        thread.start();
        m_StartupSync.waitAndSync();
    }

    @Override
    public LRESULT callback(HWND hwnd, int uMsg, WPARAM wParam, LPARAM lParam) {
        switch (uMsg) {
        case WM_DESTROY: {
            User32.INSTANCE.PostQuitMessage(0);
            return new LRESULT(0);
        }
        case WM_DEVICECHANGE: {
            LRESULT lResult = this.onDeviceChange(wParam, lParam);
            return lResult != null ? lResult : User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
        }
        default:
            return User32.INSTANCE.DefWindowProc(hwnd, uMsg, wParam, lParam);
        }
    }

    protected LRESULT onDeviceChange(WPARAM wParam, LPARAM lParam) {
        if (wParam.intValue() == DBT_DEVICEREMOVECOMPLETE) {
            return onDeviceChangeRemoveComplete(lParam);
        }
        return null;
    }

    private void handleDeviceRemoval(String name) {
        HDEVINFO deviceInfoHandle = SetupDiCreateDeviceInfoList(null, null);
        if (deviceInfoHandle != INVALID_HANDLE_VALUE) {
            SP_DEVICE_INTERFACE_DATA deviceInterfaceData = new SP_DEVICE_INTERFACE_DATA();
            deviceInterfaceData.cbSize = deviceInterfaceData.size();
            if (SetupDiOpenDeviceInterface(deviceInfoHandle, name, 0, deviceInterfaceData)) {
                SP_DEVINFO_DATA deviceInfoData = new SP_DEVINFO_DATA();
                deviceInfoData.cbSize = deviceInfoData.size();
                if (SetupDiEnumDeviceInfo(deviceInfoHandle, 0, deviceInfoData)) {
                    char[] deviceIdChars = new char[255];
                    int[] deviceIdLen = {0};
                    if (SetupDiGetDeviceInstanceId(deviceInfoHandle, deviceInfoData, deviceIdChars, deviceIdChars.length, deviceIdLen)) {
                        String deviceId = new String(deviceIdChars, 0, deviceIdLen[0] - 1);
                        m_WindowsBackend.deviceRemoved(deviceId);
                    }
                }
                if (!SetupDiDeleteDeviceInterfaceData(deviceInfoHandle, deviceInterfaceData))
                    reportLastError();
            }
        }
        if (!SetupDiDestroyDeviceInfoList(deviceInfoHandle))
            reportLastError();
    }

    protected LRESULT onDeviceChangeArrivalOrRemoveComplete(LPARAM lParam, String action) {
        DEV_BROADCAST_HDR bhdr = new DEV_BROADCAST_HDR(lParam.longValue());
        if (bhdr.dbch_devicetype == DBT_DEVTYP_DEVICEINTERFACE) {
            DEV_BROADCAST_DEVICEINTERFACE bdif = new DEV_BROADCAST_DEVICEINTERFACE(bhdr.getPointer());
            handleDeviceRemoval(bdif.getDbcc_name());
        } else {
            return null;
        }
        return new LRESULT(1);
    }

    protected LRESULT onDeviceChangeRemoveComplete(LPARAM lParam) {
        return onDeviceChangeArrivalOrRemoveComplete(lParam, "Remove Complete");
    }

}
