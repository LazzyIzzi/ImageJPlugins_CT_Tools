package CT_Tools;

import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Font;
import java.awt.TextField;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageStatistics;

import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.MessageField;
import jhd.ImageJAddins.GenericDialogAddin.NumericField;
import jhd.ImageJAddins.GenericDialogAddin.SpinnerPanel;

import jhd.MuMassCalculator.*;
import tagTools.TagListImageTools;
import tagTools.TagListTools;
import tagTools.TagListTools.TagSet;

/**
 * Displays the difference of linear attenuation between<br>
 * a reconstructed slice and a model slice calculated at a <br>
 * given X-ray energy from a tag image.
 */
public class Linear_Attenuation_Error implements PlugIn, DialogListener {

	final Color myColor = new Color(240, 230, 190);// slightly darker than buff
	final Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	GenericDialog gd = new GenericDialog("Attenuation Error Mapper");
	// GenericDialog gd= GUI.newNonBlockingDialog("Attenuation Error Mapper");
	GenericDialogAddin gda = new GenericDialogAddin();
	MuMassCalculator mmc = new MuMassCalculator();
	TagListTools matlTags = new TagListTools();
	TagListImageTools matlImgTools = new TagListImageTools();

	TagSet tagSet;
	ImagePlus tagImp, imgImp, errImp;
	String tagName, imgName;
	double keV;
	NumericField kevNF;
	NumericField kevIncNF;
	MessageField statsMF;

	ImageStatistics stats;
	

	@Override
	public void run(String arg) {
		// Create an image to display the result
		// errImp = IJ.createImage("Error Image", 100, 100, 1, 32);
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		tagSet = matlTags.readTagSetFile(path);
		doDialog();
		if (gd.wasOKed())
			setErrImageProps();
	}

	private void doDialog() {
		int winCnt = WindowManager.getImageCount();
		if (winCnt < 2) {
			IJ.error("This plugin requires a 32-bit CT slice and a corresponding 32bit \"tag image\" model slice");
			return;
		}
		String[] winTitles = WindowManager.getImageTitles();

		gd.addMessage("Compute (Slice MuLin)-(Tag MuLin) at selected X-ray energy", myFont);
		gd.addChoice("CT Slice:", winTitles, winTitles[0]);
		gda.getChoiceField(gd, null, null).getLabel().setFont(myFont);
		gd.addChoice("Tag Image:", winTitles, winTitles[0]);
		gda.getChoiceField(gd, null, null).getLabel().setFont(myFont);

		gd.addMessage("Click + or - keV to minimize error", myFont);
		gd.addNumericField("keV", 100);
		kevNF = gda.getNumericField(gd, null, "estKeV");
		kevNF.getLabel().setFont(myFont);
		SpinnerPanel sp = gda.new SpinnerPanel();
		TextField tf = (TextField) gd.getNumericFields().get(0);
		gd.addToSameRow();
		gd.addPanel(sp.addSpinner(tf, "estKev", 5.0));

		gd.addMessage("Stats are saved in ImageInfo", myFont);
		gd.addMessage("Difference mean=? stdDev=?", myFont);
		statsMF = gda.getMessageField(gd, "stats");

		gd.setBackground(myColor);
		gd.addDialogListener(this);
		gd.setOKLabel("Close");
		gd.hideCancelButton();
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.showDialog();

		if (gd.wasOKed()) {
			getSelections();
		}
	}


	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		boolean dialogOK = true;

		// getSelections();

		if (e != null) {
			Object src = e.getSource();
			if (src instanceof TextField) {
				TextField tf = (TextField) src;
				String name = tf.getName();
				switch (name) {
				case "estKev":
					keV = kevNF.getNumber();
					if (Double.isNaN(keV))
						dialogOK = false;
					else if (keV < 1 || keV > 1e9)
						dialogOK = false;
					else {
						getSelections();
						getErrImage();
					}

					break;
				}
			}
			// check sizes when user makes a selection
			if (src instanceof Choice) {
				getSelections();
				if (!(imgImp.getWidth() == tagImp.getWidth() && imgImp.getHeight() == tagImp.getHeight()
						&& imgImp.getBitDepth() == 32 && tagImp.getBitDepth() == 32)) {
					IJ.error("Linear_Attenuation_Error Error", "CT and Tag images must be 32 bit and the same size.");
					dialogOK = false;
					;
				}
			}
		}

		return dialogOK;
	}
	

	private boolean getErrImage() {

		boolean ok = false;
		// check at each run if user has altered imaeg sizes
		if (!(imgImp.getWidth() == tagImp.getWidth() && imgImp.getHeight() == tagImp.getHeight()
				&& imgImp.getBitDepth() == 32 && tagImp.getBitDepth() == 32)) {
			IJ.error("Linear_Attenuation_Error Error", "CT and Tag images must be 32 bit and the same size.");

		} else {
			
			ImageStack errStk=null;
			errImp = WindowManager.getImage("MuLin_Fit_Error");
			if (errImp == null) {
				errImp = IJ.createImage("MuLin_Fit_Error", imgImp.getWidth(), imgImp.getHeight(), 1, 32);
				errImp.show();
			}
			errStk = errImp.getStack();
			float[] tagPix = (float[]) tagImp.getProcessor().getPixels();
			float[] muLinPix = matlImgTools.tagsToLinearAttn2(tagPix, tagSet, keV);
			if (muLinPix != null) {

				float[] imgPix = (float[]) imgImp.getProcessor().getPixels();
				float[] errPix = (float[]) errStk.getProcessor(1).getPixels();
				for (int i = 0; i < muLinPix.length; i++) {
					errPix[i] = imgPix[i] - muLinPix[i];
				}
				
				stats = errStk.getProcessor(1).getStatistics();				
				String kevStr = IJ.d2s(keV, 2);
				String stdDevStr = IJ.d2s(stats.stdDev, 5);
				String meanStr = IJ.d2s(stats.mean, 5);
				statsMF.setLabel("Difference mean=" + meanStr + " stdDev=" + stdDevStr);				
				errStk.setSliceLabel( "keV=" + kevStr + " mean=" + meanStr + " stdDev=" + stdDevStr, 1);				
				errImp.getProcessor().setMinAndMax(stats.mean - 2 * stats.stdDev, stats.mean + 2 * stats.stdDev);				
				errImp.updateAndRepaintWindow();;
				ok = true;
			}
		}
		return ok;
	}

	private void getSelections() {
		gd.resetCounters();
		try {
			imgName = gd.getNextChoice();
			tagName = gd.getNextChoice();
			imgImp = WindowManager.getImage(imgName);
			tagImp = WindowManager.getImage(tagName);
			keV = gd.getNextNumber();
		} catch (Exception e) {
			if (e instanceof NullPointerException) {
				IJ.showMessage("Error",
						"To record, the Macro Recorder must be open before\nlaunching the Linearization Fitter Plugin");
			} else {
				IJ.showMessage(e.getMessage());
			}
		}
	}

	private void setErrImageProps() {
		/*
		 * Properties to set Source slice and tag image file properties Tag file data
		 * for each tag mean and stdDev and keV
		 * 
		 */

		Properties srcProps = imgImp.getImageProperties();
		ArrayList<String> destProps = new ArrayList<String>();
		if (srcProps != null) {
			// Copy the projector properties
			Enumeration<Object> srcKey = srcProps.keys();
			Enumeration<Object> srcElem = srcProps.elements();
			while (srcKey.hasMoreElements()) {
				destProps.add((String) srcKey.nextElement());
				destProps.add((String) srcElem.nextElement());
			}
			destProps.add("Slice Image");
			destProps.add(imgImp.getTitle());
		}

		srcProps = tagImp.getImageProperties();
		if (srcProps != null) {
			// Copy the projector properties
			Enumeration<Object> srcKey = srcProps.keys();
			Enumeration<Object> srcElem = srcProps.elements();
			while (srcKey.hasMoreElements()) {
				destProps.add((String) srcKey.nextElement());
				destProps.add((String) srcElem.nextElement());
			}
			destProps.add("Tag Image");
			destProps.add(tagImp.getTitle());
		}

		destProps.add("Fit keV");
		destProps.add(Double.toString(keV));
		destProps.add("Mean");
		destProps.add(Double.toString(stats.mean));
		destProps.add("StdDev");
		destProps.add(Double.toString(stats.stdDev));

		int[] tags = matlImgTools.getUniqueTags((float[]) tagImp.getProcessor().getPixels());

		String formula, name, gmPerCC;
		for (int i = 0; i < tags.length; i++) {
			formula = matlTags.getTagMatlFormula(tags[i], tagSet);
			name = matlTags.getTagMatlName(tags[i], tagSet);
			gmPerCC = Double.toString(matlTags.getTagMatlGmPerCC(tags[i], tagSet));
			destProps.add("Tag " + tags[i]);
			destProps.add(name + ", " + formula + ", " + gmPerCC);
		}

		String[] destPropsArr = destProps.toArray(new String[destProps.size()]);

		errImp.setProperties(destPropsArr);

//		ImageInfo imageInfo = new ImageInfo();
//		String info = imageInfo.getImageInfo(errImp);
//		IJ.log("ImageInfo\n" + info);
//		info = imageInfo.getExifData(errImp);
//		IJ.log("ExifInfo\n" + info);
//		for(int i =0;i<destPropsArr.length;i+=2) {
//			IJ.log(destPropsArr[i] + ", " + destPropsArr[i+1]);
//		}

	}
}
