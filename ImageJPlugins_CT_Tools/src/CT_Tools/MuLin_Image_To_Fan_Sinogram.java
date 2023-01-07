package CT_Tools;

/*
 * This plugin simulates a fan beam CT scan from 0 to 360 degrees.
 * The point source and detector array are rotated around the center of a square image.
 * The line integral of attenuation along a ray between the source and each detector element
 * is returned as a sinogram.
 * 
 * In the interest of simplicity, the Projectors in the MuMaccCalculator library do not take 
 * the number of detector pixels as an argument. The width of the detector is calculated internally.
 * 
 * 
 */

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.CanvasResizer;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import java.awt.*;
import java.util.*;

import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.Projection.FanProjectors;
import jhd.Projection.FanProjectors.*;

public class MuLin_Image_To_Fan_Sinogram implements PlugInFilter, DialogListener
{
	final String myDialogTitle = "Fan Beam CTscan";
	
	//The class that does fan projection
	FanProjectors fanPrj = new FanProjectors();
	
	//The nested class containing the simulator's user supplied parameters
	FanParams fpSet =  new FanParams();
	
	//The ImageJ GenericDialog class
	GenericDialog gd;
	
	//Globals
	boolean scale16;
	ImagePlus imageImp;
	int originalWidth,originalHeight; //the width and height of the current image
	int paddedWidth,paddedHeight; //the width and height of the current image after padding with zeros
	//double pixelSize;
	String unit;
	double scaleFactor=6000;
	
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	
	
	//For compact event handlers
	GenericDialogAddin gda = new GenericDialogAddin();
	NumericField numAnglesNF,srcToDetNF,axisToDetNF,magnificationNF,detPixCntNF;
	MessageField axisToDetMF,detMinCntMF,paddedWidthMF;
	ChoiceField padOptionsCF;
	
	//*******************************************************************************

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = true;
		double mag,srcToDet,axisToDet;
		int detMinCnt,detPixCnt,numAngles;
		String padChoice;
		if(e!=null)
		{
			getSelections();
			
			Object src = e.getSource();
			if(src instanceof Checkbox)
			{
				Checkbox cb = (Checkbox) src;
				switch(cb.getName())
				{
				case "scale16":
					break;
				}				
			}
			else if(src instanceof TextField)
			{
				TextField tf = (TextField)src;
				String name = tf.getName();
				//In this plugin the source to detector and magnification a user adjustable parameters
				//The axis to detector distance, detector minimum width and number of views are calculated.				
				padChoice =  padOptionsCF.getChoice().getSelectedItem();
				
				switch(name)
				{
				case "magnification":
				case "sourceToDetector":
					srcToDet = srcToDetNF.getNumber();
					mag = magnificationNF.getNumber();
					axisToDet = getAxisToDet(mag,srcToDet);
					detMinCnt = getMinDetCnt(originalWidth,mag,padChoice);
					paddedWidth = getMinDetCnt(originalWidth,1,padChoice);
					numAngles = getNumAngles(detMinCnt);
					axisToDetMF.getLabel().setText("Axis to Detector = " + String.format("%.3f" + " cm", axisToDet));
					//axisToDetNF.setNumber(axisToDet);
					numAnglesNF.setNumber(numAngles);
					detPixCntNF.setNumber(detMinCnt);					
					detMinCntMF.getLabel().setText("Minimum Detector Width = " + detMinCnt + " pixels");				
					paddedWidthMF.getLabel().setText("Padded Image Width = " + paddedWidth + " pixels");
					if(mag<=1 || Double.isNaN(mag)) dialogOK=false;
					break;
					
				case "detPixCnt":
					mag = magnificationNF.getNumber();
					detMinCnt = getMinDetCnt(originalWidth,mag,padChoice);
					detPixCnt = (int) detPixCntNF.getNumber();
					paddedWidth = (int)(detPixCnt/mag);
					paddedWidthMF.getLabel().setText("Padded Image Width = " + paddedWidth + " pixels");				
					if(detPixCnt< detMinCnt)
					{
						dialogOK = false;
					}
					if(mag<=1 || Double.isNaN(mag)) dialogOK=false;
					break;
					
				case "axisToDetector":
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
					padChoice = choice.getSelectedItem();
					mag = magnificationNF.getNumber();
					detMinCnt = getMinDetCnt(originalWidth,mag,padChoice);
					paddedWidth = getMinDetCnt(originalWidth,1,padChoice);
					numAngles = getNumAngles(detMinCnt);
					numAnglesNF.setNumber(numAngles);
					detPixCntNF.setNumber(detMinCnt);					
					detMinCntMF.getLabel().setText("Minimum Detector Width = " + detMinCnt + " pixels");				
					paddedWidthMF.getLabel().setText("Padded Image Width = " + paddedWidth + " pixels");				
					if(mag<1) dialogOK=false;
					break;					
				}
			}

		}
		//GenericDialog OK button calls dialogItemChanged with null event
		//GetSelections called so settings are macro recordable
		getSelections();			
		return dialogOK;
	}

	//*******************************************************************************

	private boolean doMyDialog()
	{
		double mag,srcToDet;
		int detMinCnt,numAngles;
		String padChoice;
		final String[] padOptions = {"None","Circumscribed", "Next Power of 2"};
		
		String dir = IJ.getDirectory("plugins");
		dir= dir.replace("\\","/");

		gd = new GenericDialog(myDialogTitle);
		gd.addDialogListener(this);
		gd.setInsets(10,0,0);
		gd.addMessage("This plugin scans linear attenuation\n images to sinograms.",myFont,Color.BLACK);
		gd.setInsets(10,0,0);
		gd.addMessage("360 degree Scan______________",myFont,Color.BLACK);
		
		mag = 2.0;
		srcToDet = 100;
		padChoice = padOptions[0];
		detMinCnt = getMinDetCnt(originalWidth,mag,padChoice);
		paddedWidth = getMinDetCnt(originalWidth,1,padChoice);;
		numAngles = getNumAngles(detMinCnt);

		gd.addNumericField("Suggested_View_Angles:", numAngles);		
		numAnglesNF = gda.getNumericField(gd, null, "numAngles");

		gd.addChoice("Pad_Options", padOptions, padChoice);
		padOptionsCF = gda.getChoiceField(gd, null, "padOptions");
		
		gd.addNumericField("Detector_Pixels = " , detMinCnt);
		detPixCntNF = gda.getNumericField(gd, null, "detPixCnt");
		
		gd.addNumericField("Source_to_Detector(cm):", srcToDet);
		srcToDetNF = gda.getNumericField(gd, null, "sourceToDetector");
		
		gd.addNumericField("Magnification:", mag);
		magnificationNF = gda.getNumericField(gd, null, "magnification");
						
		gd.addMessage("Padded Image Width = " +  paddedWidth + "pixels");
		paddedWidthMF = gda.getMessageField(gd, "paddedWidth");

		gd.addMessage("Minimum Detector Width = " +  detMinCnt + "pixels");
		detMinCntMF = gda.getMessageField(gd, "detMinWidth");
		
		gd.addMessage("Axis to Detector = 50");
		axisToDetMF = gda.getMessageField(gd, "axisToDetector");

		gd.addCheckbox("Scale to 16-bit proj", false);
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
			return true;
		}	
	}
		
	//*******************************************************************************
	
	private void doRoutine()
	{		
		//ImagePlus sinoImp;
		getSelections();
		Object image;


		int nslices = imageImp.getNSlices();
		float[] sinogram = null;

		ArrayList<float[]> sinograms = new ArrayList<float[]>();
		ImagePlus sinoImp;				

		for(int i=1;i<=nslices;i++)
		{
			IJ.showProgress((double)i/(double)nslices);
			imageImp.setSlice(i);

			if(paddedWidth>originalWidth || paddedWidth>originalHeight)
			{ //to conserve memory the stack slices are individually copied, padded, projected and disposed
				CanvasResizer resizer= new CanvasResizer();
				ImagePlus sliceImp = imageImp.crop();
				ImageProcessor padIp = resizer.expandImage(sliceImp.getProcessor(), paddedWidth, paddedWidth,(paddedWidth-originalWidth)/2, (paddedWidth-originalHeight)/2);
				image = padIp.getPixels();				
				sinogram = fanPrj.imageToFanBeamSinogram((float[])image,paddedWidth, paddedWidth, fpSet,true);
				sinograms.add(sinogram);
			}
			else
			{
				image = imageImp.getProcessor().getPixels();
				sinogram = fanPrj.imageToFanBeamSinogram((float[])image,originalWidth, originalHeight, fpSet,true);
				sinograms.add(sinogram);
			}
		}

		int length = sinogram.length;
		int width = length/fpSet.numAng;   //numAngles;
		int height = fpSet.numAng;   //numAngles;;
		if(scale16)
		{
			sinoImp = IJ.createImage(myDialogTitle, width, height, nslices, 16);
			short[] pixels = (short[])sinoImp.getProcessor().getPixels();
			for(int i=1;i<= nslices;i++)
			{
				float[] temp = sinograms.get(i-1);
				for(int j=0;j<pixels.length;j++)
				{
					if(temp[j]<0) temp[j]=0;
					pixels[j]= (short) (temp[j]*scaleFactor*fpSet.pixSizeCM);
					//pixels[j]= (short) (temp[j]*scaleFactor);
				}
				sinoImp.getProcessor().setPixels(pixels);
			}
		}
		else
		{
			sinoImp = IJ.createImage(myDialogTitle, width, height, nslices, 32);
			for(int i=1;i<=nslices;i++)
			{
				sinoImp.setSlice(i);
				sinoImp.getProcessor().setPixels(sinograms.get(i-1));
				sinoImp.getProcessor().multiply(fpSet.pixSizeCM);
			}
		}

		String[] props = new String[8];
		props[0]="Geometry"; 
		props[1]="Parallel";
		props[2]="Source";
		props[3]="Tau Values";			
		props[4]="Source To Detector";
		props[5]=Double.toString(fpSet.srcToDetCM);			
		props[6]="Magnification";
		props[7]=Double.toString(fpSet.magnification);			
		sinoImp.setProperties(props);

		// append "FanSino" to the image name
		String title;
		String name = imageImp.getTitle();
		int dotIndex = name.lastIndexOf(".");
		if(dotIndex != -1) title = name.substring(0, dotIndex);
		else title  = name;           	
		sinoImp.setTitle(title + "_Mag" + fpSet.magnification + "FanSino" + fpSet.numAng);

		// Set the sinogram X,Y units
		//The pixel values are in per pixel units
		Calibration sinoCal = sinoImp.getCalibration();
		sinoCal.setXUnit(unit);
		sinoCal.setYUnit("Deg");
		sinoCal.pixelWidth =fpSet.pixSizeCM/fpSet.magnification;
		sinoCal.pixelHeight = 360.0/fpSet.numAng;

		imageImp.setSlice(1);
		sinoImp.setSlice(1);
		sinoImp.show();
		IJ.run("Enhance Contrast", "saturated=0.35");	
	}
	
	//*******************************************************************************

	//Compute minimum detPixCnt, srcToSampCM,sampToDetCM and numAngles
	//from 
//	private double getMag(double axisToDet,double srcToDet)
//	{
//		return srcToDet/(srcToDet-axisToDet);
//	}
	private double getAxisToDet(double mag, double srcToDet)
	{
		return srcToDet*(1-1/mag);
	}
	
	//*******************************************************************************

	//	private double getSrcToSamp(double mag, double srcToDet)
//	{
//		return srcToDet/mag;
//	}
//	private double getSrcToDet(double mag, double srcToSamp)
//	{
//		return mag*srcToSamp;
//	}
	/**
	 * @param imageWidth the width of the image to be projected
	 * @param mag the CT scanner magnification
	 * @param padType "None", "Circumscribed","Next Power of 2"
	 * @return
	 */
	private int getMinDetCnt(int imageWidth, double mag, String padType)
	{
		int detMinCnt=0;
		switch(padType)
		{
		case "None":
			detMinCnt = (int)(imageWidth*mag);
			break;
		case "Circumscribed":
			detMinCnt = (int) (Math.sqrt(2*originalWidth*originalHeight)*mag);
			break;
		case "Next Power of 2":
			int size = originalWidth;
			if(originalHeight>size) size = originalHeight;
			detMinCnt = 0;
			for(int i=0;i< 10;i++)
			{
				detMinCnt =(int) (Math.pow(2, i)*mag);
				if(detMinCnt>size*mag) break;
			}				
			break;
		}
		//if ((detMinCnt ^ 1) == detMinCnt - 1)	detMinCnt++;	
		return detMinCnt;
	}
	
	//*******************************************************************************

	/**
	 * @param detMinCnt the detector width in pixels
	 * @return numAngles
	 */
	private int getNumAngles(int detMinCnt)
	{
		int numAngles=(int) (Math.ceil(Math.PI*detMinCnt));
		//if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
		return numAngles;
	}
	
	//*******************************************************************************

	private void getSelections()
	{
		gd.resetCounters();
		fpSet.numAng= (int)gd.getNextNumber();		
		paddedWidth = (int)(gd.getNextNumber()/fpSet.magnification);
		fpSet.srcToDetCM = (float)gd.getNextNumber();
		fpSet.magnification=(float)gd.getNextNumber();
		scale16 = gd.getNextBoolean();
		scaleFactor = gd.getNextNumber();
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
		unit = imageImp.getCalibration().getUnit().toUpperCase();
		if(!unit.equals("CM"))
		{
			IJ.error("Input image pixel units must be in cm (centimeters)");
			return;
		}
		if(imageImp.getWidth() != imageImp.getHeight())
		{
			IJ.showMessage("Image must be Square. Check the PadImage Box in the next dialog");
		}
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
		
		originalWidth =ip.getWidth();
		originalHeight =ip.getHeight();
		fpSet.pixSizeCM = cal.pixelWidth;
		unit = imageImp.getCalibration().getUnit();
		
		if(doMyDialog())
		{
			getSelections();
			if(validateParams())
			{
				doRoutine();
			}
		}
	}
		
	//*******************************************************************************

	@Override
	public int setup(String arg, ImagePlus imp)
	{
		this.imageImp = imp;
		return DOES_32;
	}

	//*******************************************************************************

	private boolean validateParams()
	{
		if(fpSet.numAng> 1)
		{
			return true;
		}
		else
		{
			IJ.error("Image To Sinogram Error", "Images must have 1 or more view angles");
			return false;
		}
//		if(fpSet.numAng> 1 && imageImp.getWidth()==imageImp.getHeight())
//		{
//			return true;
//		}
//		else
//		{
//			IJ.error("Image To Sinogram Error", "Images must be square and have 1 or more view angles");
//			return false;
//		}
	}
	
}
