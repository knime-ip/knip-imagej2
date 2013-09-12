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
package org.knime.knip.imagej1.plugin.reg;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.img.UnaryObjectFactory;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.numeric.RealType;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeView;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.GenericValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.base.nodes.view.TableCellViewNodeView;
import org.knime.knip.core.util.ImgUtils;
import org.knime.knip.imagej1.IJPlugin;
import org.knime.knip.imagej1.plugin.reg.TurboReg_.TransformationType;

/**
 * Registration powered by EPFL Laussanne
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class StackRegNodeFactory<T extends RealType<T>>
		extends
		GenericValueToCellNodeFactory<ImgPlusValue<T>, ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>> {

	private static SettingsModelString createTransformationTypeModel() {
		return new SettingsModelString("transformation_type", "");
	}

	private static SettingsModelDimSelection createDimSelectionModel() {
		return new SettingsModelDimSelection("dim_selection", "X", "Y", "Z");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> createNodeModel() {
		return new ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>() {

			private final SettingsModelString m_transformationType = createTransformationTypeModel();

			private final SettingsModelDimSelection m_dimSelection = createDimSelectionModel();

			private ImgPlusCellFactory m_imgCellFactory;

			@Override
			protected void addSettingsModels(
					final List<SettingsModel> settingsModels) {
				settingsModels.add(m_transformationType);
				settingsModels.add(m_dimSelection);

			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void prepareExecute(final ExecutionContext exec) {
				m_imgCellFactory = new ImgPlusCellFactory(exec);
			}

			@Override
			protected ImgPlusCell<T> compute(final ImgPlusValue<T> cellValue)
					throws IOException {

				// TODO: Logger

				final StackRegOp<T> op = new StackRegOp<T>(
						TransformationType.valueOf(m_transformationType
								.getStringValue()));

				Img<T> out = null;
				try {
					out = SubsetOperations.iterate(op, m_dimSelection
							.getSelectedDimIndices(cellValue.getImgPlus()),
							cellValue.getImgPlus(), ImgUtils
									.createEmptyImg(cellValue.getImgPlus()),
							getExecutorService());
				} catch (final InterruptedException e) {
					e.printStackTrace();
				} catch (final ExecutionException e) {
					e.printStackTrace();
				}

				return m_imgCellFactory.createCell(out, cellValue.getImgPlus());
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeView<ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>> createNodeView(
			final int viewIndex,
			final ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>> nodeModel) {
		return new TableCellViewNodeView<ValueToCellNodeModel<ImgPlusValue<T>, ImgPlusCell<T>>>(
				nodeModel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
		return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

			@Override
			public void addDialogComponents() {
				//
			}
		};
	}

}

class StackRegOp<T extends RealType<T>> implements
		UnaryOutputOperation<ImgPlus<T>, Img<T>> {

	private final TransformationType m_transformation;

	public StackRegOp(final TransformationType transformation) {
		m_transformation = transformation;
	}

	@Override
	public Img<T> compute(final ImgPlus<T> in, final Img<T> out) {
		final IJPlugin plugin = new IJPlugin(new StackReg_(m_transformation),
				"transformation=[Rigid Body]");

		plugin.runOn(in, out, in.firstElement().createVariable());

		return out;
	}

	@Override
	public UnaryOutputOperation<ImgPlus<T>, Img<T>> copy() {
		return new StackRegOp<T>(m_transformation);
	}

	@Override
	public UnaryObjectFactory<ImgPlus<T>, Img<T>> bufferFactory() {
		return new UnaryObjectFactory<ImgPlus<T>, Img<T>>() {

			@Override
			public Img<T> instantiate(final ImgPlus<T> src) {
				final long[] dims = new long[src.numDimensions()];
				src.dimensions(dims);
				return src.factory().create(dims,
						src.firstElement().createVariable());
			}
		};
	}
}
