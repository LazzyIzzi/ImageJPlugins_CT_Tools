package CT_Tools;

/*
 * This plugin simulates a parallel beam CT scan from 0 to 180 degrees.
 * The source and detector arrays are rotated around the center of a square image.
 * The line integral of attenuation along a ray between source-detector pairs
 * is returned as a sinogram. 
 */

import ij.IJ;

import ij.ImagePlus;
import ij.plugin.CanvasResizer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;

import java.awt.*;

import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.Projection.*;
//import CT_Tools.Calc_Sinogram_Properties;

public class MuLin_Image_To_Parallel_Sinogram implements PlugInFilter, DialogListener
{
	final String myDialogTitle = "Parallel Beam CTscan";
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	ImagePlus imageImp;
	int originalWidth,originalHeight;
	
	//DialogParameters
	boolean scale16;
	int numAng;
	int detPixCnt;
	double scaleFactor = 6000;
	//int detPixCnt;

	//The class that does the simulation
	ParallelProjectors parPrj = new ParallelProjectors();
	
	//For compact event handlers
	GenericDialogAddin gda = new GenericDialogAddin();
	CheckboxField padImageCBF, scale16CBF;
	NumericField numAnglesNF,detPixCntNF;
	RadioButtonField padOptionsRBF;
	ChoiceField padOptionsCF;
		

	//*******************************************************************************

	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		this.imageImp = imp;
		return DOES_32;
	}

	//*******************************************************************************

	@Override
	public void run(ImageProcessor ip)
	{
		if(IJ.versionLessThan("1.53u"))// GenericDialog.resetCounters()
		{
			IJ.showMessage("ImageJ version 1.53u or better required.");
			return;
		}
		String unit = imageImp.getCalibration().getUnit().toUpperCase();
		if(!unit.equals("CM"))
		{
			IJ.error("Input image pixel units must be in cm (centimeters)");
			return;
		}
		if(imageImp.getWidth() != imageImp.getHeight())
		{
			IJ.showMessage("Image must be Square. Check the PadImage Box in the next dialog");
		}

		originalWidth =ip.getWidth();
		originalHeight =ip.getHeight();
		if(doMyDialog())
		{
			if(validateParams())
			{
				DoRoutine();
			}
		}
	}


	//*******************************************************************************

	private boolean doMyDialog()
	{
		String dir = IJ.getDirectory("plugins");
		dir= dir.replace("\\","/");
		//String myURL = "file:///" + dir + "jars/MuMassCalculatorDocs/index.html";
		
		final String[] padOptions = {"None","Circumscribed", "Next Power of 2"};

		//int width = imageImp.getWidth();
		detPixCnt= originalWidth;

		GenericDialog gd = new GenericDialog(myDialogTitle);
		gd.addDialogListener(this);
		gd.setInsets(10,0,0);
		gd.addMessage("This plugin scans linear attenuation\n images to sinograms.",myFont,Color.BLACK);
		gd.setInsets(10,0,0);
		gd.addMessage("180 degree Scan______________",myFont,Color.BLACK);
		gd.setInsets(10,0,0);
		gd.addMessage("Minimum Detector Width = " + originalWidth + " pixels");
		
		int numAngles = (int) (originalWidth*Math.PI/2);
		//make numAngles even
		if ((numAngles ^ 1) == numAngles - 1)	numAngles++;			
		gd.addNumericField("Suggested_View_Angles", numAngles);
		numAnglesNF = gda.getNumericField(gd, null, "numAngles");
		
		gd.addChoice("Pad_Options", padOptions, padOptions[0]);
		padOptionsCF = gda.getChoiceField(gd, null, "padOptions");

		gd.addNumericField("Detector_Pixels = " , detPixCnt);
		detPixCntNF = gda.getNumericField(gd, null, "detPixCnt");
		
		gd.addCheckbox("Scale_to_16-bit", false);
		scale16CBF = gda.getCheckboxField(gd, "scale16");
		gd.addNumericField("Scale Factor", scaleFactor);

		gd.addHelp("https://lazzyizzi.github.io/CTsimulator.html");
		gd.setBackground(myColor);
		gd.showDialog();

		if (gd.wasCanceled())
		{
			return false;
		}
		else
		{
			getSelections(gd);
			return true;
		}

	}

	//*******************************************************************************

	private boolean validateParams()
	{
		if(numAng> 1 && imageImp.getWidth()==imageImp.getHeight())
			return true;
		else
		{
			IJ.error("Image To Sinogram Error", "Images must be square and 1 or more angles");
			return false;
		}
	}

	//*******************************************************************************

	private void DoRoutine()
	{
		if(imageImp.getBitDepth() == 32)
		{
			float[] sinogram;
			ImageProcessor sinoIp;
			Object image;
			CanvasResizer resizer= new CanvasResizer();
			
			Calibration  imgCal = imageImp.getCalibration();		
			String unit = imgCal.getUnit();	// bark if not "cm" ?
			double pixSize = imgCal.pixelWidth;
			

			int nslices = imageImp.getNSlices();
			int originalWidth =  imageImp.getWidth();
			
			int bits = 32;
			if(scale16)
			{
				bits = 16;
			}

			ImagePlus sinoImp = IJ.createImage("sino", detPixCnt, numAng, nslices, bits);

			sinoIp = sinoImp.getProcessor();

			for(int i=1;i<=nslices;i++)
			{
				IJ.showProgress((double)i/(double)nslices);				
				sinoImp.setSlice(i);
				imageImp.setSlice(i);
				
				if(detPixCnt>originalWidth)
				{ //to conserve memory the stack slices are individually padded projected and disposed
					ImagePlus sliceImp = imageImp.crop();
					ImageProcessor padIp = resizer.expandImage(sliceImp.getProcessor(), detPixCnt, detPixCnt,(detPixCnt-originalWidth)/2, (detPixCnt-originalWidth)/2);
					sliceImp.setProcessor(padIp);
					image = sliceImp.getProcessor().getPixels();
					sinogram = parPrj.imageToParallelSinogram((float[])image,detPixCnt,detPixCnt,numAng);
					sliceImp.close();
				}
				else
				{
					image = imageImp.getProcessor().getPixels();
					sinogram = parPrj.imageToParallelSinogram((float[])image,detPixCnt,detPixCnt,numAng);					
				}
				
				if(scale16)
				{
					short[] sino16 = new short[sinogram.length];
					for(int j = 0; j<sinogram.length;j++)
					{
						sino16[j] = (short) (sinogram[j]*scaleFactor*pixSize);
					}
					sinoIp.setPixels(sino16);
				}
				else
				{
					sinoIp.setPixels(sinogram);
					sinoIp.multiply(pixSize);
				}
			}

			String[] props = new String[4];
			props[0]="Geometry"; 
			props[1]="Parallel";
			props[2]="Source";
			props[3]="Tau Values";			
			sinoImp.setProperties(props);

			// append "ParSino" to the image name
			String title;
			String name = imageImp.getTitle();
			int dotIndex = name.lastIndexOf(".");
			if(dotIndex != -1) title = name.substring(0, dotIndex);
			else title  = name;
			sinoImp.setTitle(title + "_ParSino"+ numAng);

			// Set the sinogram X,Y units
			//The pixel values are in per pixel units
			Calibration sinoCal = sinoImp.getCalibration();
			sinoCal.setXUnit(unit);
			sinoCal.setYUnit("Deg");
			sinoCal.pixelWidth = pixSize;
			sinoCal.pixelHeight = 180.0/numAng;

			imageImp.setSlice(1);
			sinoImp.setSlice(1);
            ImageStatistics stats = sinoImp.getStatistics();
	        sinoImp.getProcessor().setMinAndMax(stats.min, stats.max);
			sinoImp.show();
		}
	}

	//*******************************************************************************

	private void getSelections(GenericDialog gd)
	{
		gd.resetCounters();
		numAng = (int)gd.getNextNumber();
		detPixCnt = (int)gd.getNextNumber();
		scale16 = gd.getNextBoolean();
		scaleFactor = gd.getNextNumber();
	}

	//*******************************************************************************

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = true;
		if(e!=null)
		{
			getSelections(gd);
			Object src = e.getSource();
			
			if(src instanceof Checkbox)
			{
				Checkbox cb = (Checkbox) e.getSource();
				switch(cb.getLabel())
				{
				case "scale16":
					break;
				}
			}
			
			else if(src instanceof TextField)
			{
				TextField tf= (TextField)src;
				String name = tf.getName();
				switch(name)
				{
				case "detPixCnt":
					detPixCnt = (int) detPixCntNF.getNumber();
					if(detPixCnt > originalWidth)
					{
						int numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
						//make numAngles even
						if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
						numAnglesNF.setNumber(numAngles);
					}
					else dialogOK=false;
					break;
				}				
			}
			
			else if(src instanceof Choice)
			{
				Choice choice = (Choice) src;
				String name = choice.getName();
				switch(name)
				{
				case "padOptions":
					String option = choice.getSelectedItem();
					int numAngles=0;
					switch(option)
					{
					case "None":
						detPixCnt = originalWidth;
						detPixCntNF.setNumber(detPixCnt);
						numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
						break;
					case "Circumscribed":
						detPixCnt = (int) (Math.ceil(Math.sqrt(2*originalWidth*originalWidth)));
						detPixCntNF.setNumber(detPixCnt);
						numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
						break;
					case "Next Power of 2":
						detPixCnt = 0;
						for(int i=0;i< 10;i++)
						{
							detPixCnt =(int) Math.pow(2, i);
							if(detPixCnt>originalWidth) break;
						}				
						detPixCntNF.setNumber(detPixCnt);
						numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
						break;
					case "Custom":
						break;
					}
					//make numAngles even
					if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
					numAnglesNF.setNumber(numAngles);
					break;
				}
			}
		}
		getSelections(gd);
		return dialogOK;
	}	
}
