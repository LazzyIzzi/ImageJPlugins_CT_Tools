package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.LutLoader;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.MuMassCalculator.MuMassCalculator;
import jhd.TagTools.*;

import java.awt.*;

/** A plugin for converting tag images to linear attenuation*/
public class Tag_Image_To_MuLin implements PlugInFilter, DialogListener
{
	MuMassCalculator mmc = new MuMassCalculator();
	MatlListTools mlt = new MatlListTools();
	MatlListTools.TagSet myTagSet;
	String[] matlNames;
	double keV=100;
	
	GenericDialog gd;
	GenericDialogAddin gda;
	NumericField keVNF;

	Calibration cal;
	ImagePlus imp;
	ImageProcessor ip;
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Color errColor = new Color(255,100,0);
	final Color white = new Color(255,255,255);
	
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
		if(myTagSet==null)
		{
			IJ.error("The tagSet at " + path + " failed to load.");
		}
		
		else
		{
			//Get names array from TagSet
			matlNames = mlt.getTagSetMatlNamesAsArray(myTagSet);
			gda = new GenericDialogAddin();
			
			gd = new GenericDialog("Convert Tags to MuLin");
			gd.addDialogListener(this);
			
			gd.addMessage("Enter an energy between\n1 and 10^9 keV.\nClick \"OK\"",myFont,Color.BLACK);
			gd.addNumericField("KeV:", keV);
			keVNF = gda.getNumericField(gd, null, "keV");
			gd.addHelp("https://lazzyizzi.github.io/TagsToMuLin.html");
			gd.setBackground(myColor);
			gd.showDialog();

			if(gd.wasCanceled())
			{
				return;
			}
			else
			{
				getSelections();
				if(Double.isNaN(keV) || keV < 1 || keV >1e9)
				{
					IJ.error("keV input error","Energy range 1 keV to 100 GeV");
					return;
				}
				ImagePlus tauImp = imp.duplicate();
				String title = imp.getTitle();
				if(title.endsWith(".tif"))
				{
					title = title.replace(".tif", "MuLin at " + keV + "KeV.tif");
				}
				else
				{
					title += "MuLin at " + keV + "KeV";
				}			
						
				title = WindowManager.getUniqueName(title);
				tauImp.setTitle(title);
				for(int i=1;i<=tauImp.getNSlices();i++)
				{
					tauImp.setSlice(i);
					float[] tauPix = (float[])tauImp.getProcessor().getPixels();
					mlt.tagsToLinearAttn(tauPix, myTagSet, keV);
				}
				
				tauImp.show();
				//IJ.run(tauImp, "Grays", "");
				//LutLoader lutLoader = new LutLoader();
				//lutLoader.run("grays");
				new LutLoader().run("grays");
				tauImp.getProcessor().setMinAndMax(tauImp.getStatistics().min, tauImp.getStatistics().max);
				tauImp.updateAndDraw();
			}
		}
	}
	
	//***********************************************************************************

	//@SuppressWarnings("unchecked")
	private void getSelections()
	{
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

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK=true;
		if(e!=null)
		{
			Object src = e.getSource();
			if(src instanceof TextField)
			{
				TextField tf = (TextField)src;
				String name = tf.getName();
				switch(name)
				{
				case "keV":
					String keVStr = tf.getText();
					if(!isNumeric(keVStr)) dialogOK=false;
					else
					{
						double keV = Double.valueOf(keVStr);
						if(keV<1 || keV > 1e9) dialogOK=false;
					}
					break;
				}
				if(!dialogOK) tf.setBackground(errColor);
				else tf.setBackground(white);
			}
		}
		getSelections();
		return dialogOK;
	}

}
