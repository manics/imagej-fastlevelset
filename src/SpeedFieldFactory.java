import ij.process.*;
import ij.IJ;
import java.util.LinkedList;

/**
 * Factory object for creating different types of speed/potential field for
 * controlling the evolution of the fast level set algorithm
 */
public class SpeedFieldFactory {
	/**
	 * The available methods
	 */
	public enum SFMethod {
		CHAN_VESE, HYBRID, EDGE;
	}

	static public SpeedField create(SFMethod method, ImageProcessor im,
									BinaryProcessor init) {
		switch (method) {
		case CHAN_VESE:
			return new ChanVeseSpeedField(im, init);
		default:
			assert false;
		}
		return null;
	}

	/**
	 * A basic implementation of the Chan and Vese speed field
	 */
	public static class ChanVeseSpeedField extends SpeedField {
		/**
		 * The image
		 */
		ImageProcessor im;

		/**
		 * Constructor
		 */
		ChanVeseSpeedField(ImageProcessor im, BinaryProcessor init) {
			this.im = im;
			initialise(init);
			IJ.log("ChanVeseSpeedField");
		}

		public int computeSpeed(FastLevelSet.Byte2D phi, FastLevelSet.Point p) {
			return getFLSSpeed(computeSpeedD(phi, p));
		}

		public double computeSpeedD(FastLevelSet.Byte2D phi,
									FastLevelSet.Point p) {
			// F = (I - u1)^2 - (I - u2)^2
			//   = I^2 - 2Iu1 + u1^2 - I^2 + 2Iu2 -u2^2
			//   = -2I(u1 - u2) + (u1^2 - u2^2)
			//   = -2I(u1 - u2) + (u1 + u2)(u1 - u2)
			//   = (u1 - u2)(-2I + u1 + u2)

			// Note don't call updateSpeedChanges(), leave it to the caller

			double sp = diff * (-2 * im.get(p.x, p.y) + sum);
			return sp;
		}

		public boolean requiresSpeedUpdate() {
			return in2out.size() > 0 || out2in.size() > 0;
		}

		public void switchOut(FastLevelSet.Point p) {
			assert p.x < im.getWidth() && p.y < im.getHeight();
			in2out.addFirst(p);
		}

		public void switchIn(FastLevelSet.Point p) {
			assert p.x < im.getWidth() && p.y < im.getHeight();
			out2in.addFirst(p);
		}

		public void updateSpeedChanges() {
			for (FastLevelSet.Point p : in2out) {
				--ain;
				++aout;
				tin -= im.get(p.x, p.y);
				tout += im.get(p.x, p.y);
			}
			in2out.clear();

			for (FastLevelSet.Point p : out2in) {
				++ain;
				--aout;
				tin += im.get(p.x, p.y);
				tout -= im.get(p.x, p.y);
			}
			out2in.clear();

			double meanin = tin / ain;
			double meanout = tout / aout;
			sum = meanin + meanout;
			diff = meanin - meanout;
		}

		/**
		 * Calculate the initial inside and outside mean intensities
		 */
		private void initialise(BinaryProcessor init) {
			tin = 0;
			tout = 0;
			ain = 0;
			aout = 0;

			int w = im.getWidth();
			int h = im.getHeight();
			for(int y = 0; y < h; ++y) {
				for(int x = 0; x < w; ++x) {
					if (init.get(x, y) > 0) {
						++ain;
						tin += im.get(x, y);
					}
					else {
						++aout;
						tout += im.get(x, y);
					}
				}
			}

			// This will take care of recalculate the sum and difference
			updateSpeedChanges();
		}

		/**
		 * Current list of points which have moved from inside to outside
		 */
		private LinkedList<FastLevelSet.Point> in2out =
			new LinkedList<FastLevelSet.Point>();

		/**
		 * Current list of points which have moved from outside to inside
		 */
		private LinkedList<FastLevelSet.Point> out2in =
			new LinkedList<FastLevelSet.Point>();

		/**
		 * Total inside intensity
		 */
		private double tin;

		/**
		 * Total outside intensity
		 */
		private double tout;

		/**
		 * Inside area
		 */
		private int ain;

		/**
		 * Outside area
		 */
		private int aout;

		/**
		 * Sum of means (inside + outside)
		 */
		private double sum;

		/**
		 * Difference between means (inside - outside)
		 */
		private double diff;
	}



	/**
	 * A speed field for a level sets segmentation
	 * Taken from "Hybrid geodesic region-based curve evolutions for image
	 * segmentation", 2007, SPIE, Lankton, Nain, Yezzi, Tannenbaum.
	 */
	public static class HybridSpeedField extends SpeedField {
		/**
		 * Parameters for the hybrid speed field
		 */
		public class Parameters {
            /**
			 * Radius of local region
			 */
			int neighbourhoodRadius;

			/**
			 * Filter out intensities above this level, 0 means ignore
			 */
			int cutoffIntensity;
		}

		/**
		 * Parameters for this speed field
		 */
		Parameters params;

		/**
		 * The (optionally filtered) image
		 */
		ImageProcessor filt;

		/**
		 * Constructor
		 * @param params Parameters for calculating the speed field
		 */
		HybridSpeedField(Parameters params, ImageProcessor im) {
			this.params = params;
			this.filt = im;
			IJ.log("HybridSpeedField Parameters: neighbourhoodRadius:"
				   + params.neighbourhoodRadius + " cutoffIntensity:"
				   + params.cutoffIntensity);

			if (params.cutoffIntensity > 0) {
				filterImage();
			}
		}

		public int computeSpeed(FastLevelSet.Byte2D phi, FastLevelSet.Point p) {
			return getFLSSpeed(computeSpeedD(phi, p));
		}

		public double computeSpeedD(FastLevelSet.Byte2D phi,
									FastLevelSet.Point p) {
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
		private void filterImage() {
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

}

