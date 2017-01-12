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
package org.knime.knip.imagej2.core.adapter.impl.basicoutput;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.vector.doublevector.DoubleVectorCellFactory;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapter;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapterInstance;

/**
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 */
@SuppressWarnings("unchecked")
public class FloatArrayOutputAdapter implements IJOutputAdapter<float[]> {

    @Override
    public Class<float[]> getIJType() {
        return float[].class;
    }

    @Override
    public Class<? extends DataValue>[] getDataValueTypes() {
        return new Class[]{DoubleVectorValue.class};
    }

    @Override
    public DataType[] getDataTypes() {
        return new DataType[]{DoubleVectorCellFactory.TYPE};
    }

    @Override
    public int getCellCount() {
        return 1;
    }

    @Override
    public IJOutputAdapterInstance<float[]> createAdapterInstance(final ExecutionContext exec) {

        return new IJOutputAdapterInstance<float[]>() {

            @Override
            public DataCell[] getDataCells(final float[] ijObject) {
                DoubleVectorCellFactory fac = new DoubleVectorCellFactory();
                fac.init(exec);

                final double[] out = new double[ijObject.length];
                for (int d = 0; d < ijObject.length; d++) {
                    out[d] = ijObject[d];
                }

                return new DataCell[]{DoubleVectorCellFactory.createCell((out))};
            }
        };
    }

}
