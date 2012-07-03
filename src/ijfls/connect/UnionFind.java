package ijfls.connect;

/**
 * Implementation of the union-find algorithm for finding and merging disjoint
 * components.
 * The root of each tree defines the label of that component, two components
 * are merged by attaching one tree to the root of the other.
 * See http://en.wikipedia.org/wiki/Union_find
 */
public class UnionFind<E> {
	/**
	 * A label for this component
	 */
	private E label;

	/**
	 * The parent of this node
	 */
	private UnionFind<E> parent;

	/**
	 * The number of nodes in this tree
	 */
	private int rank;

	/**
	 * Create a new component
	 * @label The label for this component
	 */
	public UnionFind(E label) {
		this.label = label;
		parent = this;
		rank = 0;
	}

	/**
	 * Merge two components
	 * @param x A component
	 * @param y Another component
	 * @return A single component consisting of the two merged components
	 */
	public static <E> UnionFind<E> union(UnionFind<E> x, UnionFind<E> y) {
		UnionFind<E> xroot = x.findRoot();
		UnionFind<E> yroot = y.findRoot();
		if (xroot == yroot) {
			// x and y are already in the same component
			return xroot;
		}

		// Merge smaller tree into the larger one
		if (xroot.rank < yroot.rank) {
			xroot.parent = yroot;
			return yroot;
		}
		if (xroot.rank > yroot.rank) {
			yroot.parent = xroot;
			return xroot;
		}

		yroot.parent = xroot;
		xroot.rank++;
		return xroot;
	}

	/**
	 * Search for the root of a tree, when found attach this node directly to
	 * root to avoid traversing the tree in future
	 * @return The root of this tree (i.e. the label for this component)
	 */
	public UnionFind<E> findRoot() {
		if (parent != this) {
			parent = parent.findRoot();
		}
		return parent;
	}

	/**
	 * Get the label for this component. Note that a tree may contain multiple
	 * labels due to components being merged, so this is the label assigned
	 * to the component when it was first created, and may not be the same as
	 * the root label.
	 * @return The label of this component
	 */
	public E getOrigLabel() {
		return label;
	}

	/**
	 * Get the root label for this component.
	 * @return The root label of this component
	 */
	public E getRootLabel() {
		return findRoot().label;
	}
}
