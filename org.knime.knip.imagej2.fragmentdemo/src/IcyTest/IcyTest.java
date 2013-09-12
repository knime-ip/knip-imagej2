package IcyTest;

import imagej.command.Command;

import java.util.Arrays;

import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.ItemIO;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "IcyTest")}, description = "spot", headless = true, type = Command.class)
public class IcyTest<T extends RealType<T>> implements Command {

        @Parameter(type = ItemIO.INPUT)
        private Img<T> inputImg;

        @Parameter(type = ItemIO.OUTPUT)
        private Img<BitType> outputImg;

        @Parameter(type = ItemIO.OUTPUT)
        private Img<FloatType> outputImg2;

        @Parameter(type = ItemIO.OUTPUT)
        private Img<FloatType> out2;

        @Parameter(type = ItemIO.INPUT)
        private boolean detectNegative;

        private void createCoefficientImage(float[][] coefficients, int sizeX, int sizeY) {
                ArrayImgFactory<FloatType> fac = new ArrayImgFactory<FloatType>();
                out2 = fac.create(new int[] {sizeX, sizeY, coefficients.length}, new FloatType());
                RandomAccess<FloatType> ra = out2.randomAccess();

                for (int i = 0; i < coefficients.length; i++) {
                        for (int j = 0; j < coefficients[0].length; j++) {

                                int x = j % sizeX;
                                int y = j / sizeX;

                                ra.setPosition(new int[] {x, y, i});
                                ra.get().setReal(coefficients[i][j]);
                        }
                }
        }

        public void run() {

                int MAX_ENABLED_NUM_SCALE = 4;

                //brute force it into a 1d array

                int sizeX = (int) inputImg.dimension(0);
                int sizeY = (int) inputImg.dimension(1);

                float dataIn[] = new float[sizeX * sizeY];

                Cursor<T> curso = inputImg.cursor();

                int k = 0;
                while (curso.hasNext()) {
                        dataIn[k] = curso.next().getRealFloat();
                        k++;
                }

                //apply implementation from UDWTWaveletCore

                //decompose the image
                B3SplineUDWT waveletTransform = new B3SplineUDWT();

                float[][] scales = null;

                try {
                        scales = waveletTransform.b3WaveletScales2D(dataIn, sizeX, sizeY, MAX_ENABLED_NUM_SCALE);
                } catch (WaveletConfigException e1) {
                        e1.printStackTrace();
                }

                float[][] coefficients = waveletTransform.b3WaveletCoefficients2D(scales, dataIn, MAX_ENABLED_NUM_SCALE, sizeX * sizeY);

                createCoefficientImage(coefficients, sizeX, sizeY);

                // Apply threshold to coefficients but not last one ( residual )
                //...        

                filterPaperlike(coefficients.clone(), new boolean[] {false, false, false, true, false}, sizeX, sizeY);

                filterICYlike(coefficients.clone(), new boolean[] {false, false, false, true, false}, sizeX, sizeY);

        }

        private void filterPaperlike(float[][] coefficients, boolean[] enabledScales, int width, int height) {

                //filter
                for (int i = 0; i < coefficients.length; i++) {
                        //float mad =  getMad(coefficients[i]);
                        float mad = getMeanAverageDistance(coefficients[i], null);
                        float thresh = 3.0f * mad / 0.67f;

                        for (int j = 0; j < coefficients[i].length; j++) {
                                if (enabledScales[i]) {
                                        if (coefficients[i][j] < thresh) {
                                                coefficients[i][j] = 0;
                                        }
                                }
                        }
                }
                //combine

                outputImg2 = new ArrayImgFactory<FloatType>().create(new int[] {width, height}, new FloatType());
                RandomAccess<FloatType> ra = outputImg2.randomAccess();

                for (int i = 0; i < coefficients[0].length; i++) {

                        float val = 1;

                        //do not include the residual
                        for (int j = 0; j < coefficients.length - 1; j++) {
                                if (enabledScales[j]) {
                                        val *= coefficients[j][i];
                                }
                        }

                        int x = i % width;
                        int y = i / width;

                        ra.setPosition(new int[] {x, y});
                        ra.get().setReal(val);
                }
        }

        private void filterICYlike(float[][] coefficients, boolean[] enabledScales, int width, int height) {

                for (int i = 0; i < 20; i++) {
                        System.out.println(createIcyLambda(i, width, height));
                }

                //filter
                for (int i = 0; i < coefficients.length; i++) {
                        if (enabledScales[i]) {
                                double mad = getMeanAverageDistance(coefficients[i], null);
                                double coeffThr = createIcyLambda(i, width, height) * mad;

                                for (int j = 0; j < coefficients[i].length; j++) {
                                        if (coefficients[i][j] < coeffThr) {
                                                coefficients[i][j] = 0;
                                        }
                                }
                        }
                }

                //combine

                outputImg = new ArrayImgFactory<BitType>().create(new int[] {width, height}, new BitType());
                RandomAccess<BitType> ra = outputImg.randomAccess();

                for (int i = 0; i < coefficients[0].length; i++) {
                        boolean notNull = true;

                        for (int j = 0; j < coefficients.length; j++) {
                                if (enabledScales[j] && coefficients[j][i] == 0) {
                                        notNull = false;
                                }
                        }

                        if (notNull) {
                                int x = i % width;
                                int y = i / width;

                                ra.setPosition(new int[] {x, y});
                                ra.get().setOne();
                        }
                }

        }

        private double createIcyLambda(int depth, int width, int height) {
                return Math.sqrt(2 * Math.log(width * height / (1 << (2 * (depth + 1)))));
        }

        //expensive mean calculations
        public float getMad(float[] data) {
                float[] d2 = data.clone();
                Arrays.sort(d2);

                float mean = d2[d2.length / 2];

                float[] meansDev = new float[d2.length];

                for (int i = 0; i < d2.length; i++) {
                        meansDev[i] = Math.abs(d2[i] - mean);
                }

                Arrays.sort(meansDev);

                return meansDev[meansDev.length / 2];
        }

        public float getMeanAverageDistance(float[] data, boolean mask[]) {
                float mean = getMean(data, mask);
                float a = 0;
                float s;

                if (mask == null) {
                        for (int i = 0; i < data.length; i++) {
                                s = data[i] - mean;
                                a = a + Math.abs(s);
                        }

                        if (data.length > 0)
                                return a / data.length;
                } else {
                        float nbValue = 0;
                        for (int i = 0; i < data.length; i++) {
                                if (mask[i]) {
                                        s = data[i] - mean;
                                        a = a + Math.abs(s);
                                        nbValue++;
                                }
                        }

                        if (nbValue > 0)
                                return a / nbValue;

                }

                return 0;
        }

        public float getMean(float[] data, boolean mask[]) {
                float mean = 0;
                float sum = 0;

                if (mask == null) {
                        for (int i = 0; i < data.length; i++) {
                                sum += data[i];
                        }
                        if (data.length > 0)
                                mean = sum / data.length;
                } else {
                        float nbValue = 0;
                        for (int i = 0; i < data.length; i++) {
                                if (mask[i]) {
                                        sum += data[i];
                                        nbValue++;
                                }
                        }
                        if (nbValue > 0)
                                mean = sum / nbValue;
                }

                return mean;
        }
}