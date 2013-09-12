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

import imagej.data.display.ImageDisplayService;
import imagej.data.display.OverlayService;

import org.knime.knip.imagej2.core.adapter.IJAdapterFactory;
import org.knime.knip.imagej2.core.adapter.IJInputAdapter;
import org.knime.knip.imagej2.core.adapter.IJOutputAdapter;
import org.knime.knip.imagej2.core.adapter.IJServiceAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.BooleanInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.ByteInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.CharacterInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.DoubleInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.FloatInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.IntegerInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.LongInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PBooleanInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PByteInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PCharacterInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PDoubleInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PFloatInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PIntegerInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PLongInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.PShortInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.ShortInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicinput.StringInputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.BooleanOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.ByteOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.CharacterOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.DoubleOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.FloatOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.IntegerOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.LongOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PBooleanOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PByteOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PCharacterOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PDoubleOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PFloatOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PIntegerOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PLongOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.PShortOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.ShortOutputAdapter;
import org.knime.knip.imagej2.core.adapter.impl.basicoutput.StringOutputAdapter;
import org.scijava.service.Service;

/**
 * uses the adapter extension point to add all input, output and service adapters that are supported by the core
 * implementation. This includes especially the converters for all basic data types that can be handled by the ImageJ
 * parameter dialog.
 * 
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class CoreAdapterFactory implements IJAdapterFactory {

    @Override
    public IJOutputAdapter[] getOutputAdapters() {
        return new IJOutputAdapter[]{new DatasetOutputAdapter(), new ImageDisplayOutputAdapter(),
                new ImagePlusOutputAdapter(), new BooleanOutputAdapter(), new ByteOutputAdapter(),
                new CharacterOutputAdapter(), new DoubleOutputAdapter(), new FloatOutputAdapter(),
                new IntegerOutputAdapter(), new LongOutputAdapter(), new ShortOutputAdapter(),
                new StringOutputAdapter(), new PBooleanOutputAdapter(), new PByteOutputAdapter(),
                new PCharacterOutputAdapter(), new PDoubleOutputAdapter(), new PFloatOutputAdapter(),
                new PIntegerOutputAdapter(), new PLongOutputAdapter(), new PShortOutputAdapter(),
                new ImgOutputAdapter(), new LabelingOutputAdapter()};
    }

    @Override
    public IJInputAdapter[] getInputAdapters() {
        return new IJInputAdapter[]{new DatasetInputAdapter(), new ImageDisplayInputAdapter(),
                new ImagePlusInputAdapter(), new BooleanInputAdapter(), new ByteInputAdapter(),
                new CharacterInputAdapter(), new DoubleInputAdapter(), new FloatInputAdapter(),
                new IntegerInputAdapter(), new LongInputAdapter(), new ShortInputAdapter(), new StringInputAdapter(),
                new PBooleanInputAdapter(), new PByteInputAdapter(), new PCharacterInputAdapter(),
                new PDoubleInputAdapter(), new PFloatInputAdapter(), new PIntegerInputAdapter(),
                new PLongInputAdapter(), new PShortInputAdapter(), new ImgInputAdapter(), new LabelingInputAdapter()};
    }

    @Override
    public IJServiceAdapter<? extends Service>[] getServiceAdapters() {

        // construct simple service class adapters
        final IJServiceAdapter<OverlayService> overlayServiceAdapter = new IJServiceAdapter<OverlayService>() {
            @Override
            public Class<OverlayService> getIJType() {
                return OverlayService.class;
            }
        };

        final IJServiceAdapter<ImageDisplayService> imageServiceAdapter = new IJServiceAdapter<ImageDisplayService>() {
            @Override
            public Class<ImageDisplayService> getIJType() {
                return ImageDisplayService.class;
            }
        };

        // return them
        return new IJServiceAdapter[]{overlayServiceAdapter, imageServiceAdapter};
    }
}
