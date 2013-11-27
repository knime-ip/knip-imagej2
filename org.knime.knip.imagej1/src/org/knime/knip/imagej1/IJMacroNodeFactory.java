/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.imagej1;

import ij.measure.ResultsTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.ImgPlusMetadata;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.Inset;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.GenericValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelSerializableObjects;
import org.knime.knip.base.nodes.io.kernel.DialogComponentSerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.imagej1.macro.AnalyzeParticlesIJMacro;
import org.knime.knip.imagej1.macro.CLAHEIJMacro;
import org.knime.knip.imagej1.macro.FindEdgesIJMacro;
import org.knime.knip.imagej1.macro.FindMaximaIJMacro;
import org.knime.knip.imagej1.macro.GaussianBlurIJMacro;
import org.knime.knip.imagej1.macro.PureCodeIJMacro;
import org.knime.knip.imagej1.macro.SharpenIJMacro;
import org.knime.knip.imagej1.macro.SubstractBackgroundIJMacro;
import org.knime.knip.imagej1.macro.WatershedIJMacro;

/**
 * TODO Auto-generated
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class IJMacroNodeFactory<T extends RealType<T>> extends
        GenericValueToCellNodeFactory<ImgPlusValue, ValueToCellNodeModel<ImgPlusValue, ImgPlusCell>> {

    private static SettingsModelSerializableObjects<SerializableSetting<String>> createMacroSelectionModel() {
        return new SettingsModelSerializableObjects<SerializableSetting<String>>("kernels", new ImageJ1ObjectExt0());
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelString createFlowVariableControllableCode() {
        return new SettingsModelString("flow_variable_controlable_code", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue, ImgPlusCell> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue, ImgPlusCell>(new PortType[]{},
                new PortType[]{BufferedDataTable.TYPE}) {

            private final SettingsModelSerializableObjects<SerializableSetting<String>> m_macroSelection =
                    createMacroSelectionModel();

            private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

            private final SettingsModelString m_flowVarCode = createFlowVariableControllableCode();

            private IJMacro m_macro;

            private ImgPlusCellFactory m_imgCellFactory;

            private BufferedDataContainer m_resTableContainer;

            private String m_currentRowKey;

            private ExecutionContext m_exec;

            /**
             * {@inheritDoc}
             */
            @Override
            protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
                final PortObjectSpec firstSpec = super.configure(inSpecs)[0];
                // PortObjectSpec secondSpec = null;

                final List<SerializableSetting<String>> conf = m_macroSelection.getObjects();
                String code = "";
                for (final SerializableSetting<String> fc : conf) {
                    code += fc.get();
                }
                // IJMacro macro = new IJMacro(true, code);
                // // pre-run macro to determine the format of the result table
                // if
                // // existent
                // Img<UnsignedByteType> testImg = new
                // ArrayImgFactory<UnsignedByteType>()
                // .create(new int[] { 10, 10 }, new UnsignedByteType());
                // macro.runOn(testImg, new UnsignedByteType());
                // if (macro.resTable() != null) {
                // secondSpec = createResultTableSpec(macro.resTable());
                // }
                // if (secondSpec == null) {
                // secondSpec = new DataTableSpec();
                // }
                return new PortObjectSpec[]{firstSpec, null};
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
                final PortObject firstPort = super.execute(inObjects, exec)[0];
                PortObject secondPort = null;
                if (m_resTableContainer == null) {
                    m_resTableContainer = exec.createDataContainer(new DataTableSpec());
                }
                m_resTableContainer.close();
                secondPort = m_resTableContainer.getTable();
                return new PortObject[]{firstPort, secondPort};

            }

            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                final List<SerializableSetting<String>> conf = m_macroSelection.getObjects();
                String code = "";
                if (m_flowVarCode.getStringValue().length() == 0) {
                    for (final SerializableSetting<String> fc : conf) {
                        code += fc.get();
                    }
                } else {
                    setWarningMessage("IJ macro code controlled by a flow variable!");
                    code = m_flowVarCode.getStringValue();
                    m_flowVarCode.setStringValue("");
                }

                m_macro = new IJMacro(true, code);
                m_imgCellFactory = new ImgPlusCellFactory(exec);

                m_exec = exec;
                m_resTableContainer = null;

                // pre-run macro to determine the format of the result table if
                // existent
                // Img<UnsignedByteType> testImg = new
                // ArrayImgFactory<UnsignedByteType>()
                // .create(new int[] { 10, 10 }, new UnsignedByteType());
                // m_macro.runOn(testImg, new UnsignedByteType());
                // if (m_macro.resTable() != null) {
                // DataTableSpec spec = createResultTableSpec(m_macro
                // .resTable());
                // if (spec != null) {
                // m_resTableContainer = exec.createDataContainer(spec);
                // } else {
                // m_resTableContainer = null;
                // }
                // } else {
                // m_resTableContainer = null;
                // }

            }

            private DataTableSpec createResultTableSpec(final ResultsTable table) {
                final String colHeadings = table.getColumnHeadings();
                final StringTokenizer tk = new StringTokenizer(colHeadings, "\t");
                final ArrayList<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>(tk.countTokens());
                while (tk.hasMoreTokens()) {
                    final String token = tk.nextToken().trim();
                    if (token.length() > 0) {
                        colSpecs.add(new DataColumnSpecCreator(token, DoubleCell.TYPE).createSpec());
                    }
                }
                return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));

            }

            @Override
            protected ImgPlusCell compute(final ImgPlusValue cellValue) throws Exception {

                RealType<?> matchingType = null;
                final ImgPlus img = cellValue.getImgPlus();
                ImgPlus res = null;
                final Interval[] intervals = m_dimSelection.getIntervals(img, img);

                final int[] m_selectedDims = m_dimSelection.getSelectedDimIndices(img);

                if (m_selectedDims.length < 2) {
                    throw new KNIPException(
                            "Selected dimensions do result in an Img with less than two dimensions for Img "
                                    + cellValue.getMetadata().getName() + ". MissingCell is created.");
                }

                final long[] min = new long[img.numDimensions()];
                final Inset inset = new Inset(min);

                for (final Interval interval : intervals) {
                    RandomAccessibleInterval subsetview = SubsetOperations.subsetview(img, interval);
                    ImgPlusMetadata meta =
                            MetadataUtil
                                    .copyAndCleanImgPlusMetadata(interval, img,
                                                                 new DefaultImgMetadata(subsetview.numDimensions()));

                    matchingType =
                            m_macro.runOn(new ImgPlus<T>(new ImgView<T>(subsetview, img.factory()), meta), matchingType);
                    if (m_macro.resImgPlus() == null) {
                        throw new IllegalArgumentException("The specified macro didn't return an image.");
                    }
                    interval.min(min);
                    if (res == null) {
                        final long[] dims = new long[img.numDimensions()];
                        img.dimensions(dims);
                        for (int i = 0; i < m_selectedDims.length; i++) {
                            dims[m_selectedDims[i]] = m_macro.resImgPlus().dimension(i);
                        }
                        res =
                                new ImgPlus(img.factory().create(dims,
                                                                 m_macro.resImgPlus().firstElement().createVariable()),
                                        img);
                    }

                    inset.compute(m_macro.resImgPlus(), res);

                    // fill result table if available
                    if (m_macro.resTable() != null) {
                        if (m_resTableContainer == null) {
                            final DataTableSpec spec = createResultTableSpec(m_macro.resTable());
                            if (spec != null) {
                                m_resTableContainer = m_exec.createDataContainer(spec);
                            } else {
                                m_resTableContainer = null;
                            }
                        }
                        if (m_resTableContainer != null) {
                            final int numCols = m_resTableContainer.getTableSpec().getNumColumns();
                            final int[] colIndices = new int[numCols];
                            for (int i = 0; i < colIndices.length; i++) {
                                colIndices[i] =
                                        m_macro.resTable().getColumnIndex(m_resTableContainer.getTableSpec()
                                                                                  .getColumnSpec(i).getName());
                            }
                            for (int r = 0; r < m_macro.resTable().getCounter(); r++) {
                                final DoubleCell[] cells = new DoubleCell[numCols];
                                for (int c = 0; c < cells.length; c++) {
                                    cells[c] = new DoubleCell(m_macro.resTable().getValueAsDouble(colIndices[c], r));
                                }
                                String rowKey;
                                if (intervals.length > 1) {
                                    rowKey = m_currentRowKey + "#" + Arrays.toString(min) + "#" + r;
                                } else {
                                    rowKey = m_currentRowKey + "#" + r;
                                }
                                m_resTableContainer.addRowToTable(new DefaultRow(rowKey, cells));
                            }
                        }

                    }
                }

                return m_imgCellFactory.createCell(res);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void computeDataRow(final DataRow row) {
                m_currentRowKey = row.getKey().toString();
            }

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_macroSelection);
                settingsModels.add(m_dimSelection);
                settingsModels.add(m_flowVarCode);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue>() {

            @Override
            public void addDialogComponents() {
                // coll.addDialogComponent("ImageJ", new
                // DialogComponentFileChooser(
                // DialogComponentCollection.cloneSettingsModel(m_ijPath),
                // "ij-path-history", JFileChooser.OPEN_DIALOG,
                // true));
                final Map<String, Class<?>> pool = new LinkedHashMap<String, Class<?>>();
                pool.put("Enhance Local Constract (CLAHE)", CLAHEIJMacro.class);
                pool.put("Pure Code", PureCodeIJMacro.class);
                pool.put("Gaussian Blur", GaussianBlurIJMacro.class);
                pool.put("Find Edges", FindEdgesIJMacro.class);
                pool.put("Find Maxima", FindMaximaIJMacro.class);
                pool.put("Analyze Particles", AnalyzeParticlesIJMacro.class);
                pool.put("Sharpen", SharpenIJMacro.class);
                pool.put("Watersched", WatershedIJMacro.class);
                pool.put("Substract Background", SubstractBackgroundIJMacro.class);
                addDialogComponent("Options", "Snippets", new DialogComponentSerializableConfiguration(
                        createMacroSelectionModel(), pool));
                addDialogComponent("Options", "", new DialogComponentDimSelection(createDimSelectionModel(),
                        "Dimension Selection", 2, 3));
                // hidden dialog component to be able to controll the imagej
                // macro node by flow variables
                addDialogComponent("Options", "Snippets", new DialogComponent(createFlowVariableControllableCode()) {

                    @Override
                    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
                    }

                    @Override
                    protected void updateComponent() {
                    }

                    @Override
                    public void setToolTipText(final String text) {
                    }

                    @Override
                    protected void setEnabledComponents(final boolean enabled) {
                    }

                    @Override
                    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
                            throws NotConfigurableException {

                    }
                });

            }
        };
    }

}
