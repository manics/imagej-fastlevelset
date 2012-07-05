package ijfls.levelset;

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
	public enum SfMethod {
		// http://stackoverflow.com/a/9781903
		// This is probably overkill for a small number of enums

		CHAN_VESE("Region (Chan Vese)"),
		HYBRID("Local region (Hybrid)"),
		EDGE("Edge (Not implemented) (Geodesic active contours)");

		private final String value;

		SfMethod(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

		private static final java.util.Map<String, SfMethod> stringToEnum =
			new java.util.HashMap<String, SfMethod>();

		static {
			for (SfMethod m : values()) {
				stringToEnum.put(m.toString(), m);
			}
		}

		public static SfMethod fromValue(String value) {
			return stringToEnum.get(value);
		}
	}

	/**
	 * Create speedfield (note some parameters may be null)
	 * @param method The name of the speedfield algorithm
	 * @param im The image to be segmented
	 * @param init The initialisation
	 * @param hsfp Parameters for the HyrbidSpeedField
	 */
	static public SpeedField create(String method, ImageProcessor im,
									BinaryProcessor init,
									HybridSpeedField.Parameters hsfp) {
		switch (SfMethod.fromValue(method)) {
		case CHAN_VESE:
			return new ChanVeseSpeedField(im, init);
		case HYBRID:
			return new HybridSpeedField(hsfp, im);
		case EDGE:
		default:
			throw new IllegalArgumentException(
				"Speed field method not implemented");
		}
	}
}

