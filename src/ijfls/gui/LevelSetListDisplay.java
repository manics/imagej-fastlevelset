package ijfls.gui;

import java.util.Iterator;
import ij.ImagePlus;
import ij.process.*;

import ijfls.levelset.*;

/**
 * A class which displays the level set inside and outside lists on an image
 */
public class LevelSetListDisplay implements LevelSetListListener {
	/**
	 * The ImagePlus object managing the display
	 */
	private ImagePlus imp = null;

	/**
	 * The displayed image
	 */
	private ColorProcessor im;

	/**
	 * Show intermediate iterations?
	 */
	private boolean showIntermediates;

	/**
	 * Colour of inside boundary (default red)
	 */
	private int colourIn = 0xff0000;

	/**
	 * Colour of outside boundary (default blue)
	 */
	private int colourOut = 0x0000ff;

	/**
	 * Creates a new window displaying the intermediate evolution of the
	 * FastLevelSet
	 * @param background The image to be used as a background (will be copied)
	 * @param showIntermediates If true show intermediate speed and smooth
	 *        iterations, otherwise only show full iterations
	 */
	public LevelSetListDisplay(ImageProcessor background,
							   boolean showIntermediates) {
		this.showIntermediates = showIntermediates;
		setBackground(background);
	}

	/**
	 * Set the background image for the level set display (will be copied)
	 * @param background The background image
	 */
	public void setBackground(ImageProcessor background) {
		im = (ColorProcessor)background.duplicate().convertToRGB();
		if (imp == null) {
			imp = new ImagePlus("Level set iterations", im);
		}
		else {
			imp.setProcessor(im);
		}

		im.snapshot();
		imp.show();
	}

	/**
	 * Set the colours used to display the inside and outside boundaries
	 * @param colourIn The colour of the inside boundary
	 * @param colourOut The colour of the outside boundary
	 */
	public void setColours(int colourIn, int colourOut) {
		this.colourIn = colourIn;
		this.colourOut = colourOut;
	}

	/**
	 * Redraw the boundaries
	 * @param lin Iterator to the list of inside points
	 * @param lout Iterator to the list of outside points
	 */
	protected void update(Iterator<Point> lin, Iterator<Point> lout) {
		// Discard previous level set display
		im.reset();
		im.snapshot();

		while (lin.hasNext()) {
			Point p = lin.next();
			im.set(p.x, p.y, colourIn);
		}

		while (lout.hasNext()) {
			Point p = lout.next();
			im.set(p.x, p.y, colourOut);
		}

		imp.updateAndDraw();
	}


	public void fullIteration(Iterator<Point> lin, Iterator<Point> lout) {
		update(lin, lout);
	}

	public void speedIteration(Iterator<Point> lin, Iterator<Point> lout) {
		if (showIntermediates) {
			update(lin, lout);
		}
	}

	public void smoothIteration(Iterator<Point> lin, Iterator<Point> lout) {
		if (showIntermediates) {
			update(lin, lout);
		}
	}
}

