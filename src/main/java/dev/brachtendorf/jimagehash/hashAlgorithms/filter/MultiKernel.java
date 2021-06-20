package dev.brachtendorf.jimagehash.hashAlgorithms.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wrapper class to apply multiple kernels in a consecutive manner.
 * 
 * <p>
 * Be aware that intermediary calculations are performed on an double precision
 * and only the resulting value will be casted to the lesser precision type if
 * applicable.
 * 
 * @author Kilian
 * @since 2.0.0
 */
public class MultiKernel extends Kernel {

	private static final long serialVersionUID = -95494777151267950L;

	/** The kernels to apply back to back */
	protected List<Kernel> kernels = new ArrayList<>();

	/**
	 * Create a multi kernel from the given kernels
	 * 
	 * @param kernels the kernels which will be chained
	 */
	@SuppressWarnings("deprecation")
	public MultiKernel(Kernel... kernels) {
		super(EdgeHandlingStrategy.EXPAND);
		this.kernels.addAll(Arrays.asList(kernels));
	}

	/**
	 * Create a multi kernel with the given kernel maks. The kernels will use the
	 * default
	 * {@link dev.brachtendorf.jimagehash.hashAlgorithms.filter.Kernel.EdgeHandlingStrategy#EXPAND
	 * EdgeHandlingStrategy#EXPAND}
	 * 
	 * @param kernelMasks used to construct the kernels
	 */
	@SuppressWarnings("deprecation")
	public MultiKernel(double[][]... kernelMasks) {
		super(EdgeHandlingStrategy.EXPAND);
		for (double[][] mask : kernelMasks) {
			this.kernels.add(new Kernel(mask));
		}
	}

	@SuppressWarnings("deprecation")
	public MultiKernel(EdgeHandlingStrategy[] edgeHandlingStrategies, double[][]... kernelMasks) {
		super(EdgeHandlingStrategy.EXPAND);

		if (edgeHandlingStrategies.length != kernelMasks.length) {
			throw new IllegalArgumentException("Not the same number of edge strategies and kernel masks supplied");
		}
		for (int i = 0; i < kernelMasks.length; i++) {
			this.kernels.add(new Kernel(kernelMasks[i], edgeHandlingStrategies[i]));
		}
	}

	@Override
	public double[][] apply(int[][] input) {
		int width = input.length;
		int height = input[0].length;

		double[][] result = new double[width][height];

		// First pass
		for (int i = 0; i < kernels.size(); i++) {

			if (i == 0) {
				result = kernels.get(0).apply(input);
			} else {
				result = kernels.get(i).apply(result);
			}
		}
		return result;

	}

	/**
	 * Apply the kernel to the 2d array with each value casted to a int value.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	@Override
	public int[][] applyInt(int[][] input) {
		int width = input.length;
		int height = input[0].length;

		double[][] intermmed = new double[width][height];
		int[][] result = new int[width][height];

		// First pass
		for (int i = 0; i < kernels.size(); i++) {
			if (i == 0) {
				intermmed = kernels.get(0).apply(input);
			} else if (i < kernels.size() - 1) {
				intermmed = kernels.get(i).apply(intermmed);
			} else {
				result = kernels.get(i).applyInt(intermmed);
			}
		}
		return result;
	}

	/**
	 * Apply the kernel to the 2d array.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	@Override
	public double[][] apply(double[][] input) {
		int width = input.length;
		int height = input[0].length;

		double[][] result = new double[width][height];

		// First pass
		for (int i = 0; i < kernels.size(); i++) {

			if (i == 0) {
				result = kernels.get(0).apply(input);
			} else {
				result = kernels.get(i).apply(result);
			}
		}
		return result;
	}

	/**
	 * Apply the kernel to the 2d array. If the desired output is a byte[][] array
	 * refer to {@link #applyByte(byte[][])}.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	@Override
	public double[][] apply(byte[][] input) {
		int width = input.length;
		int height = input[0].length;

		double[][] result = new double[width][height];

		// First pass
		for (int i = 0; i < kernels.size(); i++) {
			if (i == 0) {
				result = kernels.get(0).apply(input);
			} else {
				result = kernels.get(i).apply(result);
			}
		}
		return result;
	}

	/**
	 * Apply the kernel to the 2d array with each value casted to a byte value.
	 * 
	 * @param input the input array to apply the kernel on
	 * @return a new array created by the kernel
	 */
	@Override
	public byte[][] applyByte(byte[][] input) {
		int width = input.length;
		int height = input[0].length;

		byte[][] result = new byte[width][height];
		double[][] intermmed = new double[width][height];
		// First pass
		for (int i = 0; i < kernels.size(); i++) {
			if (i == 0) {
				intermmed = kernels.get(0).apply(input);
			} else if (i < kernels.size() - 1) {
				intermmed = kernels.get(i).apply(intermmed);
			} else {
				result = kernels.get(i).applyByte(intermmed);
			}
		}
		return result;
	}

	@Override
	public String toString() {
		return "MultiKernel [kernels=" + kernels + "]";
	}

}
