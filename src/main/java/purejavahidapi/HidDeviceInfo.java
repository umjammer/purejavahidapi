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

package purejavahidapi;

/**
 * HidDeviceInfo instances represent all that can be found out about a specific
 * connected USB HID device without actually opening it. Basically the
 * information that the lower level USB system learns from the device descriptor
 * when the devise is plugged in.
 * <p>
 * For more details of see the USB Specification.
 * <p>
 *
 * @author nyholku
 */
public class HidDeviceInfo {

    protected String m_DeviceId;
    protected String m_DevicePath;
    protected short m_VendorId;
    protected short m_ProductId;
    protected short m_ReleaseNumber;
    protected short m_UsagePage;
    protected short m_UsageId;
    protected String m_SerialNumberString;
    protected String m_ManufactureString;
    protected String m_ProductString;

    /**
     * This method returns a string that represents a platform dependent path that
     * describes the 'physical' path through hubs and ports to the device.
     * <p>
     * The main use of the path is to pass it to the
     * {@link PureJavaHidApi#openDevice(HidDeviceInfo)} to obtain a
     * {@link HidDevice} instance which can subsequently be used to communicate with
     * the device.
     *
     * @return a string representing a 'path' to the device
     */
    public String getPath() {
        return m_DevicePath;
    }

    /**
     * This method returns the 16 bit Vendor Id of the device.
     * <p>
     * Note that the return type is <code>short</code> so when compared against
     * literals or variables of type <code>int</code> sign extension interferes and
     * thus it is necessary to cast the <code>int</code> type to <code>short</code>.
     *
     * @return the 16 bit Vendor Id
     */
    public int getVendorId() {
        return m_VendorId & 0xffff;
    }

    /**
     * This method returns the 16 bit Product Id of the device.
     * <p>
     * Note that the return type is <code>short</code> so when compared against
     * literals or variables of type <code>int</code> sign extension interferes and
     * thus it is necessary to cast the <code>int</code> type to <code>short</code>.
     *
     * @return the 16 bit Product Id
     */
    public int getProductId() {
        return m_ProductId & 0xffff;
    }

    /**
     * This method returns the 16 bit Release Number of the device.
     * <p>
     * Note that the return type is <code>short</code> so when compared against
     * literals or variables of type <code>int</code> sign extension interferes and
     * thus it is necessary to cast the <code>int</code> type to <code>short</code>
     *
     * @return the 16 bit Release Number
     */
    public int getReleaseNumber() {
        return m_ReleaseNumber & 0xffff;
    }

    /**
     * This method returns the 16 bit Usage Page number of the device.
     * <p>
     * Note that the return type is <code>short</code> so when compared against
     * literals or variables of type <code>int</code> sign extension interferes and
     * thus it is necessary to cast the <code>int</code> type to <code>short</code>
     *
     * @return the 16 bit Usage Page number
     */
    public int getUsagePage() {
        return m_UsagePage & 0xffff;
    }

    /**
     * This method returns the 16 bit Usage Id number of the device.
     * <p>
     * Note that the return type is <code>short</code> so when compared against
     * literals or variables of type <code>int</code> sign extension interferes and
     * thus it is necessary to cast the <code>int</code> type to <code>short</code>
     *
     * @return the 16 bit Usage Page number
     */
    public int getUsageId() {
        return m_UsageId & 0xffff;
    }

    /**
     * This method returns the Manufacturer String if available otherwise null is
     * returned.
     * <p>
     *
     * @return the Manufacturer String or null
     */
    public String getManufacturerString() {
        return m_ManufactureString;
    }

    /**
     * This method returns the Product String if available otherwise null is
     * returned.
     * <p>
     *
     * @return the Product String or null
     */
    public String getProductString() {
        return m_ProductString;
    }

    /**
     * This method returns the Serial Number String if available otherwise null is
     * returned.
     * <p>
     *
     * @return the Serial Number String or null
     */
    public String getSerialNumberString() {
        return m_SerialNumberString;
    }

    /**
     * This method returns the Device Id String if available otherwise null is
     * returned.
     * <p>
     *
     * @return the Device Id String or null
     */
    public String getDeviceId() {
        return m_DeviceId;
    }

    @Override
    public String toString() {

        return String.format("["
                        + "device id '%s', "
                        + "path '%s', "
                        + "vendor id 0x%04X, "
                        + "product id 0x%04X, "
                        + "release num 0x%04X, "
                        + "usage page 0x%04X, "
                        + "usage id 0x%04X, "
                        + "serial '%s', "
                        + "manufacturer '%s', "
                        + "product id '%s'] "
                ,
                m_DeviceId.trim(),
                m_DevicePath != null ? m_DevicePath.trim(): "",
                m_VendorId & 0xFFFF,
                m_ProductId & 0xFFFF,
                m_ReleaseNumber & 0xFFFF,
                m_UsagePage & 0xFFFF,
                m_UsageId & 0xFFFF,
                m_SerialNumberString != null ? m_SerialNumberString.trim() : "",
                m_ManufactureString != null ? m_ManufactureString.trim() : "",
                m_ProductString != null ? m_ProductString.trim() : ""
        );
    }
}
