package CT_Tools;

import java.awt.Color;
import java.awt.Font;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.LutLoader;
import ij.plugin.PlugIn;
import jhd.MuMassCalculator.*;
import tagTools.TagListImageTools;
import tagTools.TagListTools;
import tagTools.TagListTools.TagSet;

/**Computes the effective x-ray energy at each image pixel using <br>
 * the reconstructed linear attenuation and known composition(tag image)<br>
 * Images must be the same size.
 */
public class Linear_Attenuation_To_Eeff implements PlugIn {

	GenericDialog gd;
	MuMassCalculator mmc = new MuMassCalculator();
	TagListTools matlTags = new TagListTools();
	TagListImageTools matlImgTools = new TagListImageTools();
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	@Override
	public void run(String arg) {
		doDialog();
		if(gd.wasOKed()) doRoutine();
	}
	
	private void doRoutine()
	{
		ImagePlus dataImp, modelImp;
		String dataImageName, modelImageName;
		TagSet myTagSet;	
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		
		//myTagSet = mlt.loadTagFile(path);
		myTagSet = matlTags.readTagSetFile(path);
		if(myTagSet==null)
		{
			//IJ.error("Linear_Attenuation_To_Eeff Error", "Unable to locate the Tag file " + path);
			IJ.error("Unable to load/create plugins/DialogData/DefaultMaterials.csv");
			return;
		}

		dataImageName = gd.getNextChoice();
		modelImageName   = gd.getNextChoice();

		dataImp = WindowManager.getImage(dataImageName);
		modelImp = WindowManager.getImage(modelImageName);

		if(!(	dataImp.getWidth()	== modelImp.getWidth() && 
				dataImp.getHeight()	== modelImp.getHeight() &&
				dataImp.getBitDepth() == 32 && modelImp.getBitDepth()==32))
		{
			IJ.error("Linear_Attenuation_To_Eeff Error","CT and Tag images must be 32 bit and the same size.");			
			return;
		}

		//CT_Recon images are shifted -1 pixel wrt tagimage
		IJ.run(modelImp, "Translate...", "x=1 y=0 interpolation=None");

		float [] dataPix =(float[]) dataImp.getProcessor().getPixels();
		float [] modelPix =(float[]) modelImp.getProcessor().getPixels();
		float[] effPix = matlImgTools.muLinandTagsToMevImage(dataPix,modelPix,myTagSet);
		
		if(effPix!=null)
		{
			//convert from MeV to keV
			for(int i=0;i<effPix.length;i++) effPix[i]*=1000;
			ImagePlus effImp = dataImp.duplicate();
			effImp.getProcessor().setPixels(effPix);			
			effImp.setTitle("Effective X-ray Energy (keV)");
			//undo model x translation
			IJ.run(modelImp, "Translate...", "x=-1 y=0 interpolation=None");
			//suppress noise
			IJ.run(effImp, "Median...", "radius=0.5");		
			effImp.getProcessor().setMinAndMax(effImp.getStatistics().min, effImp.getStatistics().max);
			effImp.show();
			new LutLoader().run("grays");
		}
		else
		{
			IJ.error("an error occurred");
		}
	}

	//***************************************************************

	private void doDialog()
	{
		
		gd = new GenericDialog("Effective Energy Mapper");
		int winCnt = WindowManager.getImageCount();
		if(winCnt<2)
		{
			IJ.error("This plugin requires a 32-bit CT slice and a 23bit \"tag image\" model slice");
			return;
		}
		String[] winTitles = WindowManager.getImageTitles();
		
		gd.addMessage("Convert Linear Attenuations to effective energy",myFont);
		gd.addChoice("CT Slice:", winTitles, winTitles[0]);
		gd.addChoice("Tag Image:", winTitles, winTitles[0]);
		gd.setBackground(myColor);
		gd.showDialog();
	}
	

}
