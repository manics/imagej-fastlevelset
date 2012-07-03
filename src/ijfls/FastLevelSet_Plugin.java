package ijfls;

import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import ijfls.levelset.*;
import ijfls.gui.LevelSetListDisplay;

import java.awt.Font;
import java.util.LinkedList;


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
	 * The initialisation image
	 */
	protected ImagePlus impInit = null;

	/**
	 * The segmentation image
	 */
	protected ImagePlus impSeg = null;

	/**
	 * The window for displaying intermediate level set results
	 */
	LevelSetListDisplay lsDisplay = null;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G + DOES_16 + DOES_32;
	}

	/**
	 * All plugin parameters
	 */
	public static class Parameters {
		/**
		 * The speed field method
		 */
		public String sfmethod;

		/**
		 * The initialisation method
		 */
		public String initMethod;

		/**
		 * Should subsequent frames be initialised from the previous
		 * segmentation (true) or indenpendently using the specified method
		 * (false)
		 */
		public boolean initFromPrevious;
		
		/**
		 * Should each iteration of the level set be plotted?
		 */
		public boolean plotProgress;
		
		/**
		 * Fast level set parameters
		 */
		public FastLevelSet.Parameters lsparams;

		/**
		 * Hybrid speed field parameters
		 */
		public HybridSpeedField.Parameters hsfparams;

		/**
		 * Set parameters to defaults
		 */
		public Parameters() {
			sfmethod = null;
			initMethod = null;
			initFromPrevious = true;
			plotProgress = true;			

			lsparams = new FastLevelSet.Parameters();
			lsparams.speedIterations = 5;
			lsparams.smoothIterations = 2;
			lsparams.maxIterations = 10;
			lsparams.gaussWidth = 7;
			lsparams.gaussSigma = 3;

			hsfparams = new HybridSpeedField.Parameters();
			hsfparams.neighbourhoodRadius = 16;
			hsfparams.cutoffIntensity = 0;
		}
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		Parameters params = new Parameters();

		if (!getUserParameters(params)) {
			IJ.log("Plugin cancelled");
			return;
		}

		try {
			int stackSize = stack.getSize();
			BinaryProcessor prevSeg = null;

			// stack.getProcessor(i) uses 1-based indexing
			for (int i = 1; i <= stackSize; ++i) {
				IJ.log("Processing slice " + i);
				IJ.showStatus("Processing slice " + i + "/" + stackSize);

				ImageProcessor im = stack.getProcessor(i);

				BinaryProcessor init;
				if (params.initFromPrevious && prevSeg != null) {
					init = prevSeg;
				}
				else {
					init = Initialiser.getInitialisation(
						imp, im, params.initMethod);
				}

				updateInitDisplay(init);

				BinaryProcessor seg = levelset(params, im, init);
				prevSeg = seg;

				updateSegDisplay(seg);
			}
		}
		catch (Error e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	private void updateInitDisplay(BinaryProcessor init) {
		if (impInit == null) {
			ImageStack s = imp.createEmptyStack();
			s.addSlice(init);
			impInit = new ImagePlus(imp.getShortTitle() + " Initialisation", s);
			impInit.show();
		}
		else {
			// None of the ImagePlus.update... methods seem to work.
			// Re-adding the stack seems to be the only way to get an update
			ImageStack s = impInit.getStack();
			s.addSlice(init);
			impInit.setStack(s);
			impInit.setSlice(s.getSize());
		}
	}

	private void updateSegDisplay(BinaryProcessor seg) {
		if (impSeg == null) {
			ImageStack s = imp.createEmptyStack();
			s.addSlice(seg);
			impSeg = new ImagePlus(imp.getShortTitle() + " Segmentation", s);
			impSeg.show();
		}
		else {
			// None of the ImagePlus.update... methods seem to work.
			// Re-adding the stack seems to be the only way to get an update
			ImageStack s = impSeg.getStack();
			s.addSlice(seg);
			impSeg.setStack(s);
			impSeg.setSlice(s.getSize());
		}
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
	protected boolean getUserParameters(Parameters params) {
		FastLevelSet.Parameters lsp = params.lsparams;
		HybridSpeedField.Parameters hsfp = params.hsfparams;

		GenericDialog gd = new GenericDialog("Fast level set settings");
		Font font = gd.getFont();
		font = font.deriveFont((float)(font.getSize2D() * 0.9));
		int charWidth = 80;

		gd.addMessage(FastLevelSet_PluginStrings.format(
						  FastLevelSet_PluginStrings.initialisation,
						  charWidth), font);

		String initMethods[] = Initialiser.getInitialisationMethods();
		gd.addChoice("Field_type", initMethods, initMethods[0]);

		gd.addCheckbox("Initialise from previous segmentation",
					   params.initFromPrevious);

		gd.addMessage(FastLevelSet_PluginStrings.format(
						  FastLevelSet_PluginStrings.levelSetParameters,
						  charWidth), font);

		gd.addNumericField("Iterations", lsp.maxIterations, 0);
		gd.addNumericField("Speed_sub-iterations", lsp.speedIterations, 0);
		gd.addNumericField("Smooth_sub-iterations", lsp.smoothIterations, 0);

		// I've never had to change these two
		//gd.addNumericField("Smoothing_kernel_width", lsp.gaussWidth, 0);
		//gd.addNumericField("Smoothing_kernel_sigma", lsp.gaussSigma, 2);

		gd.addCheckbox("Display_progress (may be slower)", params.plotProgress);

		LinkedList<String> sfmethods = new LinkedList<String>();
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

		// Retrieve the parameters

		params.initMethod = initMethods[gd.getNextChoiceIndex()];
		params.initFromPrevious = gd.getNextBoolean();

		lsp.maxIterations = (int)gd.getNextNumber();
		lsp.speedIterations = (int)gd.getNextNumber();
		lsp.smoothIterations = (int)gd.getNextNumber();
		//lsp.gaussWidth = (int)gd.getNextNumber();
		//lsp.gaussSigma = gd.getNextNumber();

		params.plotProgress = gd.getNextBoolean();

		params.sfmethod = sfmethods.get(gd.getNextChoiceIndex());

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
	 * Run the fast level set
	 * @param params The fast level set parameters
	 * @param im The image to be segmented
	 * @param init The binary initialisation
	 * @param hsfp The hybrid speed field parameters (may be null)
	 * @params addparams Additional algorithm/plugin parameters
	 * @return The binary segmentation
	 */
	protected BinaryProcessor levelset(Parameters params, ImageProcessor im,
									   BinaryProcessor init) {
		assert params != null;
		assert im != null;
		assert init != null;

		SpeedField speed = SpeedFieldFactory.create(params.sfmethod, im, init,
													params.hsfparams);

		FastLevelSet fls = new FastLevelSet(params.lsparams, im, init, speed);
		fls.addIterationListener(new ProgressReporter());

		if (params.plotProgress) {
			if (lsDisplay == null) {
				lsDisplay = new LevelSetListDisplay(im, true);
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

