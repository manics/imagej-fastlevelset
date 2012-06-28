import ij.IJ;
//import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;


/**
 * FastLevelSet
 *
 * Fast level-set segmentation using the algorithm from
 * Yonggang Shi and William Karl, 2005, Real-time Tracking Using Level Sets,
 * CVPR.
 * Also published in 2008, A Real-Time Algorithm for the Approximation of
 * Level-Set-Based Curve Evolution, IEEE Image Processing.
 *
 *
 */
public class FastLevelSet {
	public static class Parameters {
        /**
         * Number of speed evolutions
         */
        int speedIterations;

        /**
         * Number of smoothing evolutions
         */
        int smoothIterations;

        /**
         * Maximum number of iterations
         */
        int maxIterations;

        /**
         * Radius of the Gaussian filter
         */
        int gaussWidth;

        /**
         * Sigma for the Gaussian filter
         */
        double gaussSigma;
	}

	/**
	 * A 2D coordinate
	 */
	protected class Point {
		/**
		 * X coordinate
		 */
		public int x;

		/**
		 * Y coordinate
		 */
		public int y;

		/**
		 * Constructor
		 * @param x The X coordinate
		 * @param y The Y coordinate
		 */
		public Point(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	/**
	 * A 2D array which holds signed values
	 */
	protected class Byte2D {
		/**
		 * @param w Number of columns
		 */
		private int width;

		/**
		 * @param h Number of rows
		 */
		private int height;

		/**
		 * @the contents of the matrix
		 */
		private byte[] vals;

		/**
		 * @param w Number of columns
		 * @param h Number of rows
		 */
		public Byte2D(int w, int h) {
			width = w;
			height = h;
			vals = new byte[w * h];
		}

		public byte get(int x, int y) {
			return vals[y * width + x];
		}

		public void set(int x, int y, byte v) {
			vals[y * width + x] = v;
		}
	}

	public final boolean DEBUG_CHECK = false;

    /**
     * Parameters for the level-set algorithm
     */
	protected Parameters params;

    /**
     * Size of the image
     */
    protected Point size;

    /**
     * The image to be segmented
     */
    protected ImageProcessor im; // ImageType

    /**
     * Phi (level-set function)
     */
    protected Byte2D phi; // PhiType

    /**
     * Temporary speed field
     */
    protected Byte2D speed; // SpeedType

    /**
     * List of points on inside of boundary
     */
	protected List<Point> lin = new LinkedList<Point>();

    /**
     * List of points on outside of boundary
     */
	protected List<Point> lout = new LinkedList<Point>();

    /**
     * Temporary list of points to be added to Lin
     */
	protected List<Point> addlin = new LinkedList<Point>();

    /**
     * Temporary list of points to be added to Lout
     */
	protected List<Point> addlout = new LinkedList<Point>();

    /**
     * The Gaussian filter matrix
     */
    protected Byte2D gaussFilter;

    /**
     * The threshold for the Gaussian filter smoothing
     */
    protected int gaussFilterThreshold;

    /**
     * The speed field
     */
    protected SpeedField speedField;

    /**
     * A temporary variable to hold the current neighbourhood of a point
     */
    protected Point[] nhood;

    /**
     * Number of points in the neighbourhood
     */
    protected int nhSize;

	/**
	 * Constructor.
	 * Setup the intermediate matrices.
     * Initialise phi and the gaussian filter.
	 * @param params Parameters for the level set algorithm
     * @param im The image to be segmented
     * @param init The binary initialisation
     * @param speedfield The speed field
	 */
	FastLevelSet(Parameters params, ImageProcessor im, BinaryProcessor init,
				 SpeedField speedf) {
		this.params = params;
		size = new Point(im.getWidth(), im.getHeight());

		this.im = im;
		phi = new Byte2D(size.x, size.y);
		speed = new Byte2D(size.x, size.y);

		gaussFilter = new Byte2D(
			params.gaussWidth * 2 + 1, params.gaussWidth * 2 + 1);
		gaussFilterThreshold = 0;
		speedField = speedf;
		nhood = new Point[4];
		for (int i = 0; i < 4; ++i) {
			nhood[i] = new Point(0, 0);
		}
		nhSize = 0;

		initialise(init);
	}

	
    /**
     * Segment the image, subject to the maximum iterations
	 * @return true if segmentation completed, false otherwise
     */
	protected boolean segment() {
		boolean converged = false;

		IJ.log("speedIterations:" + params.speedIterations +
			   " smoothIterations:" + params.smoothIterations +
			   " maxIterations:" + params.maxIterations +
			   " gaussWidth:" + params.gaussWidth + 
			   " gaussSigma:" + IJ.d2s(params.gaussSigma));

		for(int nIts = 0; nIts < params.maxIterations; ++nIts) {
			IJ.log("Iteration: " + (nIts + 1) +
				   "/" + params.maxIterations);

			for(int nSpeedIts = 0; nSpeedIts < params.speedIterations;
				++nSpeedIts) {
				IJ.log("\tSpeed: [" + (nIts + 1) + "]" +
					   (nSpeedIts + 1) + "/" + params.speedIterations);

				evolveSpeed();
				//dbPrintf("Phi\n%s\n", LevelSetDisplay::phiString(m_phi).c_str());
				checkConsistency();
				converged = hasConverged();
				if(converged) {
					// Always do at least two iterations
					if (nIts == 0) {
						IJ.log("Converged on iteration [" + (nIts + 1)
							   + "]" + (nSpeedIts + 1) + ", ignoring");
						converged = false;

						// Always break because the level set is currently stuck
					}
					else {
						IJ.log("Converged on iteration [" + (nIts + 1)
							   + "]" + (nSpeedIts + 1));
					}

					break;
				}

				if (escapePressed()) {
					return false;
				}
			}

			for(int nSmoothIts = 0; nSmoothIts < params.smoothIterations;
				++nSmoothIts) {
				IJ.log("\tSmooth: [" + (nIts + 1) + "]"
					   + (nSmoothIts + 1) + "/" + params.smoothIterations);

				evolveSmooth();
				//dbPrintf("Phi\n%s\n", LevelSetDisplay::phiString(m_phi).c_str());
				checkConsistency();

				if (escapePressed()) {
					return false;
				}
			}

			if (converged) {
				break;
			}
		}

		return true;
	}

    /**
     * Evolve once according to the image speed field
     */
	protected void evolveSpeed() {
		/**
		 * @todo Should we set the speed at the new positions to something else
		 * to ensure the convergence check fails?
		 * Currently: Whenever a point is switched set its speed to the opposite
		 * of the convergence criteria to indicate that another iteration should
		 * be done to calculate its speed.
		 */
		if (speedField.requiresSpeedUpdate()) {
			speedField.updateSpeedChanges();
		}

		Iterator<Point> pi;

		pi = lout.iterator();
		while (pi.hasNext()) {
			Point p = pi.next();
			calculateSpeed(p);
			if (speed.get(p.x, p.y) > 0) {
				switchIn(pi, p);
			}
		}

		flushListAdditions();
		cleanLin();

		pi = lin.iterator();
		while(pi.hasNext()) {
			Point p = pi.next();
			calculateSpeed(p);
			if(speed.get(p.x, p.y) < 0) {
				switchOut(pi, p);
			}
        }

		flushListAdditions();
        cleanLout();
	}

    /**
	 * Evolve once according to the smoothing field
     */
	protected void evolveSmooth() {
		Iterator<Point> pi;

		pi = lout.iterator();
		while (pi.hasNext()) {
			Point p = pi.next();
			int f = calculateSmooth(p);
			if(f > gaussFilterThreshold) {
				switchIn(pi, p);
			}
		}

		flushListAdditions();
		cleanLin();

		pi = lin.iterator();
		while (pi.hasNext()) {
			Point p = pi.next();
			int f = calculateSmooth(p);
			if(f < gaussFilterThreshold) {
				switchOut(pi, p);
			}
		}

		flushListAdditions();
		cleanLout();
	}

    /**
     * Gets the level-set function phi
     * @return phi
     */
    Byte2D getPhi() {
		return phi;
	}

    /**
     * Gets a binary segmentation from phi
     * @return the segmented image
     */
	BinaryProcessor getSegmentation() {
		BinaryProcessor seg = new BinaryProcessor(
			new ByteProcessor(size.x, size.y));
		for(int y = 0; y < size.y; ++y) {
			for(int x = 0; x < size.x; ++x) {
				seg.set(x, y, phi.get(x, y) < 0 ? 255 : 0);
			}
		}
		return seg;
	}

    /**
     * Has the level-set converged?
     * In theory we should recalculate the speed field before checking for
     * convergence, however this may be inefficient so instead the caller must
     * either do the recalculation or ensure the speed field indicates
     * non-convergence
     * @todo Get rid of the convergence check since it probably isn't that
	 * useful
     * @return true if convergence has been reached
     */
    protected boolean hasConverged() {
		// Convergence: speed(Lin) >= 0, speed(Lout) <= 0

		for (Point p : lin) {
			if(speed.get(p.x, p.y) < 0) {
				return false;
			}
		}

		for (Point p : lout) {
			if(speed.get(p.x, p.y) > 0) {
				return false;
			}
		}

		return true;
	}

    /**
     * Initialise phi and create the Gaussian smoothing filter
     * @param init The binary initialisation
     */
	protected void initialise(BinaryProcessor init) {
		/**
		 * @todo optimise this
		 */
		Point p = new Point(0, 0);
		for (p.y = 0; p.y < size.y; ++p.y) {
			for (p.x = 0; p.x < size.x; ++p.x) {
				if (init.get(p.x, p.y) > 0) {
					addToList(p, ListType.IN);
				}
				else {
					addToList(p, ListType.OUT);
				}
			}
		}

		flushListAdditions();
		cleanLin();
		cleanLout();

		//dbPrintf("Phi\n%s\n", LevelSetDisplay::phiString(m_phi).c_str());
		checkConsistency();

		if (params.smoothIterations > 0) {
			createGaussFilter();
		}
	}

    /**
     * Creates the Gaussian filter matrix, scaled up to integers
     */
    protected void createGaussFilter() {
		int gw = params.gaussWidth;
		double gs = params.gaussSigma;
		int s = 2 * gw + 1;
		int gfScale = 0;

		// Rough heuristic: scale by number of elements in filter
		double scale1 = s * s;
		if (scale1 > 255) {
			// This is because gaussFilter is a byte array
			error("Not implemented for s > 15");
		}

		// Yes in theory only need to calculate 1/8th and duplicate but I can't
		// be bothered
		for (int y = 0; y < s; ++y) {
			for (int x = 0; x < s; ++x) {
				double d2 = (x - gw) * (x - gw) + (y - gw) * (y - gw);
				double gf = 1.0 / gs / gs * Math.exp(-0.5 / gs / gs * d2)
					* scale1;
				gaussFilter.set(x, y, (byte)gf);
				gfScale += gf;
			}
		}
		gaussFilterThreshold = gfScale / 2;

		//dbPrintf("Gauss Filter\n%s\n", LevelSetDisplay::imageToString
		//		 <LevelSetDisplay::Numeric<int,1,3> >(m_gaussFilter).c_str());
		//dbPrintf("Gauss filter threshold: %d\n", m_gaussFilterThreshold);
	}

    /**
     * Check everything is consistent
     */
    protected void checkConsistency() {
		if (!DEBUG_CHECK) {
			return;
		}

		IJ.log("Checking consistency\n");

		// Check Lin and Lout do not overlap or contain duplicates
		String errorMsg = "";
		java.util.Set<Point> setLin = new java.util.TreeSet<Point>(lin);
		java.util.Set<Point> setLout = new java.util.TreeSet<Point>(lout);

		if (lin.size() != setLin.size()) {
			errorMsg += "Lin contains " + (lin.size() - setLin.size())
				+ " duplicates. ";
		}
		if (lout.size() != setLout.size()) {
			errorMsg += "Lout contains " + (lout.size() - setLout.size())
				+ " duplicates. ";
		}

		java.util.Set<Point> inter = new java.util.TreeSet<Point>(setLin);
		inter.retainAll(setLout);
		if (inter.size() > 0) {
			errorMsg += inter.size() + " point(s) found in both Lin and Lout. ";
		}

		// Don't bother checking further if there's already an error
		if (errorMsg.length() > 0) {
			error("FastLevelSet:CheckConsistency: " +  errorMsg);
		}

		// Check phi, Lin and Lout are consistent
		Byte2D checked = new Byte2D(size.x, size.y);

		for (Point p : lin) {
			if (phi.get(p.x, p.y) == -1) {
				checked.set(p.x, p.y, (byte)1);
			}
			else {
				errorMsg += "Lin(" + p.x + "," + p.y + "): phi="
					+ phi.get(p.x, p.y) + ". ";
			}
		}

		for (Point p : lout) {
			if (phi.get(p.x, p.y) == 1) {
				checked.set(p.x, p.y, (byte)1);
			}
			else {
				errorMsg += "Lout(" + p.x + "," + p.y + "): phi="
					+ phi.get(p.x, p.y) + ". ";
			}
		}

		// Now check remaining regions are either 3 or -3
		for (int y = 0; y < size.y; ++y) {
			for (int x = 0; x < size.x; ++x) {
				if (phi.get(x, y) == 3 || phi.get(x, y) == -3) {
					checked.set(x, y, (byte)1);
				}
				else if (checked.get(x, y) == 0) {
					errorMsg += "phi(" + x + "," + y	+ ")="
						+ phi.get(x, y) + ". ";
				}
			}
		}

		if (errorMsg.length() == 0) {
			IJ.log("Consistency check succeeded\n");
		}
		else {
			error("FastLevelSet:CheckConsistency: " +  errorMsg);
		}
	}

    /**
     * Calculate the speed at a point, stores it in m_speed
     */
    protected void calculateSpeed(Point p) {
		/**
		 * @todo Remove floating point calculations
		 */
		speed.set(p.x, p.y, (byte)speedField.computeSpeed(phi, p));
		assert speed.get(p.x, p.y) >= -1 && speed.get(p.x, p.y) <= 1;
	}

    /**
     * Calculate the smoothing field at a point
     */
    protected int calculateSmooth(Point p) {
		// Convolve neighbourhood of a point with a gaussian
		int gw = params.gaussWidth;
		int dxmax = Math.min(gw + 1, size.x - p.x);
		int dymax = Math.min(gw + 1, size.y - p.y);
		int dxmin = Math.max(-gw, -p.x);
		int dymin = Math.max(-gw, -p.y);

		int f = 0;
		for(int dy = dymin; dy < dymax; ++dy) {
			for(int dx = dxmin; dx < dxmax; ++dx) {
				// conv(G, phi < 0)
				if (phi.get(p.x + dx, p.y + dy) < 0) {
					f = f + gaussFilter.get(gw + dx, gw + dy);
				}
			}
		}

		return f;
	}

    /**
     * Gets the 4-connected neighbourhood of a point
     */
    protected void getNeighbourhood(Point p) {
		/**
		 * @todo Ignore bounds, and instead check bounds when neighbourhood is
		 * used?
		 */
		if (p.x == 0) {
			if (p.y == 0) {
				nhood[0].x = p.x;
				nhood[0].y = p.y + 1;
				nhood[1].x = p.x + 1;
				nhood[1].y = p.y;
				nhSize = 2;
			}
			else if (p.y == size.y - 1) {
				nhood[0].x = p.x;
				nhood[0].y = p.y - 1;
				nhood[1].x = p.x + 1;
				nhood[1].y = p.y;
				nhSize = 2;
			}
			else {
				nhood[0].x = p.x;
				nhood[0].y = p.y + 1;
				nhood[1].x = p.x;
				nhood[1].y = p.y - 1;
				nhood[2].x = p.x + 1;
				nhood[2].y = p.y;
				nhSize = 3;
			}
		}
		else if (p.x == size.x - 1) {
			if (p.y == 0) {
				nhood[0].x = p.x;
				nhood[0].y = p.y + 1;
				nhood[1].x = p.x - 1;
				nhood[1].y = p.y;
				nhSize = 2;
			}
			else if (p.y == size.y - 1) {
				nhood[0].x = p.x;
				nhood[0].y = p.y - 1;
				nhood[1].x = p.x - 1;
				nhood[1].y = p.y;
				nhSize = 2;
			}
			else {
				nhood[0].x = p.x;
				nhood[0].y = p.y + 1;
				nhood[1].x = p.x;
				nhood[1].y = p.y - 1;
				nhood[2].x = p.x - 1;
				nhood[2].y = p.y;
				nhSize = 3;
			}
		}
		else {
			if (p.y == 0) {
				nhood[0].x = p.x;
				nhood[0].y = p.y + 1;
				nhood[1].x = p.x - 1;
				nhood[1].y = p.y;
				nhood[2].x = p.x + 1;
				nhood[2].y = p.y;
				nhSize = 3;
			}
			else if (p.y == size.y - 1) {
				nhood[0].x = p.x;
				nhood[0].y = p.y - 1;
				nhood[1].x = p.x - 1;
				nhood[1].y = p.y;
				nhood[2].x = p.x + 1;
				nhood[2].y = p.y;
				nhSize = 3; 
			}
			else {
				nhood[0].x = p.x;
				nhood[0].y = p.y + 1;
				nhood[1].x = p.x;
				nhood[1].y = p.y - 1;
				nhood[2].x = p.x + 1;
				nhood[2].y = p.y;
				nhood[3].x = p.x - 1;
				nhood[3].y = p.y;
				nhSize = 4;
			}
		}
	}

	/**
	 * Identifiers for the two lists of pixels
	 */
    protected enum ListType
    {
        IN, OUT;
    };

    /**
     * Add a point to the Lin or Lout. The point is added to the current
	 * iterator position (note this is different from the C++ version which
	 * inserts the point at the front of the list.
     */
    private void addToList(Point p, ListType ln) {
		//IJ.log("addToList: p=(" + p.x + "," + p.y + ") ln=" + ln);

		switch(ln) {
		case IN:
			addlin.add(new Point(p.x, p.y));
			phi.set(p.x, p.y, (byte)-1);
			break;
		case OUT:
			addlout.add(new Point(p.x, p.y));
			phi.set(p.x, p.y, (byte)1);
			break;
		default:
			assert false;
		}
	}

    /**
     * Remove a point from Lin or Lout
     */
    private void removeFromList(Iterator<Point> pi, Point p,
								ListType ln, byte phival) {
		//IJ.log("removeFromList: p=(" + p.x + "," + p.y + ") ln=" + ln
		//	   + " phival:" + phival);

		switch(ln) {
		case IN:
			if (phival != 0) {
				phi.set(p.x, p.y, phival);
			}
			pi.remove();
			break;
		case OUT:
			if (phival != 0) {
				phi.set(p.x, p.y, phival);
			}
			pi.remove();
			break;
		default:
			assert false ;
		}
	}

    /**
     * Move a point from Lout to the pending Lin additions
     * Changes speed field at the affected points so that convergence check
     * will fail
	 * flushListAdditions() must be called when all iterators are no longer
	 * required to ensure consistency
     */
    private void switchIn(Iterator<Point> pi, Point p) {
		//IJ.log("switchIn: p=(" + p.x + "," + p.y + ")");
		speedField.switchIn(p);

		// 1. Move point from Lout to Lin
		// 2. Add outside neighbours of p to Lout
		// 3. Set speed fields to ensure convergence check fails (as the speed
		//    field will need to be recalculated)
		addToList(p, ListType.IN);
		assert phi.get(p.x, p.y) == -1;
		speed.set(p.x, p.y, (byte)-1);

		getNeighbourhood(p);

		for (int i = 0; i < nhSize; ++i) {
			if (phi.get(nhood[i].x, nhood[i].y) == 3) {
				addToList(nhood[i], ListType.OUT);
				speed.set(nhood[i].x, nhood[i].y, (byte)1);
			}
		}

		removeFromList(pi, p, ListType.OUT, (byte)0);
		assert phi.get(p.x, p.y) == -1;
	}

    /**
     * Move a point from Lin to the pending Lout additions
     * Changes speed field at the affected points so that convergence check
     * will fail
	 * flushListAdditions() must be called when all iterators are no longer
	 * required to ensure consistency
     */
    private void switchOut(Iterator<Point> pi, Point p) {
		//IJ.log("switchOut: p=(" + p.x + "," + p.y + ")");
		speedField.switchOut(p);

		// 1. Move point from Lin to Lout
		// 2. Add inside neighbours of p to Lin
		// 3. Set speed fields to ensure convergence check fails (as the speed
		//    field will need to be recalculated)
		addToList(p, ListType.OUT);
		assert phi.get(p.x, p.y) == 1;
		speed.set(p.x, p.y, (byte)1);

		getNeighbourhood(p);

		for (int i = 0; i < nhSize; ++i) {
			if (phi.get(nhood[i].x, nhood[i].y) == -3) {
				addToList(nhood[i], ListType.IN);
				speed.set(nhood[i].x, nhood[i].y, (byte)-1);
			}
		}

		removeFromList(pi, p, ListType.IN, (byte)0);
		assert phi.get(p.x, p.y) == 1;
	}

	/**
	 * Insert any pending additions into the front (consistent with the
	 * original C++/Matlab) of the appropriate lists
	 */
	private void flushListAdditions() {
		lin.addAll(0, addlin);
		addlin.clear();
		lout.addAll(0, addlout);
		addlout.clear();
	}

    /**
     * Clean up Lin
     */
    private void cleanLin() {
		//dbPrintf("Cleaning Lin\n");

		Iterator<Point> pi = lin.iterator();
		while (pi.hasNext()) {
			Point p = pi.next();
			//IJ.log("cleanLin: p=(" + p.x + "," + p.y + ")");

			// If all neighbours are < 0, remove from Lin
			getNeighbourhood(p);
			boolean allInside = true;

			for (int i = 0; i < nhSize; ++i) {
				if (phi.get(nhood[i].x, nhood[i].y) > 0) {
					allInside = false;
					break;
				}
			}

			if (allInside) {
				removeFromList(pi, p, ListType.IN, (byte)-3);
				assert phi.get(p.x, p.y) == -3;
			}
		}
	}

    /**
     * Clean up Lout
     */
    private void cleanLout() {
		//dbPrintf("Cleaning Lout\n");

		Iterator<Point> pi = lout.iterator();
		while (pi.hasNext()) {
			Point p = pi.next();
			//IJ.log("cleanLout: p=(" + p.x + "," + p.y + ")");

			// If all neighbours are > 0, remove from Lout
			getNeighbourhood(p);
			boolean allOutside = true;

			for (int i = 0; i < nhSize; ++i) {
				if (phi.get(nhood[i].x, nhood[i].y) < 0) {
					allOutside = false;
					break;
				}
			}

			if (allOutside) {
				removeFromList(pi, p, ListType.OUT, (byte)3);
				assert phi.get(p.x, p.y) == 3;
			}
		}
	}

	/**
	 * Check if escape key pressed, print an error message if so
	 * @return true if escape pressed, false otherwise
	 */
	protected static boolean escapePressed() {
		boolean b = IJ.escapePressed();
		if (b) {
			error("Escape pressed, terminating.");
		}
		IJ.resetEscape();
		return b;
	}

	private static void error(String msg) {
		IJ.error("FastLevelSet error", msg);
	}
}

