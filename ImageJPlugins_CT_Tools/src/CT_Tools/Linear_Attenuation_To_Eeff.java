package CT_Tools;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.LutLoader;
import ij.plugin.PlugIn;
import ij.process.ImageStatistics;
import jhd.MuMassCalculator.*;
import tagTools.TagListImageTools;
import tagTools.TagListTools;
import tagTools.TagListTools.TagSet;

/**
 * Computes the effective x-ray energy at each image pixel using <br>
 * the reconstructed linear attenuation and known composition(tag image)<br>
 * Images must be the same size.
 */
public class Linear_Attenuation_To_Eeff implements PlugIn {

	class tagList {
		double[] mevArr;
		double[] muLinArr;
	}
	
	GenericDialog gd;
	MuMassCalculator mmc = new MuMassCalculator();
	TagListTools matlTags = new TagListTools();
	TagListImageTools matlImgTools = new TagListImageTools();
	final Color myColor = new Color(240, 230, 190);// slightly darker than buff

	final Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	@Override
	public void run(String arg) {
		doDialog();
		if (gd.wasOKed())
			doRoutine();
	}

	private void doDialog() {
		gd = new GenericDialog("Effective Energy Mapper");
		int winCnt = WindowManager.getImageCount();
		if (winCnt < 2) {
			IJ.error("This plugin requires a 32-bit CT slice and a 23bit \"tag image\" model slice");
			return;
		}
		String[] winTitles = WindowManager.getImageTitles();

		gd.addMessage("Convert Linear Attenuations to effective energy", myFont);
		gd.addChoice("CT Slice:", winTitles, winTitles[0]);
		gd.addChoice("Tag Image:", winTitles, winTitles[0]);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.showDialog();
	}

	private void doRoutine() {
		ImagePlus dataImp, tagImp;
		String dataImageName, tagImageName;
		TagSet myTagSet;
		
		//check for DefaultMaterials.csv
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		myTagSet = matlTags.readTagSetFile(path);
		if (myTagSet == null) {
			IJ.error("Unable to load/create plugins/DialogData/DefaultMaterials.csv");
			return;
		}
		//check that the data and tag images are the same size
		dataImageName = gd.getNextChoice();
		tagImageName = gd.getNextChoice();
		dataImp = WindowManager.getImage(dataImageName);
		tagImp = WindowManager.getImage(tagImageName);
		if (!(dataImp.getWidth() == tagImp.getWidth() && dataImp.getHeight() == tagImp.getHeight()
				&& dataImp.getBitDepth() == 32 && tagImp.getBitDepth() == 32)) {
			IJ.error("Linear_Attenuation_To_Eeff Error", "CT and Tag images must be 32 bit and the same size.");
			return;
		}
		//get the data and tag value arrays
		float[] dataPix = (float[]) dataImp.getProcessor().getPixels();
		float[] tagPix = (float[]) tagImp.getProcessor().getPixels();

		// Create list of tags in image
		ArrayList<Integer> tagArr = new ArrayList<Integer>();
		for (int i = 0; i < tagPix.length; i++) {
			if (!tagArr.contains((int) tagPix[i])) {
				tagArr.add((int) tagPix[i]);
			}
		}
		
		String formula;
		double muMass,gmPerCC;
		//pre-compute the meV and muLin data tables
		//create a data structure to hold the meV and muLin arrays
		ArrayList<tagList> myTags = new ArrayList<tagList>();
		//for each tag, create the meV and muLin arrays
		for (Integer tag : tagArr) {
			tagList tagData = new tagList();
			//get the tag's density to convert muMass to muLin
			gmPerCC = matlTags.getTagMatlGmPerCC(tag, myTagSet);
			//get the meV list for the tag's formula
			formula = matlTags.getTagMatlFormula(tag, myTagSet);
			tagData.mevArr = mmc.getMevArray(formula);						
			//create the array to hold muLin
			tagData.muLinArr = new double[tagData.mevArr.length];
			
			//for each formula, get the muMass and convert it to muLin
			for (int i = 0; i < tagData.muLinArr.length; i++) {
				muMass = mmc.getMuMass(formula, tagData.mevArr[i], "TotAttn");
				tagData.muLinArr[i] = muMass * gmPerCC;
			}
			//add the meV and muLin arrays to the list
			myTags.add(tagData);
		}//done with the pre-compute
		
		//create an array for the effective energy results 
		float[] effPix = new float[dataPix.length];

		for (int i = 0; i < dataPix.length; i++) {
			IJ.showProgress(i, dataPix.length);
			//get the meV and muLin for the tag
			int index = tagArr.indexOf((int) tagPix[i]);
			double[] mevArr = myTags.get(index).mevArr;
			double[] muLinArr = myTags.get(index).muLinArr;
			//get the observed muLin
			double linearCoeff = dataPix[i];
			// Scan the muLin array for the bracketing indices of the linearCoeff
			for (int j = 0; j < mevArr.length - 1; j++) {
				if (Math.abs(mevArr[j + 1] - mevArr[j]) > 0.001) // skip edges
				{
					if (linearCoeff < muLinArr[j] && linearCoeff > muLinArr[j + 1]
							|| linearCoeff < muLinArr[j + 1] && linearCoeff > muLinArr[j]) {
						double muLin1 = muLinArr[j];
						double muLin2 = muLinArr[j + 1];
						double mev1 = mevArr[j];
						double mev2 = mevArr[j + 1];

						effPix[i] = (float) logTerp(muLin1, muLin2, linearCoeff, mev1, mev2);
					}
				}
			}
		}

		// convert from MeV to keV
		for (int i = 0; i < effPix.length; i++) {
			effPix[i] *= 1000;
		}
		ImagePlus effImp = dataImp.duplicate();
		effImp.getProcessor().setPixels(effPix);
		effImp.setTitle("Effective X-ray Energy (keV)");
		ImageStatistics stats = effImp.getStatistics();
		effImp.getProcessor().setMinAndMax(stats.mean - stats.stdDev, stats.mean + stats.stdDev);
		effImp.show();
		new LutLoader().run("grays");
	}

	/**
	 * @param meV1    value less than newMev
	 * @param meV2    value greater than newMev
	 * @param newMeV  value for which newMuMass is sought
	 * @param muMass1 value at mev1
	 * @param muMass2 value at mev2
	 * @return the log-log interpolated newMuMass
	 */
	private double logTerp(double meV1, double meV2, double newMeV, double muMass1, double muMass2) {
		double logMeV1, logMeV2, logNewMeV, logMuMass1, logMuMass2;
		double m, diff;

		logMeV1 = Math.log(meV1);
		logMeV2 = Math.log(meV2);
		logNewMeV = Math.log(newMeV);
		logMuMass1 = Math.log(muMass1);
		logMuMass2 = Math.log(muMass2);
		m = (logMeV2 - logNewMeV) / (logMeV2 - logMeV1);
		diff = logMuMass2 - logMuMass1;
		return Math.exp(logMuMass2 - m * diff);
	}

}
