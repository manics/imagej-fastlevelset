package ijfls.levelset;

import ij.process.*;
import ij.IJ;
import java.util.LinkedList;


/**
 * A basic implementation of the Chan and Vese speed field
 */
public class ChanVeseSpeedField extends SpeedField {
	/**
	 * The image
	 */
	private ImageProcessor im;

	/**
	 * Constructor
	 */
	public ChanVeseSpeedField(ImageProcessor im, BinaryProcessor init) {
		this.im = im;
		initialise(init);
		IJ.log("ChanVeseSpeedField");
	}

	public int computeSpeed(FastLevelSet.Byte2D phi, Point p) {
		return getFLSSpeed(computeSpeedD(phi, p));
	}

	public double computeSpeedD(FastLevelSet.Byte2D phi, Point p) {
		// F = (I - u1)^2 - (I - u2)^2
		//	 = I^2 - 2Iu1 + u1^2 - I^2 + 2Iu2 -u2^2
		//	 = -2I(u1 - u2) + (u1^2 - u2^2)
		//	 = -2I(u1 - u2) + (u1 + u2)(u1 - u2)
		//	 = (u1 - u2)(-2I + u1 + u2)

		// Note don't call updateSpeedChanges(), leave it to the caller

		double sp = diff * (-2 * im.get(p.x, p.y) + sum);
		return sp;
	}

	public boolean requiresSpeedUpdate() {
		return in2out.size() > 0 || out2in.size() > 0;
	}

	public void switchOut(Point p) {
		assert p.x < im.getWidth() && p.y < im.getHeight();
		in2out.addFirst(p);
	}

	public void switchIn(Point p) {
		assert p.x < im.getWidth() && p.y < im.getHeight();
		out2in.addFirst(p);
	}

	public void updateSpeedChanges() {
		for (Point p : in2out) {
			--ain;
			++aout;
			tin -= im.get(p.x, p.y);
			tout += im.get(p.x, p.y);
		}
		in2out.clear();

		for (Point p : out2in) {
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
	protected void initialise(BinaryProcessor init) {
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
	private LinkedList<Point> in2out = new LinkedList<Point>();

	/**
	 * Current list of points which have moved from outside to inside
	 */
	private LinkedList<Point> out2in = new LinkedList<Point>();

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


