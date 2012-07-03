package ijfls;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Run a plugin on a test image without running ImageJ (useful for testing)
 */
public class RunPluginWithoutIJ {
	/**
	 * Load an image and run a plugin
	 * pluginName: The classname of the plugin
	 * imName: The image filename
	 */
	public static void run(String pluginName, String imName) {
		PlugInFilter plugin = loadPlugin(pluginName);
		if (plugin == null) {
			return;
		}

		ImagePlus imp = IJ.openImage(imName);
		imp.show();

		plugin.setup("Test " + pluginName, imp);
		plugin.run(imp.getProcessor());
	}

	/**
	 * Attempts to load an ImageJ plugin
	 * @string The plugin classname
	 * @return the plugin object, or null if an error occurred
	 */
	protected static PlugInFilter loadPlugin(String pluginName) {
		try {
			Class<?> piClass = Class.forName(pluginName);
			Constructor<?> piCtor = piClass.getConstructor();
			PlugInFilter plugin = (PlugInFilter)piCtor.newInstance();
			return plugin;
		}
		catch (ClassNotFoundException e) {
			System.err.println("Error loading plugin:\n\t" + e);
		}
		catch (NoSuchMethodException e) {
			System.err.println("Error loading plugin:\n\t" + e);
		}
		catch (InstantiationException e) {
			System.err.println("Error loading plugin:\n\t" + e);
		}
		catch (IllegalAccessException e) {
			System.err.println("Error loading plugin:\n\t" + e);
		}
		catch (InvocationTargetException e) {
			System.err.println("Error loading plugin:\n\t" + e);
		}

		return null;
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Expected args: PluginName image.file");
			return;
		}

		run(args[0], args[1]);
	}
}
