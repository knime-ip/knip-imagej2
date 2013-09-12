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
package org.knime.knip.imagej2.core.adapter.impl;

import ij.ImagePlus;

import java.io.IOException;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapter;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapterInstance;
import org.knime.knip.imagej2.core.util.IJToImg;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImagePlusOutputAdapter implements IJOutputAdapter<ImagePlus> {

    @Override
    public Class<ImagePlus> getIJType() {
        return ImagePlus.class;
    }

    @Override
    public Class<? extends DataValue>[] getDataValueTypes() {
        return new Class[]{ImgPlusValue.class};
    }

    @Override
    public DataType[] getDataTypes() {
        return new DataType[]{ImgPlusCell.TYPE};
    }

    @Override
    public int getCellCount() {
        return 1;
    }

    @Override
    public IJOutputAdapterInstance<ImagePlus> createAdapterInstance(final ExecutionContext exec) {

        final ImgPlusCellFactory factory = new ImgPlusCellFactory(exec);

        return new IJOutputAdapterInstance<ImagePlus>() {

            @Override
            public DataCell[] getDataCells(final ImagePlus ijObject) {

                final IJToImg convert = new IJToImg(IJToImg.createMatchingType(ijObject));
                final Img image = Operations.compute(convert, ijObject);

                ImgPlusCell output = null;
                try {
                    output = factory.createCell(new ImgPlus(image));
                } catch (final IOException e) {
                    e.printStackTrace();
                }

                return new DataCell[]{output};
            }
        };
    }
}
