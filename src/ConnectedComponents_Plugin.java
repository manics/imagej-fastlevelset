import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import connect.ConnectedComponents;
import connect.ConnectSlices;


/**
 * ConnectedComponents_Plugin
 *
 * Finds 4-connected components in a binary image, creates a coloured image.
 */
public class ConnectedComponents_Plugin implements PlugInFilter {

	/**
	 * The image to be labelled
	 */
	protected ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		ImageStack labelStack = new ImageStack(
			stack.getWidth(), stack.getHeight());
		ImageStack colourLabelStack = new ImageStack(
			stack.getWidth(), stack.getHeight());
		int stackSize = stack.getSize();

		try {
			// stack.getProcessor(i) uses 1-based indexing
			for (int i = 1; i <= stackSize; ++i) {
				IJ.log("Processing slice " + i);
				IJ.showStatus("Processing slice " + i + "/" + stackSize);

				ImageProcessor im = stack.getProcessor(i);
				ConnectedComponents cc = new ConnectedComponents(
					new BinaryProcessor((ByteProcessor)im));
				int ncomponents = cc.labelComponents4();
				IJ.log("Found " + ncomponents + " components");

				labelStack.addSlice(cc.getLabelImage());
				colourLabelStack.addSlice(cc.getColouredLabels());
			}
		}
		catch (Error e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
			throw e;
		}

		String title = imp.getShortTitle() + " Coloured Regions";
		ImagePlus result = new ImagePlus(title, colourLabelStack);
		result.show();

		if (stackSize > 1) {
			ImageStack colourConnect3d = connectSlices(labelStack);
			String title2 = imp.getShortTitle() + " Coloured 3D Regions";
			ImagePlus result2 = new ImagePlus(title2, colourConnect3d);
			result2.show();
		}
	}

	/**
	 * Connect components across slices in a stack
	 * @param labelled The iamge stack in which each image has been
	 *        independently labelled
	 * @return A new label stack in which regions have been coloured taking
	 *         into account connections between slices
	 */
	ImageStack connectSlices(ImageStack labelled) {
		IJ.log("Merging labels across slices");
		ConnectSlices cs = new ConnectSlices(labelled);
		int ncomponents = cs.connectSlices();
		IJ.log("Found " + ncomponents + " components");

		IJ.log(cs.mapToString(ConnectSlices.MapDirection.FORWARD));
		return cs.getColouredLabels();
	}
}

