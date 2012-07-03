package ijfls.levelset;

import java.util.Iterator;

/**
 * A class which is notified of the completion of intermediate iterations
 * during the evolution of a FastLevelSet
 */
public interface LevelSetIterationListener {
	/**
	 * A full iteration (i.e. all speed and smooth iterations) has completed
	 * @param full The number of full iterations completed
	 * @param fullT The total number of full iterations
	 */
	public void fullIteration(int full, int fullT);

	/**
	 * A speed sub-iteration has completed
	 * @param full The number of full iterations completed
	 * @param fullT The total number of full iterations
	 * @param speed The number of speed sub-iterations completed during this
	 *        cycle
	 * @param speedT The total number of speed sub-iterations during this cycle
	 */
	public void speedIteration(int full, int fullT, int speed, int speedT);

	/**
	 * A smooth sub-iteration has completed
	 * @param full The number of full iterations completed
	 * @param fullT The total number of full iterations
	 * @param smooth The number of smooth sub-iterations completed during
	 *        this cycle
	 * @param smoothT The total number of smooth sub-iterations during this
	 *        cycle
	 */
	public void smoothIteration(int full, int fullT, int smooth, int smoothT);
}
