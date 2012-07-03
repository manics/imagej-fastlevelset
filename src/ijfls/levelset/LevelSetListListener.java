package ijfls.levelset;

import java.util.Iterator;

/**
 * A class which is notified of the changes to the internal boundary lists
 * after each intermediate iteration during the evolution of a FastLevelSet
 */
public interface LevelSetListListener {
	/**
	 * A full iteration (i.e. all speed and smooth iterations) has completed
	 * @param lin Iterator to the list of inner boundary points
	 * @param lout Iterator to the list of outer boundary points
	 */
	public void fullIteration(Iterator<Point> lin, Iterator<Point> lout);

	/**
	 * A speed sub-iteration has completed
	 * @param lin Iterator to the list of inner boundary points
	 * @param lout Iterator to the list of outer boundary points
	 */
	public void speedIteration(Iterator<Point> lin, Iterator<Point> lout);

	/**
	 * A smooth sub-iteration has completed
	 * @param lin Iterator to the list of inner boundary points
	 * @param lout Iterator to the list of outer boundary points
	 */
	public void smoothIteration(Iterator<Point> lin, Iterator<Point> lout);
}
