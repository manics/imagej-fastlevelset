package connect;

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
	 */
	public void labelComponents4() {
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
			for (x = 0; x < w; ++x) {
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
				}
			}
		}

		pruneLabels();
	}

	/**
	 * Add a new region
	 * @return the label for this region
	 */
	protected int newRegion() {
		int label = regions.size() + 1;
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
		ncomponents = 0;
		for (UnionFindL r : regions) {
			if (r.newLabel == -1) {
				r.newLabel = ++ncomponents;
			}
		}

		for (int y = 0; y < labelim.getHeight(); ++y) {
			for (int x = 0; x < labelim.getWidth(); ++x) {
				labelim.set(x, y, regions.get(labelim.get(x, y)).newLabel);
			}
		}
	}
}

