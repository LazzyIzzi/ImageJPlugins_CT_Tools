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

/**Displays the difference of linear attenuation between<br>
 * a reconstructed slice and a model slice calculated at a <br>
 * given X-ray energy from a tag image.
 * TODO replace with simple macro.
 */
public class Linear_Attenuation_Error implements PlugIn {

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
		double keV;
		TagSet myTagSet;	
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		
		//myTagSet = mlt.loadTagFile(path);
		myTagSet = matlTags.readTagSetFile(path);
		if(myTagSet==null)
		{
			IJ.error("Linear_Attenuation_To_Eeff Error", "Unable to locate the Tag file " + path);
			return;
		}

		dataImageName = gd.getNextChoice();
		modelImageName   = gd.getNextChoice();
		keV=gd.getNextNumber();

		dataImp = WindowManager.getImage(dataImageName);
		modelImp = WindowManager.getImage(modelImageName);

		if(!(	dataImp.getWidth()	== modelImp.getWidth() && 
				dataImp.getHeight()	== modelImp.getHeight() &&
				dataImp.getBitDepth() == 32 && modelImp.getBitDepth()==32))
		{
			IJ.error("Linear_Attenuation_Error Error","CT and Tag images must be 32 bit and the same size.");			
			return;
		}

		//CT_Recon images are shifted -1 pixel wrt tagimage
		IJ.run(modelImp, "Translate...", "x=1 y=0 interpolation=None");

		float [] dataPix =(float[]) dataImp.getProcessor().getPixels();
		//float [] modelPix =(float[]) modelImp.getProcessor().getPixels();
		ImagePlus errImp = modelImp.duplicate();
		float[] errPix = (float[]) errImp.getProcessor().getPixels();
		boolean ok = matlImgTools.tagsToLinearAttn(errPix, myTagSet, keV);

		if(ok)		
		{
			for(int i =0;i<errPix.length;i++)
			{
				errPix[i] = dataPix[i]-errPix[i];
			}
			errImp.setTitle("Attenuation error (cm-1) at "+keV+"keV");
			//undo model x translation
			IJ.run(modelImp, "Translate...", "x=-1 y=0 interpolation=None");
			//suppress noise
			IJ.run(errImp, "Median...", "radius=0.5");		
			errImp.getProcessor().setMinAndMax(errImp.getStatistics().min, errImp.getStatistics().max);
			errImp.show();
			new LutLoader().run("grays");
		}
		else
		{
			IJ.error("Linear_Attenuation_Error Error","Unknown error");			
			
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
		gd.addNumericField("keV", 100);
		gd.setBackground(myColor);
		gd.showDialog();
	}
	

}
