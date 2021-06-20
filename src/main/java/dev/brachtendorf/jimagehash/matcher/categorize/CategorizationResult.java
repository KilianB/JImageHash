package dev.brachtendorf.jimagehash.matcher.categorize;

/**
 * A categorization result describes the membership to a cluster. It contains
 * the identifier of the category this image was matched to as well as a quality
 * measurement.
 * 
 * <p>
 * The quality metric is depended on the the actual implementation of the
 * categorizer.
 * 
 * <p>
 * Categorization results can be nested in case that multiple categorizers are
 * chained together. The first result will point to the categorization result of
 * the main matcher. The next to the first nested categorizer.
 * 
 * <p>
 * Categorization results are only valid at the time they are created and may
 * represent an invalid state as soon as the categorizer who produces this stage
 * was changed.
 * 
 * @author Kilian
 *
 * @since 3.0.0
 * @see dev.brachtendorf.jimagehash.matcher.categorize.CategoricalImageMatcher
 */
public class CategorizationResult {

	protected int category;
	protected double qualityMeasurement;

	//TODO not yet implemented
//	/** In case of nested matchers this will point to the next result */
	protected CategorizationResult subResult = null;

	public CategorizationResult(int category, double qualityMeasurement) {
		this.category = category;
		this.qualityMeasurement = qualityMeasurement;
	}

	/**
	 * Get the category of this result object. The category uniquely identifies the
	 * cluster this image was matched to.
	 * 
	 * @return the category
	 */
	public int getCategory() {
		return category;
	}

	
//	public List<CategorizationResult> getAllCategories() {
//		List<CategorizationResult> cRes = new ArrayList<>();
//		CategorizationResult temp = subResult;
//		do {
//			cRes.add(temp);
//		} while ((temp = temp.subResult) != null);
//		return cRes;
//	}

	public double getQuality() {
		return qualityMeasurement;
	}

	public void addCategory(CategorizationResult catgeorizationResult) {
		this.subResult = catgeorizationResult;
	}

	@Override
	public String toString() {
		return "CategorizationResult [category=" + category + ", qualityMeasurement=" + qualityMeasurement+  "]";
	}
	
	
}
