package CT_Tools;

/*
 * This plugin simulates a Fan beam CT scan from 0 to 360 degrees
 * of a segmented 2D image using a conventional x-ray source and a scintillation
 * detector.
 * 
 *  The input image must be segmented into N components. The pixel values of each component must be
 *  set to an integer (tagged) beginning 1, corresponding to a composition and density in a
 *  Materials text file.
 *
 * A Materials file is a simple text file containing
 * a formula in the Atom1:Count1:atom2:Count2 format ,comma, followed by a density in gmPerCC.
 * One formula,density pair per line.
 * The first material is assigned to tag=1
 * The second in assigned to tag=2 etc.
 * e.g. for an image of Sandstone with Calcite cements
 * Ca:1:C:1:O:3, 2.71
 * Si:1:O:2, 2.53
 * 
 * This may seem a bit awkward but it allows the user to create a libraries of tagged components
 * so they won't need to be typed in every time.  Excel csv files will work for this purpose.
 * 
 * The tags are used to convert the image to linear attenuation at each energy in the
 * polychromatic scan.
 *  
 * The spectral intensity of the source is estimated using the Kramers equation.
 * The transmission of the filter, the absorption of the image (sample) and the
 * absorption of the detector are computed using Beer-Lambert.
 * 
 * Polychromatic x-rays are simulated by combining the sample signals taken at
 * increments of x-ray energy.
 * The attenuation of each component is computed from the components formula 
 * and density and the x-ray energy.
 * 
 * A CT scan is simulated by rotating the source detector about the center of the image and p
 * collecting projections to form a sinogram.
 * 
 * This version defines the scanner geometry in centimeters.
 * Attempts to serialize the dialog parameters.
 */

import ij.IJ;

import ij.ImagePlus;
import ij.plugin.CanvasResizer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.gui.*;
import ij.measure.Calibration;

import java.io.File;
import java.awt.*;
import java.util.ArrayList;

import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.MuMassCalculator.*;
import gray.AtomData.*;
import jhd.Projection.*;
import jhd.Serialize.*;
import jhd.TagTools.*;


//*******************************************************************************

public class Tag_Image_To_Fan_Brems_Sinogram implements PlugInFilter , DialogListener//, ActionListener
{
	final String myDialogTitle = "Polychromatic Fan Beam CTscan";	
	final String mySettingsTitle = "Polychromatic_FanBeam_Params";
	
	//Used to test formulas prior to launching the simulator
	MuMassCalculator mmc = new MuMassCalculator();
	
	//The class that does fan projection
	FanProjectors fanPrj = new FanProjectors();
	
	//The nested class containing the simulator's user supplied parameters
	FanProjectors.BremFanParams bfpSet =  new FanProjectors.BremFanParams();	
	
	//The class used to serialize (save) the users selections
	Serializer ser = new Serializer();
	
	//The class used to manage materials Lists
	MatlListTools mlt=new MatlListTools();
	
	//The nested class containing  materials list tag information
	MatlListTools.TagSet tagSet;
	
	//the full path to the default tagSet file
	String tagSetPath =  IJ.getDirectory("plugins") + "DialogData\\DefaultMaterials.csv";
	
	//The ImageJ GenericDialog class
	GenericDialog gd = new GenericDialog(myDialogTitle);
	
	//Addins to make referencing the dialog's components much simpler
	GenericDialogAddin gda = new GenericDialogAddin();
	
	//Arrays to unpack Text file materials lists
	String[] matlArr;	
	String[] formula;
	double[] gmPerCC;
	
	//GLOBALS
	boolean scale16;
	ImagePlus imageImp;
	int originalWidth,originalHeight; //the width and height of the current image
	int paddedWidth,paddedHeight; //the width and height of the current image after padding with zeros
	double pixelSize;
	String unit;
	double scaleFactor=6000;
	
	//the full path to the dialog settings
	String settingsPath = IJ.getDirectory("plugins") + "DialogSettings" + File.separator + mySettingsTitle + ".ser";
		
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		
	ChoiceField padOptionsCF;
	NumericField detPixCntNF;
	NumericField scaleFactorNF;
	NumericField numAnglesNF;
	NumericField magnificationNF;
	NumericField srcToDetNF;	
	MessageField axisToDetMF,detMinCntMF,paddedWidthMF;
	
	//*******************************************************************************
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{

		int	detMinCnt; //the minimum number of detector pixels to  project an image after magnification
		int detPixCnt; //the actual number of detector pixels entered by the user. Must be >= detMinCnt
		int paddedWidth;
		double mag,srcToDet,axisToDet;
		int numAngles;	
		String padChoice;
		boolean dialogOK = true;
		
		if(e!=null)
		{
			//getSelections(gd);
			Object src = e.getSource();
			
			if(src instanceof Checkbox)
			{
				Checkbox cb = (Checkbox)src;
				switch(cb.getName())
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
				case "magnification":
				case "sourceToDetector":
					padChoice = padOptionsCF.getChoice().getSelectedItem();
					srcToDet = srcToDetNF.getNumber();
					mag = magnificationNF.getNumber();
					axisToDet = getAxisToDet(mag,srcToDet);
					detMinCnt = getMinDetCnt(originalWidth,mag,padChoice);
					paddedWidth = getMinDetCnt(originalWidth,1,padChoice);
					numAngles = getNumAngles(detMinCnt);
					axisToDetMF.getLabel().setText("Axis to Detector = " + String.format("%.3f" + " cm", axisToDet));
					numAnglesNF.setNumber(numAngles);
					detPixCntNF.setNumber(detMinCnt);					
					detMinCntMF.getLabel().setText("Minimum Detector Width = " + detMinCnt + " pixels");				
					paddedWidthMF.getLabel().setText("Padded Image Width = " + paddedWidth + " pixels");
					case "detPixCnt":
					detPixCnt = (int) detPixCntNF.getNumber();
					if(detPixCnt > originalWidth)
					{
						numAngles = (int) (Math.ceil(Math.PI*detPixCnt/2));
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
					String padOption = choice.getSelectedItem();
					paddedWidth = getMinDetCnt(originalWidth,1,padOption);
					paddedWidthMF.getLabel().setText("Padded Image Width = " + paddedWidth + " pixels");				
					detMinCnt = getMinDetCnt(paddedWidth,bfpSet.magnification,padOption);
					detPixCntNF.setNumber(detMinCnt);
					detMinCntMF.getLabel().setText("Minimum Detector Width = " + detMinCnt + " pixels");				
					numAngles = (int) (Math.ceil(Math.PI*detMinCnt/2));					
					if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
					numAnglesNF.setNumber(numAngles);
					break;
				}
			}

		}
		getSelections(gd);
		
		return dialogOK;
	}

	//*******************************************************************************

	private boolean doDialog()
	{
		//String dir = IJ.getDirectory("plugins");
		//dir= dir.replace("\\","/");
		//String myURL = "file:///" + dir + "jars/MuMassCalculatorDocs/index.html";

		final String[] targetSymb = {"Ag","Au","Cr","Cu","Mo","Rh","W"};
		final String[] filterSymb = {"Ag","Al","Cu","Er","Mo","Nb","Rh","Ta"};
		final String[] padOptions = {"None","Circumscribed", "Next Power of 2"};

		//srcToSampCM is presented for user information
		double srcToSampCM = bfpSet.srcToDetCM/bfpSet.magnification;		
		String padChoice = padOptions[0];
		int detMinCnt = getMinDetCnt(originalWidth,bfpSet.magnification,padChoice);
		int paddedWidth = getMinDetCnt(originalWidth,1,padChoice);;

		gd.addDialogListener(this);
		gd.setInsets(10,0,0);
		gd.addMessage("This plugin scans tagged images\nto bremsstrahlung sinograms.",myFont,Color.BLACK);
		gd.setInsets(10,0,0);
		gd.addMessage("360 degree Scan______________",myFont,Color.BLACK);
		gd.setInsets(10,0,0);
		
		if ((bfpSet.numAng ^ 1) == bfpSet.numAng - 1)	bfpSet.numAng++;			
		gd.addNumericField("Suggested_View_Angles", bfpSet.numAng);
		numAnglesNF = gda.getNumericField(gd, null, "numAngles");

		gd.addChoice("Pad_Options", padOptions, padOptions[0]);
		padOptionsCF = gda.getChoiceField(gd, null, "padOptions");
		gd.addNumericField("Detector_Pixels = " , detMinCnt);
		detPixCntNF = gda.getNumericField(gd, null, "detPixCnt");
		gd.addNumericField("Source to Detector(cm):", bfpSet.srcToDetCM);
		srcToDetNF = gda.getNumericField(gd, null, "srcToDet");
		gd.addNumericField("Magnification:", bfpSet.magnification);
		magnificationNF = gda.getNumericField(gd, null, "magnification");
		gd.addMessage("Axis to Detector = " + String.format("%.3f" + " cm", bfpSet.srcToDetCM - srcToSampCM ));
		axisToDetMF = gda.getMessageField(gd, "axisToDetector");
		gd.addMessage("Padded Image Width = " +  paddedWidth + "pixels");
		paddedWidthMF = gda.getMessageField(gd, "paddedWidth");
		gd.addMessage("Minimum Detector Width = " +  detMinCnt + "pixels");
		detMinCntMF = gda.getMessageField(gd, "detMinWidth");

		//X-ray Source
		gd.setInsets(10,0,0);
		gd.addMessage("X-ray Source________________",myFont,Color.BLACK);
		gd.addChoice("Target",targetSymb,"W");
		gd.addNumericField("KV", bfpSet.kv);
		gd.addNumericField("mA", bfpSet.ma);
		gd.addNumericField("KeV Bins", bfpSet.nBins);
		gd.addNumericField("Min KeV", bfpSet.minKV);
		
		//Filter
		gd.setInsets(10,0,0);
		gd.addMessage("Source Filter________________",myFont,Color.BLACK);
		gd.addChoice("Material",filterSymb,bfpSet.filter);
		gd.addNumericField("Thickness(cm)", bfpSet.filterCM);
		
		//Detector
		gd.setInsets(10,0,0);
		gd.addMessage("Detector___________________",myFont,Color.BLACK);
		gd.addStringField("Formula", bfpSet.detFormula);
		gd.addNumericField("Thickness(cm)", bfpSet.detCM);
		gd.addNumericField("Density(gm/cc)", bfpSet.detGmPerCC);
		gd.addCheckbox("Scale to 16-bit proj", scale16);
		gd.addNumericField("Scale Factor", scaleFactor);
		
		//gd.addCheckbox("Pad Image", padImage);
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

	private void doRoutine(FanProjectors.BremFanParams bfpSet)
	{

		float[] sinogram = null;
		ImagePlus sinoImp;				
		Object image;		
		CanvasResizer resizer= new CanvasResizer();
		
		int nslices = imageImp.getNSlices();

		ArrayList<float[]> sinograms = new ArrayList<float[]>();


		for(int i=1;i<=nslices;i++)
		{
			IJ.showProgress((double)i/(double)nslices);
			imageImp.setSlice(i);

			if(paddedWidth>originalWidth || paddedWidth>originalHeight)
			{ //to conserve memory the stack slices are individually copied, padded, projected and disposed
				ImagePlus sliceImp = imageImp.crop();
				ImageProcessor padIp = resizer.expandImage(sliceImp.getProcessor(), paddedWidth, paddedWidth,(paddedWidth-originalWidth)/2, (paddedWidth-originalHeight)/2);
				image = padIp.getPixels();
				sinogram = fanPrj.imageToBremsstrahlungFanBeamSinogram2(bfpSet,(float [])image,paddedWidth,paddedWidth);
				sinograms.add(sinogram);
			}
			else
			{
				image = imageImp.getProcessor().getPixels();
				sinogram = fanPrj.imageToBremsstrahlungFanBeamSinogram2(bfpSet,(float [])image,originalWidth,originalWidth);
				sinograms.add(sinogram);
			}
		}

		String title;
		String name = imageImp.getTitle();
		int dotIndex = name.lastIndexOf(".");
		if(dotIndex != -1) title = name.substring(0, dotIndex);
		else title  = name;
		title += "_FanBremSino";

		int length = sinogram.length;
		int width = length/bfpSet.numAng;
		int height = bfpSet.numAng;
		if(scale16)
		{
			sinoImp = IJ.createImage(title, width, height, nslices, 16);
			short[] pixels = (short[])sinoImp.getProcessor().getPixels();
			for(int i=1;i<= nslices;i++)
			{
				float[] temp = sinograms.get(i-1);
				for(int j=0;j<pixels.length;j++)
				{
					if(temp[j]<0) temp[j]=0;
					pixels[j]= (short) (temp[j]*scaleFactor);
				}
				sinoImp.getProcessor().setPixels(pixels);
			}
		}
		else
		{
			sinoImp = IJ.createImage(title, width, height, nslices, 32);
			for(int i=1;i<=nslices;i++)
			{
				sinoImp.setSlice(i);
				sinoImp.getProcessor().setPixels(sinograms.get(i-1));
			}
		}

		//these properties are preserved in the files tiff header
		String[] props = new String[28];
		props[0]="Geometry"; 
		props[1]="Fan Beam"; 
		props[2]="Source";
		props[3]="Bremsstrahlung";
		props[4]="Source KV";
		props[5]=Double.toString(bfpSet.kv);
		props[6]="Source mA";
		props[7]=Double.toString(bfpSet.ma);
		props[8]="Source Target";
		props[9]=bfpSet.target;
		props[10]="Min keV";
		props[11]=Double.toString(bfpSet.minKV);
		props[12]="Bins";
		props[13]=Double.toString(bfpSet.nBins);
		props[14]="Filter";
		props[15]=bfpSet.filter;
		props[16]="Filter(cm)";
		props[17]=Double.toString(bfpSet.filterCM);			
		props[18]="Source To Detector";
		props[19]=Double.toString(bfpSet.srcToDetCM);			
		props[20]="Magnification";
		props[21]=Double.toString(bfpSet.magnification);			
		props[22]="Detector";
		props[23]=bfpSet.detFormula;
		props[24]="Detector(cm)";
		props[25]=Double.toString(bfpSet.detCM);
		props[26]="Detector(gm/cc)";
		props[27]=Double.toString(bfpSet.detGmPerCC);
		sinoImp.setProperties(props);

		// Set the sinogram X,Y units
		//The pixel values are in per pixel units
		Calibration  imgCal = imageImp.getCalibration();		
		String unit = imgCal.getUnit();	// bark if not "cm" ?
		double pixSize = imgCal.getX(1); //cm per pixel
		Calibration sinoCal = sinoImp.getCalibration();
		sinoCal.setXUnit(unit);
		sinoCal.setYUnit("Deg");
		sinoCal.pixelWidth = pixSize/bfpSet.magnification;
		sinoCal.pixelHeight = 360.0/bfpSet.numAng;

		ImageStatistics stats = sinoImp.getStatistics();
		sinoImp.getProcessor().setMinAndMax(stats.min, stats.max);
		imageImp.setSlice(1);
		sinoImp.setSlice(1);
		sinoImp.show();			
	}
	
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
//	private double getSrcToSamp(double mag, double srcToDet)
//	{
//		return srcToDet/mag;
//	}
//	private double getSrcToDet(double mag, double srcToSamp)
//	{
//		return mag*srcToSamp;
//	}
	//*******************************************************************************

	private FanProjectors.BremFanParams getDialogDefaultSettings()
	{
		FanProjectors.BremFanParams dlogSet = new FanProjectors.BremFanParams();
		dlogSet.target = "W";
		dlogSet.kv = 160;
		dlogSet.ma = 100;
		dlogSet.nBins = 20;
		dlogSet.minKV = 20;

		dlogSet.filter = "Cu";
		dlogSet.filterCM = 0.1f;
		dlogSet.filterGmPerCC = 8.41;

		//Tagged Image
		dlogSet.pixSizeCM=.001f;
		//convert Default tag data to arrays
		int[] tag = new int[tagSet.tagData.size()];
		String[] name =  new String[tagSet.tagData.size()];
		String[] formula =  new String[tagSet.tagData.size()];
		double[] gmPerCC =  new double[tagSet.tagData.size()];
		int i=0;
		for(MatlListTools.TagData td : tagSet.tagData)
		{
			tag[i] = td.matlTag;
			name[i] = td.matlName;
			formula[i] = td.matlFormula;
			gmPerCC[i] = td.matlGmPerCC;
			i++;
		}
		
		dlogSet.matlTag=tag;
		dlogSet.matlName=name;
		dlogSet.matlFormula=formula;
		dlogSet.matlGmPerCC=gmPerCC;

		//CT params
		dlogSet.numAng=(int)(0.5*Math.PI*imageImp.getWidth()*1.5f);
		dlogSet.srcToDetCM=100;
		dlogSet.magnification=1.5f;

		//Detector
		dlogSet.detFormula="Cs:1:I:1";
		dlogSet.detCM=.01;
		dlogSet.detGmPerCC=8.41;		
		scale16=false;
		
		return dlogSet;		
	}
	
	//*******************************************************************************

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
		if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
		return numAngles;
	}

	//*******************************************************************************

	private void getSelections(GenericDialog gd)
{
	//GenericDialog.getNext... calls are required for macro recording
	//They depend on the ordering of the Dialog components
	//Rearranging the Dialog components breaks this code
	gd.resetCounters();

	bfpSet.numAng = (int)gd.getNextNumber();
	bfpSet.pixSizeCM = imageImp.getCalibration().pixelWidth;
	String padOption = gd.getNextChoice();	
	double mag = magnificationNF.getNumber();
	paddedWidth =(int)(gd.getNextNumber()/mag);
	bfpSet.srcToDetCM = (float)gd.getNextNumber();
	bfpSet.magnification = (float)gd.getNextNumber();
	bfpSet.target = gd.getNextChoice();
	bfpSet.kv = (float)gd.getNextNumber();
	bfpSet.ma =	(float)gd.getNextNumber();
	bfpSet.nBins = (int)gd.getNextNumber();
	bfpSet.minKV =(float)gd.getNextNumber();
	bfpSet.filter = gd.getNextChoice();
	bfpSet.filterCM = (float)gd.getNextNumber();		
	bfpSet.filterGmPerCC = mmc.getAtomGmPerCC(bfpSet.filter);		
	bfpSet.detFormula = gd.getNextString();
	bfpSet.detCM = 	(float)gd.getNextNumber();
	bfpSet.detGmPerCC =	(float)gd.getNextNumber();
	scale16= gd.getNextBoolean();
	scaleFactor =gd.getNextNumber();
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
		
		//the original image width, height, pixelSize and unit
		originalWidth =ip.getWidth();
		originalHeight =ip.getHeight();
		if(originalHeight != originalWidth)
		{
			IJ.showMessage("Image must be Square. Check the PadImage Box in the next dialog");
			return;
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
		pixelSize = cal.pixelWidth;
		
		tagSet = mlt.loadTagFile(tagSetPath);

		//Read the saved dialog settings
		bfpSet = (FanProjectors.BremFanParams)ser.ReadSerializedObject(settingsPath);		
		if(bfpSet==null)
		{
			bfpSet = getDialogDefaultSettings();
		}
		else // the DefaultMaterials.csv file may have been modified since previous plugin run
		{
			bfpSet.matlFormula = mlt.getTagSetMatlFormulasAsArray(tagSet);
			bfpSet.matlGmPerCC = mlt.getTagSetMatlGmPerccAsArray(tagSet);
			bfpSet.matlName = mlt.getTagSetMatlNamesAsArray(tagSet);
			bfpSet.matlTag = mlt.getTagSetMatlTagAsArray(tagSet);
		}
				
		if(doDialog())
		{
			getSelections(gd);
			if(validateParams(bfpSet))
			{
				doRoutine(bfpSet);
				ser.SaveObjectAsSerialized(bfpSet, settingsPath);
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

	private boolean validateParams(FanProjectors.BremFanParams bfpSet)
	{
		//Test the formulas
		ArrayList<AtomData> formula;
		formula = mmc.createFormulaList(bfpSet.target);
		if(formula==null) {IJ.error(bfpSet.target + " Is not a valid target material"); return false;}
		formula = mmc.createFormulaList(bfpSet.filter);
		if(formula==null) {IJ.error(bfpSet.filter + " Is not a valid filter material"); return false;}
		
		if(bfpSet.matlFormula==null){IJ.error("Missing Formulas"); return false;}
		if(bfpSet.matlGmPerCC==null){IJ.error("Missing Densities"); return false;}
		
		for(int i=1;i< bfpSet.matlFormula.length;i++)
		{
			if(bfpSet.matlGmPerCC[i] < 0){IJ.error("Material 1 Density " + bfpSet.matlGmPerCC[i] + " Cannot be negative"); return false;}
			if(bfpSet.matlFormula[i] == null){IJ.error("Missing Formula at item " + i); return false;}
			formula = mmc.createFormulaList(bfpSet.matlFormula[i]);
			if(formula==null) {IJ.error(bfpSet.matlFormula[i] + " Is not a valid  material"); return false;}			
		}
		
		formula = mmc.createFormulaList(bfpSet.detFormula);
		if(formula==null) {IJ.error(bfpSet.detFormula + " Is not a valid detector material"); return false;}
		
		//Test the numbers
		if(bfpSet.kv < bfpSet.minKV){IJ.error("Source KV " + bfpSet.kv + " Must be greater than " + bfpSet.minKV + "KV"); return false;}
		if(bfpSet.kv <=0){IJ.error("Source KV " + bfpSet.kv + " Must be greater than 0 KV"); return false;}
		if(bfpSet.ma <=0){IJ.error("Source mA " + bfpSet.ma + " Must be greater than 0 mA"); return false;}
		if(bfpSet.nBins <=0){IJ.error("Bin Count " + bfpSet.nBins + " Must be greater than 0"); return false;}
		if(bfpSet.minKV > bfpSet.kv){IJ.error("Source minMV " + bfpSet.minKV + " Must be less than " + bfpSet.kv + "KV"); return false;}
		if(bfpSet.filterCM < 0){IJ.error("Filter Thickness " + bfpSet.filterCM + " Cannot be negative"); return false;}
		if(bfpSet.filterGmPerCC <= 0){IJ.error("Filter Density " + bfpSet.filterGmPerCC + " Cannot be negative"); return false;}
		
		if(bfpSet.numAng < 1){IJ.error("Number of angles " + bfpSet.numAng + " Cannot be negative or zero"); return false;}
		if(bfpSet.detCM <= 0){IJ.error("Detector Thickness " + bfpSet.detCM + " Cannot be negative"); return false;}
		if(bfpSet.detGmPerCC <= 0){IJ.error("Detector Densith " + bfpSet.detCM + " Cannot be negative or zero"); return false;}
		
		return true;
	}

}
