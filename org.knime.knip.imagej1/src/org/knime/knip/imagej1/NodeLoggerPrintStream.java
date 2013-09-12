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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;

import org.knime.core.node.NodeLogger;

/**
 * Redirects this print streams input to the node logger.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 */
public class NodeLoggerPrintStream extends PrintStream {

	private final NodeLogger m_logger;

	private final NodeLogger.LEVEL m_level;

	/**
	 * @param logger
	 * @param level
	 */
	public NodeLoggerPrintStream(final NodeLogger logger,
			final NodeLogger.LEVEL level) {
		super(System.out);
		m_logger = logger;
		m_level = level;
	}

	private void log(final String mess) {
		switch (m_level) {
		case INFO:
			m_logger.info(mess);
			break;
		case WARN:
			m_logger.warn(mess);
			break;
		case ERROR:
			m_logger.error(mess);
			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintStream append(final char c) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintStream append(final CharSequence csq, final int start,
			final int end) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintStream append(final CharSequence csq) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean checkError() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintStream format(final Locale l, final String format,
			final Object... args) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintStream format(final String format, final Object... args) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final boolean x) {
		log(Boolean.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final char x) {
		log(Character.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final char[] x) {
		log(new String(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final double x) {
		log(Double.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final float x) {
		log(Float.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final int x) {
		log(Integer.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final long x) {
		log(Long.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final Object x) {
		log(x.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void print(final String x) {
		log(x);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintStream printf(final Locale l, final String format,
			final Object... args) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrintStream printf(final String format, final Object... args) {
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println() {
		//
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final boolean x) {
		log(Boolean.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final char x) {
		log(Character.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final char[] x) {
		log(new String(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final double x) {
		log(Double.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final float x) {
		log(Float.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final int x) {
		log(Integer.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final long x) {
		log(Long.toString(x));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final Object x) {
		log(x.toString());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void println(final String x) {
		m_logger.error(x);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final byte[] buf, final int off, final int len) {
		log(new String(Arrays.copyOfRange(buf, off, len)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(final int b) {
		log(Integer.toString(b));
	}

}
