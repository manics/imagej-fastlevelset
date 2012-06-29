import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import levelset.*;

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
		return DOES_8G + DOES_16 + DOES_32;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		ImageStack segstack = new ImageStack(
			stack.getWidth(), stack.getHeight());
		ImageStack tmpstack = new ImageStack(
			stack.getWidth(), stack.getHeight());


		FastLevelSet.Parameters params = defaultParams();
		if (!getUserParameters(params)) {
			IJ.log("Plugin cancelled");
			return;
		}
		SpeedFieldFactory.SFMethod sf =
			SpeedFieldFactory.SFMethod.CHAN_VESE;

		try {
			// stack.getProcessor(i) uses 1-based indexing
			for (int i = 1; i <= stack.getSize(); ++i) {
				IJ.log("Processing slice " + i);

				ImageProcessor im = stack.getProcessor(i);
				BinaryProcessor init = initFromMean(im, false);
				BinaryProcessor seg = levelset(params, im, init, sf);

				segstack.addSlice(seg);
				tmpstack.addSlice(init);
			}
		}
		catch (Error e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
			throw e;
		}

		ImagePlus result = new ImagePlus("Segmentation", segstack);
		result.show();
		new ImagePlus("Initialisation", tmpstack).show();
	}

	/**
	 * Show a dialog to request user parameters for the segmentation
	 * algorithm
	 * @param The parameters object which must hold default values, and whose
	 *        fields will be changed to any user selected parameters
	 * @return true if the user clicked OK, false if cancelled
	 */
	protected boolean getUserParameters(FastLevelSet.Parameters params) {
		GenericDialog gd = new GenericDialog("Fast level set settings");
		gd.addMessage(
		/*123456789012345678901234567890123456789012345678901234567890*/
		 "The level set works by starting from the initialisation and\n" +
		 "iteratively growing/shrinking the boundary.\n" +
		 "If the initialisation is quite far from the actual boundary\n" +
		 "then increase 'Iterations' and/or 'Speed sub iterations'.\n" +
		 "If the boundary is too jagged then increase 'Smooth sub-\n" +
		 "iterations' and/or decrease 'Speed sub-iterations' to vary\n" +
		 "the smoothness of the segmentation boundary, and vice-versa.\n" +
		 "The greater the number of iterations the longer this\n" +
		 "algorithm will take to run.");
		gd.addNumericField("Iterations", params.maxIterations, 0);
		gd.addNumericField("Speed_sub-iterations", params.speedIterations, 0);
		gd.addNumericField("Smooth_sub-iterations", params.smoothIterations, 0);

		// I've never had to change these two
		//gd.addNumericField("Smoothing_kernel_width", params.gaussWidth, 0);
		//gd.addNumericField("Smoothing_kernel_sigma", params.gaussSigma, 2);

		gd.addCheckbox("Display_progress", false);

		String choices[] = {"Chan Vese", "Hybrid"};
		gd.addChoice("Field_type", choices, choices[0]);

		gd.addMessage("Hybrid speed field parameters");
		gd.addNumericField("Local radius", 16, 0);

		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		params.maxIterations = (int)gd.getNextNumber();
		params.speedIterations = (int)gd.getNextNumber();
		params.smoothIterations = (int)gd.getNextNumber();
		//params.gaussWidth = (int)gd.getNextNumber();
		//params.gaussSigma = gd.getNextNumber();

		boolean plotProgress = gd.getNextBoolean();

		int field = gd.getNextChoiceIndex();
		if (field != 0) {
			IJ.error("Invalid choice");
			return false;
		}

		int cr = (int)gd.getNextNumber();

		return true;
	}

	/**
	 * Create an initialisation by labelling pixels with intensity greater than
	 * the mean as foreground
	 * @param im The image (single slice)
	 * @param global If true use the mean calculated over all slices, if false
	 *        calculate the mean for this slice only
	 * @return The binary initialisation image
	 */
	protected BinaryProcessor initFromMean(ImageProcessor im, boolean global) {
		ImageStatistics stats;
		if (global) {
			stats = imp.getStatistics(ij.measure.Measurements.MEAN);
		}
		else {
			stats = ImageStatistics.getStatistics(
				im, ij.measure.Measurements.MEAN, imp.getCalibration());
		}

		ImageProcessor bin = im.duplicate();
		bin.threshold((int)stats.mean);
		return new BinaryProcessor(new ByteProcessor(bin, false));
	}

	/**
	 * Default fast level set parameters
	 * @return default parameters
	 */
	protected FastLevelSet.Parameters defaultParams() {
		FastLevelSet.Parameters params = new FastLevelSet.Parameters();
		params.speedIterations = 5;
        params.smoothIterations = 2;
        params.maxIterations = 10;
        params.gaussWidth = 7;
        params.gaussSigma = 3;
		return params;
	}

	/**
	 * Run the fast level set
	 * @param params The fast level set parameters
	 * @param im The image to be segmented
	 * @param init The binary initialisation
	 * @param sf The method to use for calculating the speed field
	 * @return The binary segmentation
	 */
	protected BinaryProcessor levelset(
		FastLevelSet.Parameters params, ImageProcessor im,
		BinaryProcessor init, SpeedFieldFactory.SFMethod sf) {

		assert params != null;
		assert im != null;
		assert init != null;
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

