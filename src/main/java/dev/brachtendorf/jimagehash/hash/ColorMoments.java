package dev.brachtendorf.jimagehash.hash;

import java.util.Objects;

/**
 * A color moment is an aggregation of mean, standard deviation, and skewness moments.
 */
public final class ColorMoments {
    private final double[] meanMoments;
    private final double[] stdDevMoments;
    private final double[] skewnessMoments;

    public ColorMoments(
		double[] meanMoments,
		double[] stdDevMoments,
		double[] skewnessMoments
	) {
        this.meanMoments = meanMoments;
        this.stdDevMoments = stdDevMoments;
        this.skewnessMoments = skewnessMoments;
    }

	public double distance(ColorMoments other) {
		double distance = 0;
		for (int i = 0; i < 3; i++) {
			double meanDiff = Math.abs(meanMoments[i] - other.meanMoments[i]);
			double stdDevDiff = Math.abs(stdDevMoments[i] - other.stdDevMoments[i]);
			double skewnessDiff = Math.abs(skewnessMoments[i] - other.skewnessMoments[i]);
			distance += meanDiff + stdDevDiff + skewnessDiff;
		}
		return distance;
	}

    public double[] meanMoments() {
        return meanMoments;
    }

    public double[] stdDevMoments() {
        return stdDevMoments;
    }

    public double[] skewnessMoments() {
        return skewnessMoments;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ColorMoments) obj;
        return Objects.equals(this.meanMoments, that.meanMoments) &&
                Objects.equals(this.stdDevMoments, that.stdDevMoments) &&
                Objects.equals(this.skewnessMoments, that.skewnessMoments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meanMoments, stdDevMoments, skewnessMoments);
    }

    @Override
    public String toString() {
        return "MomentHash[" +
                "meanMoments=" + meanMoments + ", " +
                "stdDevMoments=" + stdDevMoments + ", " +
                "skewnessMoments=" + skewnessMoments + ']';
    }
}