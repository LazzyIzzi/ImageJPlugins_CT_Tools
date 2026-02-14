package CT_Tools;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import CT_Tools.DFIutils.DFIparams;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GUI;
//import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.plugin.ContrastEnhancer;
//import ij.plugin.Duplicator;
import ij.plugin.filter.PlugInFilter;
//import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;
import ij.ImageListener;

import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;

/**
 * DFI_Jtransforms reconstructs tomographic slices from sinograms and sinogram
 * stacks.<br>
 * It uses SinogramUtils to adjust the sinograms and FFTutils to convert data to
 * and from JTransforms sequenced format and run the DFI algorithm: <br>
 * ImageJ methods are used to display the results.
 * 
 * @author LazzyIzzi
 * @see SinogramUtils
 * @see JTransformsUtils
 *
 */
public class DFI_JTransformsFancyUI implements ActionListener, PlugInFilter, ImageListener {

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

	@Override
	public int setup(String arg, ImagePlus imp) {
		return DOES_32;
	}

	@Override
	public void run(ImageProcessor ip) {
		ImagePlus.addImageListener(this);

		DialogParams dlp = DoDFIdialog();

		if (dlp != null) {
			// Close the test image
//			ImagePlus testImp = WindowManager.getImage("TestSlice");
//			if(testImp!=null) {
//				testImp.close();
//			}
			if (dlp.sinoImp == null) {
				return;
			} else {
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
			}
		} else {
			return;
		}
	}

	GenericDialog gd = GUI.newNonBlockingDialog("Direct Fourier Reconstruction");
	// GenericDialog gd = new GenericDialog("DFI Debug 2");
	JTransformsUtils ftu = new JTransformsUtils();
	DFIutils dfiu = new DFIutils();
	SinogramUtils su = new SinogramUtils();
//	DebugUtils dbu = new DebugUtils();

	private DialogParams DoDFIdialog() {
		int winNum;

		GenericDialogAddin gda = new GenericDialogAddin();
		String[] winTitles = WindowManager.getImageTitles();
		winTitles = filterImageTitles(winTitles);
		String curTitle = WindowManager.getCurrentImage().getTitle();
		for (winNum = 0; winNum < winTitles.length; winNum++) {
			if (winTitles[winNum].equals(curTitle))
				break;
		}
		String msg = "Reconstructs 32-bit parallel-projection 0-180deg sinograms to 2D images." +
				"\r\nSinogram columns=pixel position, rows=rotation angle with axis at center column." +
				"\r\nLocal reconstruction uses a linear profile extensions" +
				"\r\nBeam-hardening applies a second-order polynomial correction";
		gd.addMessage(msg, myFont);
		gd.addChoice("Sinogram Window:", winTitles, winTitles[winNum]);
		sinoCF = gda.getChoiceField(gd, null, "sinogram");
		gd.addSlider("Beam_Hardening:", 0, 1, 0, .01);
		gd.addSlider("Extension Width:", 0, 100, 0, 0);
		gd.addSlider("Axis Shift:", -5, 5, 0, .1);
		gd.addCheckbox("Show_run_time", false);
//		gd.addCheckbox("Debug Lookup Table", false);
//		gd.addCheckbox("Debug Padded Sino FTs", false);
//		gd.addCheckbox("Debug Cartesian FT", false);
		gd.addCheckbox("Show_ROI", true);
		gd.addButton("Reconstruct Test Slice", this);
		reconSliceBF = gda.getButtonField(gd, "reconSliceBtn");
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));

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
//			dlp.showLUT = gd.getNextBoolean();
//			dlp.showPolarFT = gd.getNextBoolean();
//			dlp.showCartFT = gd.getNextBoolean();
			dlp.showLUT = false;
			dlp.showPolarFT = false;
			dlp.showCartFT = false;
			dlp.showReconROI = gd.getNextBoolean();
			dlp.sinoImp = WindowManager.getImage(gd.getNextChoice());
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
		DFIparams dfp = dfiu.new DFIparams();
		dfp.paddedSinoWidth = paddedImp.getWidth();
		dfp.semiBicubicLUT = dfiu.makeSemiBicubicLUT(dfp.paddedSinoWidth, paddedImp.getHeight(), dlp.padFactor);

		// Pass the debug options
		dfp.showLUT = dlp.showLUT;
		dfp.showPolarFT = dlp.showPolarFT;
		dfp.showCartFT = dlp.showCartFT;

		// Create an image to hold the reconstructed images
		// ImagePlus reconImp = IJ.createImage("Recon", sinoCols, sinoCols,
		// sinoSliceCnt, 32);
		ImagePlus reconImp = NewImage.createFloatImage("Recon", sinoCols, sinoCols, sinoSliceCnt, NewImage.FILL_BLACK);

		long start = System.nanoTime();
		// reconstruct the sinogram stack two slices at a time
		float[] dfiData;
		for (int slice = 1; slice <= sinoPairs * 2; slice += 2) {
			IJ.showProgress(slice, sinoPairs);

			// Fetch pairs of sinograms from the padded image
			paddedImp.setSlice(slice);
			dfp.paddedSino1 = (float[]) paddedImp.getProcessor().getPixels();
			paddedImp.setSlice(slice + 1);
			dfp.paddedSino2 = (float[]) paddedImp.getProcessor().getPixels();
			dfp.padFactor = dlp.padFactor;

			// dfiRecon returns both slices in JTransforms sequence format
			// because it can't return two separate arrays
			dfiData = dfiu.dfiRecon(dfp);

			// Convert 1/pixel to 1/cm
			for (int i = 0; i < dfiData.length; i++) {
				dfiData[i] /= pixelWidth;
			}

			// put the separated images into the reconstructed image stack slices
			ImageStack reconStk = reconImp.getStack();
			reconStk.setPixels(ftu.getJTransformsReal(dfiData), slice);
			reconStk.setPixels(ftu.getJTransformsImaginary(dfiData), slice + 1);
		}

		if (sliceAdded == true) {
			// deleteLastSlice fails when there are 2 slices in the stack
			reconImp.getStack().deleteLastSlice();
			// Below is a workaround for a bug in deleteLastSlice which should do what it
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
					if(dlp.sinoImp == null) {
						IJ.error("Please select a sinogram to reconstruct.");
						return;
					}
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
						testImp.setRoi(new OvalRoi(x, y, sinoW, sinoW),true);
						testImp.updateAndDraw();
					}
					ContrastEnhancer ce = new ContrastEnhancer();
					ce.stretchHistogram(testImp, 0.35);
					break;
				}
			}
		}
	}
	
	private String[] filterImageTitles(String[] imageTitles) {
		ArrayList<String> okNames = new ArrayList<String>();
		boolean nameOK;
		// Don't load recon slices or TestSlices
		// run.pluginFilter returns DOES_32 so at least one 32-bit image is initially present 
		String[] badNames = {"TestSlice","Recon"}; //,".png",".bmp",".gif",".pgm"};
		for(int i =0;i<imageTitles.length;i++) {
			nameOK = true;
			for(int j=0;j<badNames.length;j++) {
				if(imageTitles[i].contains(badNames[j])) {
					nameOK = false;
					continue;
				}
			}
			if(nameOK) {
				ImagePlus imp = WindowManager.getImage(imageTitles[i]);
				if(imp.getBitDepth() == 32) {
					okNames.add(imageTitles[i]);
				}
			}			
		}		
		String[] filteredTitles = new String[okNames.size()];		
		okNames.toArray(filteredTitles);
		return filteredTitles;
	}

	private void rebuildList(String[] titles) {
		if (IJ.isMacro()) {
			return;
		} else {
			Choice sinoChoice = sinoCF.getChoice();
			String curSinoTitle = sinoChoice.getSelectedItem();
			sinoChoice.removeAll();
			if (titles.length > 0) {
				// rebuild the list with the current selection on top
				if (curSinoTitle != null)
					sinoChoice.add(curSinoTitle);
				for (int i = 0; i < titles.length; i++) {
					if (!titles[i].equals(curSinoTitle)) {
						sinoChoice.add(titles[i]);
					}
				}
				sinoChoice.select(curSinoTitle);
			}
			sinoChoice.setBounds(sinoChoice.getX(),sinoChoice.getY(),350,sinoChoice.getHeight());

		}

		

	}

	@Override
	public void imageOpened(ImagePlus imp) {
		String[] titles = filterImageTitles(WindowManager.getImageTitles());
		//if(titles!=null)	
			rebuildList(titles);
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		String[] titles = filterImageTitles(WindowManager.getImageTitles());
		//if(titles!=null)	
			rebuildList(titles);
	}
	@Override
	public void imageUpdated(ImagePlus imp) {
		// IJ.log(imp.getTitle() + " Updated");

	}
}
