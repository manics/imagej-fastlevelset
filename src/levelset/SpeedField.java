package levelset;

/**
 * A speed field for a level sets segmentation.
 * In general it is expected that the FastLevelSet algoithm will call
 * computeSpeed() to allow a complete integer-only algorithm.
 * However since in practice double precision may be used in calculations
 * computeSpeedD() is also provided.
 *
 * Note that the speed definition used in the fast level set implementation
 * treats positive speeds as an expansion of the boundary, and negative as a
 * contraction. This is in contrast to most edge-based level set papers where
 * a positive speed corresponds to increasing the signed distance function
 * which is negative inside the boundary, and therefore the boundary contracts.
 *
 * These two definitions make everything confusing, so to be clear from now on
 * The all floating point speed fields will assume the conventional definition
 * (positive: contract, negative:expand) and these will be converted into the
 * FastLevelSet convention by getFLSSpeed()
 *
 * @todo Represent the fast level set speed field values as a enum to hide
 * these details
 */
public abstract class SpeedField {
	/**
     * Compute the speed at a single point
     * @param phi Level-set phi function
     * @param p The point
     * @return The speed at the point: [-1 0 1]
     */
	abstract int computeSpeed(FastLevelSet.Byte2D phi, Point p);

    /**
     * Compute the speed at a single point
     * @param phi Level-set phi function
     * @param p The point
     * @return The speed at the point as a double
     */
	abstract double computeSpeedD(FastLevelSet.Byte2D phi, Point p);

    /**
     * Does this speed field need to be updated with changed points?
     * @return true if updateSpeedChanges() should be called, false otherwise
     */
	boolean requiresSpeedUpdate() {
        return false;
	}

    /**
     * Notify the speed field that a point has moved from inside to outside
     * @param p The point which has moved from inside to outside
     */
	void switchOut(Point p) {
	}

    /**
     * Notify the speed field that a point has moved from outside to inside
     * @param p The point which has moved from outside to inside
     */
	void switchIn(Point p) {
	}

    /**
     * Update the speed field based on points which have changed sign
     */
	void updateSpeedChanges() {
	}

    /**
     * Compute the fast level set speed from a given value
     * See the comment at the top of this class declaration for more
     * explanation of the sign change.
     * @param s The speed
     * @return The speed at the point: [-1 0 1]
     */
    public static int getFLSSpeed(double s) {
		return (int)(-Math.signum(s));
    }
}


