package CT_Tools;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;

/**
 * DFI_Jtransforms reconstructs tomographic slices from sinograms and sinogram
 * stacks.<br>
 * Dependencies: SinogramUtils to adjust the sinograms, FFTutils to convert data to
 * and from JTransforms sequenced format, and JTransforms-3.1-with-dependencies.jar for the FFTs <br>
 * Download JTransforms-3.1-with-dependencies.jar to the plugins folder for the forward and inverse FTs<br>
 * 
 * @author LazzyIzzi
 * @see <a href=
 *      "https://javadoc.io/doc/com.github.wendykierp/JTransforms/latest/index.html">JTransforms</a>
 * @see SinogramUtils
 * @see JTransformsUtils
 *
 */
public class DFI_JTransforms implements ActionListener, PlugInFilter {
	GenericDialog gd = new GenericDialog("DFI Sinogram Reconstruction");
	DFIutils dfiu = new DFIutils(); // does the reconstruction
	JTransformsUtils jtu = new JTransformsUtils(); //Converts data to-from JTransforms sequenced format
	SinogramUtils su = new SinogramUtils();// Uses ImageJ methods to prepare the sinograms for reconstruction.
	
	class DialogParams {
		int padFactor, interpMethod, extensionWidth;
		String interpChoice, padChoice;
		float beamHardening;
		float axisShift;
		ImagePlus sinoImp;
		boolean showTime, showLUT, showPolarFT, showCartFT, showReconROI;
	}

	ButtonField reconSliceBF;
	ChoiceField sinoCF;

	final Color myColor = new Color(240, 230, 190);// slightly darker than buff
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	ImagePlus sinoImp;

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.sinoImp = imp;
		return DOES_32;
	}

	@Override
	public void run(ImageProcessor ip) {
		if(!jtu.JtransformsJarPresent()) {
			IJ.error(jtu.getJtransformsJarErrorMsg());
			return;
		}
		
		DialogParams dlp = DoDFIdialog();

		if (dlp != null) {
			// Close the test image if present
			ImagePlus testImp = WindowManager.getImage("TestSlice");
			if (testImp != null) {
				testImp.close();
			}

			ImagePlus reconImp = DoDFIrecon(dlp);
			reconImp.show();
			if (dlp.showReconROI == true) {
				int sinoW = dlp.sinoImp.getWidth();
				int x = (reconImp.getWidth() - sinoW) / 2;
				int y = (reconImp.getHeight() - sinoW) / 2;
				reconImp.setRoi(new OvalRoi(x, y, sinoW, sinoW));
			}
			ContrastEnhancer ce = new ContrastEnhancer();
			ce.stretchHistogram(reconImp, 0.35);
			reconImp.updateAndDraw();
		} else {
			return;
		}
	}

	private DialogParams DoDFIdialog() {
		GenericDialogAddin gda = new GenericDialogAddin();
		String msg = "Reconstructs 32-bit parallel-projection 0-180deg sinograms to 2D images."
				+ "\r\nSinogram columns=pixel position, rows=rotation angle with axis at center column."
				+ "\r\nLocal reconstruction uses a linear profile extensions"
				+ "\r\nBeam-hardening applies a second-order polynomial correction";
		gd.addMessage(msg, myFont);
		gd.addSlider("Beam_Hardening:", 0, 1, 0, .01);
		gd.addSlider("Extension Width:", 0, 100, 0, 0);
		gd.addSlider("Axis Shift:", -5, 5, 0, .1);
		gd.addCheckbox("Show_run_time", false);
		gd.addCheckbox("Show_ROI", true);
		gd.addButton("Reconstruct Test Slice", this);
		reconSliceBF = gda.getButtonField(gd, "reconSliceBtn");
		gd.setBackground(myColor);
		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/DFI_Recon.html");
		gd.showDialog();

		if (gd.wasCanceled()) {
			ImagePlus testImp = WindowManager.getImage("TestSlice");
			if (testImp != null) {
				testImp.close();
			}
			return null;
		} else {
			DialogParams dlp = getSelections(gd);
			return dlp;
		}
	}

	private DialogParams getSelections(GenericDialog gd) {
		gd.resetCounters();
		DialogParams dlp = new DialogParams();
		try {
			dlp.beamHardening = (float) gd.getNextNumber();
			dlp.extensionWidth = (int) gd.getNextNumber();
			dlp.axisShift = (float) gd.getNextNumber();
			dlp.showTime = gd.getNextBoolean();
			dlp.showLUT = false;
			dlp.showPolarFT = false;
			dlp.showCartFT = false;
			dlp.showReconROI = gd.getNextBoolean();
			dlp.sinoImp = sinoImp;
			dlp.padFactor = 4;
		} catch (Exception e) {
			IJ.showMessage("Error",
					"To record, the Macro Recorder must be open before\nlaunching the DFI Recon plugin");
			dlp = null;
		}
		return dlp;
	}

	private ImagePlus DoDFIrecon(DialogParams dlp) {

		// Prepare a copy of the sinogram Image(Stack)
		int sinoSliceCnt = dlp.sinoImp.getNSlices();
		// if extensionWidth = 0 then the sinoCols is unchanged
		int sinoCols = dlp.sinoImp.getWidth() + 2 * dlp.extensionWidth;
		double pixelWidth = dlp.sinoImp.getCalibration().pixelWidth;
		String unit = dlp.sinoImp.getCalibration().getUnit();

		// Avoid the ImageJ built-in duplicate methods posting to the Macro Recorder
		ImagePlus paddedImp = su.duplicate(dlp.sinoImp);

		// Apply the sinogram adjustments in this order
		double extTime = 0;
		if (dlp.extensionWidth > 0) {
			extTime = su.profileExtend(paddedImp, dlp.extensionWidth);
		}
		// paddedImp.updateAndDraw();
		double axisTime = 0;
		if (dlp.axisShift != 0) {
			axisTime = su.axisShift(paddedImp, dlp.axisShift);
		}
		// paddedImp.updateAndDraw();
		double bhTime = 0;
		if (dlp.beamHardening > 0) {
			bhTime = su.applyBeamHardeningCorrection(paddedImp, dlp.beamHardening);
		}
		// paddedImp.updateAndDraw();
		double padTime = su.padSinogram(paddedImp, dlp.padFactor);

		// if the number of slices is odd then add another zero slice to the bottom of
		// the stack
		boolean sliceAdded = false;
		if (Math.floorMod(sinoSliceCnt, 2) > 0) {
			paddedImp.getStack().addSlice("TempSlice", paddedImp.getProcessor(), sinoSliceCnt);
			sinoSliceCnt++;
			sliceAdded = true;
		}
		int sinoPairs = sinoSliceCnt / 2;

		// Create the polar To Cartesian Lookup table
		// If dfp.semiBicubicLUT = null, DFIrecon will build the LUT each time it is
		// called
		CT_Tools.DFIutils.DFIparams dfiParams = dfiu.new DFIparams();
		dfiParams.paddedSinoWidth = paddedImp.getWidth();
		dfiParams.semiBicubicLUT = dfiu.makeSemiBicubicLUT(dfiParams.paddedSinoWidth, paddedImp.getHeight(), dlp.padFactor);

		// Pass the debug options
		dfiParams.showLUT = dlp.showLUT;
		dfiParams.showPolarFT = dlp.showPolarFT;
		dfiParams.showCartFT = dlp.showCartFT;

		// Create an image to hold the reconstructed images
		ImagePlus reconImp = NewImage.createFloatImage("Recon", sinoCols, sinoCols, sinoSliceCnt, NewImage.FILL_BLACK);
		ImageStack reconStk = reconImp.getStack();

		long start = System.nanoTime();
		// reconstruct the sinogram stack two slices at a time
		float[] dfiData;
		for (int slice = 1; slice <= sinoPairs * 2; slice += 2) {
			IJ.showProgress(slice, sinoPairs);

			// Fetch pairs of sinograms from the padded image
			paddedImp.setSlice(slice);
			dfiParams.paddedSino1 = (float[]) paddedImp.getProcessor().getPixels();
			paddedImp.setSlice(slice + 1);
			dfiParams.paddedSino2 = (float[]) paddedImp.getProcessor().getPixels();
			dfiParams.padFactor = dlp.padFactor;

			// dfiRecon returns both slices in JTransforms sequence format
			// because it can't return two separate arrays
			dfiData = dfiu.dfiRecon(dfiParams);

			// Convert 1/pixel to 1/cm
			for (int i = 0; i < dfiData.length; i++) {
				dfiData[i] /= pixelWidth;
			}

			// put the separated images into the reconstructed image stack slices
			reconStk.setPixels(jtu.getJTransformsReal(dfiData), slice);
			reconStk.setPixels(jtu.getJTransformsImaginary(dfiData), slice + 1);
		}

		if (sliceAdded == true) {
			// deleteLastSlice fails when there are 2 slices in the stack
			reconStk.deleteLastSlice();
			// Below is the workaround for a bug in deleteLastSlice which should do what it
			// says without further intervention
			reconImp.setStack(reconImp.getStack());
		}

		long end = System.nanoTime();
		double reconTime = (end - start) / 1e9;

		reconImp.getCalibration().pixelWidth = pixelWidth;
		reconImp.getCalibration().pixelHeight = pixelWidth;
		reconImp.getCalibration().setUnit(unit);
		String title = dlp.sinoImp.getTitle();
		if (title.endsWith(".tif")) {
			title = title.replace(".tif", "_Recon.tif");
		}
		else {
			title = title + "_Recon";
		}
		reconImp.setTitle(title);

		if (dlp.showTime) {
			IJ.log("Sinogram width =" + dlp.sinoImp.getWidth());
			IJ.log("Sinogram height =" + dlp.sinoImp.getHeight());
			IJ.log("Sinogram padding =" + dlp.padFactor);
			IJ.log("Beam Hardening =" + bhTime + "Sec");
			IJ.log("Profile Extension =" + extTime + "Sec");
			IJ.log("AxisShift =" + axisTime + "Sec");
			IJ.log("Padding =" + padTime + "Sec");
			IJ.log("Reconstruction =" + reconTime + "Sec");
			IJ.log("Reconstruction =" + reconTime / dlp.sinoImp.getNSlices() + "Sec/Slice");

			double totTime = bhTime + padTime + reconTime + extTime + axisTime;
			IJ.log("Total Time =" + totTime + "Sec");
			IJ.log("Total Time =" + totTime / dlp.sinoImp.getNSlices() + "Sec/Slice");
		}
		if (reconImp != null) {
			setImageProperties(dlp.sinoImp, reconImp, dlp);
		}
		paddedImp.close();
		System.gc();
		return reconImp;
	}

	private void setImageProperties(ImagePlus srcImp, ImagePlus destImp, DialogParams dlp) {
		Properties srcProps = srcImp.getImageProperties();
		ArrayList<String> destProps = new ArrayList<String>();
		if (srcProps != null) {
			// Copy the projector properties
			Enumeration<Object> srcKey = srcProps.keys();
			Enumeration<Object> srcElem = srcProps.elements();
			while (srcKey.hasMoreElements()) {
				destProps.add((String) srcKey.nextElement());
				destProps.add((String) srcElem.nextElement());
			}
		}
		destProps.add("Sinogram");
		destProps.add(srcImp.getTitle());
		destProps.add("Reconstructor");
		destProps.add("DFI_Recon_Simplified");
		destProps.add("Pad Factor");
		destProps.add(String.valueOf(dlp.padFactor));
		destProps.add("Interpolation");
		destProps.add("SemiBicubic");
		destProps.add("Cartesian Filter");
		destProps.add("Rect");
		destProps.add("Cartesian Cutoff");
		destProps.add("1");
		destProps.add("Axis Shift");
		destProps.add(String.valueOf(dlp.axisShift));
		destProps.add("Beam Hardening");
		destProps.add(String.valueOf(dlp.beamHardening));
		destProps.add("Extension Shape");
		destProps.add("Linear");
		destProps.add("Extension Width");
		destProps.add(String.valueOf(dlp.extensionWidth));
		destProps.add("Fix Rays");
		destProps.add("Not Supported");
		destProps.add("Fix Rings");
		destProps.add("Not Supported");
		destProps.add("SumRule");
		destProps.add("Not Supported");
		String[] destPropsArr = destProps.toArray(new String[destProps.size()]);

		destImp.setProperties(destPropsArr);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e != null) {
			Object src = e.getSource();
			if (src instanceof Button) {
				Button btn = (Button) src;
				String name = btn.getName();
				switch (name) {
				case "reconSliceBtn":
					DialogParams dlp = getSelections(gd);
					dlp.sinoImp = dlp.sinoImp.crop("whole-slice");
					ImagePlus reconImp = DoDFIrecon(dlp);
					ImagePlus testImp;
					reconImp.setTitle("TestSlice");
					testImp = WindowManager.getImage("TestSlice");
					if (testImp == null) {
						testImp = NewImage.createFloatImage("TestSlice", reconImp.getWidth(), reconImp.getHeight(), 2,
								NewImage.FILL_BLACK);
					}
					testImp.setStack(reconImp.getStack());
					testImp.show();
					WindowManager.setTempCurrentImage(testImp);
					// put the result next to the dialog box
					int dlogW = gd.getSize().width;
					int dlogL = gd.getLocation().x;
					int dlogT = gd.getLocation().y;
					testImp.getWindow().setLocation(dlogL + dlogW, dlogT);
					// Create an ROI around valid reconstruction data
					if (dlp.showReconROI == true) {
						int sinoW = dlp.sinoImp.getWidth();
						int x = (testImp.getWidth() - sinoW) / 2;
						int y = (testImp.getHeight() - sinoW) / 2;
						testImp.setRoi(new OvalRoi(x, y, sinoW, sinoW));
					}
					ContrastEnhancer ce = new ContrastEnhancer();
					ce.stretchHistogram(testImp, 0.35);

					break;
				}
			}
		}
	}
}
