package ijfls;

/**
 * Long strings for the FastLevelSet_Plugin dialog
 */
class FastLevelSet_PluginStrings {
	public static String initialisation =
		"The level set requires an initialisation. You can either specify this as one or more ROIs on the image, one of the following auto-thresholding methods, or a previously opened binary image.";

	public static String levelSetParameters =
		"The level set works by starting from the initialisation and iteratively growing/shrinking the boundary. If the initialisation is quite far from the actual boundary then increase 'Iterations' and/or 'Speed sub iterations'. If the boundary is too jagged then increase 'Smooth sub-iterations' and/or decrease 'Speed sub-iterations' to vary the smoothness of the segmentation boundary, and vice-versa. The greater the number of iterations the longer this algorithm will take to run.";

	/**
	 * Insert line-breaks at spaces or hyphens to ensure each line of text is
	 * no longer than the specified width
	 * @param s The string to be formatted
	 * @param w The maximum line length in characters
	 * @return The string with line breaks added
	 */
	public static String format(String s, int w) {
		int p = 0;
		int q = 0;

		StringBuilder sb = new StringBuilder();

		while (p < s.length()) {
			// space at line end can be discarded, - can't
			if (p + w >= s.length()) {
				q = s.length();
			}
			else {
				q = Math.max(s.lastIndexOf(' ', p + w),
							 s.lastIndexOf('-', p + w - 1));
				if (q <= p) {
					// Continuous text longer than w, so force break
					q = p + w;
				}
			}

			sb.append(s.substring(p, q) + "\n");
			if (q < s.length() && s.charAt(q) == ' ') {
				p = q + 1;
			}
			else {
				p = q;
			}
		}

		return sb.toString();
	}
}

