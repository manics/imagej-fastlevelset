package levelset;

import ij.process.*;
import ij.IJ;


/**
 * A speed field for a level sets segmentation
 * Taken from "Hybrid geodesic region-based curve evolutions for image
 * segmentation", 2007, SPIE, Lankton, Nain, Yezzi, Tannenbaum.
 */
public class HybridSpeedField extends SpeedField {
	/**
	 * Parameters for the hybrid speed field
	 */
	public static class Parameters {
		/**
		 * Radius of local region
		 */
		public int neighbourhoodRadius;

		/**
		 * Filter out intensities above this level, 0 means ignore
		 */
		public int cutoffIntensity;
	}

	/**
	 * Parameters for this speed field
	 */
	private Parameters params;

	/**
	 * The (optionally filtered) image
	 */
	private ImageProcessor filt;

	/**
	 * Constructor
	 * @param params Parameters for calculating the speed field
	 */
	public HybridSpeedField(Parameters params, ImageProcessor im) {
		this.params = params;
		this.filt = im;
		IJ.log("HybridSpeedField Parameters: neighbourhoodRadius:"
			   + params.neighbourhoodRadius + " cutoffIntensity:"
			   + params.cutoffIntensity);

		if (params.cutoffIntensity > 0) {
			filterImage();
		}
	}

	public int computeSpeed(FastLevelSet.Byte2D phi, Point p) {
		return getFLSSpeed(computeSpeedD(phi, p));
	}

	public double computeSpeedD(FastLevelSet.Byte2D phi, Point p) {
		int cr = params.neighbourhoodRadius;
		int pmaxx = Math.min(p.x + cr, filt.getWidth());
		int pmaxy = Math.min(p.y + cr, filt.getHeight());
		int pminx = Math.max(p.x - cr, 0);
		int pminy = Math.max(p.y - cr, 0);

		double areaIn = 0, areaOut = 0, meanIn = 0, meanOut = 0;

		for (int y = pminy; y < pmaxy; ++y) {
			for (int x = pminx; x < pmaxx; ++x) {
				int im = filt.get(x, y);
				if (phi.get(x, y) < 0) {
					++areaIn;
					meanIn += im;
				}
				else {
					++areaOut;
					meanOut += im;
				}
			}
		}

		meanIn /= areaIn;
		meanOut /= areaOut;

		// Chan-Vese
		double sp = - (meanIn - meanOut) *
			(2 * filt.get(p.x, p.y) - meanIn - meanOut);
		return sp;
	}

	/**
	 * Execute a low-intensity pass filter on the image
	 */
	protected void filterImage() {
		int w = filt.getWidth();
		int h = filt.getHeight();

		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				double tmp = filt.get(x, y) / params.cutoffIntensity;
				tmp = filt.get(x, y) * Math.sqrt(1 / (1 + tmp * tmp));
				filt.set(x, y, (int)tmp);
			}
		}
	}
}

