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

package purejavahidapi.macosx;

import static purejavahidapi.macosx.CoreFoundationLibrary.CFSTR;
import static purejavahidapi.macosx.HidDevice.createPathForDevice;
import static purejavahidapi.macosx.HidDevice.getIntProperty;
import static purejavahidapi.macosx.HidDevice.getStringProperty;
import static purejavahidapi.macosx.IOHIDManagerLibrary.IOHIDDeviceRef;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDManufacturerKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDPrimaryUsageKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDPrimaryUsagePageKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDProductIDKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDProductKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDSerialNumberKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDVendorIDKey;
import static purejavahidapi.macosx.IOHIDManagerLibrary.kIOHIDVersionNumberKey;


/* package */class HidDeviceInfo extends purejavahidapi.HidDeviceInfo {

    private static int m_NextDeviceId = 1;

    public HidDeviceInfo(IOHIDDeviceRef dev) {
        m_DeviceId = Integer.toString(m_NextDeviceId++);
        m_ProductId = (short) getIntProperty(dev, CFSTR(kIOHIDProductIDKey));
        m_VendorId = (short) getIntProperty(dev, CFSTR(kIOHIDVendorIDKey));
        m_DevicePath = createPathForDevice(dev);
        m_ManufactureString = getStringProperty(dev, CFSTR(kIOHIDManufacturerKey));
        m_SerialNumberString = getStringProperty(dev, CFSTR(kIOHIDSerialNumberKey));
        m_ProductString = getStringProperty(dev, CFSTR(kIOHIDProductKey));
        m_ReleaseNumber = (short) getIntProperty(dev, CFSTR(kIOHIDVersionNumberKey));
        m_UsagePage = (short) getIntProperty(dev, CFSTR(kIOHIDPrimaryUsagePageKey));
        m_UsageId = (short) getIntProperty(dev, CFSTR(kIOHIDPrimaryUsageKey));
    }
}
