package ijfls.connect;

import java.util.*;
import ij.ImageStack;
import ij.process.*;
import ij.IJ;


/**
 * Trace components across consecutive slices
 */
public class ConnectSlices {
	/**
	 * The input labels, each image has been labelled independently
	 */
	private ImageStack labelIms;

	/**
	 * The size of the image stacks [width, height, depth]
	 */
	private int[] size = new int[3];

	/**
	 * A slice index and a label
	 */
	private class SliceLabel implements Comparable {
		/**
		 * The slice index
		 */
		public final int slice;

		/**
		 * The region label
		 */
		public final int label;

		/**
		 * Create a new object holding a slice index and label
		 * @param slice The slice index
		 * @param label The region label
		 */
		public SliceLabel(int slice, int label) {
			this.slice = slice;
			this.label = label;
		}

		public int compareTo(Object o) {
			SliceLabel sl = (SliceLabel)o;
			if (slice < sl.slice || (slice == sl.slice && label < sl.label)) {
				return -1;
			}
			if (slice > sl.slice || (slice == sl.slice && label > sl.label)) {
				return 1;
			}
			return 0;
		}

		public boolean equals(Object o) {
			return (o instanceof SliceLabel) &&
				slice == ((SliceLabel)o).slice &&
				label == ((SliceLabel)o).label;
		}

		public int hashCode() {
			return slice ^ ~label;
		}

		public String toString() {
			return "[slice:" + slice + " label:" + label + "]";
		}
	}

	/**
	 * A modified UnionFind class that contains an additional field to hold
	 * the reordered pruned label
	 */
	private static class UnionFindSL extends UnionFind<SliceLabel> {
		/**
		 * The modified label after pruning and reordering. -1 indicates unset.
		 */
		public int newLabel = -1;

		/**
		 * Constructor, just calls super()
		 */
		public UnionFindSL(SliceLabel sl) {
			super(sl);
		}
	}

	/**
	 * The overall list of components (in 3D) found so far
	 */
	private NavigableMap<SliceLabel, UnionFindSL> regions;

	/**
	 * Indicator for the forward (i to i+1) and backward (i to i-1) maps
	 */
	public enum MapDirection { FORWARD, BACKWARD };

	/**
	 * A mapping of labels in slice(i) to slice(i+1).
	 * fwdLabelMap.get(SliceLabel(i,j)) returns the set of labels in slice(i+1)
	 * corresponding to label j in slice(i).
	 * Last element will be empty.
	 */
	private NavigableMap<SliceLabel, TreeSet<Integer>> fwdLabelMap;

	/**
	 * A mapping of labels in slice(i) to slice(i-1).
	 * First element will be empty.
	 */
	private NavigableMap<SliceLabel, TreeSet<Integer>> bwdLabelMap;

	/**
	 * The output label image stack, on which components joined across slices
	 * are assigned the same label
	 */
	private ImageStack relabelStack;

	/**
	 * The number of components after pruning
	 */
	private int ncomponents;

	/**
	 * Create a new class for finding connected components in an image
	 * @param labelIms An image stack consisting of images which were
	 *        independently labelled using ConnectedComponents
	 */
	public ConnectSlices(ImageStack labelIms) {
		this.labelIms = labelIms;
		size[0] = labelIms.getWidth();
		size[1] = labelIms.getHeight();
		size[2] = labelIms.getSize();

		fwdLabelMap = new TreeMap<SliceLabel, TreeSet<Integer>>();
		bwdLabelMap = new TreeMap<SliceLabel, TreeSet<Integer>>();

		// Add a dummy region with label 0 to account for background when
		// colouring
		regions = new TreeMap<SliceLabel, UnionFindSL>();
		SliceLabel sl = new SliceLabel(0, 0);
		regions.put(sl, new UnionFindSL(sl));
		ncomponents = 0;
	}

	/**
	 * Gets the mapping of region labels between slices
	 * @param dir Whether to get the forward (slice i to slice i+1) or
	 *        backward (slice i to i-1) mapping
	 * @return The correspondance between labels of consecutive slices.
	 *         Keys are slice-label pairs, values are lists of corresponding
	 *         labels in either the next (forward) or previous (backward)
	 *         slice.
	 */
	public NavigableMap<SliceLabel, TreeSet<Integer>> getSliceLabelMap(
		MapDirection dir) {
		if (dir == MapDirection.FORWARD) {
			return fwdLabelMap;
		}
		return bwdLabelMap;
	}

	/**
	 * Get the relabelled image stack
	 * @return the image stack relabelled across slices
	 */
	public ImageStack getRelabelStack() {
		return relabelStack;
	}

	/**
	 * Get the number of components
	 * @return the number of components
	 */
	public int getNumComponents() {
		return ncomponents;
	}

	/**
	 * Connect slices
	 * Create a mapping of labels between consecutive slices
	 * @return The number of components
	 */
	public int connectSlices() {
		relabelStack = new ImageStack(size[0], size[1]);

		// [0..N] inclusive to ensure keys are added for the end images
		// (remember stacks use 1-based indexing)
		for (int i = 0; i <= size[2]; ++i) {
			connectSlicePair(i, i + 1);
		}

		create3DRegionLabels();
		pruneLabels();
		return ncomponents;
	}

	/**
	 * Trace the components two slice(i) and slice(j)
	 * If i or j are out of range then empty keys are added to the maps
	 * @param i index of the first slice
	 * @param j index of the second slice
	 */
	private void connectSlicePair(int i, int j) {
		ImageProcessor labels1 = null;
		ImageProcessor labels2 = null;
		if (i >= 1 && i <= size[2]) {
			labels1 = labelIms.getProcessor(i);
		}
		if (j >= 1 && j <= size[2]) {
			labels2 = labelIms.getProcessor(j);
		}

		for (int y = 0; y < size[1]; ++y) {
			for (int x = 0; x < size[0]; ++x) {
				int p = (labels1 != null) ? labels1.get(x, y) : 0;
				int q = (labels2 != null) ? labels2.get(x, y) : 0;

				if (p != 0) {
					SliceLabel kp = new SliceLabel(i, p);
					if (!fwdLabelMap.containsKey(kp)) {
						fwdLabelMap.put(kp, new TreeSet<Integer>());
					}
					if (q != 0) {
						fwdLabelMap.get(kp).add(q);
					}
				}

				if (q != 0) {
					SliceLabel kq = new SliceLabel(j, q);
					if (!bwdLabelMap.containsKey(kq)) {
						bwdLabelMap.put(kq, new TreeSet<Integer>());
					}
					if (p != 0) {
						bwdLabelMap.get(kq).add(p);
					}
				}
			}
		}
	}

	/**
	 * Convert the map of labels between slices into a list of merged regions
	 */
	private void create3DRegionLabels() {
		// Sanity check
		assert fwdLabelMap.keySet().equals(bwdLabelMap.keySet());

		// First create empty labels for all slices and labels
		for (SliceLabel sl: fwdLabelMap.keySet()) {
			regions.put(sl, new UnionFindSL(sl));
		}

		// Now merge
		for (Map.Entry<SliceLabel, TreeSet<Integer>> fromKV :
				 fwdLabelMap.entrySet()) {
			for (Integer i : fromKV.getValue()) {
				SliceLabel toK = new SliceLabel(fromKV.getKey().slice + 1, i);
				mergeRegions(fromKV.getKey(), toK);
			}
		}
	}

	/**
	 * Output a mapping as a string
	 * @param dir Process map forwards or backwards
	 * @return a string
	 */
	public String mapToString(MapDirection dir) {
		StringBuilder s = new StringBuilder();
		NavigableMap<SliceLabel, TreeSet<Integer>> m;
		if (dir == MapDirection.FORWARD) {
			m = fwdLabelMap;
		}
		else {
			m = bwdLabelMap.descendingMap();
		}

		for (Map.Entry<SliceLabel, TreeSet<Integer>> kv : m.entrySet()) {
			s.append(String.format("Slice %3d Label %3d -> %s",
								   kv.getKey().slice, kv.getKey().label,
								   kv.getValue()) + "\n");
		}

		return s.toString();
	}

	/**
	 * Merge two regions (does not update the label image)
	 * @param a the slice-label of the first region
	 * @param b the slice-label of the second region
	 */
	private SliceLabel mergeRegions(SliceLabel a, SliceLabel b) {
		UnionFindSL merged = (UnionFindSL)UnionFindSL.union(
			regions.get(a), regions.get(b));
		return merged.getRootLabel();
	}

	/**
	 * Prune non-root labels, and create new labels that are consecutive.
	 * Relabel the image using the pruned labels.
	 */
	private void pruneLabels() {
		ncomponents = -1;

		for (UnionFindSL r : regions.values()) {
			UnionFindSL root = (UnionFindSL)r.findRoot();

			// If the root hasn't been relabelled yet then assign one to it
			if (root.newLabel == -1) {
				root.newLabel = ++ncomponents;
			}
			// Although this component may have been merged it's label still
			// needs to be set because labeim still has the unmerged labels
			r.newLabel = root.newLabel;
		}

		for (int z = 1; z <= size[2]; ++z) {
			ImageProcessor sliceIn = labelIms.getProcessor(z);
			ShortProcessor sliceOut = new ShortProcessor(size[0], size[1]);

			for (int y = 0; y < size[1]; ++y) {
				for (int x = 0; x < size[0]; ++x) {
					int label = sliceIn.get(x, y);
					if (label != 0) {
						SliceLabel sl = new SliceLabel(z, label);
						assert regions.containsKey(sl);
						sliceOut.set(x, y, regions.get(sl).newLabel);
					}
				}
			}

			relabelStack.addSlice(sliceOut);
		}
	}

	/**
	 * Get a coloured version of the label image for display purposes
	 * @return An RGB image where each label is coloured
	 */
	public ImageStack getColouredLabels() {
		ImageStack coloured = new ImageStack(size[0], size[1]);
		for (int z = 1; z <= size[2]; ++z) {
			coloured.addSlice(
				ConnectedComponents.colourLabels(
					(ShortProcessor)relabelStack.getProcessor(z), ncomponents));
		}
		return coloured;
	}

}

