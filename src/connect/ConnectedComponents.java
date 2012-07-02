package connect;

import java.awt.Color;
import java.util.ArrayList;
import ij.process.*;


/**
 * Find the connected components in a binary image
 */
public class ConnectedComponents {
	/**
	 * The binary image
	 */
	private BinaryProcessor binim;

	/**
	 * A modified UnionFind class that contains an additional field to hold
	 * the reordered pruned label
	 */
	private static class UnionFindL extends UnionFind<Integer> {
		/**
		 * The modified label after pruning and reordering. -1 indicates unset.
		 */
		public int newLabel = -1;

		/**
		 * Constructor, just calls super()
		 */
		public UnionFindL(Integer label) {
			super(label);
		}
	}

	/**
	 * The list of components found so far
	 */
	private ArrayList<UnionFindL> regions;

	/**
	 * The label image
	 */
	private ShortProcessor labelim;

	/**
	 * The number of components after pruning
	 */
	private int ncomponents;

	/**
	 * Create a new class for finding connected components in an image
	 */
	public ConnectedComponents(BinaryProcessor binim) {
		this.binim = binim;
		regions = new ArrayList<UnionFindL>();
		labelim = new ShortProcessor(binim.getWidth(), binim.getHeight());

		// Add a dummy region with label 0 (makes indexing easier)
		newRegion();
		ncomponents = 0;
	}

	/**
	 * Get the labelled image
	 * @return the labelled image
	 */
	public ShortProcessor getLabelImage() {
		return labelim;
	}

	/**
	 * Get the number of components
	 * @return the number of components
	 */
	public int getNumComponents() {
		return ncomponents;
	}

	/**
	 * Label the components of the binary image using 4-connectivity
	 * @return The number of components
	 */
	public int labelComponents4() {
		int w = binim.getWidth();
		int h = binim.getHeight();

		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				boolean p = binim.get(x, y) > 0;

				if (p) {
					// Need to check west and north labels,
					// merge regions if necessary
					int lwest = getLabelWest(x, y);
					int lnorth = getLabelNorth(x, y);

					if (lwest > 0) {
						labelim.set(x, y, lwest);
						if (lnorth > 0 && lnorth != lwest) {
							mergeRegions(lwest, lnorth);
						}
					}
					else if (lnorth > 0) {
						labelim.set(x, y, lnorth);
					}
					else {
						labelim.set(x, y, newRegion());
					}
				}
			}
		}

		pruneLabels();
		return ncomponents;
	}

	/**
	 * Get the label of the pixel to the west of this pixel
	 * @param x X-coordinate of this pixel
	 * @param y Y-coordinate of this pixel
	 * @return The label, or -1 if beyond the edge of the image
	 */
	private int getLabelWest(int x, int y) {
		return x == 0 ? -1 : labelim.get(x - 1, y);
	}

	/**
	 * Get the label of the pixel to the north of this pixel
	 * @param x X-coordinate of this pixel
	 * @param y Y-coordinate of this pixel
	 * @return The label, or -1 if beyond the edge of the image
	 */
	private int getLabelNorth(int x, int y) {
		return y == 0 ? -1 : labelim.get(x, y - 1);
	}

	/**
	 * Add a new region
	 * @return the label for this region
	 */
	private int newRegion() {
		int label = regions.size();
		if (label > Short.MAX_VALUE) {
			throw new ArithmeticException("Maximum number of labels (" +
										  Short.MAX_VALUE +") exceeded");
		}

		regions.add(new UnionFindL(label));
		return label;
	}

	/**
	 * Merge two regions (does not update the label image)
	 * @param a the index of the first region
	 * @param b the index of the second region
	 */
	private int mergeRegions(int a, int b) {
		UnionFindL merged =
			(UnionFindL)UnionFindL.union(regions.get(a), regions.get(b));
		return merged.getRootLabel();
	}

	/**
	 * Prune non-root labels, and create new labels that are consecutive.
	 * Relabel the image using the pruned labels.
	 */
	private void pruneLabels() {
		ncomponents = -1;

		for (UnionFindL r : regions) {
			UnionFindL root = (UnionFindL)r.findRoot();

			// If the root hasn't been relabelled yet then assign one to it
			if (root.newLabel == -1) {
				root.newLabel = ++ncomponents;
			}
			// Although this component may have been merged it's label still
			// needs to be set because labeim still has the unmerged labels
			r.newLabel = root.newLabel;
		}

		for (int y = 0; y < labelim.getHeight(); ++y) {
			for (int x = 0; x < labelim.getWidth(); ++x) {
				labelim.set(x, y, regions.get(labelim.get(x, y)).newLabel);
			}
		}

		//for (int y = 0; y < labelim.getHeight(); ++y) {
		//	int[] data = new int[labelim.getWidth()];
		//	labelim.getRow(0, y, data, labelim.getWidth());
		//	IJ.log(java.util.Arrays.toString(data));
		//}
	}

	/**
	 * Get a coloured version of the label image for display purposes
	 * @return An RGB image where each label is coloured
	 */
	public ColorProcessor getColouredLabels() {
		return colourLabels(labelim, ncomponents);
	}

	/**
	 * Create a colourmap of contrasting colours with at least n+1 entries
	 * (there may be more), where entry[0] is black
	 * @param n The minimum number of different colours required (ignoring
	 *          background)
	 * @return The colourmap, entry [0] will be black (for background)
	 */
	public static int[] createColourmap(int n) {
		// Use the HSB colourspace to find different hues
		// n^(1/5) as a rough heuristic for choosing the number of
		// lightnesses vs hues
		int nlightness = (int)Math.floor(Math.pow(n, 0.2));
		int nhues = (int)Math.ceil((double)n / nlightness);

		int[] cmap = new int[nlightness * nhues + 1];
		cmap[0] = 0;

		for (int light = nlightness; light > 0; --light) {
			for (int hue = 0; hue < nhues; ++hue) {
				cmap[(nlightness - light) * nhues + hue + 1] = Color.HSBtoRGB(
					(float)hue / nhues, 1, (float)light / nlightness);
			}
		}

		return cmap;
	}

	/**
	 * Colour in a label image for display purposes
	 * @param labelim The label image
	 * @param ncolours The number of colours to use, excluding background
	 *        (will be recycled if there are more labels than colours)
	 * @return An RGB image where each label is coloured
	 */
	public static ColorProcessor colourLabels(ShortProcessor labelim,
											  int ncolours) {
		int w = labelim.getWidth();
		int h = labelim.getHeight();
		ColorProcessor colourLabels = new ColorProcessor(w, h);
		int[] cmap = createColourmap(ncolours);

		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				int c = labelim.get(x, y);
				// c=0: background
				if (c > 0) {
					c = (c % ncolours) + 1;
				}
				colourLabels.set(x, y, cmap[c]);
			}
		}

		return colourLabels;
	}
}

