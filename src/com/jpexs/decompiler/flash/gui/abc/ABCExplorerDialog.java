/*
 * Copyright (C) 2023 JPEXS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.gui.abc;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.types.ABCException;
import com.jpexs.decompiler.flash.abc.types.ClassInfo;
import com.jpexs.decompiler.flash.abc.types.Float4;
import com.jpexs.decompiler.flash.abc.types.InstanceInfo;
import com.jpexs.decompiler.flash.abc.types.MetadataInfo;
import com.jpexs.decompiler.flash.abc.types.MethodBody;
import com.jpexs.decompiler.flash.abc.types.MethodInfo;
import com.jpexs.decompiler.flash.abc.types.Multiname;
import com.jpexs.decompiler.flash.abc.types.Namespace;
import com.jpexs.decompiler.flash.abc.types.NamespaceSet;
import com.jpexs.decompiler.flash.abc.types.ScriptInfo;
import com.jpexs.decompiler.flash.abc.types.ValueKind;
import com.jpexs.decompiler.flash.abc.types.traits.Trait;
import com.jpexs.decompiler.flash.abc.types.traits.TraitClass;
import com.jpexs.decompiler.flash.abc.types.traits.TraitFunction;
import com.jpexs.decompiler.flash.abc.types.traits.TraitMethodGetterSetter;
import com.jpexs.decompiler.flash.abc.types.traits.TraitSlotConst;
import com.jpexs.decompiler.flash.abc.types.traits.Traits;
import com.jpexs.decompiler.flash.ecma.EcmaScript;
import com.jpexs.decompiler.flash.gui.AppDialog;
import com.jpexs.decompiler.flash.gui.FasterScrollPane;
import com.jpexs.decompiler.flash.gui.View;
import com.jpexs.decompiler.flash.helpers.CodeFormatting;
import com.jpexs.decompiler.flash.helpers.StringBuilderTextWriter;
import com.jpexs.decompiler.flash.tags.ABCContainerTag;
import com.jpexs.decompiler.flash.tags.DoABC2Tag;
import com.jpexs.decompiler.flash.tags.ShowFrameTag;
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.graph.DottedChain;
import com.jpexs.helpers.Helper;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.basic.BasicLabelUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 *
 * @author JPEXS
 */
public class ABCExplorerDialog extends AppDialog {

    private final List<ABCContainerTag> abcContainers = new ArrayList<>();
    private final JComboBox<String> abcComboBox;
    private final JLabel tagInfoLabel;
    private final List<Integer> abcFrames = new ArrayList<>();
    private final JTabbedPane mainTabbedPane;
    private final JTabbedPane cpTabbedPane;

    public ABCExplorerDialog(Window owner, SWF swf, ABC abc) {
        super(owner);
        Container cnt = getContentPane();
        cnt.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel(translate("abc")));
        int selectedIndex = 0;
        int frame = 1;
        for (Tag t : swf.getTags()) {
            if (t instanceof ShowFrameTag) {
                frame++;
            }
            if (t instanceof ABCContainerTag) {
                ABCContainerTag abcCnt = (ABCContainerTag) t;
                if (abcCnt.getABC() == abc) {
                    selectedIndex = abcContainers.size();
                }
                abcContainers.add(abcCnt);
                abcFrames.add(frame);
            }
        }
        String[] abcComboBoxData = new String[abcContainers.size()];
        for (int i = 0; i < abcContainers.size(); i++) {
            abcComboBoxData[i] = "tag" + (i + 1);
            if (abcContainers.get(i) instanceof DoABC2Tag) {
                DoABC2Tag doa2 = (DoABC2Tag) abcContainers.get(i);
                if (doa2.name != null && !doa2.name.isEmpty()) {
                    abcComboBoxData[i] += " (\"" + Helper.escapePCodeString(doa2.name) + "\")";
                }
            }
        }
        abcComboBox = new JComboBox<>(abcComboBoxData);
        Dimension abcComboBoxSize = new Dimension(500, abcComboBox.getPreferredSize().height);
        abcComboBox.setMinimumSize(abcComboBoxSize);
        abcComboBox.setPreferredSize(abcComboBoxSize);
        topPanel.add(abcComboBox);

        tagInfoLabel = new JLabel();
        topPanel.add(tagInfoLabel);

        abcComboBox.addActionListener(this::abcComboBoxActionPerformed);

        mainTabbedPane = new JTabbedPane();
        cpTabbedPane = new JTabbedPane();

        cnt.add(topPanel, BorderLayout.NORTH);
        cnt.add(mainTabbedPane, BorderLayout.CENTER);

        if (!abcContainers.isEmpty()) {
            abcComboBox.setSelectedIndex(selectedIndex);
        }
        setSize(800, 600);
        setTitle(translate("title") + " - " + swf.getTitleOrShortFileName());
        View.setWindowIcon(this);
        View.centerScreen(this);
    }
    
    public void selectAbc(ABC abc) {
        if (abc == null && !abcContainers.isEmpty()) {
            abcComboBox.setSelectedIndex(0);
            return;
        }
        for (int i = 0; i < abcContainers.size(); i++) {
            if (abcContainers.get(i).getABC() == abc) {
                abcComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    private ABC getSelectedAbc() {
        return abcContainers.get(abcComboBox.getSelectedIndex()).getABC();
    }

    private void abcComboBoxActionPerformed(ActionEvent e) {
        int index = abcComboBox.getSelectedIndex();
        if (index == -1) {
            return;
        }
        ABC abc = abcContainers.get(index).getABC();
        tagInfoLabel.setText(
                translate("abc.info")
                        .replace("%index%", "" + (index + 1))
                        .replace("%count%", "" + abcComboBox.getItemCount())
                        .replace("%major%", "" + abc.version.major)
                        .replace("%minor%", "" + abc.version.minor)
                        .replace("%size%", Helper.formatFileSize(abc.getDataSize()))
                        .replace("%frame%", "" + abcFrames.get(index))
        );

        cpTabbedPane.removeAll();

        cpTabbedPane.addTab("int (" + Math.max(0, abc.constants.getIntCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_INT));
        cpTabbedPane.addTab("uint (" + Math.max(0, abc.constants.getUIntCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_UINT));
        cpTabbedPane.addTab("dbl (" + Math.max(0, abc.constants.getDoubleCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_DOUBLE));
        if (abc.hasDecimalSupport()) {
            cpTabbedPane.addTab("dc (" + Math.max(0, abc.constants.getDecimalCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_DECIMAL));
        }
        if (abc.hasFloatSupport()) {
            cpTabbedPane.addTab("fl (" + Math.max(0, abc.constants.getFloatCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_FLOAT));
            cpTabbedPane.addTab("fl4 (" + Math.max(0, abc.constants.getFloat4Count() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_FLOAT_4));
        }
        cpTabbedPane.addTab("str (" + Math.max(0, abc.constants.getStringCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_STRING));
        cpTabbedPane.addTab("ns (" + Math.max(0, abc.constants.getNamespaceCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_NAMESPACE));
        cpTabbedPane.addTab("nss (" + Math.max(0, abc.constants.getNamespaceSetCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_NAMESPACE_SET));
        cpTabbedPane.addTab("mn (" + Math.max(0, abc.constants.getMultinameCount() - 1) + ")", makeTreePanel(abc, TreeType.CONSTANT_MULTINAME));

        mainTabbedPane.removeAll();

        JPanel cpPanel = new JPanel(new BorderLayout());
        cpPanel.add(cpTabbedPane, BorderLayout.CENTER);

        int cpCount = Math.max(0, abc.constants.getIntCount() - 1)
                + Math.max(0, abc.constants.getUIntCount() - 1)
                + Math.max(0, abc.constants.getDoubleCount() - 1)
                + Math.max(0, abc.constants.getStringCount() - 1)
                + Math.max(0, abc.constants.getNamespaceCount() - 1)
                + Math.max(0, abc.constants.getNamespaceSetCount() - 1)
                + Math.max(0, abc.constants.getMultinameCount() - 1)
                + (abc.hasDecimalSupport() ? Math.max(0, abc.constants.getDecimalCount() - 1) : 0)
                + (abc.hasFloatSupport() ? (Math.max(0, abc.constants.getFloatCount() - 1) + Math.max(0, abc.constants.getFloat4Count() - 1)) : 0);
        mainTabbedPane.addTab("cp (" + cpCount + ")", cpPanel);
        mainTabbedPane.addTab("mi (" + abc.method_info.size() + ")", makeTreePanel(abc, TreeType.METHOD_INFO));
        mainTabbedPane.addTab("md (" + abc.metadata_info.size() + ")", makeTreePanel(abc, TreeType.METADATA_INFO));
        mainTabbedPane.addTab("ii (" + abc.instance_info.size() + ")", makeTreePanel(abc, TreeType.INSTANCE_INFO));
        mainTabbedPane.addTab("ci (" + abc.class_info.size() + ")", makeTreePanel(abc, TreeType.CLASS_INFO));
        mainTabbedPane.addTab("si (" + abc.script_info.size() + ")", makeTreePanel(abc, TreeType.SCRIPT_INFO));
        mainTabbedPane.addTab("mb (" + abc.bodies.size() + ")", makeTreePanel(abc, TreeType.METHOD_BODY));
    }
    
    public void selectScriptInfo(int scriptIndex) {
        if (mainTabbedPane.getTabCount() > 0) {
            mainTabbedPane.setSelectedIndex(5);
            JPanel pan = (JPanel) mainTabbedPane.getComponentAt(5);
            FasterScrollPane fasterScrollPane = (FasterScrollPane) pan.getComponent(0);
            JTree tree = (JTree)fasterScrollPane.getViewport().getView();
            TreeModel model = tree.getModel();
            if (scriptIndex >= model.getChildCount(model.getRoot())) {
                return;
            }
            Object scriptInfoNode = model.getChild(model.getRoot(), scriptIndex);
            TreePath path = new TreePath(new Object[]{
                model.getRoot(),
                scriptInfoNode
            });
            tree.setSelectionPath(path);
            tree.scrollPathToVisible(path);
        }
    }

    private JPanel makeTreePanel(ABC abc, TreeType type) {
        JTree tree = new JTree(new ExplorerTreeModel(abc, type));
        if (View.isOceanic()) {
            tree.setBackground(Color.white);
        }
        tree.setCellRenderer(new ExplorerTreeCellRenderer());
        tree.setUI(new BasicTreeUI() {
            {
                if (View.isOceanic()) {
                    setHashColor(Color.gray);
                }
            }
        });

        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.add(new FasterScrollPane(tree), BorderLayout.CENTER);
        return treePanel;
    }

    private enum TreeType {
        CONSTANT_INT("Integers", "int"),
        CONSTANT_UINT("UnsignedIntegers", "uint"),
        CONSTANT_DOUBLE("Doubles", "dbl"),
        CONSTANT_DECIMAL("Decimals", "dc"), //needs ABC decimal support
        CONSTANT_FLOAT("Floats", "fl"), //needs ABC float support
        CONSTANT_FLOAT_4("Floats4", "fl4"), //needs ABC float support
        CONSTANT_STRING("Strings", "str"),
        CONSTANT_NAMESPACE("Namespaces", "ns"),
        CONSTANT_NAMESPACE_SET("NamespaceSets", "nss"),
        CONSTANT_MULTINAME("Multinames", "mn"),
        METHOD_INFO("MethodInfos", "mi"),
        METADATA_INFO("MetadataInfos", "md"),
        INSTANCE_INFO("InstanceInfos", "ii"),
        CLASS_INFO("ClassInfos", "ci"),
        SCRIPT_INFO("ScriptInfos", "si"),
        METHOD_BODY("MethodBodys", "mb");

        private final String name;
        private final String abbreviation;

        TreeType(String name, String abbreviation) {
            this.name = name;
            this.abbreviation = abbreviation;
        }

        public String getName() {
            return name;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private class SimpleValue {

        private final int currentLevelIndex;
        private final Object parent;
        private final String title;

        public SimpleValue(Object parent, int currentLevelIndex, String title) {
            this.currentLevelIndex = currentLevelIndex;
            this.parent = parent;
            this.title = title;
        }

        public int getCurrentLevelIndex() {
            return currentLevelIndex;
        }

        public Object getParent() {
            return parent;
        }

        @Override
        public String toString() {
            return title;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + this.currentLevelIndex;
            hash = 47 * hash + Objects.hashCode(this.parent);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SimpleValue other = (SimpleValue) obj;
            if (this.currentLevelIndex != other.currentLevelIndex) {
                return false;
            }
            return Objects.equals(this.parent, other.parent);
        }

    }

    private class SubValue {

        private final int currentLevelIndex;
        private final Object parent;
        private final Object parentValue;
        private final String property;
        private final String title;
        private final int index;

        public SubValue(Object parent, int currentLevelIndex, Object parentValue, String property, String title) {
            this.currentLevelIndex = currentLevelIndex;
            this.parent = parent;
            this.parentValue = parentValue;
            this.property = property;
            this.title = title;
            this.index = -1;
        }

        public SubValue(Object parent, int currentLevelIndex, int index, Object parentValue, String property, String title) {
            this.currentLevelIndex = currentLevelIndex;
            this.index = index;
            this.parent = parent;
            this.parentValue = parentValue;
            this.property = property;
            this.title = title;
        }

        public int getIndex() {
            return index;
        }

        public int getCurrentLevelIndex() {
            return currentLevelIndex;
        }

        @Override
        public String toString() {
            return title;
        }

        public Object getParent() {
            return parent;
        }

        public Object getParentValue() {
            return parentValue;
        }

        public String getProperty() {
            return property;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 41 * hash + this.currentLevelIndex;
            hash = 41 * hash + Objects.hashCode(this.parent);
            hash = 41 * hash + Objects.hashCode(this.property);
            hash = 41 * hash + this.index;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SubValue other = (SubValue) obj;
            if (this.currentLevelIndex != other.currentLevelIndex) {
                return false;
            }
            if (this.index != other.index) {
                return false;
            }
            if (!Objects.equals(this.property, other.property)) {
                return false;
            }
            return Objects.equals(this.parent, other.parent);
        }

    }

    private class ValueWithIndex {

        private final Object parent;
        private final int index;
        private final int currentLevelIndex;
        private final TreeType type;
        private final Object value;
        private final String title;
        private final String prefix;

        public ValueWithIndex(Object parent, int currentLevelIndex, int index, TreeType type, Object value, String title) {
            this.parent = parent;
            this.currentLevelIndex = currentLevelIndex;
            this.index = index;
            this.type = type;
            this.value = value;
            this.title = title;
            this.prefix = "";
        }

        public ValueWithIndex(Object parent, int currentLevelIndex, int index, TreeType type, Object value, String title, String prefix) {
            this.parent = parent;
            this.currentLevelIndex = currentLevelIndex;
            this.index = index;
            this.type = type;
            this.value = value;
            this.title = title;
            this.prefix = prefix;
        }

        public int getCurrentLevelIndex() {
            return currentLevelIndex;
        }

        public Object getParent() {
            return parent;
        }

        public int getIndex() {
            return index;
        }

        public TreeType getType() {
            return type;
        }

        @Override
        public String toString() {
            boolean implicit = false;
            if (index == 0) {
                switch (type) {
                    case CONSTANT_INT:
                    case CONSTANT_UINT:
                    case CONSTANT_DOUBLE:
                    case CONSTANT_DECIMAL:
                    case CONSTANT_FLOAT:
                    case CONSTANT_FLOAT_4:
                    case CONSTANT_STRING:
                    case CONSTANT_NAMESPACE:
                    case CONSTANT_NAMESPACE_SET:
                    case CONSTANT_MULTINAME:
                        implicit = true;
                }
            }

            return prefix + (implicit ? "[" : "") + type.getAbbreviation() + index + (implicit ? "]" : "") + ": " + title;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 31 * hash + Objects.hashCode(this.parent);
            hash = 31 * hash + this.currentLevelIndex;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ValueWithIndex other = (ValueWithIndex) obj;
            if (this.currentLevelIndex != other.currentLevelIndex) {
                return false;
            }
            return Objects.equals(this.parent, other.parent);
        }

    }

    private class ExplorerTreeModel implements TreeModel {

        private TreeType type;
        private ABC abc;

        public ExplorerTreeModel(ABC abc, TreeType type) {
            this.type = type;
            this.abc = abc;
        }

        @Override
        public Object getRoot() {
            return type;
        }

        private ValueWithIndex createValueWithIndex(Object parent, int currentLevelIndex, int index, TreeType valueType, String prefix) {
            if (index == 0) {
                switch (valueType) {
                    case CONSTANT_INT:
                    case CONSTANT_UINT:
                    case CONSTANT_DOUBLE:
                    case CONSTANT_DECIMAL:
                    case CONSTANT_FLOAT:
                    case CONSTANT_FLOAT_4:
                    case CONSTANT_STRING:
                    case CONSTANT_NAMESPACE:
                    case CONSTANT_NAMESPACE_SET:
                    case CONSTANT_MULTINAME:
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "null", prefix);
                }
            }
            switch (valueType) {
                case CONSTANT_INT:
                    if (index >= abc.constants.getIntCount()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }                    
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, abc.constants.getInt(index), "" + abc.constants.getInt(index), prefix);
                case CONSTANT_UINT:
                    if (index >= abc.constants.getUIntCount()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }                    
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, abc.constants.getUInt(index), "" + abc.constants.getUInt(index), prefix);
                case CONSTANT_DOUBLE:
                    if (index >= abc.constants.getDoubleCount()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }                    
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, abc.constants.getDouble(index), EcmaScript.toString(abc.constants.getDouble(index)), prefix);
                case CONSTANT_DECIMAL:
                    if (index >= abc.constants.getDecimalCount()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }                    
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, abc.constants.getDecimal(index), "" + abc.constants.getDecimal(index), prefix);
                case CONSTANT_FLOAT:
                    if (index >= abc.constants.getFloatCount()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, abc.constants.getFloat(index), EcmaScript.toString(abc.constants.getFloat(index)), prefix);
                case CONSTANT_FLOAT_4:
                    if (index >= abc.constants.getFloat4Count()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    
                    Float4 f4 = abc.constants.getFloat4(index);
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, f4,
                            EcmaScript.toString(f4.values[0]) + " "
                            + EcmaScript.toString(f4.values[1]) + " "
                            + EcmaScript.toString(f4.values[2]) + " "
                            + EcmaScript.toString(f4.values[3]),
                            prefix
                    );
                case CONSTANT_STRING:
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, index < abc.constants.getStringCount() ? abc.constants.getString(index) : null, formatString(index), prefix);
                case CONSTANT_NAMESPACE:
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, index < abc.constants.getNamespaceCount() ? abc.constants.getNamespace(index) : null, Multiname.namespaceToString(abc.constants, index), prefix);
                case CONSTANT_NAMESPACE_SET:
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, abc.constants.getNamespaceSet(index), Multiname.namespaceSetToString(abc.constants, index), prefix);
                case CONSTANT_MULTINAME:
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, index < abc.constants.getMultinameCount() ? abc.constants.getMultiname(index) : null,
                            index < abc.constants.getMultinameCount()
                            ? abc.constants.getMultiname(index).toString(abc.constants, new ArrayList<DottedChain>())
                            : "Unknown(" + index + ")",
                            prefix);
                case METHOD_INFO:
                    if (index >= abc.method_info.size()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    MethodInfo mi = abc.method_info.get(index);
                    StringBuilder miStrSb = new StringBuilder();
                    miStrSb.append("(");
                    StringBuilderTextWriter miParamStrSbW = new StringBuilderTextWriter(new CodeFormatting(), miStrSb);
                    mi.getParamStr(miParamStrSbW, abc.constants, null, abc, new ArrayList<>());
                    miStrSb.append("): ");
                    String miReturnType = mi.getReturnTypeRaw(abc.constants, new ArrayList<>());
                    miStrSb.append(miReturnType);
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, mi, miStrSb.toString(), prefix);
                case METHOD_BODY:
                    if (index >= abc.bodies.size()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    MethodBody b = abc.bodies.get(index);
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, b, "mi" + b.method_info + ", " + b.getCodeBytes().length + " bytes code", prefix);
                case INSTANCE_INFO:
                    if (index >= abc.instance_info.size()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    InstanceInfo ii = abc.instance_info.get(index);
                    String iiName;
                    if (ii.name_index >= abc.constants.getMultinameCount() || ii.getName(abc.constants).namespace_index >= abc.constants.getNamespaceCount()) {
                        iiName = "";
                    } else {
                        iiName = "\"" + Helper.escapePCodeString(ii.getName(abc.constants).getNameWithNamespace(abc.constants, false).toRawString()) + "\"";
                    }
                    return new ValueWithIndex(parent, currentLevelIndex, index, TreeType.INSTANCE_INFO, ii, iiName + (ii.instance_traits.traits.isEmpty() ? "" : ", " + ii.instance_traits.traits.size() + " traits"), prefix);
                case CLASS_INFO:
                    if (index >= abc.class_info.size()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    ClassInfo ci = abc.class_info.get(index);
                    return new ValueWithIndex(parent, currentLevelIndex, index, TreeType.CLASS_INFO, ci, "mi" + ci.cinit_index + (ci.static_traits.traits.isEmpty() ? "" : ", " + ci.static_traits.traits.size() + " traits"), prefix);
                case SCRIPT_INFO:
                    if (index >= abc.script_info.size()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    ScriptInfo si = abc.script_info.get(index);
                    String siName = "";
                    try {
                        DottedChain simplePackName = si.getSimplePackName(abc);
                        if (simplePackName != null) {
                            siName = " (\"" + Helper.escapePCodeString(simplePackName.toRawString()) + "\")";
                        }
                    } catch (IndexOutOfBoundsException iob) {
                        //ignore
                    }
                    return new ValueWithIndex(parent, currentLevelIndex, index, TreeType.SCRIPT_INFO, si, "mi" + si.init_index + (si.traits.traits.isEmpty() ? "" : ", " + si.traits.traits.size() + " traits") + siName, prefix);

                case METADATA_INFO:
                    if (index >= abc.metadata_info.size()) {
                        return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "Unknown(" + index + ")", prefix);
                    }
                    MetadataInfo md = abc.metadata_info.get(index);
                    String mdName = formatString(md.name_index);
                    mdName += " (" + md.values.length + " items)";
                    return new ValueWithIndex(parent, currentLevelIndex, index, TreeType.METADATA_INFO, md, mdName);
                default:
                    return new ValueWithIndex(parent, currentLevelIndex, index, valueType, null, "", prefix);
            }
        }

        private int handleGetChildCountTrait(SubValue sv, Traits traits) {
            if (sv.getIndex() > -1) {
                Trait t = traits.traits.get(sv.getIndex());
                int count = 3;
                if ((t.kindFlags & Trait.ATTR_Metadata) > 0) {
                    count++;
                }
                if (t instanceof TraitSlotConst) {
                    TraitSlotConst tsc = (TraitSlotConst) t;
                    if (tsc.value_index == 0) {
                        return count + 3;
                    }
                    return count + 4;
                }
                if (t instanceof TraitMethodGetterSetter) {
                    return count + 2;
                }

                if (t instanceof TraitClass) {
                    return count + 3;
                }

                if (t instanceof TraitFunction) {
                    return count + 2;
                }
            }
            return traits.traits.size();
        }

        private Object handleGetChildTrait(Object parent, int index, Object parentValue, SubValue sv, Traits traits) {
            if (sv.getIndex() > -1) {
                Trait t = traits.traits.get(sv.getIndex());

                int currentIndex = 0;
                switch (index) {
                    case 0:
                        return createValueWithIndex(parent, index, t.name_index, TreeType.CONSTANT_MULTINAME, "name: ");
                    case 1:
                        return new SimpleValue(parent, index, "kind: " + String.format("0x%02X", t.kindType) + " (" + t.getKindToStr() + ")");
                    case 2:
                        List<String> flagList = new ArrayList<>();
                        if ((t.kindFlags & Trait.ATTR_Final) > 0) {
                            flagList.add("FINAL");
                        }
                        if ((t.kindFlags & Trait.ATTR_Override) > 0) {
                            flagList.add("OVERRIDE");
                        }
                        if ((t.kindFlags & Trait.ATTR_Metadata) > 0) {
                            flagList.add("METADATA");
                        }
                        if ((t.kindFlags & Trait.ATTR_0x8) > 0) {
                            flagList.add("0x8");
                        }
                        return new SimpleValue(parent, index, "kind_flags: " + String.format("0x%02X", t.kindFlags) + (flagList.isEmpty() ? "" : " (" + String.join(", ", flagList) + ")"));
                }
                if (t instanceof TraitSlotConst) {
                    TraitSlotConst tsc = (TraitSlotConst) t;
                    switch (index) {
                        case 3:
                            return new SimpleValue(parent, index, "slot_id: " + tsc.slot_id);
                        case 4:
                            return createValueWithIndex(parent, index, tsc.type_index, TreeType.CONSTANT_MULTINAME, "type: ");
                        case 5:
                            if (tsc.value_index == 0) {
                                return new SimpleValue(parent, index, "value_index: null");
                            }
                            switch (tsc.value_kind) {
                                case ValueKind.CONSTANT_Int:
                                    return createValueWithIndex(parent, index, tsc.value_index, TreeType.CONSTANT_INT, "value_index: ");
                                case ValueKind.CONSTANT_UInt:
                                    return createValueWithIndex(parent, index, tsc.value_index, TreeType.CONSTANT_UINT, "value_index: ");
                                case ValueKind.CONSTANT_Double:
                                    return createValueWithIndex(parent, index, tsc.value_index, TreeType.CONSTANT_DOUBLE, "value_index: ");
                                case ValueKind.CONSTANT_DecimalOrFloat: //?? or float ??
                                    return createValueWithIndex(parent, index, tsc.value_index, TreeType.CONSTANT_DECIMAL, "value_index: ");
                                case ValueKind.CONSTANT_Utf8:
                                    return createValueWithIndex(parent, index, tsc.value_index, TreeType.CONSTANT_STRING, "value_index: ");
                                case ValueKind.CONSTANT_True:
                                case ValueKind.CONSTANT_False:
                                case ValueKind.CONSTANT_Null:
                                case ValueKind.CONSTANT_Undefined:
                                    return new SimpleValue(parent, index, "value_index: " + tsc.value_index);
                                case ValueKind.CONSTANT_Namespace:
                                case ValueKind.CONSTANT_PackageInternalNs:
                                case ValueKind.CONSTANT_ProtectedNamespace:
                                case ValueKind.CONSTANT_ExplicitNamespace:
                                case ValueKind.CONSTANT_StaticProtectedNs:
                                case ValueKind.CONSTANT_PrivateNs:
                                    return createValueWithIndex(parent, index, tsc.value_index, TreeType.CONSTANT_NAMESPACE, "value_index: ");
                            }
                        case 6:
                            switch (tsc.value_kind) {
                                case ValueKind.CONSTANT_Int:
                                    return new SimpleValue(parent, index, "value_kind: Integer");
                                case ValueKind.CONSTANT_UInt:
                                    return new SimpleValue(parent, index, "value_kind: UInteger");
                                case ValueKind.CONSTANT_Double:
                                    return new SimpleValue(parent, index, "value_kind: Double");
                                case ValueKind.CONSTANT_DecimalOrFloat: //?? or float ??
                                    return new SimpleValue(parent, index, "value_kind: Decimal");
                                case ValueKind.CONSTANT_Utf8:
                                    return new SimpleValue(parent, index, "value_kind: String");
                                case ValueKind.CONSTANT_True:
                                    return new SimpleValue(parent, index, "value_kind: True");
                                case ValueKind.CONSTANT_False:
                                    return new SimpleValue(parent, index, "value_kind: False");
                                case ValueKind.CONSTANT_Null:
                                    return new SimpleValue(parent, index, "value_kind: Null");
                                case ValueKind.CONSTANT_Undefined:
                                    return new SimpleValue(parent, index, "value_kind: Undefined");
                                case ValueKind.CONSTANT_Namespace:
                                    return new SimpleValue(parent, index, "value_kind: Namespace");
                                case ValueKind.CONSTANT_PackageInternalNs:
                                    return new SimpleValue(parent, index, "value_kind: PackageInternalNs");
                                case ValueKind.CONSTANT_ProtectedNamespace:
                                    return new SimpleValue(parent, index, "value_kind: ProtectedNamespace");
                                case ValueKind.CONSTANT_ExplicitNamespace:
                                    return new SimpleValue(parent, index, "value_kind: ExplicitNamespace");
                                case ValueKind.CONSTANT_StaticProtectedNs:
                                    return new SimpleValue(parent, index, "value_kind: StaticProtectedNs");
                                case ValueKind.CONSTANT_PrivateNs:
                                    return new SimpleValue(parent, index, "value_kind: PrivateNamespace");
                            }
                    }
                    currentIndex = 7;
                }
                if (t instanceof TraitMethodGetterSetter) {
                    TraitMethodGetterSetter tmgs = (TraitMethodGetterSetter) t;
                    switch (index) {
                        case 3:
                            return new SimpleValue(parent, index, "disp_id: " + tmgs.disp_id);
                        case 4:
                            return createValueWithIndex(parent, index, tmgs.method_info, TreeType.METHOD_INFO, "method_info: ");
                    }
                    currentIndex = 5;
                }
                if (t instanceof TraitClass) {
                    TraitClass tc = (TraitClass) t;
                    switch (index) {
                        case 3:
                            return new SimpleValue(parent, index, "slot_id: " + tc.slot_id);
                        case 4:
                            return createValueWithIndex(parent, index, tc.class_info, TreeType.INSTANCE_INFO, "instance_info: ");
                        case 5:
                            return createValueWithIndex(parent, index, tc.class_info, TreeType.CLASS_INFO, "class_info: ");
                    }
                    currentIndex = 6;
                }

                if (t instanceof TraitFunction) {
                    TraitFunction tf = (TraitFunction) t;
                    switch (index) {
                        case 3:
                            return new SimpleValue(parent, index, "slot_id: " + tf.slot_id);
                        case 4:
                            return createValueWithIndex(parent, index, tf.method_info, TreeType.METHOD_INFO, "method_index: ");
                    }
                    currentIndex = 5;
                }

                if (index == currentIndex) {
                    if ((t.kindFlags & Trait.ATTR_Metadata) > 0) {
                        return new SubValue(parent, currentIndex, t, "metadata", "metadata");
                    }
                }
            }
            Trait t = traits.traits.get(index);
            String traitName = formatString(t.getName(abc).name_index);
            return new SubValue(parent, index, index, parentValue, "traits", "t" + index + ": " + t.getKindToStr() + ": " + traitName);
        }

        private String formatString(int index) {
            if (index == 0) {
                return "null";
            }
            if (index >= abc.constants.getStringCount()) {
                return "Unknown(" + index + ")";
            }
            return "\"" + Helper.escapePCodeString(abc.constants.getString(index)) + "\"";
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent == type) {
                return createValueWithIndex(parent, index, index, type, "");
            }
            if (parent instanceof ValueWithIndex) {
                ValueWithIndex vwi = (ValueWithIndex) parent;
                if (vwi.value instanceof NamespaceSet) {
                    NamespaceSet nss = (NamespaceSet) vwi.value;
                    int ns = nss.namespaces[index];
                    return new ValueWithIndex(parent, index, ns, TreeType.CONSTANT_NAMESPACE, abc.constants.getNamespace(ns), Multiname.namespaceToString(abc.constants, ns));
                }
                if (vwi.value instanceof Namespace) {
                    Namespace ns = (Namespace) vwi.value;
                    switch (index) {
                        case 0:
                            return new SimpleValue(parent, index, "kind: " + Namespace.kindToStr(ns.kind));
                        case 1:
                            return createValueWithIndex(parent, index, ns.name_index, TreeType.CONSTANT_STRING, "name: ");
                    }
                }
                if (vwi.value instanceof Multiname) {
                    Multiname m = (Multiname) vwi.value;
                    if (index == 0) {
                        return new SimpleValue(parent, index, "kind: " + m.getKindStr());
                    }
                    int kind = m.kind;
                    if ((kind == Multiname.QNAME) || (kind == Multiname.QNAMEA)) {
                        switch (index) {
                            case 1:
                                return createValueWithIndex(parent, index, m.namespace_index, TreeType.CONSTANT_NAMESPACE, "namespace: ");
                            case 2:
                                return createValueWithIndex(parent, index, m.name_index, TreeType.CONSTANT_STRING, "name: ");
                        }
                    } else if ((kind == Multiname.RTQNAME) || (kind == Multiname.RTQNAMEA)) {
                        if (index == 1) {
                            return createValueWithIndex(parent, index, m.name_index, TreeType.CONSTANT_STRING, "name: ");
                        }
                    } else if ((kind == Multiname.RTQNAMEL) || (kind == Multiname.RTQNAMELA)) {
                        //ignore
                    } else if ((kind == Multiname.MULTINAME) || (kind == Multiname.MULTINAMEA)) {
                        switch (index) {
                            case 1:
                                return createValueWithIndex(parent, index, m.name_index, TreeType.CONSTANT_STRING, "name: ");
                            case 2:
                                return createValueWithIndex(parent, index, m.namespace_set_index, TreeType.CONSTANT_NAMESPACE_SET, "namespace_set: ");
                        }
                    } else if ((kind == Multiname.MULTINAMEL) || (kind == Multiname.MULTINAMELA)) {
                        if (index == 1) {
                            return createValueWithIndex(parent, index, m.namespace_set_index, TreeType.CONSTANT_NAMESPACE_SET, "namespace_set: ");
                        }
                    } else if (kind == Multiname.TYPENAME) {
                        if (index == 1) {
                            return createValueWithIndex(parent, index, m.qname_index, TreeType.CONSTANT_MULTINAME, "qname: ");
                        }
                        if (index >= 2 && index - 2 < m.params.length) {
                            return createValueWithIndex(parent, index, m.params[index - 2], TreeType.CONSTANT_MULTINAME, "param" + (index - 2) + ": ");
                        }
                    }
                }
                if (vwi.value instanceof MethodInfo) {
                    MethodInfo mi = (MethodInfo) vwi.value;
                    switch (index) {
                        case 0:
                            return new SubValue(parent, index, mi, "param_types", "param_types");
                        case 1:
                            return createValueWithIndex(parent, index, mi.ret_type, TreeType.CONSTANT_MULTINAME, "return_type: ");
                        case 2:
                            return createValueWithIndex(parent, index, mi.name_index, TreeType.CONSTANT_STRING, "name: ");
                        case 3:
                            List<String> flagList = new ArrayList<>();
                            if (mi.flagNative()) {
                                flagList.add("NATIVE");
                            }
                            if (mi.flagHas_optional()) {
                                flagList.add("HAS_OPTIONAL");
                            }
                            if (mi.flagHas_paramnames()) {
                                flagList.add("HAS_PARAM_NAMES");
                            }
                            if (mi.flagIgnore_rest()) {
                                flagList.add("IGNORE_REST");
                            }
                            if (mi.flagNeed_activation()) {
                                flagList.add("NEED_ACTIVATION");
                            }
                            if (mi.flagNeed_arguments()) {
                                flagList.add("NEED_ARGUMENTS");
                            }
                            if (mi.flagNeed_rest()) {
                                flagList.add("NEED_REST");
                            }
                            if (mi.flagSetsdxns()) {
                                flagList.add("SET_DXNS");
                            }

                            return new SimpleValue(parent, index, "flags: " + String.format("0x%02X", mi.flags) + (!flagList.isEmpty() ? " (" + String.join(", ", flagList) + ")" : ""));
                    }

                    int currentIndex = 4;

                    if (mi.flagHas_optional()) {
                        if (index == currentIndex) {
                            return new SubValue(parent, index, mi, "optional", "optional");
                        }
                        currentIndex++;
                    }

                    if (mi.flagHas_paramnames()) {
                        if (index == currentIndex) {
                            return new SubValue(parent, index, mi, "param_names", "param_names");
                        }
                        currentIndex++;
                    }

                    if (index == currentIndex) {
                        int bodyIndex = abc.findBodyIndex(vwi.getIndex());
                        if (bodyIndex != -1) {
                            return createValueWithIndex(parent, index, bodyIndex, TreeType.METHOD_BODY, "method_body: ");
                        }
                    }
                }
                if (vwi.value instanceof MethodBody) {
                    MethodBody body = (MethodBody) vwi.value;
                    switch (index) {
                        case 0:
                            return createValueWithIndex(parent, index, body.method_info, TreeType.METHOD_INFO, "method_info: ");
                        case 1:
                            return new SimpleValue(parent, index, "max_stack: " + body.max_stack);
                        case 2:
                            return new SimpleValue(parent, index, "max_regs: " + body.max_regs);
                        case 3:
                            return new SimpleValue(parent, index, "init_scope_depth: " + body.init_scope_depth);
                        case 4:
                            return new SimpleValue(parent, index, "max_scope_depth: " + body.max_scope_depth);
                        case 5:
                            return new SimpleValue(parent, index, "code: " + body.getCodeBytes().length + " bytes");
                        case 6:
                            return new SubValue(parent, index, body, "exceptions", "exceptions");
                        case 7:
                            return new SubValue(parent, index, body, "traits", "traits");
                    }
                }
                if (vwi.value instanceof InstanceInfo) {
                    InstanceInfo ii = (InstanceInfo) vwi.value;
                    switch (index) {
                        case 0:
                            return createValueWithIndex(parent, index, ii.name_index, TreeType.CONSTANT_MULTINAME, "name: ");
                        case 1:
                            return createValueWithIndex(parent, index, ii.super_index, TreeType.CONSTANT_MULTINAME, "super: ");
                        case 2:
                            List<String> flagList = new ArrayList<>();
                            if ((ii.flags & InstanceInfo.CLASS_SEALED) == InstanceInfo.CLASS_SEALED) {
                                flagList.add("SEALED");
                            }
                            if ((ii.flags & InstanceInfo.CLASS_FINAL) == InstanceInfo.CLASS_FINAL) {
                                flagList.add("FINAL");
                            }
                            if ((ii.flags & InstanceInfo.CLASS_INTERFACE) == InstanceInfo.CLASS_INTERFACE) {
                                flagList.add("INTERFACE");
                            }
                            if ((ii.flags & InstanceInfo.CLASS_PROTECTEDNS) == InstanceInfo.CLASS_PROTECTEDNS) {
                                flagList.add("PROTECTEDNS");
                            }
                            if ((ii.flags & InstanceInfo.CLASS_NON_NULLABLE) == InstanceInfo.CLASS_NON_NULLABLE) {
                                flagList.add("NON_NULLABLE");
                            }
                            return new SimpleValue(parent, index, "flags: " + String.format("0x%02X", ii.flags) + (!flagList.isEmpty() ? " (" + String.join(", ", flagList) + ")" : ""));
                    }
                    int currentIndex = 3;
                    if ((ii.flags & InstanceInfo.CLASS_PROTECTEDNS) == InstanceInfo.CLASS_PROTECTEDNS) {
                        if (index == currentIndex) {
                            return createValueWithIndex(parent, index, ii.protectedNS, TreeType.CONSTANT_NAMESPACE, "protected_ns: ");
                        }
                        currentIndex++;
                    }
                    if (index == currentIndex) {
                        return new SubValue(parent, index, ii, "interfaces", "interfaces");
                    }
                    currentIndex++;
                    if (index == currentIndex) {
                        return createValueWithIndex(parent, currentIndex, ii.iinit_index, TreeType.METHOD_INFO, "iinit: ");
                    }
                    currentIndex++;
                    if (index == currentIndex) {
                        return new SubValue(parent, index, ii, "traits", "traits");
                    }
                }
                if (vwi.value instanceof ClassInfo) {
                    ClassInfo ci = (ClassInfo) vwi.value;
                    switch (index) {
                        case 0:
                            return createValueWithIndex(parent, index, ci.cinit_index, TreeType.METHOD_INFO, "cinit: ");
                        case 1:
                            return new SubValue(parent, index, ci, "traits", "traits");
                    }
                }
                if (vwi.value instanceof ScriptInfo) {
                    ScriptInfo si = (ScriptInfo) vwi.value;
                    switch (index) {
                        case 0:
                            return createValueWithIndex(parent, index, si.init_index, TreeType.METHOD_INFO, "init: ");
                        case 1:
                            return new SubValue(parent, index, si, "traits", "traits");
                    }
                }

                if (vwi.value instanceof MetadataInfo) {
                    MetadataInfo md = (MetadataInfo) vwi.value;
                    switch (index) {
                        case 0:
                            return createValueWithIndex(parent, index, md.name_index, TreeType.CONSTANT_STRING, "name: ");
                        case 1:
                            return new SubValue(parent, index, md, "pairs", "pairs");
                    }
                }
            }
            if (parent instanceof SubValue) {
                SubValue sv = (SubValue) parent;
                if (sv.getParentValue() instanceof MethodInfo) {
                    MethodInfo mi = (MethodInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "param_types":
                            return createValueWithIndex(parent, index, mi.param_types[index], TreeType.CONSTANT_MULTINAME, "pt" + index + ": ");
                        case "optional":
                            if (sv.getIndex() > -1) {
                                if (index == 0) {
                                    switch (mi.optional[sv.getIndex()].value_kind) {
                                        case ValueKind.CONSTANT_Int:
                                            return new SimpleValue(parent, index, "value_kind: Integer");
                                        case ValueKind.CONSTANT_UInt:
                                            return new SimpleValue(parent, index, "value_kind: UInteger");
                                        case ValueKind.CONSTANT_Double:
                                            return new SimpleValue(parent, index, "value_kind: Double");
                                        case ValueKind.CONSTANT_DecimalOrFloat: //?? or float ??
                                            return new SimpleValue(parent, index, "value_kind: Decimal");
                                        case ValueKind.CONSTANT_Utf8:
                                            return new SimpleValue(parent, index, "value_kind: String");
                                        case ValueKind.CONSTANT_True:
                                            return new SimpleValue(parent, index, "value_kind: True");
                                        case ValueKind.CONSTANT_False:
                                            return new SimpleValue(parent, index, "value_kind: False");
                                        case ValueKind.CONSTANT_Null:
                                            return new SimpleValue(parent, index, "value_kind: Null");
                                        case ValueKind.CONSTANT_Undefined:
                                            return new SimpleValue(parent, index, "value_kind: Undefined");
                                        case ValueKind.CONSTANT_Namespace:
                                            return new SimpleValue(parent, index, "value_kind: Namespace");
                                        case ValueKind.CONSTANT_PackageInternalNs:
                                            return new SimpleValue(parent, index, "value_kind: PackageInternalNs");
                                        case ValueKind.CONSTANT_ProtectedNamespace:
                                            return new SimpleValue(parent, index, "value_kind: ProtectedNamespace");
                                        case ValueKind.CONSTANT_ExplicitNamespace:
                                            return new SimpleValue(parent, index, "value_kind: ExplicitNamespace");
                                        case ValueKind.CONSTANT_StaticProtectedNs:
                                            return new SimpleValue(parent, index, "value_kind: StaticProtectedNs");
                                        case ValueKind.CONSTANT_PrivateNs:
                                            return new SimpleValue(parent, index, "value_kind: PrivateNamespace");
                                    }
                                }
                                if (index == 1) {
                                    int value_index = mi.optional[sv.getIndex()].value_index;
                                    switch (mi.optional[sv.getIndex()].value_kind) {
                                        case ValueKind.CONSTANT_Int:
                                            return createValueWithIndex(parent, index, value_index, TreeType.CONSTANT_INT, "value_index: ");
                                        case ValueKind.CONSTANT_UInt:
                                            return createValueWithIndex(parent, index, value_index, TreeType.CONSTANT_UINT, "value_index: ");
                                        case ValueKind.CONSTANT_Double:
                                            return createValueWithIndex(parent, index, value_index, TreeType.CONSTANT_DOUBLE, "value_index: ");
                                        case ValueKind.CONSTANT_DecimalOrFloat: //?? or float ??
                                            return createValueWithIndex(parent, index, value_index, TreeType.CONSTANT_DECIMAL, "value_index: ");
                                        case ValueKind.CONSTANT_Utf8:
                                            return createValueWithIndex(parent, index, value_index, TreeType.CONSTANT_STRING, "value_index: ");
                                        case ValueKind.CONSTANT_True:
                                            break;
                                        case ValueKind.CONSTANT_False:
                                            break;
                                        case ValueKind.CONSTANT_Null:
                                            break;
                                        case ValueKind.CONSTANT_Undefined:
                                            break;
                                        case ValueKind.CONSTANT_Namespace:
                                        case ValueKind.CONSTANT_PackageInternalNs:
                                        case ValueKind.CONSTANT_ProtectedNamespace:
                                        case ValueKind.CONSTANT_ExplicitNamespace:
                                        case ValueKind.CONSTANT_StaticProtectedNs:
                                        case ValueKind.CONSTANT_PrivateNs:
                                            return createValueWithIndex(parent, index, value_index, TreeType.CONSTANT_NAMESPACE, "value_index: ");
                                    }
                                }
                            } else {
                                return new SubValue(parent, index, index, mi, "optional", "op" + index + ": " + mi.optional[index].toASMString(abc.constants));
                            }
                        case "param_names":
                            return createValueWithIndex(parent, index, mi.paramNames[index], TreeType.CONSTANT_STRING, "pn" + index + ": ");
                    }
                }
                if (sv.getParentValue() instanceof MethodBody) {
                    MethodBody body = (MethodBody) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "exceptions":
                            if (sv.getIndex() > -1) {
                                ABCException ex = body.exceptions[sv.getIndex()];
                                switch (index) {
                                    case 0:
                                        return new SimpleValue(parent, index, "start: " + ex.start);
                                    case 1:
                                        return new SimpleValue(parent, index, "end: " + ex.end);
                                    case 2:
                                        return new SimpleValue(parent, index, "target: " + ex.target);
                                    case 3:
                                        return createValueWithIndex(parent, index, ex.name_index, TreeType.CONSTANT_MULTINAME, "name: ");
                                    case 4:
                                        return createValueWithIndex(parent, index, ex.type_index, TreeType.CONSTANT_MULTINAME, "type: ");
                                }
                            } else {
                                return new SubValue(parent, index, index, body, "exceptions", "ex" + index);
                            }
                        case "traits":
                            return handleGetChildTrait(parent, index, body, sv, body.traits);
                    }
                }
                if (sv.getParentValue() instanceof InstanceInfo) {
                    InstanceInfo ii = (InstanceInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "interfaces":
                            return createValueWithIndex(parent, index, ii.interfaces[index], TreeType.CONSTANT_MULTINAME, "in" + index + ": ");
                        case "traits":
                            return handleGetChildTrait(parent, index, ii, sv, ii.instance_traits);
                    }
                }
                if (sv.getParentValue() instanceof ClassInfo) {
                    ClassInfo ci = (ClassInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "traits":
                            return handleGetChildTrait(parent, index, ci, sv, ci.static_traits);
                    }
                }
                if (sv.getParentValue() instanceof ScriptInfo) {
                    ScriptInfo ci = (ScriptInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "traits":
                            return handleGetChildTrait(parent, index, ci, sv, ci.traits);
                    }
                }

                if (sv.getParentValue() instanceof MetadataInfo) {
                    MetadataInfo md = (MetadataInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "pairs":
                            if (sv.getIndex() > -1) {
                                switch (index) {
                                    case 0:
                                        return createValueWithIndex(parent, index, md.keys[sv.getIndex()], TreeType.CONSTANT_STRING, "key: ");
                                    case 1:
                                        return createValueWithIndex(parent, index, md.values[sv.getIndex()], TreeType.CONSTANT_STRING, "value: ");
                                }
                                return null;
                            }
                            String pairTitle = formatString(md.keys[index]) + " : " + formatString(md.values[index]);
                            return new SubValue(parent, index, index, md, "pairs", "p" + index + ": " + pairTitle);
                    }
                }

                if (sv.getParentValue() instanceof Trait) {
                    Trait t = (Trait) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "metadata":
                            return createValueWithIndex(parent, index, t.metadata[index], TreeType.METADATA_INFO, "");
                    }
                }

            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent == type) {
                switch (type) {
                    case CONSTANT_INT:
                        return Math.max(1, abc.constants.getIntCount());
                    case CONSTANT_UINT:
                        return Math.max(1, abc.constants.getUIntCount());
                    case CONSTANT_DOUBLE:
                        return Math.max(1, abc.constants.getDoubleCount());
                    case CONSTANT_DECIMAL:
                        return Math.max(1, abc.constants.getDecimalCount());
                    case CONSTANT_FLOAT:
                        return Math.max(1, abc.constants.getFloatCount());
                    case CONSTANT_FLOAT_4:
                        return Math.max(1, abc.constants.getFloat4Count());
                    case CONSTANT_STRING:
                        return Math.max(1, abc.constants.getStringCount());
                    case CONSTANT_NAMESPACE:
                        return Math.max(1, abc.constants.getNamespaceCount());
                    case CONSTANT_NAMESPACE_SET:
                        return abc.constants.getNamespaceSetCount();
                    case CONSTANT_MULTINAME:
                        return Math.max(1, abc.constants.getMultinameCount());
                    case METHOD_INFO:
                        return abc.method_info.size();
                    case METADATA_INFO:
                        return abc.metadata_info.size();
                    case INSTANCE_INFO:
                        return abc.instance_info.size();
                    case CLASS_INFO:
                        return abc.class_info.size();
                    case SCRIPT_INFO:
                        return abc.script_info.size();
                    case METHOD_BODY:
                        return abc.bodies.size();
                }
            }
            if (parent instanceof ValueWithIndex) {
                ValueWithIndex vwi = (ValueWithIndex) parent;
                if (vwi.value instanceof NamespaceSet) {
                    NamespaceSet nss = (NamespaceSet) vwi.value;
                    return nss.namespaces.length;
                }
                if (vwi.value instanceof Namespace) {
                    //kind, name
                    return 2;
                }
                if (vwi.value instanceof Multiname) {
                    Multiname m = (Multiname) vwi.value;
                    int kind = m.kind;
                    if ((kind == Multiname.QNAME) || (kind == Multiname.QNAMEA)) {
                        return 1 + 2;
                    } else if ((kind == Multiname.RTQNAME) || (kind == Multiname.RTQNAMEA)) {
                        return 1 + 1;
                    } else if ((kind == Multiname.RTQNAMEL) || (kind == Multiname.RTQNAMELA)) {
                        return 1;
                    } else if ((kind == Multiname.MULTINAME) || (kind == Multiname.MULTINAMEA)) {
                        return 1 + 2;
                    } else if ((kind == Multiname.MULTINAMEL) || (kind == Multiname.MULTINAMELA)) {
                        return 1 + 1;
                    } else if (kind == Multiname.TYPENAME) {
                        return 1 + 1 + m.params.length;
                    }
                }
                if (vwi.value instanceof MethodInfo) {
                    MethodInfo mi = (MethodInfo) vwi.value;

                    int count = 4;
                    if (mi.flagHas_optional()) {
                        count++;
                    }
                    if (mi.flagHas_paramnames()) {
                        count++;
                    }
                    int bodyIndex = abc.findBodyIndex(vwi.getIndex());
                    if (bodyIndex != -1) {
                        count++;
                    }

                    return count;
                }
                if (vwi.value instanceof MethodBody) {
                    return 8;
                }
                if (vwi.value instanceof InstanceInfo) {
                    InstanceInfo ii = (InstanceInfo) vwi.value;
                    if ((ii.flags & InstanceInfo.CLASS_PROTECTEDNS) == InstanceInfo.CLASS_PROTECTEDNS) {
                        return 7;
                    }
                    return 6;
                }

                if (vwi.value instanceof ClassInfo) {
                    return 2;
                }

                if (vwi.value instanceof ScriptInfo) {
                    return 2;
                }

                if (vwi.value instanceof MetadataInfo) {
                    MetadataInfo md = (MetadataInfo) vwi.value;
                    return 2;
                }
            }
            if (parent instanceof SubValue) {
                SubValue sv = (SubValue) parent;
                if (sv.getParentValue() instanceof MethodInfo) {
                    MethodInfo mi = (MethodInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "param_types":
                            return mi.param_types.length;
                        case "optional":
                            if (sv.getIndex() > -1) {
                                int index = sv.getIndex();
                                int value_index = mi.optional[index].value_index;
                                switch (mi.optional[index].value_kind) {
                                    case ValueKind.CONSTANT_True:
                                    case ValueKind.CONSTANT_False:
                                    case ValueKind.CONSTANT_Null:
                                    case ValueKind.CONSTANT_Undefined:
                                        return 1;
                                    case ValueKind.CONSTANT_Int:
                                    case ValueKind.CONSTANT_UInt:
                                    case ValueKind.CONSTANT_Double:
                                    case ValueKind.CONSTANT_DecimalOrFloat: //?? or float ??
                                    case ValueKind.CONSTANT_Utf8:
                                    case ValueKind.CONSTANT_Namespace:
                                    case ValueKind.CONSTANT_PackageInternalNs:
                                    case ValueKind.CONSTANT_ProtectedNamespace:
                                    case ValueKind.CONSTANT_ExplicitNamespace:
                                    case ValueKind.CONSTANT_StaticProtectedNs:
                                    case ValueKind.CONSTANT_PrivateNs:
                                        return 2;
                                }
                                return 0;
                            }
                            return mi.optional.length;
                        case "param_names":
                            return mi.paramNames.length;
                    }
                }
                if (sv.getParentValue() instanceof MethodBody) {
                    MethodBody body = (MethodBody) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "exceptions":
                            if (sv.getIndex() > -1) {
                                return 5;
                            }
                            return body.exceptions.length;
                        case "traits":
                            return handleGetChildCountTrait(sv, body.traits);
                    }
                }
                if (sv.getParentValue() instanceof InstanceInfo) {
                    InstanceInfo ii = (InstanceInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "interfaces":
                            return ii.interfaces.length;
                        case "traits":
                            return handleGetChildCountTrait(sv, ii.instance_traits);
                    }
                }
                if (sv.getParentValue() instanceof ClassInfo) {
                    ClassInfo ci = (ClassInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "traits":
                            return handleGetChildCountTrait(sv, ci.static_traits);
                    }
                }
                if (sv.getParentValue() instanceof ScriptInfo) {
                    ScriptInfo ci = (ScriptInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "traits":
                            return handleGetChildCountTrait(sv, ci.traits);
                    }
                }

                if (sv.getParentValue() instanceof MetadataInfo) {
                    MetadataInfo md = (MetadataInfo) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "pairs":
                            if (sv.getIndex() > -1) {
                                return 2;
                            }
                            return md.keys.length;
                    }
                }

                if (sv.getParentValue() instanceof Trait) {
                    Trait t = (Trait) sv.getParentValue();
                    switch (sv.getProperty()) {
                        case "metadata":
                            return t.metadata.length;
                    }
                }
            }
            return 0;
        }

        @Override
        public boolean isLeaf(Object node) {
            return getChildCount(node) == 0;
        }

        @Override
        public void valueForPathChanged(TreePath path, Object newValue) {
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            System.err.println("getting index of child " + child + " in parent " + parent);
            if (child instanceof ValueWithIndex) {
                ValueWithIndex vwi = (ValueWithIndex) child;
                if (vwi.getParent() == parent) {
                    return vwi.getCurrentLevelIndex();
                }
            }
            if (child instanceof SubValue) {
                SubValue sv = (SubValue) child;
                if (sv.parent == parent) {
                    return sv.getCurrentLevelIndex();
                }
            }
            if (child instanceof SimpleValue) {
                SimpleValue sv = (SimpleValue) child;
                if (sv.parent == parent) {
                    return sv.getCurrentLevelIndex();
                }
            }
            return -1;
        }

        @Override
        public void addTreeModelListener(TreeModelListener l) {
        }

        @Override
        public void removeTreeModelListener(TreeModelListener l) {
        }
    }

    public static class ExplorerTreeCellRenderer extends DefaultTreeCellRenderer {

        public ExplorerTreeCellRenderer() {
            setUI(new BasicLabelUI());
            setOpaque(false);
            if (View.isOceanic()) {
                setBackgroundNonSelectionColor(Color.white);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            /*if (semiTransparent) {
                if (getIcon() != null) {
                    Color color = getBackground();
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 2));
                    g2d.setComposite(AlphaComposite.SrcOver);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }*/
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);

            if (View.isOceanic()) {
                setForeground(Color.BLACK);
            }
            setToolTipText(null);

            //semitransparent = true;
            return this;
        }
    }
}
