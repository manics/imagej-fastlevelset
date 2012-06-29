import ij.*;
import levelset.*;


/**
 * Run the FastLevelSet plugin without having to run ImageJ
 */
public class FastLevelSet_Test {
	public static void run(String[] args) {
		if (args.length == 0) {
			System.err.println("No image specified");
			System.exit(2);
		}
		String filename = args[0];

		ImagePlus imp = IJ.openImage(filename);
		imp.show();

		FastLevelSet_Plugin plugin = new FastLevelSet_Plugin();
		plugin.setup("Test", imp);
		plugin.run(imp.getProcessor());
	}

	public static void main(String[] args) {
		run(args);
	}
}
