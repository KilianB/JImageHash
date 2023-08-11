package interpolator.utils.sorter;

import dev.brachtendorf.graphics.FastPixel;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashBuilder;
import dev.brachtendorf.jimagehash.hashAlgorithms.HashingAlgorithm;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

public class ColorMomentsHash extends HashingAlgorithm {
    private static final long serialVersionUID = -5234612717498362659L;

    /**
     * The height and width of the scaled instance used to compute the hash
     */
    protected int height, width;

    public ColorMomentsHash(int bitResolution) {
        super(bitResolution);
        /*
         * Figure out how big our resized image has to be in order to create a hash with
         * approximately bit resolution bits while trying to stay as squared as possible
         * to not introduce bias via stretching or shrinking the image asymmetrically.
         */
        computeDimension(bitResolution);
    }

    @Override
    protected BigInteger hash(BufferedImage image, HashBuilder hash) {
        FastPixel fp = createPixelAccessor(image, width, height);

        int[][]    hue = getHue(fp);
        double[][] sat = getSaturation(fp);
        int[][]    val = getValue(fp);

        double[] moments = new double[9];
        moments[0] = mean(hue);
        moments[1] = mean(sat);
        moments[2] = mean(val);
        moments[3] = standardDeviation(hue);
        moments[4] = standardDeviation(sat);
        moments[5] = standardDeviation(val);
        moments[6] = skewness(hue);
        moments[7] = skewness(sat);
        moments[8] = skewness(val);

        for (int i = 0; i < moments.length; i++) {
            double moment = moments[i];
            // Weight mean 4x
            if (i < 3) {
                IntStream.range(0, 3).forEach(j -> computeHash(hash, moment));
            }
            computeHash(hash, moment);
        }
        return hash.toBigInteger();
    }

    public void computeHash(HashBuilder hash, double moment) {
        String binary = Long.toBinaryString(Double.doubleToRawLongBits(moment));
        for (char c : binary.toCharArray()) {
            if (c == '0') {
                hash.prependZero();
            } else {
                hash.prependOne();
            }
        }
    }

    /**
     * Compute the dimension for the resize operation. We want to get to close to a
     * quadratic images as possible to counteract scaling bias.
     *
     * @param bitResolution the desired resolution
     */
    private void computeDimension(int bitResolution) {

        // Allow for slightly non symmetry to get closer to the true bit resolution
        int dimension = (int) Math.round(Math.sqrt(bitResolution));

        // Lets allow for a +1 or -1 asymmetry and find the most fitting value
        int normalBound = (dimension * dimension);
        int higherBound = (dimension * (dimension + 1));

        this.height = dimension;
        this.width = dimension;
        if (normalBound < bitResolution || (normalBound - bitResolution) > (higherBound - bitResolution)) {
            this.width++;
        }
    }

    @Override
    protected int precomputeAlgoId() {
        /*
         * String and int hashes stays consistent throughout different JVM invocations.
         * Algorithm changed between version 1.x.x and 2.x.x ensure algorithms are
         * flagged as incompatible. Dimension are what makes average hashes unique
         * therefore, even
         */
        return Objects.hash("com.github.kilianB.hashAlgorithms."+getClass().getSimpleName(), height, width);
    }

    public int[][] getHue(FastPixel fp) {
        int[][] blueArr = fp.getBlue();
        int[][] greenArr = fp.getGreen();
        int[][] redArr = fp.getRed();

        int[][] hueArr = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int blue = blueArr[x][y];
                int green = greenArr[x][y];
                int red = redArr[x][y];

                int min = Math.min(blue, Math.min(green, red));
                int max = Math.max(blue, Math.max(green, red));

                if (max == min) {
                    hueArr[x][y] = 0;
                    continue;
                }

                double range = max - min;

                double h;
                if (red == max) {
                    h = 60 * ((green - blue) / range);
                } else if (green == max) {
                    h = 60 * (2 + (blue - red) / range);
                } else {
                    h = 60 * (4 + (red - green) / range);
                }

                int hue = (int) Math.round(h);

                if (hue < 0)
                    hue += 360;

                hueArr[x][y] = hue;
            }
        }

        return hueArr;
    }

    public double[][] getSaturation(FastPixel fp) {
        int[][] blueArr = fp.getBlue();
        int[][] greenArr = fp.getGreen();
        int[][] redArr = fp.getRed();

        double[][] satArr = new double[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int blue = blueArr[x][y];
                int green = greenArr[x][y];
                int red = redArr[x][y];

                int max = Math.max(blue, Math.max(green, red));
                if (max == 0) {
                    satArr[x][y] = 0;
                    continue;
                }
                int min = Math.min(blue, Math.min(green, red));

                satArr[x][y] = ((max - min) / (double) max);
            }
        }

        return satArr;
    }

    public int[][] getValue(FastPixel fp) {
        int[][] blueArr = fp.getBlue();
        int[][] greenArr = fp.getGreen();
        int[][] redArr = fp.getRed();

        int[][] valArr = new int[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int blue = blueArr[x][y];
                int green = greenArr[x][y];
                int red = redArr[x][y];

                int max = Math.max(blue, Math.max(green, red));

                valArr[x][y] = max;
            }
        }

        return valArr;
    }

    public static double skewness(final double[][] arr) {
        double[] flattened = Arrays.stream(arr)
                .flatMapToDouble(Arrays::stream)
                .toArray();
        int length = flattened.length;

        // Initialize the skewness
        double skew = Double.NaN;
        // Get the mean and the standard deviation
        double m = Arrays.stream(flattened).average().getAsDouble();

        // Calc the std, this is implemented here instead
        // of using the standardDeviation method eliminate
        // a duplicate pass to get the mean
        double accum = 0.0;
        double accum2 = 0.0;
        for (int i = 0; i < length; i++) {
            final double d = flattened[i] - m;
            accum  += d * d;
            accum2 += d;
        }
        final double variance = (accum - (accum2 * accum2 / length)) / (length - 1);

        double accum3 = 0.0;
        for (int i = 0; i < length; i++) {
            final double d = flattened[i] - m;
            accum3 += d * d * d;
        }
        accum3 /= variance * Math.sqrt(variance);

        // Get N
        double n0 = length;

        // Calculate skewness
        return (n0 / ((n0 - 1) * (n0 - 2))) * accum3;
    }

    public static double skewness(final int[][] arr) {
        int[] flattened = Arrays.stream(arr)
                .flatMapToInt(Arrays::stream)
                .toArray();
        int length = flattened.length;

        // Initialize the skewness
        double skew = Double.NaN;
        // Get the mean and the standard deviation
        double m = Arrays.stream(flattened).average().getAsDouble();

        // Calc the std, this is implemented here instead
        // of using the standardDeviation method eliminate
        // a duplicate pass to get the mean
        double accum = 0.0;
        double accum2 = 0.0;
        for (int i = 0; i < length; i++) {
            final double d = flattened[i] - m;
            accum  += d * d;
            accum2 += d;
        }
        final double variance = (accum - (accum2 * accum2 / length)) / (length - 1);

        double accum3 = 0.0;
        for (int i = 0; i < length; i++) {
            final double d = flattened[i] - m;
            accum3 += d * d * d;
        }
        accum3 /= variance * Math.sqrt(variance);

        // Get N
        double n0 = length;

        // Calculate skewness
        return (n0 / ((n0 - 1) * (n0 - 2))) * accum3;
    }

    public double standardDeviation(double[][] arr) {
        double sum = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                sum += arr[i][j];
            }
        }
        double mean = sum / (width * height);
        double sd = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                sd += Math.pow(arr[i][j] - mean, 2);
            }
        }
        return Math.sqrt(sd / (width * height));
    }

    public double standardDeviation(int[][] arr) {
        int sum = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                sum += arr[i][j];
            }
        }
        double mean = sum / (width * height);
        double sd = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                sd += Math.pow(arr[i][j] - mean, 2);
            }
        }
        return Math.sqrt(sd / (width * height));
    }

    public double mean(double[][] arr) {
        return Arrays.stream(arr).flatMapToDouble(Arrays::stream).average().getAsDouble();
    }

    public double mean(int[][] arr) {
        return Arrays.stream(arr).flatMapToInt(Arrays::stream).average().getAsDouble();
    }
}
