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
package org.knime.knip.imagej2.core.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.knip.imagej2.core.IJGateway;
import org.knime.knip.imagej2.core.adapter.DataValueConfigGuiInfos;
import org.knime.knip.imagej2.core.adapter.IJAdapterProvider;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapterInstance;
import org.knime.knip.imagej2.core.adapter.ModuleItemConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemDataValueConfig;
import org.knime.knip.imagej2.core.adapter.ModuleItemRowConfig;
import org.scijava.module.MethodCallException;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleRunner;
import org.scijava.module.process.InitPreprocessor;
import org.scijava.module.process.ModulePreprocessor;

/**
 * Provides a basic set of methods that are common to all IJCellFactories like support for missing cell count and the
 * {@link #executeRowModule(Module)} method. Additionally provides helper methods for the configuration
 * {@link ModuleItemRowConfig} and {@link ModuleItemDataValueConfig}.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public abstract class AbstractIJCellFactory implements CellFactory {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(AbstractIJCellFactory.class);

    /** counts the number of errors that resulted in missing cell output. */
    private int m_missingCellCount;

    private final ExecutionContext m_exec;

    /**
     * Create a new {@link AbstractIJCellFactory} which uses the given {@link ExecutionContext}
     *
     * @param exec
     */
    public AbstractIJCellFactory(final ExecutionContext exec) {
        m_missingCellCount = 0;
        m_exec = exec;
    }

    /**
     * @return the amount of cells that could not be processed and have been set to MissingCell.
     */
    public int getMissingCellCount() {
        return m_missingCellCount;
    }

    /**
     * Configures parts of a module with the content of the current row.<br>
     * <br>
     * Only module configs that are in the list and that are of type {@link ModuleItemRowConfig} get resolved and
     * configured.
     *
     * @param row the currently processed DataRow that contains the values that should be bound to the module items
     * @param module contains the module items (parameters) that should be configured
     * @param moduleItemConfigs contains the guiding configuration objects (only RowConfigs are processed in this
     *            method)
     */
    protected void configureRowConfigItems(final DataRow row, final Module module,
                                           final List<ModuleItemConfig> moduleItemConfigs) {
        if (row != null) {
            for (final ModuleItemConfig itemConfig : moduleItemConfigs) {
                if (itemConfig instanceof ModuleItemRowConfig) {
                    //configure all row dependent
                    ((ModuleItemRowConfig)itemConfig).setConfigurationData(row);

                    // resolve and configure the handled module parts
                    itemConfig.resolveHandledModuleItems(module, false);
                    itemConfig.configureModuleItem(module);
                    ModuleItem<?> item = itemConfig.getItem();
                    try {
                        item.callback(module);
                    } catch (MethodCallException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * Configures parts of a module with the content of the current row. <br>
     * <br>
     * Only module configs that are in the list, that are of type {@link ModuleItemDataValueConfig} and that can be
     * fully configured using the identifier2CellID mapping get resolved and configured.
     *
     * @param row the currently processed DataRow that contains the values that should be bound to the module items
     * @param module contains the module items (parameters) that should be configured
     * @param identifier2CellID maps unique parameter identifiers (see {@link DataValueConfigGuiInfos}) to cell ids
     * @param moduleItemConfigs contains the guiding configuration objects (only RowConfigs are processed in this
     *            method)
     * @throws MethodCallException
     */
    protected void configureDataValueConfigItems(final DataRow row, final Module module,
                                                 final List<ModuleItemConfig> moduleItemConfigs,
                                                 final Map<String, Integer> identifier2CellID)
            throws MethodCallException {
        if (row != null) {
            for (final ModuleItemConfig itemConfig : moduleItemConfigs) {
                if (itemConfig instanceof ModuleItemDataValueConfig) {
                    //configure if the parameter identifiers map to column ids
                    final DataValueConfigGuiInfos[] infos = ((ModuleItemDataValueConfig)itemConfig).getGuiMetaInfo();
                    boolean containsAll = true;
                    for (final DataValueConfigGuiInfos guiInfo : infos) {
                        if (!identifier2CellID.containsKey(guiInfo.identifier)) {
                            containsAll = false;
                        }
                    }

                    if (containsAll) {
                        final DataValue[] dvs = new DataValue[infos.length];
                        int i = 0;
                        for (final DataValueConfigGuiInfos guiInfo : infos) {
                            final int index = identifier2CellID.get(guiInfo.identifier);
                            dvs[i] = row.getCell(index);
                            i++;
                        }
                        ((ModuleItemDataValueConfig)itemConfig).setConfigurationData(dvs);
                    }

                    // resolve and configure the handled module parts
                    itemConfig.resolveHandledModuleItems(module, false);
                    itemConfig.configureModuleItem(module);

                    ModuleItem<?> item = itemConfig.getItem();
                    item.callback(module);
                }
            }
        }
    }

    /**
     * executes a preconfigured row module and collects the results. Also updates the missing cell counter if necessary.
     *
     * @param rowModule a fully configured module ready for execution
     * @return list of DataCells that contains the module results
     * @throws Exception
     */
    protected List<DataCell> executeRowModule(final Module rowModule) throws Exception {
        @SuppressWarnings("rawtypes")
        final Map<Class<?>, IJOutputAdapterInstance> adapterMap = new HashMap<Class<?>, IJOutputAdapterInstance>();

        for (final ModuleItem<?> outItem : rowModule.getInfo().outputs()) {
            if (!adapterMap.containsKey(outItem.getType())) {
                adapterMap.put(outItem.getType(), IJAdapterProvider.getOutputAdapter(outItem.getType())
                        .createAdapterInstance(m_exec));
            }
        }

        // execute the module
        final List<ModulePreprocessor> pre = new ArrayList<ModulePreprocessor>();
        final InitPreprocessor ip = new InitPreprocessor();
        // TODO        final ValidityPreprocessor
        ip.setContext(IJGateway.getImageJContext());

        pre.add(ip);

        // TODO potentially: ModuleService.run(...) for ...
        final ModuleRunner runner = new ModuleRunner(IJGateway.getImageJContext(), rowModule, pre, null);

        runner.run();

        // TODO: potentially just make use of a postprocessor. but for now its fine!!
        // collect the outputs
        final List<DataCell> resCells = new ArrayList<DataCell>(rowModule.getOutputs().size());
        for (final ModuleItem<?> outItem : rowModule.getInfo().outputs()) {
            final Object ijObject = rowModule.getOutput(outItem.getName());

            if (ijObject == null) {
                resCells.add(DataType.getMissingCell());
                m_missingCellCount++;
            } else {

                @SuppressWarnings("unchecked")
                final DataCell[] dataCells = adapterMap.get(outItem.getType()).getDataCells(ijObject);

                for (final DataCell dataCell : dataCells) {
                    resCells.add(dataCell);
                }
            }

        }
        return resCells;
    }

    @Override
    public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey, final ExecutionMonitor exec) {
        exec.setProgress((double)curRowNr / rowCount);
    }

    protected void fireWarning(final String rowKey, final String cause) {
        LOGGER.warn("Error while executing row: " + rowKey + "! Cause: " + cause);
    }

}
