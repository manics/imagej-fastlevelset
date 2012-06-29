package levelset;

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
}

