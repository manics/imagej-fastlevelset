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

	/**
	 * The image to be segmented
	 */
	protected ImagePlus imp;

	/**
	 * The window for displaying intermediate level set results
	 */
	gui.LevelSetListDisplay lsDisplay = null;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G + DOES_16 + DOES_32;
	}

	/**
	 * Additional plugin parameters
	 */
	protected class AdditionalParameters {
		/**
		 * The speed field method
		 */
		public String sfmethod;

		/**
		 * Should each iteration of the level set be plotted?
		 */
		public boolean plotProgress;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		ImageStack segstack = new ImageStack(
			stack.getWidth(), stack.getHeight());
		ImageStack tmpstack = new ImageStack(
			stack.getWidth(), stack.getHeight());


		FastLevelSet.Parameters lsparams = defaultLevelSetParams();
		HybridSpeedField.Parameters hsfparams = defaultHybridParams();
		AdditionalParameters addparams = new AdditionalParameters();

		if (!getUserParameters(lsparams, hsfparams, addparams)) {
			IJ.log("Plugin cancelled");
			return;
		}

		try {
			// stack.getProcessor(i) uses 1-based indexing
			int stackSize = stack.getSize();
			for (int i = 1; i <= stackSize; ++i) {
				IJ.log("Processing slice " + i);
				IJ.showStatus("Processing slice " + i + "/" + stackSize);

				ImageProcessor im = stack.getProcessor(i);
				BinaryProcessor init = initFromMean(im, false);
				BinaryProcessor seg = levelset(lsparams, im, init, hsfparams,
											   addparams);

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
	 * @lsp The FastLevelSet parameters object which must hold default
	 *      values, will be updated with any changed parameters
	 * @hsfp The HybridSpeedField parameters object which must hold default
	 *       values, will be updated with any changed parameters
	 * @adp Additional algorithm/plugin parameters, no defaults required
	 * @return true if the user clicked OK, false if cancelled
	 */
	protected boolean getUserParameters(FastLevelSet.Parameters lsp,
										HybridSpeedField.Parameters hsfp,
										AdditionalParameters adp) {
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
		gd.addNumericField("Iterations", lsp.maxIterations, 0);
		gd.addNumericField("Speed_sub-iterations", lsp.speedIterations, 0);
		gd.addNumericField("Smooth_sub-iterations", lsp.smoothIterations, 0);

		// I've never had to change these two
		//gd.addNumericField("Smoothing_kernel_width", lsp.gaussWidth, 0);
		//gd.addNumericField("Smoothing_kernel_sigma", lsp.gaussSigma, 2);

		gd.addCheckbox("Display_progress (may be slower)", true);

		List<String> sfmethods = new LinkedList<String>();
		for (SpeedFieldFactory.SfMethod e :
				 SpeedFieldFactory.SfMethod.values()) {
			sfmethods.add(e.toString());
		}

		gd.addChoice("Field_type", sfmethods.toArray(new String[0]),
					 sfmethods.get(0));

		gd.addMessage("Hybrid speed field parameters");
		gd.addNumericField("Local_radius", hsfp.neighbourhoodRadius, 0);
		//gd.addNumericField("Intensity_cut-off", hsfp.cutoffIntensity, 0);

		gd.showDialog();
		if (gd.wasCanceled()) {
			return false;
		}

		lsp.maxIterations = (int)gd.getNextNumber();
		lsp.speedIterations = (int)gd.getNextNumber();
		lsp.smoothIterations = (int)gd.getNextNumber();
		//lsp.gaussWidth = (int)gd.getNextNumber();
		//lsp.gaussSigma = gd.getNextNumber();

		adp.plotProgress = gd.getNextBoolean();

		adp.sfmethod = sfmethods.get(gd.getNextChoiceIndex());

		hsfp.neighbourhoodRadius = (int)gd.getNextNumber();

		return true;
	}

	protected class ProgressReporter implements LevelSetIterationListener {
		public void fullIteration(int full, int fullT) {
			//IJ.log("Completed iteration: " + full + "/" + fullT);
			IJ.showProgress((double)full / (double)fullT);
		}

		public void speedIteration(int full, int fullT, int speed, int speedT) {
			//IJ.log("\tCompleted speed: [" + full + "]" +
			//speed + "/" + speedT);
		}

		public void smoothIteration(int full, int fullT,
									int smooth, int smoothT) {
			//IJ.log("\tCompleted smooth: [" + full + "]" +
			//smooth + "/" + smoothT);
		}
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
	protected FastLevelSet.Parameters defaultLevelSetParams() {
		FastLevelSet.Parameters params = new FastLevelSet.Parameters();
		params.speedIterations = 5;
        params.smoothIterations = 2;
        params.maxIterations = 10;
        params.gaussWidth = 7;
        params.gaussSigma = 3;
		return params;
	}

	/**
	 * Default hybrid speed field parameters
	 * @return default parameters
	 */
	HybridSpeedField.Parameters defaultHybridParams() {
		HybridSpeedField.Parameters params = new HybridSpeedField.Parameters();
		params.neighbourhoodRadius = 16;
		params.cutoffIntensity = 0;
		return params;
	}

	/**
	 * Run the fast level set
	 * @param params The fast level set parameters
	 * @param im The image to be segmented
	 * @param init The binary initialisation
	 * @param hsfp The hybrid speed field parameters (may be null)
	 * @params addparams Additional algorithm/plugin parameters
	 * @return The binary segmentation
	 */
	protected BinaryProcessor levelset(FastLevelSet.Parameters params,
									   ImageProcessor im, BinaryProcessor init,
									   HybridSpeedField.Parameters hsfp,
									   AdditionalParameters addparams) {
		assert params != null;
		assert im != null;
		assert init != null;
		assert addparams != null;

		SpeedField speed = SpeedFieldFactory.create(addparams.sfmethod,
													im, init, hsfp);

		FastLevelSet fls = new FastLevelSet(params, im, init, speed);
		fls.addIterationListener(new ProgressReporter());

		if (addparams.plotProgress) {
			if (lsDisplay == null) {
				lsDisplay = new gui.LevelSetListDisplay(im, true);
			}
			else {
				lsDisplay.setBackground(im);
			}

			fls.addListListener(lsDisplay);
		}

		boolean b = fls.segment();

		if (!b) {
			IJ.error("Segmentation failed");
			return null;
		}
		return fls.getSegmentation();
	}
}

