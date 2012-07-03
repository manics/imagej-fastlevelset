package ijfls.measure;

import ij.IJ;
import ij.ImagePlus;
import ij.process.*;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;


/**
 * Get measurements from a segmented image stack
 */
public class MeasureSegmentation {
	/**
	 * Parameters for the fitted ellipse
	 */
	public class Ellipse {
		/**
		 * Angle of major axis from positive x-axis in degrees
		 * (anti-clockwise in image coordinates)
		 */
		public double angle;

		/**
		 * Length of major axis
		 */
		public double majorAxis;

		/**
		 * Length of minor axis
		 */
		public double minorAxis;

		/**
		 * X-coordinate of centre
		 */
		public double cx;

		/**
		 * Y-coordinate of centre
		 */
		public double cy;

		/**
		 * Create an object using the parameters calculated by the EllipseFitter
		 * @param ef The completed EllipseFitter
		 */
		public Ellipse(EllipseFitter ef) {
			angle = ef.angle;
			majorAxis = ef.major;
			minorAxis = ef.minor;
			cx = ef.xCenter;
			cy = ef.yCenter;
		}

		/**
		 * Return these ellipse parameters as a string
		 * @return string containing ellipse parameters
		 */
		public String toString() {
			return "angle: " + IJ.d2s(angle) +
				" majorAxis: " + IJ.d2s(majorAxis) +
				" minorAxis: " + IJ.d2s(minorAxis) +
				" centre: (" + IJ.d2s(cx) + "," + IJ.d2s(cy) + ")";
		}
	}

	/**
	 * Convert a segmentation into an ROI
	 * @param im The segmentation image
	 * @return the ROI
	 */
	public Roi segmentationToRoi(ImageProcessor im) {
		// Based on lines 68-76 in
		// https://github.com/dscho/fiji/blob/187bff510842e7c17a7dcf8e99c51e3a39089248/src-plugins/VIB_/vib/Local_Threshold.java
		im.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		ImagePlus tmp = new ImagePlus("", im);

		ThresholdToSelection tts = new ThresholdToSelection();
		tts.setup("", tmp);
		tts.run(im);

		im.resetThreshold();
		Roi roi = tmp.getRoi();
		return roi;
	}

	/**
	 * Fit an ellipse to an ROI and get measurements
	 * @param seg The segmentation image
	 * @return the fitted ellipse parameters
	 */
	public Ellipse fitEllipse(ImageProcessor seg) {
		EllipseFitter ef = new EllipseFitter();

		seg.setMask(seg);
		ef.fit(seg, seg.getStatistics());
		ef.drawEllipse(seg);
		seg.setMask(null);

		return new Ellipse(ef);
	}

	public class ProjectedSegmentation {
		public class Projections {
			double[] longAxis;
			double[] shortAxis;
			double total;
		}

		public Projections max = new Projections();
		public Projections mean = new Projections();
		public Projections sum = new Projections();
	}

	/**
	 * Get projections from a segmentation after aligning using a fitted
	 * ellipse
	 */
	public ProjectedSegmentation getProjections(Roi roi, ImageProcessor im) {
		ProjectedSegmentation proj = new ProjectedSegmentation();
		return proj;
	}

}


