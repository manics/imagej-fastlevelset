package ijfls;

import ij.*;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ijfls.measure.MeasureSegmentation;


/**
 * MeasureSegmentation_Plugin
 *
 * Obtain measurements from a segmentation.
 * At the moment this just returns the fitted ellipse parameters for a stack.
 *
 * TODO: If multiple regions are present analyse them separately
 * TODO: Obtain projected measurements for a kymograph
 */
public class MeasureSegmentation_Plugin implements PlugInFilter {

	/**
	 * The segmentation image
	 */
	protected ImagePlus imp;

	/**
	 * The Analyzer object which calculates the measurements
	 */
	Analyzer an;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		int stackSize = stack.getSize();

		initialiseAnalyser();

		try {
			// stack.getProcessor(i) uses 1-based indexing
			for (int i = 1; i <= stackSize; ++i) {
				IJ.log("Measuring slice " + i);
				IJ.showStatus("Measuring slice " + i + "/" + stackSize);

				getMeasurements(imp, i);
			}
		}
		catch (Error e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Create the Analyser object for calculating measurements
	 */
	void initialiseAnalyser() {
		// Partial explanation of available measurements:
		// http://imagejdocu.tudor.lu/doku.php?id=gui:analyze:set_measurements
		int[] measures = {
			Measurements.AREA,
			Measurements.ELLIPSE,
			Measurements.FERET,
			//Measurements.MEAN,
			//Measurements.MIN_MAX,
			Measurements.PERIMETER
		};

		int ms = 0;
		for (int m : measures) {
			ms += m;
		}

		an = new Analyzer(imp, ms, new ResultsTable());
	}

	/**
	 * Convert the binary segmentation into an ROI, and get shape measurements
	 * TODO: Fit an ellipse to a segmentation
	 * TODO: Consider handling multiple regions separately
	 * TOOD: Combine segmentation and original image to get intensity
	 *       measurements
	 * @param imp The ImagePlus object holding the segmentation
	 * @param slice The slice index
	 */
	void getMeasurements(ImagePlus imp, int slice) {
		imp.setSlice(slice);
		ImageProcessor seg = imp.getProcessor();
		MeasureSegmentation ms = new MeasureSegmentation();

		Roi roi = ms.segmentationToRoi(seg);
		imp.setRoi(roi);

		an.measure();
		an.displayResults();
	}
}

