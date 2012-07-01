import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import connect.ConnectedComponents;


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
		ImageStack labelstack = new ImageStack(
			stack.getWidth(), stack.getHeight());

		try {
			// stack.getProcessor(i) uses 1-based indexing
			int stackSize = stack.getSize();
			for (int i = 1; i <= stackSize; ++i) {
				IJ.log("Processing slice " + i);
				IJ.showStatus("Processing slice " + i + "/" + stackSize);

				ImageProcessor im = stack.getProcessor(i);
				ConnectedComponents cc = new ConnectedComponents(
					new BinaryProcessor((ByteProcessor)im));
				int ncomponents = cc.labelComponents4();
				IJ.log("Found " + ncomponents + " components");

				labelstack.addSlice(cc.getColouredLabels());
			}
		}
		catch (Error e) {
			IJ.log(e.getMessage());
			e.printStackTrace();
			throw e;
		}

		String title = imp.getShortTitle() + " Coloured Regions";
		ImagePlus result = new ImagePlus(title, labelstack);
		result.show();
	}

}

