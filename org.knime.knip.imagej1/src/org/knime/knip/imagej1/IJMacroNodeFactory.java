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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.GenericValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.dialog.DialogComponentDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.node.nodesettings.SettingsModelSerializableObjects;
import org.knime.knip.base.nodes.io.kernel.DialogComponentSerializableConfiguration;
import org.knime.knip.base.nodes.io.kernel.SerializableSetting;
import org.knime.knip.core.data.img.DefaultImgMetadata;
import org.knime.knip.core.util.CellUtil;
import org.knime.knip.imagej1.macro.AnalyzeParticlesIJMacro;
import org.knime.knip.imagej1.macro.CLAHEIJMacro;
import org.knime.knip.imagej1.macro.DespeckleIJMacro;
import org.knime.knip.imagej1.macro.EDT3DIJMacro;
import org.knime.knip.imagej1.macro.FindEdgesIJMacro;
import org.knime.knip.imagej1.macro.FindMaximaIJMacro;
import org.knime.knip.imagej1.macro.GaussianBlurIJMacro;
import org.knime.knip.imagej1.macro.PureCodeIJMacro;
import org.knime.knip.imagej1.macro.ROFDenoiseIJMacro;
import org.knime.knip.imagej1.macro.SharpenIJMacro;
import org.knime.knip.imagej1.macro.SkeletonizeIJMacro;
import org.knime.knip.imagej1.macro.SubstractBackgroundIJMacro;
import org.knime.knip.imagej1.macro.WatershedIJMacro;
import org.knime.knip.imagej1.prefs.IJ1Preferences;
import org.knime.knip.imagej2.core.util.UntransformableIJTypeException;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;

import ij.measure.ResultsTable;
import net.imagej.ImgPlus;
import net.imagej.ImgPlusMetadata;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgView;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.iterableinterval.unary.IterableIntervalCopy;
import net.imglib2.ops.util.MetadataUtil;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * {@link NodeFactory} to run {@link IJMacro}s
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 *
 * @param <T>
 *
 */
@SuppressWarnings("rawtypes")
public class IJMacroNodeFactory<T extends RealType<T>>
        extends GenericValueToCellNodeFactory<ImgPlusValue, ValueToCellNodeModel<ImgPlusValue, ImgPlusCell>> {

    private static SettingsModelSerializableObjects<SerializableSetting<String>> createMacroSelectionModel() {
        return new SettingsModelSerializableObjects<SerializableSetting<String>>("kernels", new ImageJ1ObjectExt0());
    }

    private static SettingsModelDimSelection createDimSelectionModel() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    private static SettingsModelString createFlowVariableControllableCode() {
        return new SettingsModelString("flow_variable_controlable_code", "");
    }

    private static SettingsModelBoolean createResultTableEntriesAsStringModel() {
        return new SettingsModelBoolean("resulttable_entries_as_string", false);
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

            private final SettingsModelBoolean m_resultTableEntriesAsString = createResultTableEntriesAsStringModel();

            private IJMacro<T> m_macro;

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

                m_macro = new IJMacro<T>(code);
                m_imgCellFactory = new ImgPlusCellFactory(exec);

                m_exec = exec;
                m_resTableContainer = null;

                System.setProperty("plugins.dir", IJ1Preferences.getIJ1PluginPath());
            }

            private DataTableSpec createResultTableSpec(final ResultsTable table) {
                final String colHeadings = table.getColumnHeadings();
                final StringTokenizer tk = new StringTokenizer(colHeadings, "\t");
                final ArrayList<DataColumnSpec> colSpecs = new ArrayList<DataColumnSpec>(tk.countTokens());
                while (tk.hasMoreTokens()) {
                    final String token = tk.nextToken().trim();
                    if (token.length() > 0) {
                        if (!m_resultTableEntriesAsString.getBooleanValue()) {
                            colSpecs.add(new DataColumnSpecCreator(token, DoubleCell.TYPE).createSpec());
                        } else {
                            colSpecs.add(new DataColumnSpecCreator(token, StringCell.TYPE).createSpec());
                        }
                    }
                }
                return new DataTableSpec(colSpecs.toArray(new DataColumnSpec[colSpecs.size()]));

            }

            @SuppressWarnings("unchecked")
            @Override
            protected ImgPlusCell compute(final ImgPlusValue cellValue) throws Exception {

                final ImgPlus img = CellUtil.getZeroMinImgPlus(cellValue.getImgPlus());

                final Interval[] intervals = m_dimSelection.getIntervals(img, img);
                final int[] m_selectedDims = m_dimSelection.getSelectedDimIndices(img);
                final IterableIntervalCopy copyOp = new IterableIntervalCopy();

                ImgPlus res = null;

                if (m_selectedDims.length < 2) {
                    throw new KNIPException(
                            "Selected dimensions do result in an Img with less than two dimensions for Img "
                                    + cellValue.getMetadata().getName() + ". MissingCell is created.");
                }

                long[] min = new long[img.numDimensions()];
                for (final Interval interval : intervals) {
                    interval.min(min);

                    RandomAccessibleInterval subsetview = SubsetOperations.subsetview(img.getImg(), interval);
                    ImgPlusMetadata meta = MetadataUtil
                            .copyAndCleanImgPlusMetadata(interval, img,
                                                         new DefaultImgMetadata(subsetview.numDimensions()));

                    try {
                        ImgPlus<T> imgPlus = new ImgPlus<T>(ImgView.wrap(subsetview, img.factory()), meta);
                        m_macro.run(imgPlus);
                    } catch (UntransformableIJTypeException e) {
                        throw new KNIPRuntimeException(e.getMessage(), e);
                    } catch (KNIPRuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new KNIPRuntimeException(
                                "The specified macro has thrown an error while execution. Make sure that the used plugins are available in the selected IJ1 plugin folder! See KNIME Log for details!",
                                e);
                    }

                    if (res == null && intervals.length > 1) {
                        final long[] dims = new long[img.numDimensions()];
                        img.dimensions(dims);
                        for (int i = 0; i < m_selectedDims.length; i++) {
                            dims[m_selectedDims[i]] = m_macro.resImgPlus().dimension(i);
                        }
                        res = new ImgPlus(
                                img.factory().create(dims, m_macro.resImgPlus().firstElement().createVariable()), img);
                        res.setSource(img.getSource());
                    }

                    if (intervals.length > 1) {
                        copyOp.compute(m_macro.resImgPlus(), Views
                                .iterable((RandomAccessibleInterval<T>)SubsetOperations.subsetview(res, interval)));
                    } else {
                        res = m_macro.resImgPlus();
                        res.setSource(img.getSource());
                    }

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
                                colIndices[i] = m_macro.resTable()
                                        .getColumnIndex(m_resTableContainer.getTableSpec().getColumnSpec(i).getName());
                            }
                            for (int r = 0; r < m_macro.resTable().getCounter(); r++) {
                                final DataCell[] cells;
                                if (!m_resultTableEntriesAsString.getBooleanValue()) {
                                    cells = new DoubleCell[numCols];
                                    for (int c = 0; c < cells.length; c++) {
                                        cells[c] =
                                                new DoubleCell(m_macro.resTable().getValueAsDouble(colIndices[c], r));
                                    }
                                } else {
                                    cells = new StringCell[numCols];
                                    for (int c = 0; c < cells.length; c++) {
                                        cells[c] = new StringCell(m_macro.resTable().getStringValue(colIndices[c], r));
                                    }
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
                settingsModels.add(m_resultTableEntriesAsString);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue>() {

            @SuppressWarnings("unchecked")
            @Override
            public void addDialogComponents() {

                //TODO make macros a extension point / Image2 Service
                final Map<String, Class<?>> pool = new LinkedHashMap<String, Class<?>>();
                pool.put("Pure Code", PureCodeIJMacro.class);
                pool.put("Gaussian Blur", GaussianBlurIJMacro.class);
                pool.put("Find Edges", FindEdgesIJMacro.class);
                pool.put("Find Maxima", FindMaximaIJMacro.class);
                pool.put("Analyze Particles", AnalyzeParticlesIJMacro.class);
                pool.put("Sharpen", SharpenIJMacro.class);
                pool.put("Watersched", WatershedIJMacro.class);
                pool.put("Substract Background", SubstractBackgroundIJMacro.class);
                pool.put("Enhance Local Constract (CLAHE)", CLAHEIJMacro.class);
                pool.put("Exact Euclidean Distance Transform (3D)", EDT3DIJMacro.class);
                pool.put("Skeletonize", SkeletonizeIJMacro.class);
                pool.put("ROF Denoise", ROFDenoiseIJMacro.class);
                pool.put("Despeckle", DespeckleIJMacro.class);

                addDialogComponent("Options", "Snippets",
                                   new DialogComponentSerializableConfiguration(createMacroSelectionModel(), pool));
                addDialogComponent("Options", "Dimension Selection",
                                   new DialogComponentDimSelection(createDimSelectionModel(), "", 2, 5));
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

                addDialogComponent("Additional Options", "Result Table", new DialogComponentBoolean(
                        createResultTableEntriesAsStringModel(), "Return result table entries as strings"));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeDescriptionContent(final KnimeNode node) {
        DialogComponentDimSelection.createNodeDescription(node.getFullDescription().getTabList().get(0).addNewOption());
    }

}
