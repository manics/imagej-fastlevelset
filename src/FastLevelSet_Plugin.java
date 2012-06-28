import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;


/**
 * FastLevelSet_Plugin
 *
 * Fast level-set segmentation using the algorithm from
 * Yonggang Shi and William Karl 2008, A Real-Time Algorithm for the
 * Approximation of Level-Set-Based Curve Evolution, IEEE Image Processing.
 *
 * ImageJ seems to require at least one underscore in the main plugin class
 */
public class FastLevelSet_Plugin implements PlugInFilter {

	protected ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G + DOES_16 + DOES_32 + DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		int[] sum;
		// takes pixels of one slice
		byte[] pixels;
		int dimension = ip.getWidth()*ip.getHeight();
		sum = new int[dimension];
		// get the pixels of each slice in the stack
		for (int i=1;i<=stack.getSize();i++) {
			pixels = (byte[]) stack.getPixels(i);
			// add the value of each pixel an the corresponding position of the sum array
			for (int j=0;j<dimension;j++) {
				sum[j]+=0xff & pixels[j];
			}
		}
		byte[] average = new byte[dimension];
		// divide each entry by the number of slices
		for (int j=0;j<dimension;j++) {
			average[j] = (byte) ((sum[j]/stack.getSize()) & 0xff);
		}
		/*
		// add the resulting image as new slice
		stack.addSlice("Average",average);
		imp.setSlice(stack.getSize());
		*/

		ImagePlus result = imp.createImagePlus();
		ImageProcessor resim = new ByteProcessor(ip.getWidth(), ip.getHeight(),
												 average);
		//result.setProcessor(resim);
		BinaryProcessor seg;
		try {
			seg = levelset(null, ip, null,
						   SpeedFieldFactory.SFMethod.CHAN_VESE);
		}
		catch (Error e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
			throw e;
		}

		result.setProcessor(seg);
		result.show();
	}

	/**
	 * Create an initialisation by labelling pixels with intensity greater than
	 * the mean as foreground
	 */
	protected BinaryProcessor initFromMean() {
		ImageStatistics stats = imp.getStatistics(ij.measure.Measurements.MEAN);
		ImageProcessor bin = imp.getProcessor().duplicate();
		bin.threshold((int)stats.mean);
		return new BinaryProcessor(new ByteProcessor(bin, false));
	}

	protected FastLevelSet.Parameters defaultParams() {
		FastLevelSet.Parameters params = new FastLevelSet.Parameters();
		params.speedIterations = 5;
        params.smoothIterations = 2;
        params.maxIterations = 10;
        params.gaussWidth = 7;
        params.gaussSigma = 3;
		return params;
	}

	protected BinaryProcessor levelset(
		FastLevelSet.Parameters params, ImageProcessor im,
		BinaryProcessor init, SpeedFieldFactory.SFMethod sf) {
		if (params == null) {
			params = defaultParams();
		}
		assert im != null;
		if (init == null) {
			init = initFromMean();
		}
		//SpeedFieldFactory.SFMethod.CHAN_VESE
		SpeedField speed = SpeedFieldFactory.create(sf, im, init);
		FastLevelSet fls = new FastLevelSet(params, im, init, speed);

		boolean b = fls.segment();
		if (!b) {
			IJ.error("Segmentation failed");
			return null;
		}
		return fls.getSegmentation();
	}
}

