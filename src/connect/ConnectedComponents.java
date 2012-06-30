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
	private class UnionFindL extends UnionFind<Integer> {
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
		labelim = new ShortProcessor(w, h);

		// First pixel
		boolean p = binim.get(0, 0) > 0;
		if (p) {
			labelim.set(0, 0, newRegion());
		}

		// First line
		for (int x = 1; x < h; ++x) {
			int y = 0;
			p = binim.get(x, y) > 0;

			if (p) {
				int lwest = labelim.get(x - 1, y);

				if (lwest > 0) {
					labelim.set(x, y, lwest);
				}
				else {
					labelim.set(x, y, newRegion());
				}
			}
		}

		// All other lines
		for (int y = 1; y < h; ++y) {
			int x = 0;
			p = binim.get(x, y) > 0;

			if (p) {
				int lnorth = labelim.get(x, y - 1);
				if (lnorth > 0) {
					labelim.set(x, y, lnorth);
				}
				else {
					labelim.set(x, y, newRegion());
				}
			}

			// Need to check west and north, and merge regions if necessary
			for (x = 1; x < w; ++x) {
				p = binim.get(x, y) > 0;

				if (p) {
					int lwest = labelim.get(x - 1, y);
					int lnorth = labelim.get(x, y - 1);

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
	 * Add a new region
	 * @return the label for this region
	 */
	protected int newRegion() {
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
	 */
	protected int mergeRegions(int a, int b) {
		UnionFindL merged =
			(UnionFindL)UnionFindL.union(regions.get(a), regions.get(b));
		return merged.getRootLabel();
	}

	/**
	 * Prune non-root labels, and create new labels that are consecutive.
	 * Relabel the image using the pruned labels.
	 */
	protected void pruneLabels() {
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
	 * Create a colourmap of contrasting colours, at least one for each label
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

