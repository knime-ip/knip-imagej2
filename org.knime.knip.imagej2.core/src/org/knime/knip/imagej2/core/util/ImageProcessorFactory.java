package org.knime.knip.imagej2.core.util;

import ij.process.ImageProcessor;
import net.imglib2.type.Type;

/**
 * @author Christian Dietz
 */
public interface ImageProcessorFactory {

    /**
     * @param width
     * @param height
     * @param type
     * @return {@link ImageProcessor} given w, height and type
     */
    <T extends Type<T>> ImageProcessor createProcessor(final int width, int height, final T type);
}