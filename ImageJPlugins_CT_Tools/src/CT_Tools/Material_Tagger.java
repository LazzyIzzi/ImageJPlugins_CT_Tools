package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;

import java.awt.event.*;
import java.awt.*;
import tagTools.*;
import tagTools.TagListTools.TagSet;


/**A tool for creating "tagged" images. Uses a Materials List JFrame viewer to show tags.
 * The viewer cannot be accessed by the user because of the parent genericDialog
 * The integer tag values are the indices of a  list of material names, formulas and densities.
 * A tag's formula and density is used to compute linear attenuation at a single photon energy.	
 * JHD 12/4/2021
*/

public class Material_Tagger implements PlugInFilter, DialogListener, ActionListener
{
	TagListTools mlt = new TagListTools();
	TagSet tagSet;
	
	String[] matlNames;
	int[] matlTags;
	String[] filteredMatlNames;
	int[] filteredMatlTags;
	String matlFilter;
	
	ChoiceField materialCF;
	StringField filterSF;
	SliderField lowerSF,upperSF;
	ButtonField addMaterialBF;
	
	int		matlIndex;	// the position of the material in the list
	String	path;		// a file path for saving the dialog box values
	float	low,high;
	int width,height,depth;
	
	GenericDialog gd;
	ImagePlus grayImp;
	ImagePlus tagImp;
	ImageProcessor ip;
	
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);	
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	
	//*****************************************************************
	
	@Override
	public int setup(String arg, ImagePlus imp)
	{
		// TODO Auto-generated method stub
		this.grayImp=imp;
		return DOES_32;
	}

	//*****************************************************************

	@Override
	public void run(ImageProcessor ip)
	{
		this.ip = ip;
		if(IJ.versionLessThan("1.53u"))
		{
			IJ.showMessage("Newer ImageJ Version Required", "Update ImageJ to version 1.53u or better to run this plugin");
			return;
		}

		Calibration cal = grayImp.getCalibration();
		if(!cal.getUnit().toUpperCase().equals("CM"))
		{
			IJ.error("Pixel units must be cm");
			return;
		}
		
		//Location of the default materials list
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		tagSet = mlt.readTagSetFile(path);
		if(tagSet==null)
		{
			IJ.error("The Materials tagSet failed to load\n"
					+ "Please locate or create \"DefaultMaterials.csv\"\n"
					+ "and place it in the plugins/DialogData folder");
			return;
		}
		
		//Initialize master list of material data
		matlNames = mlt.getTagSetMatlNamesAsArray(tagSet);
		matlTags =  mlt.getTagSetMatlTagAsArray(tagSet);
		//Initialize filtered copies
		filteredMatlNames = matlNames;
		filteredMatlTags =matlTags;
						
		if(tagSet!=null)
		{
			width = grayImp.getWidth();
			height = grayImp.getHeight();
			depth = grayImp.getNSlices();
			//create a blank Tag Image image next to the dialog box
			tagImp = IJ.createImage("TagImage", width, height,  depth, grayImp.getBitDepth());
			tagImp.setCalibration(cal);
			tagImp.show();
			int left = grayImp.getWindow().getX();
			int top = grayImp.getWindow().getY();
			tagImp.getWindow().setLocation(left+grayImp.getWidth() + 20, top);
			
			//Use GenericDialogPlusLib for the event handlers
			GenericDialogAddin gda = new GenericDialogAddin();
			
			gd = new GenericDialog("Material Tagger");
			gd.addDialogListener(this);
			gd.addMessage("Select a material from the menu.\n"
					+ "Move the sliders to select thresholds.\n"
					+ "Click \"Add \" button.\nClick \"OK\" when done.",myFont,Color.BLACK);

			gd.setInsets(0, 0, 0);
			gd.addStringField("Filter List", "");
			filterSF = gda.getStringField(gd, null, "filterString");
			gd.setInsets(0, 0, 5);
			gd.addChoice("Material: ",filteredMatlNames,filteredMatlNames[0]);
			materialCF = gda.getChoiceField(gd, null, "materialChoice");
			
			ImageStatistics stats = grayImp.getStatistics();
			gd.addSlider("Lower", stats.min, stats.max, stats.min);
			lowerSF = gda.getSliderField(gd, null, null, "lowerSlider");
			gd.addSlider("Upper", stats.min, stats.max, stats.max);
			upperSF = gda.getSliderField(gd, null, null, "upperSlider");

			gd.addButton("Add Material to Tag Image", this);
			addMaterialBF = gda.getButtonField(gd, "addMatlBtn");
			
			gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/MaterialTagger.html");
			gd.setBackground(myColor);
			gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
			gd.showDialog();
			
			if(gd.wasOKed())
			{
				ip.resetThreshold();
				grayImp.updateAndDraw();				
			}
			if(gd.wasCanceled())
			{
				tagImp.close();
				return;
			}
		}
		else return;		
	}
	
	//*****************************************************************
	
	private boolean getSelections()
	{
		boolean settingsOK=true;
		matlFilter = gd.getNextString();
		matlIndex = gd.getNextChoiceIndex();
		low = (float)gd.getNextNumber();
		high = (float)gd.getNextNumber();	
		if(Double.isNaN(low) || Double.isNaN(high)) settingsOK=false;
		return settingsOK;
	}

	//*****************************************************************
	
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = false;
		if(e!=null)
		{
			dialogOK =getSelections();
			if(dialogOK)
			{
				Object src = e.getSource();
				if(src instanceof TextField)
				{
					TextField tf = (TextField)src;
					String name = tf.getName();
					switch(name)
					{
					case "lowerSlider":
					case "upperSlider":
						ip.setThreshold(low, high, ImageProcessor.RED_LUT);
						grayImp.updateAndDraw();
						break;
					case "filterString":
						String filterStr = tf.getText();
						TagSet filteredTagData = mlt.filterTagData(tagSet, filterStr);
						if(matlFilter.equals(""))
						{
							filteredMatlNames = matlNames;
							filteredMatlTags =matlTags;
						}
						else
						{
							filteredMatlNames = mlt.getTagSetMatlNamesAsArray(filteredTagData);
							filteredMatlTags = mlt.getTagSetMatlTagAsArray(filteredTagData);							
						}
						materialCF.getChoice().setVisible(false);
						materialCF.getChoice().removeAll();
						materialCF.setChoices(filteredMatlNames);
						materialCF.getChoice().setVisible(true);
						if(filteredMatlNames.length>0)
						{
							materialCF.getChoice().select(0);
						}
						break;
					}
				}
			}
		}
		addMaterialBF.getButton().setEnabled(dialogOK);
		return dialogOK;
	}
	

	//*****************************************************************

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Button btn = (Button)e.getSource();
		String btnLabel = btn.getLabel();
		switch(btnLabel)
		{
		case "Add Material to Tag Image":
			getSelections();
			if(matlIndex>=0)
			{		
				float[] tagPix = (float[])tagImp.getStack().getVoxels(0, 0, 0, width, height, depth, null);
				float[] grayPix = (float[])grayImp.getStack().getVoxels(0, 0, 0, width, height, depth, null);
				int size = width*height*depth;	
				for(int i=0;i<size;i++)
				{
					if(grayPix[i]<=high && grayPix[i] >= low)
					{
							tagPix[i] =filteredMatlTags[matlIndex];
					}
				}
				grayImp.getProcessor().resetThreshold();
				tagImp.getStack().setVoxels(0, 0, 0, width, height, depth, tagPix);
				ImageStatistics stats = tagImp.getStatistics();
				tagImp.getProcessor().setMinAndMax(stats.min, stats.max);
				IJ.run(tagImp, "3-3-2 RGB", "");
				tagImp.updateAndDraw();
			break;
			}
		}
	} 
}
