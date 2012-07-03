package levelset;

import ij.*;
import ij.gui.Roi;
import ij.process.*;
import ij.process.AutoThresholder;

import java.util.LinkedList;


/**
 * Handles the creation of an initialisation for the level set.
 */
public class Initialiser {
	/**
	 * The string representing the use ROI method
	 */
	private static final String useRoiStr = "Use ROI";

	/**
	 * Create an initialisation image
	 * @param imp The ImagePlus object holding the image to be thresholded
	 *            (needed because ROIs seem to be attached to this)
	 * @param im The image to be thresholded (will be duplicated)
	 * @param method The name of the method from AutoThresholder
	 */
	public static BinaryProcessor getInitialisation(ImagePlus imp,
													ImageProcessor im,
													String method) {
		BinaryProcessor init;

		if (method == useRoiStr) {
			init =  initFromRoi(imp);
		}
		else {
			init = autoThreshold(im, method);
		}

		return init;
	}

	/**
	 * Get the list of supported initialisation methods
	 * @return a list of the names of initialisation/thresholding methods
	 */
	public static String[] getInitialisationMethods() {
		LinkedList<String> methods = new LinkedList<String>();
		methods.add(useRoiStr);
		for (String s : AutoThresholder.getMethods()) {
			methods.add(s);
		}

		return methods.toArray(new String[0]);
	}

	/**
	 * Create a binary image using
	 * {@link ij.process.AutoThresholder AutoThresholder}
	 * @param im The image to be thresholded (will be duplicated)
	 * @param method The name of the method from AutoThresholder
	 */
	private static BinaryProcessor autoThreshold(ImageProcessor im,
												 String method) {
		AutoThresholder thresholder = new AutoThresholder();
		int threshold = thresholder.getThreshold(method, im.getHistogram());

		ImageProcessor init = im.duplicate();
		init.threshold(threshold);
		init.convertToByte(false);
		return new BinaryProcessor((ByteProcessor)init);
	}

	/**
	 * Create an initialisation image from an ROI
	 * @param imp: The image plus object (for some reason selected ROIs seem
	 *             to be attached to the ImagePlus instead of the
	 *             ImageProcessor)
	 * @return The binary initialisation
	 */
	private static BinaryProcessor initFromRoi(ImagePlus imp) {
		ByteProcessor init = new ByteProcessor(imp.getWidth(), imp.getHeight());
		ImageProcessor mask = imp.getMask();
		Roi roi = imp.getRoi();

		// roi and mask: roi is the bounding box for the mask
		// roi only: rectangular roi

		if (roi == null) {
			IJ.error("No ROI found");
			return null;
		}

		java.awt.Rectangle rect = roi.getBounds();

		if (mask == null) {
			for (int x = 0; x < rect.width; ++x) {
				for (int y = 0; y < rect.height; ++y) {
					init.set(x + rect.x, y + rect.y, 255);
				}
			}
		}
		else {
			for (int x = 0; x < rect.width; ++x) {
				for (int y = 0; y < rect.height; ++y) {
					init.set(x + rect.x, y + rect.y, mask.get(x, y));
				}
			}
		}

		return new BinaryProcessor(init);
	}

}

