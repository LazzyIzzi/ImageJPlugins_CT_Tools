package CT_Tools;

/*
 * This plugin simulates a parallel beam CT scan from 0 to 180 degrees.
 * The source and detector arrays are rotated around the center of a square image.
 * The line integral of attenuation along a ray between source-detector pairs
 * is returned as a sinogram. 
 */

import ij.IJ;

import ij.ImagePlus;
import ij.WindowManager;
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
	final String[] padOptions = {"None","Circumscribed", "Next Power of 2"};
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Color errColor = new Color(255,100,0);
	final Color white = new Color(255,255,255);
	final Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	//The class that does the projection
	ParallelProjectors parPrj = new ParallelProjectors();
	
	ImagePlus imageImp;
	int originalWidth,originalHeight;
	
	String padOption;
	boolean scale16;
	int numAng;
	int detPixCnt;
	double pixelSize,scaleFactor = 6000;
	String unit;
	
	GenericDialog gd;
	GenericDialogAddin gda = new GenericDialogAddin();
	CheckboxField padImageCBF, scale16CBF;
	NumericField numAnglesNF,detPixCntNF;
	RadioButtonField padOptionsRBF;
	ChoiceField padOptionsCF;
		
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
					dialogOK = handleDetPixCntEvent();
					break;
				case "numAngles":
					int numAngles =(int)numAnglesNF.getNumber();
					if(Double.isNaN(numAngles) || numAngles <1) dialogOK=false;						
					break;
				default:
					//all of the others are numeric
					String numStr = tf.getText();
					if(!isNumeric(numStr)) dialogOK=false;
					else
					{
						double num = Double.valueOf(numStr);
						if(num<0) dialogOK=false;
					}
				break;
				}				
				if(!dialogOK) tf.setBackground(errColor);
				else tf.setBackground(white);
			}
			
			else if(src instanceof Choice)
			{
				Choice choice = (Choice) src;
				String name = choice.getName();
				switch(name)
				{
				case "padOptions":
					String option = choice.getSelectedItem();
					handlePadOptionsEvent(option);
					break;
				}
			}
		}
		getSelections(gd);
		return dialogOK;
	}
	
	//*******************************************************************************

	private boolean doMyDialog()
	{
		String dir = IJ.getDirectory("plugins");
		dir= dir.replace("\\","/");
		//String myURL = "file:///" + dir + "jars/MuMassCalculatorDocs/index.html";
		

		//int width = imageImp.getWidth();
		detPixCnt= originalWidth;

		gd = new GenericDialog(myDialogTitle);
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

		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/CTsimulator.html");
		gd.setBackground(myColor);
		
		//the pad option can be switched after the dialog fields
		//have been set up;
		if(originalWidth!= originalHeight)
		{
			padOption = padOptions[2];
			padOptionsCF.getChoice().select(padOption);
			handlePadOptionsEvent(padOption);
		}
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
	
	private void DoRoutine()
	{
		float[] sinogram;
		ImagePlus sinoImp;
		ImageProcessor sinoIp;
		Object image;
		CanvasResizer resizer= new CanvasResizer();

		int nslices = imageImp.getNSlices();

		String title;
		String name = imageImp.getTitle();
		int dotIndex = name.lastIndexOf(".");
		if(dotIndex != -1) title = name.substring(0, dotIndex);
		else title  = name;
		title += "_ParMuLinSino";

		title = WindowManager.getUniqueName(title);

		if(scale16) sinoImp = IJ.createImage(title, detPixCnt, numAng, nslices, 16);
		else sinoImp = IJ.createImage(title, detPixCnt, numAng, nslices, 32);

		sinoIp = sinoImp.getProcessor();

		for(int i=1;i<=nslices;i++)
		{
			IJ.showProgress((double)i/(double)nslices);				
			sinoImp.setSlice(i);
			imageImp.setSlice(i);

			if(detPixCnt>originalWidth)
			{ //to conserve memory the stack slices are individually padded projected and disposed
				ImagePlus sliceImp = imageImp.crop();
				ImageProcessor padIp = resizer.expandImage(sliceImp.getProcessor(), detPixCnt, detPixCnt,(detPixCnt-originalWidth)/2, (detPixCnt-originalHeight)/2);
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
					sino16[j] = (short) (sinogram[j]*scaleFactor*pixelSize);
				}
				sinoIp.setPixels(sino16);
			}
			else
			{
				sinoIp.setPixels(sinogram);
				sinoIp.multiply(pixelSize);
			}
		}

		String[] props = new String[4];
		props[0]="Geometry"; 
		props[1]="Parallel";
		props[2]="Source";
		props[3]="Tau Values";			
		sinoImp.setProperties(props);

		// Set the sinogram X,Y units
		//The pixel values are in per pixel units
		Calibration sinoCal = sinoImp.getCalibration();
		sinoCal.setXUnit(unit);
		sinoCal.setYUnit("Deg");
		sinoCal.pixelWidth = pixelSize;
		sinoCal.pixelHeight = 180.0/numAng;

		imageImp.setSlice(1);
		sinoImp.setSlice(1);
		ImageStatistics stats = sinoImp.getStatistics();
		sinoImp.getProcessor().setMinAndMax(stats.min, stats.max);
		sinoImp.show();		
	}

	//*******************************************************************************

	private void getSelections(GenericDialog gd)
	{
		gd.resetCounters();
		padOption = gd.getNextChoice();
		numAng = (int)gd.getNextNumber();
		detPixCnt = (int)gd.getNextNumber();
		scale16 = gd.getNextBoolean();
		scaleFactor = gd.getNextNumber();
	}

	//*******************************************************************************

	private boolean handleDetPixCntEvent()
	{
		boolean dialogOK = true;
		detPixCnt = (int) detPixCntNF.getNumber();
		//make Detector pixel count an even number
		if(detPixCnt % 2 !=  0) {
			detPixCnt+=1;
			detPixCntNF.setNumber(detPixCnt);
		}
		if(detPixCnt >= originalWidth)
		{
			int numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
			//make numAngles even
			if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
			numAnglesNF.setNumber(numAngles);
		}
		else dialogOK=false;
		return dialogOK;
		
	}

	//*******************************************************************************

	private void handlePadOptionsEvent(String padOption)
	{
		int numAngles=0;
		switch(padOption)
		{
		case "None":
			detPixCnt = originalWidth;
			detPixCntNF.setNumber(detPixCnt);
			numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
			break;
		case "Circumscribed":
			detPixCnt = (int) (Math.ceil(Math.sqrt(originalWidth*originalWidth + originalHeight*originalHeight)));
			detPixCntNF.setNumber(detPixCnt);
			numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
			break;
		case "Next Power of 2":
			int size = originalWidth;
			if(originalHeight>size) size = originalHeight;
			detPixCnt  =findNextPowerOfTwo(detPixCnt);			
			detPixCntNF.setNumber(detPixCnt);
			numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
			break;
		case "Custom":
			break;
		}
		if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
		numAnglesNF.setNumber(numAngles);
	}
	
    private  int findNextPowerOfTwo(int n) {
        if (n <= 0) {
            return 1; // Return 1 for non-positive numbers
        }
        
        // Decrement n by 1 to handle the case when n is already a power of 2
       // n--;
        
        // Set all bits to the right of the highest set bit
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16; // For 32-bit integers
        
        // Return the next power of 2
        return n + 1;
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
		
		originalWidth =ip.getWidth();
		originalHeight =ip.getHeight();
		
		Calibration cal = imageImp.getCalibration();
		unit = cal.getUnit().toUpperCase();
		if(!unit.equals("CM"))
		{
			IJ.error("Input image pixel units must be in cm (centimeters)");
			return;
		}
		
		if(cal.pixelWidth != cal.pixelHeight)
		{
			IJ.showMessage("Pixel width and height must be the same.");
			return;
		}

		pixelSize = cal.pixelWidth;
		
		if(doMyDialog())
		{
			if(validateParams())
			{
				DoRoutine();
			}
		}
	}

	//*******************************************************************************

	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		this.imageImp = imp;
		return DOES_32;
	}

	//*******************************************************************************

	private boolean validateParams()
	{

		if(originalWidth!=originalHeight && padOption.equals(padOptions[0]))
		{
			IJ.error("Non-square images require a pad option.");
			return false;
		}

//		if(numAng<1)
//		{
//			IJ.error("Image To Sinogram Error", "Images must have one or more angles");
//			return false;
//		}
		
		return true;
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
