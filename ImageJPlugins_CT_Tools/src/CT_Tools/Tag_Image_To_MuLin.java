package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import jhd.MuMassCalculator.MuMassCalculator;
import jhd.TagTools.*;

import java.util.Vector;
import java.awt.*;

/** A plugin for converting tag images to linear attenuation*/
public class Tag_Image_To_MuLin implements PlugInFilter
{
	MuMassCalculator mmc = new MuMassCalculator();
	MatlListTools mlt = new MatlListTools();
	MatlListTools.TagSet myTagSet;
	String[] matlNames;
	double keV=100;
	
	GenericDialog gd;
	Calibration cal;
	ImagePlus imp;
	ImageProcessor ip;
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	
	//***********************************************************************************

	@Override
	public int setup(String arg, ImagePlus imp)
	{
		this.imp = imp;
		return DOES_32;
	}

	//***********************************************************************************

	@Override
	public void run(ImageProcessor ip)
	{
		this.ip = ip;
		cal = imp.getCalibration();
		if(!cal.getUnit().equals("cm"))
		{
			IJ.error("Pixel units must be cm");
			return;
		}
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		myTagSet = mlt.loadTagFile(path);

		//Get names array from TagSet
		matlNames = new String[myTagSet.tagData.size()];
		int i=0;
		for(MatlListTools.TagData td : myTagSet.tagData)
		{
			matlNames[i]= td.matlName;
			i++;
		}
		
		if(myTagSet!=null)
		{
			gd = new GenericDialog("Convert Tags to MuLin");
			gd.addMessage("Select and energy.\nClick \"Convert\"",myFont,Color.BLACK);
			gd.addNumericField("KeV:", keV);
			gd.addHelp("https://lazzyizzi.github.io/");
			gd.setBackground(myColor);
			gd.showDialog();

			if(gd.wasCanceled())
			{
				return;
			}
			else
			{
				GetSelections();
				ImagePlus tauImp = imp.duplicate();
				tauImp.setTitle("MuLin at " + keV + "KeV");
				for(i=1;i<=tauImp.getNSlices();i++)
				{
					tauImp.setSlice(i);
					float[] tauPix = (float[])tauImp.getProcessor().getPixels();
					mlt.tagsToLinearAttn(tauPix, myTagSet, keV);
				}
				tauImp.show();
				tauImp.getProcessor().setMinAndMax(tauImp.getStatistics().min, tauImp.getStatistics().max);
				tauImp.updateAndDraw();
			}
		}
	}
	
	//***********************************************************************************

	//@SuppressWarnings("unchecked")
	private void GetSelections()
	{
//		Vector<TextField> numbers = gd.getNumericFields();
//		String str= numbers.get(0).getText();
//		if(isNumeric(str))	keV =  Float.valueOf(str);
		keV = gd.getNextNumber();
	}
	public static boolean isNumeric(String str)
	{ 
		try
		{  
			Double.parseDouble(str);  
			return true;
		}
		catch(NumberFormatException e)
		{  
			return false;  
		}  
	}

}
