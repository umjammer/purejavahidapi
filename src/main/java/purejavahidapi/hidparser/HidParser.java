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

package purejavahidapi.hidparser;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * HidParser
 *
 * Mandatory items for REPORT
 * <pre>
 * Input (Output or Feature)
 * Usage
 *  Usage Page
 *  Logical Minimum
 *  Logical Maximum
 *  Report Size
 *  Report Count
 * </pre>
 * @see "http://msdn.microsoft.com/en-us/library/windows/hardware/hh975383.aspx"
 */
public class HidParser {

    private static final Logger logger = Logger.getLogger(HidParser.class.getName());
    
    private Collection m_RootCollection;
    private Collection m_TopCollection;
    private LinkedList<Global> m_GlobalStack;
    private int m_DelimiterDepth;
    private int m_ParseIndex;
    private byte[] m_Descriptor;
    private int m_DescriptorLength;
    private Local m_Local;
    private Global m_Global;
    private LinkedList<Report> m_Reports;
    final static int HID_MAX_FIELDS = 256;
    private final static int HID_MAX_IDS = 256;
    private final static int HID_MAX_APPLICATIONS = 16;
    private final static int HID_MAX_USAGES = 12288;

    private static final int HID_INPUT_REPORT = 0;
    private static final int HID_OUTPUT_REPORT = 1;
    private static final int HID_FEATURE_REPORT = 2;

    private static final int HID_COLLECTION_PHYSICAL = 0;
    private static final int HID_COLLECTION_APPLICATION = 1;
    private static final int HID_COLLECTION_LOGICAL = 2;

    enum ItemType {// order import, do not change
        MAIN, //
        GLOBAL, //
        LOCAL, //
        RESERVED, //
        LONG, //
    }

    enum MainTag { // order import, do not change
        PADDING_0, //
        PADDING_1, //
        PADDING_2, //
        PADDING_3, //
        PADDING_4, //
        PADDING_5, //
        PADDING_6, //
        PADDING_7, //
        INPUT, //
        OUTPUT, //
        COLLECTION, //
        FEATURE, //
        ENDCOLLECTION, //
    }

    enum LocalTag {// order import, do not change
        USAGE, //
        USAGE_MINIMUM, //
        USAGE_MAXIMUM, //
        DESIGNATOR_INDEX, //
        DESIGNATOR_MINIMUM, //
        DESIGNATOR_MAXIMUM, //
        STRING_INDEX, //
        STRING_MINIMUM, //
        STRING_MAXIMUM, //
        DELIMITER, //
    }

    enum GlobalTag {
        USAGE_PAGE, //
        LOGICAL_MINIMUM, //
        LOGICAL_MAXIMUM, //
        PHYSICAL_MINIMUM, //
        PHYSICAL_MAXIMUM, //
        UNIT_EXPONENT, //
        UNIT, //
        REPORT_SIZE, //
        REPORT_ID, //
        REPORT_COUNT, //
        PUSH, //
        POP, //
    }

    private static final int HID_MAIN_ITEM_CONSTANT = 0x001;
    private static final int HID_MAIN_ITEM_VARIABLE = 0x002;
    private static final int HID_MAIN_ITEM_RELATIVE = 0x004;
    private static final int HID_MAIN_ITEM_WRAP = 0x008;
    private static final int HID_MAIN_ITEM_NONLINEAR = 0x010;
    private static final int HID_MAIN_ITEM_NO_PREFERRED = 0x020;
    private static final int HID_MAIN_ITEM_NULL_STATE = 0x040;
    private static final int HID_MAIN_ITEM_VOLATILE = 0x080;
    private static final int HID_MAIN_ITEM_BUFFERED_BYTE = 0x100;

    private static final int HID_LONG_ITEM_PREFIX = 0xfe;

    static void printf(String format, Object... args) {
        System.out.printf(format, args);
    }

    private static final class Local {

        public int[] m_Usages = new int[HID_MAX_USAGES];
        public int[] m_CollectionIndex = new int[HID_MAX_USAGES];
        public int m_UsageIndex;
        public int m_UsageMinimum;
        public int m_DelimiterDepth;
        public int m_DelimiterBranch;

        public void reset() {
            m_UsageIndex = 0;
            m_UsageMinimum = 0;
            m_DelimiterDepth = 0;
            m_DelimiterBranch = 0;
            Arrays.fill(m_Usages, 0);
            Arrays.fill(m_CollectionIndex, 0);
        }
    }

    public static final class Global {

        int m_UsagePage;
        int m_LogicalMinimum;
        int m_LogicalMaximum;
        int m_PhysicalMinimum;
        int m_PhysicalMaximum;
        int m_UnitExponent;
        int m_Unit;
        int m_ReportId;
        int m_ReportSize;
        int m_ReportCount;

        public Global() {
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
                return 0;
            }

        }
    }

    public static final class Item {

        int m_Size;
        ItemType m_Type;
        //int m_Tag;
        MainTag m_MTag;
        GlobalTag m_GTag;
        LocalTag m_LTag;
        int m_UValue;
        int m_SValue;

        public Item(int size, ItemType type, int tag, int value) {
            m_Size = size;
            m_Type = type;
            //m_Tag = tag;
            if (m_Type == ItemType.MAIN) {
                if (tag < 8 || tag >= MainTag.values().length)
                    throw new IllegalStateException(String.format("illegal/unsupported main tag %d", tag));
                m_MTag = MainTag.values()[tag];
            }
            if (m_Type == ItemType.GLOBAL) {
                if (tag < 0 || tag >= GlobalTag.values().length)
                    throw new IllegalStateException(String.format("illegal/unsupported global tag %d", tag));
                m_GTag = GlobalTag.values()[tag];
            }
            if (m_Type == ItemType.LOCAL) {
                if (tag < 0 || tag >= LocalTag.values().length)
                    throw new IllegalStateException(String.format("illegal/unsupported local tag %d", tag));
                m_LTag = LocalTag.values()[tag];
            }

            m_UValue = value;
            m_SValue = value;
            switch (size) { // for long items 'size' is not valid, but they are no supported anyway and have value==0
            case 1:
                if ((value & 0xFFFFFF80) != 0)
                    m_SValue |= 0xFFFFFF00;
                break;
            case 2:
                if ((value & 0xFFFF8000) != 0)
                    m_SValue |= 0xFFFF0000;
                break;
            default:
                break;
            }
        }
    }

    private Report registerReport(int type, int id) {
        for (Report r : m_Reports)
            if (r.m_Type == type && r.m_Id == id)
                return r;
        Report r = new Report(type, id, m_TopCollection);
        m_Reports.add(r);
        return r;
    }

    private Field registerField(Report report, int values) {
        Field field;

        if (report.m_MaxField == HID_MAX_FIELDS)
            throw new IllegalStateException("too many fields in report");

        field = new Field(m_TopCollection);
        report.m_Fields[report.m_MaxField++] = field;
        field.m_Report = report;

        return field;
    }

    private int lookUpCollection(int type) {
        for (Collection c = m_TopCollection; c.m_Parent != null; c = c.m_Parent) {
            if (c.m_Type == type)
                return c.m_Usage;
        }
        return 0;
    }

    private void addUsage(int usage) {
        if (m_Local.m_UsageIndex >= m_Local.m_Usages.length)
            throw new IllegalStateException("usage index exceeded");
        m_Local.m_Usages[m_Local.m_UsageIndex++] = usage;
    }

    private void addField(int reportType, int flags) {
        Report report;
        if (null == (report = registerReport(reportType, m_Global.m_ReportId)))
            throw new IllegalStateException("failed to register report");

//		if ((parser -> global.logical_minimum < 0 &&
//				parser -> global.logical_maximum <
//						parser -> global.logical_minimum) ||
//				(parser -> global.logical_minimum >= 0 &&
//						(__u32) parser -> global.logical_maximum <
//								(__u32) parser -> global.logical_minimum)) {
//			dbg_hid("logical range invalid 0x%x 0x%x\n",
//					parser -> global.logical_minimum,
//					parser -> global.logical_maximum);
//			return -1;
//		}

        int j = 0;
        for (int i = 0; i < m_Global.m_ReportCount; i++) {
            if (i < m_Local.m_UsageIndex)
                j = i;
            int offset = report.m_Size;
            report.m_Size += m_Global.m_ReportSize;
            Field field;
            if (null == (field = registerField(report, m_Global.m_ReportCount)))
                throw new IllegalStateException("failed to register field");

            field.m_Physical = lookUpCollection(HID_COLLECTION_PHYSICAL);
            field.m_Logical = lookUpCollection(HID_COLLECTION_LOGICAL);
            field.m_Application = lookUpCollection(HID_COLLECTION_APPLICATION);

            field.m_Usage = m_Local.m_Usages[j];
            field.m_Flags = flags;
            field.m_ReportOffset = offset;
            field.m_ReportType = reportType;
            field.m_ReportSize = m_Global.m_ReportSize;
            field.m_LogicalMinimum = m_Global.m_LogicalMinimum;
            field.m_LogicalMaximum = m_Global.m_LogicalMaximum;
            field.m_PhysicalMinimum = m_Global.m_PhysicalMinimum;
            field.m_PhysicalMaximum = m_Global.m_PhysicalMaximum;
            field.m_UnitExponent = m_Global.m_UnitExponent;
            field.m_Unit = m_Global.m_Unit;
        }

    }

    private void parseGlobalItem(Item item) {
        {
            switch (item.m_GTag) {
            case PUSH:
                m_GlobalStack.push((Global) m_Global.clone());
                break;

            case POP:
                if (m_GlobalStack.isEmpty())
                    throw new IllegalStateException("global environment stack underflow");
                m_Global = m_GlobalStack.pop();
                break;

            case USAGE_PAGE:
                m_Global.m_UsagePage = item.m_UValue;
                break;

            case LOGICAL_MINIMUM:
                m_Global.m_LogicalMinimum = item.m_SValue;
                break;

            case LOGICAL_MAXIMUM:
                m_Global.m_LogicalMaximum = item.m_SValue;
                break;

            case PHYSICAL_MINIMUM:
                m_Global.m_PhysicalMinimum = item.m_SValue;
                break;

            case PHYSICAL_MAXIMUM:
                m_Global.m_PhysicalMaximum = item.m_SValue;
                logger.fine("m_Global.m_PhysicalMaximum " + m_Global.m_PhysicalMaximum);
                break;

            case UNIT_EXPONENT:
                m_Global.m_UnitExponent = item.m_SValue;
                break;

            case UNIT:
                m_Global.m_Unit = item.m_UValue;
                break;

            case REPORT_SIZE:
                if (item.m_UValue < 0 || item.m_UValue > 32)
                    throw new IllegalStateException(String.format("invalid report size %d", item.m_UValue));
                m_Global.m_ReportSize = item.m_UValue;
                break;

            case REPORT_COUNT:
                if (item.m_UValue < 0 || item.m_UValue > HID_MAX_USAGES)
                    throw new IllegalStateException(String.format("invalid report count %d", item.m_UValue));
                m_Global.m_ReportCount = item.m_UValue;
                break;

            case REPORT_ID:
                if (item.m_UValue == 0)
                    throw new IllegalStateException("report_id 0 is invalid");
                m_Global.m_ReportId = item.m_UValue;
                break;
            default:
                throw new IllegalStateException("unsupported global tag");
            }
        }
    }

    private void parseMainItem(Item item) {
        switch (item.m_MTag) {
        case COLLECTION:
            m_TopCollection = new Collection(m_TopCollection, m_Local.m_Usages[0], item.m_UValue & 3);
            break;

        case ENDCOLLECTION:
            if (m_TopCollection.m_Parent == null)
                throw new IllegalStateException("collection stack underflow");
            m_TopCollection = m_TopCollection.m_Parent;
            m_Local.reset();
            break;

        case INPUT:
            addField(HID_INPUT_REPORT, item.m_UValue);
            m_Local.reset();
            break;

        case OUTPUT:
            addField(HID_OUTPUT_REPORT, item.m_UValue);
            m_Local.reset();
            break;

        case FEATURE:
            addField(HID_FEATURE_REPORT, item.m_UValue);
            m_Local.reset();
            break;

        default:
            throw new IllegalStateException("unsupported main tag");
        }

    }

    private void parseLocalItem(Item item) {
        {
            if (item.m_Size == 0)
                throw new IllegalStateException("item data expected for local item");

            switch (item.m_LTag) {
            case DELIMITER:
                if (item.m_UValue > 0) {
                    if (m_Local.m_DelimiterDepth != 0)
                        throw new IllegalStateException("nested delimiters");
                    m_Local.m_DelimiterDepth++;
                    m_Local.m_DelimiterBranch++;
                } else {
                    if (m_Local.m_DelimiterDepth < 1)
                        throw new IllegalStateException("extra delimiters");
                    m_Local.m_DelimiterDepth--;
                }
                break;

            case USAGE:

                int usage = item.m_UValue;
                if (item.m_Size <= 2) // FIXME is this in the spec?
                    usage = (m_Global.m_UsagePage << 16) + usage;

                if (m_Local.m_DelimiterBranch > 1)
                    // alternative usage ignored
                    break;
                addUsage(usage);
                break;

            case USAGE_MINIMUM:

//                if (m_Local.m_DelimiterDepth > 1)
                    // alternative usage ignored

                    m_Local.m_UsageMinimum = item.m_UValue;
                break;

            case USAGE_MAXIMUM:

//                if (m_Local.m_DelimiterBranch > 1)
                    // alternative usage ignored

                    for (int n = m_Local.m_UsageMinimum; n <= item.m_UValue; n++)
                        addUsage((m_Global.m_UsagePage << 16) + n);
                break;
            // ignore the rest
            case DESIGNATOR_INDEX:
                break;
            case DESIGNATOR_MAXIMUM:
            case DESIGNATOR_MINIMUM:
            case STRING_INDEX:
            case STRING_MAXIMUM:
            case STRING_MINIMUM:
                break;
            default:
                throw new IllegalStateException("unsupported local tag");
            }
        }
    }

    private Item getNextItem() {
        if (m_ParseIndex >= m_DescriptorLength)
            return null;
        Item item;
        int at = m_ParseIndex;
        int prev = m_Descriptor[m_ParseIndex++] & 0xFF;

        if (prev == HID_LONG_ITEM_PREFIX) {
            if (m_ParseIndex >= m_DescriptorLength)
                throw new IllegalStateException("unexpected end of data white fetching long item size");

            int size = m_Descriptor[m_ParseIndex++] & 0xFF;
            if (m_ParseIndex >= m_DescriptorLength)
                throw new IllegalStateException("unexpected end of data white fetching long item tag");
            int tag = m_Descriptor[m_ParseIndex++] & 0xFF;

            if (m_ParseIndex + size - 1 >= m_DescriptorLength)
                throw new IllegalStateException("unexpected end of data white fetching long item");
            m_ParseIndex += size;
            item = new Item(size, ItemType.LONG, tag, 0);
        } else {
            int type = (prev >> 2) & 3;
            int tag = (prev >> 4) & 15;
            int size = prev & 3;
            int value = 0;
            switch (size) {
            case 0:
                break;

            case 1:
                if (m_ParseIndex >= m_DescriptorLength)
                    throw new IllegalStateException("unexpected end of data white fetching item size==1");
                value = m_Descriptor[m_ParseIndex++] & 0xFF;
                break;

            case 2:
                if (m_ParseIndex + 1 >= m_DescriptorLength)
                    throw new IllegalStateException("unexpected end of data white fetching item size==1");
                value = (m_Descriptor[m_ParseIndex++] & 0xFF) | ((m_Descriptor[m_ParseIndex++] & 0xFF) << 8);
                break;

            case 3:
                size++; // 3 means 4 bytes
                if (m_ParseIndex + 1 >= m_DescriptorLength)
                    throw new IllegalStateException("unexpected end of data white fetching item size==1");
                value = (m_Descriptor[m_ParseIndex++] & 0xFF) | ((m_Descriptor[m_ParseIndex++] & 0xFF) << 8) | ((m_Descriptor[m_ParseIndex++] & 0xFF) << 16) | (m_Descriptor[m_ParseIndex++] << 24);
            }

            if (type < 0 || type >= ItemType.values().length)
                throw new IllegalStateException(String.format("illegal/unsupported type %d", type));
            item = new Item(size, ItemType.values()[type], tag, value);
        }

        if (logger.isLoggable(Level.FINER)) {
            String tags = "?";
            if (item.m_GTag != null)
                tags = item.m_GTag.toString();
            if (item.m_MTag != null)
                tags = item.m_MTag.toString();
            if (item.m_LTag != null)
                tags = item.m_LTag.toString();
            printf("[%3d] = 0x%02X:  size %d  type %-8s  tag %-20s  value 0x%08X (%d)\n", at, prev, item.m_Size, item.m_Type, tags, item.m_SValue, item.m_SValue);
        }
        return item;

    }

    private void resetParser() {
        m_RootCollection = new Collection(null, 0, 0);
        m_TopCollection = m_RootCollection;
        m_GlobalStack = new LinkedList<>();
        m_DelimiterDepth = 0;
        m_ParseIndex = 0;
        m_Descriptor = null;
        m_DescriptorLength = 0;
        m_Local = new Local();
        m_Global = new Global();
        m_Reports = new LinkedList<>();
    }

    public void dump(String tab) {
        for (Report r : m_Reports) {
            r.dump(tab);
        }
    }

    public void parse(byte[] descriptor, int length) {
        resetParser();
        m_Descriptor = descriptor;
        m_DescriptorLength = length;
        Item item;
        while (null != (item = getNextItem())) {
            switch (item.m_Type) {
            case MAIN:
                parseMainItem(item);
                break;
            case LOCAL:
                parseLocalItem(item);
                break;
            case GLOBAL:
                parseGlobalItem(item);
                break;
            case LONG:
                throw new IllegalStateException("unexpected long global item");
            default:
                throw new IllegalStateException("unknown global item type (bug)");
            }
        }
        if (m_TopCollection.m_Parent != null)
            throw new IllegalStateException("unbalanced collection at end of report description");

        if (m_DelimiterDepth > 0)
            throw new IllegalStateException("unbalanced delimiter at end of report description");
        m_RootCollection.dump("");
    }
}
