package CT_Tools;

/*
 * This plugin simulates a parallel beam CT scan from 0 to 180 degrees
 * of a segmented 2D image using a conventional x-ray source and a scintillation
 * detector.
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
 * A Ct scan is simulated by rotating and projecting the image to form a simogram.
 * 
 *  The Image:
 *  1. is segmented into N components. The pixel values of each component is assigned a number ID.
 *  2. for each
 */

import ij.IJ;

import ij.ImagePlus;
import ij.plugin.CanvasResizer;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.gui.*;
import ij.measure.Calibration;

//import java.awt.event.*;
import java.io.File;
import java.awt.*;
import java.util.ArrayList;
//import java.util.Vector;
//import java.util.Properties;
import java.util.Arrays;

import jhd.MuMassCalculator.*;
//import jhd.MuMassCalculator.ParallelProjectors;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import gray.AtomData.*;
//import jhd.Projection.*;
import jhd.Projection.ParallelProjectors;
import jhd.Projection.ParallelProjectors.BremParallelParams;
import jhd.Serialize.Serializer;
import jhd.TagTools.MatlListTools;

//*******************************************************************************

public class Tag_Image_To_Parallel_Brems_Sinogram implements PlugInFilter , DialogListener
{
	final String myDialogTitle = "Polychromatic Parallel Beam CTscan";	
	final String mySettingsTitle = "Polychromatic_ParallelBeam_Params";
		
	//Used to test formulas prior to launching the simulator
	MuMassCalculator mmc = new MuMassCalculator();
	
	//The methods that do the projections
	ParallelProjectors parPrj = new ParallelProjectors();	
	
	//A serializable class for storing the  user supplied parameters
	BremParallelParams bppSet = new  BremParallelParams();
	
	//The class used to serialize and save the users selections
	Serializer ser = new Serializer();
		
	//The class used to manage materials Lists
	MatlListTools mlt=new MatlListTools();
	
	//The nested class containing  materials list tag information
	MatlListTools.TagSet tagSet;
	
	//the full path to the default tagSet
	String tagSetPath = IJ.getDirectory("plugins") + "DialogData\\DefaultMaterials.csv";

	//The ImageJ GenericDialog class
	GenericDialog gd = new GenericDialog(myDialogTitle);
	//Addins to make referencing the dialog's components much simpler
	GenericDialogAddin gda = new GenericDialogAddin();
	
	//Arrays to unpack TagData materials lists
	String[] matlArr;
	String[] formula;	
	double[] gmPerCC;
	
	//Local parameters
	String[] targetSymb = Arrays.copyOf(mmc.getAtomSymbols(),mmc.getAtomSymbols().length); //{"Ag","Au","Cr","Cu","Mo","Rh","W"};
	String[] filterSymb = Arrays.copyOf(mmc.getAtomSymbols(),mmc.getAtomSymbols().length); //{"Ag","Al","Cu","Er","Mo","Nb","Rh","Ta"};

	final String[] padOptions = {"None","Circumscribed", "Next Power of 2"};
	boolean scale16;//,padImage;
	ImagePlus imageImp;
	int originalWidth,originalHeight;
	double pixelSize;
	String unit;
	int	detPixCnt;		
	double scaleFactor = 6000;
	
	String dir = IJ.getDirectory("plugins");
	
	String settingsPath = dir+ "DialogSettings" + File.separator + mySettingsTitle + ".ser";

	final Color myColor = new Color(240,230,190);//slightly darker than buff
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	//Some of these are not be used
//	ChoiceField srcTargetCF;
//	NumericField srcAccelVoltsNF,srcMilliAmpsNF,srcKevBinsNF,srcKevMinNF;	
//	ChoiceField filterMaterialCF;
//	NumericField filterThicknessNF;	
	NumericField detThicknessNF,detDensityNF;
	StringField detFormulaSF;
	ChoiceField padOptionsCF,detMaterialCF;
//	CheckboxField scale16CBF;
	NumericField detPixCntNF;
	NumericField scaleFactorNF;	
	NumericField numAnglesNF;
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = true;
		if(e!=null)
		{
			getSelections(gd);
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
						numAngles = (int) (Math.ceil(Math.PI*detPixCnt));
						break;
					case "Custom":
						break;
					}
					//make numAngles even
					if ((numAngles ^ 1) == numAngles - 1)	numAngles++;	
					numAnglesNF.setNumber(numAngles);
					break;
				case "detectorMaterial":
					int index = detMaterialCF.getChoice().getSelectedIndex();
					detFormulaSF.getTextField().setText(bppSet.matlFormula[index]);
					detDensityNF.setNumber(bppSet.matlGmPerCC[index]);					
					break;
				}
			}

		}
		getSelections(gd);
		
		return dialogOK;
	}
	
	//*******************************************************************************

	private boolean DoDialog()
	{
		int detPixCnt= imageImp.getWidth();

		gd.addDialogListener(this);
		gd.setInsets(10,0,0);
		gd.addMessage("This plugin scans tagged images\nto bremsstrahlung sinograms.",myFont,Color.BLACK);
		gd.setInsets(10,0,0);
		gd.addMessage("180 degree Scan______________",myFont,Color.BLACK);
		gd.setInsets(10,0,0);
		gd.addMessage("Minimum Detector Width = " + originalWidth + " pixels");
		
		//set numAngles according to the current image
		int numAngles = (int) (originalWidth*Math.PI/2);
		//make numAngles even
		if ((numAngles ^ 1) == numAngles - 1)	numAngles++;			
		gd.addNumericField("Suggested_View_Angles", numAngles);
		numAnglesNF = gda.getNumericField(gd, null, "numAngles");
		gd.addChoice("Pad_Options", padOptions, padOptions[0]);
		padOptionsCF = gda.getChoiceField(gd, null, "padOptions");
		gd.addNumericField("Detector_Pixels = " , detPixCnt);
		detPixCntNF = gda.getNumericField(gd, null, "detPixCnt");

		//X-ray Source set to previous selections
		gd.addMessage("X-ray Source________________",myFont,Color.BLACK);
		gd.addChoice("Target",targetSymb,bppSet.target);
		//srcTargetCF = gda.getChoiceField(gd, null, "srcTarget");
		gd.addNumericField("KV", bppSet.kv);
		//srcAccelVoltsNF = gda.getNumericField(gd, null, "srcAccelVolts");
		gd.addNumericField("mA", bppSet.ma);
		//srcMilliAmpsNF = gda.getNumericField(gd, null, "srcMilliAmps");
		gd.addNumericField("KeV Bins", bppSet.nBins);
		//srcKevBinsNF = gda.getNumericField(gd, null, "srcKevBins");
		gd.addNumericField("Min KeV", bppSet.minKV);
		//srcKevMinNF = gda.getNumericField(gd, null, "srcKevMin");
		
		//Filter set to previous selections
		gd.setInsets(10,0,0);
		gd.addMessage("Source Filter________________",myFont,Color.BLACK);
		gd.addChoice("Material",filterSymb,bppSet.filter);
		//filterMaterialCF = gda.getChoiceField(gd, null, "filterMaterial");
		gd.addNumericField("Thickness(cm)", bppSet.filterCM);
		//filterThicknessNF = gda.getNumericField(gd, null, "filterThickness");
								
		//Detector set to previous selections
		gd.setInsets(10,0,0);
		gd.addMessage("Detector___________________",myFont,Color.BLACK);
		
		//because the detector material name is not saved in the parameters
		//find the detector material name using the detector formula
		int index = 0;
		for(int i =0;i<bppSet.matlFormula.length;i++)
		{
			if(bppSet.detFormula.equals(bppSet.matlFormula[i]))
			{
				index=i;
				break;
			}			
		}
		gd.addChoice("Detector",bppSet.matlName, bppSet.matlName[index]);
		detMaterialCF = gda.getChoiceField(gd, null, "detectorMaterial");
	
		gd.addStringField("Formula", bppSet.detFormula);
		detFormulaSF = gda.getStringField(gd, null, "detFormula");
		gd.addNumericField("Thickness(cm)", bppSet.detCM);
		detThicknessNF = gda.getNumericField(gd, null, "detThickness");
		gd.addNumericField("Density(gm/cc)", bppSet.detGmPerCC);
		detDensityNF = gda.getNumericField(gd, null, "detDensity");
		
		gd.addCheckbox("Scale to 16-bit proj", false);
		//scale16CBF = gda.getCheckboxField(gd, "scale16");
		gd.addNumericField("Scale_Factor", scaleFactor);
		scaleFactorNF = gda.getNumericField(gd, null, "scaleFactor");
				
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
	
	private void DoRoutine(ParallelProjectors.BremParallelParams bppSet)
	{
		if(imageImp.getBitDepth() == 32)
		{
			float[] sinogram;
			ImagePlus sinoImp;
			ImageProcessor sinoIp;
			Object image;
			CanvasResizer resizer= new CanvasResizer();

			Calibration  imgCal = imageImp.getCalibration();		
			String unit = imgCal.getUnit();	// bark if not "cm" ?
			double pixSize = imgCal.pixelWidth;
			int nslices = imageImp.getNSlices();

			String title;
			String name = imageImp.getTitle();
			int dotIndex = name.lastIndexOf(".");
			if(dotIndex != -1) title = name.substring(0, dotIndex);
			else title  = name;
			title += "_ParBremSino";

			if(scale16)sinoImp = IJ.createImage(title, detPixCnt, bppSet.numAng, nslices, 16);				
			else sinoImp = IJ.createImage(title, detPixCnt, bppSet.numAng, nslices, 32);
			// append "ParBremSino" and the angle count to the image name

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
					sinogram = parPrj.imageToBremsstrahlungParallelSinogram2(bppSet, (float [])image, detPixCnt, detPixCnt);
					sliceImp.close();
				}
				else
				{
					image = imageImp.getProcessor().getPixels();
					sinogram = parPrj.imageToBremsstrahlungParallelSinogram2(bppSet, (float [])image, detPixCnt, detPixCnt);					
				}				
				if(scale16)
				{
					short[] sino16 = new short[sinogram.length];
					for(int j = 0; j<sinogram.length;j++)
					{
						sino16[j] = (short) (sinogram[j]*scaleFactor);
					}
					sinoIp.setPixels(sino16);
				}
				else
				{
					sinoIp.setPixels(sinogram);
				}
			}

			//these properties are preserved in the images tiff file header
			String[] props = new String[26];
			props[0]="Geometry"; 
			props[1]="Parallel";
			props[2]="Source";
			props[3]="Bremsstrahlung";			
			props[4]="Source KV";
			props[5]=Double.toString(bppSet.kv);
			props[6]="Source mA";
			props[7]=Double.toString(bppSet.ma);
			props[8]="Source Target";
			props[9]=bppSet.target;
			props[10]="Min keV";
			props[11]=Double.toString(bppSet.minKV);
			props[12]="Bins";
			props[13]=Double.toString(bppSet.nBins);
			props[14]="Filter";
			props[15]=bppSet.filter;
			props[16]="Filter(cm)";
			props[17]=Double.toString(bppSet.filterCM);
			props[18]="Detector";
			props[19]=bppSet.detFormula;
			props[20]="Detector(cm)";
			props[21]=Double.toString(bppSet.detCM);
			props[22]="Detector(gm/cc)";
			props[23]=Double.toString(bppSet.detGmPerCC);
			props[24]="ScaleFactor";
			if(scale16) props[25]=Double.toString(scaleFactor);
			else props[25]="Not Scaled";
			sinoImp.setProperties(props);

			// Set the sinogram X,Y units
			//The sinogram pixel values are in per pixel units
			imgCal = imageImp.getCalibration();		
			unit = imgCal.getUnit();	// bark if not "cm" ?
			pixSize = imgCal.getX(1); //cm per pixel
			Calibration sinoCal = sinoImp.getCalibration();
			sinoCal.setXUnit(unit);
			sinoCal.setYUnit("Deg");
			sinoCal.pixelWidth = pixSize;
			sinoCal.pixelHeight = 180.0/bppSet.numAng;

			ImageStatistics stats = sinoImp.getStatistics();
			sinoIp.setMinAndMax(stats.min, stats.max);
			imageImp.setSlice(1);
			sinoImp.setSlice(1);
			sinoImp.show();			
		}
	}
	
	//*******************************************************************************

	private ParallelProjectors.BremParallelParams getDialogDefaultSettings()
	{
		ParallelProjectors.BremParallelParams dlogSet = new ParallelProjectors.BremParallelParams();
		dlogSet.target = "W";
		dlogSet.kv = 160;
		dlogSet.ma = 100;
		dlogSet.nBins = 20;
		dlogSet.minKV = 20;

		dlogSet.filter = "Cu";
		dlogSet.filterCM = 0.1f;
		dlogSet.filterGmPerCC = 8.41;

		//Tagged Image
		
		dlogSet.pixSizeCM=imageImp.getCalibration().pixelWidth;		
		//convert Default tag data to arrays
		int[] tag =  mlt.getTagSetMatlTagAsArray(tagSet);//new int[tagSet.tagData.size()];
		String[] name =  mlt.getTagSetMatlNamesAsArray(tagSet);//new String[tagSet.tagData.size()];
		String[] formula =  mlt.getTagSetMatlFormulasAsArray(tagSet);//new String[tagSet.tagData.size()];
		double[] gmPerCC =  mlt.getTagSetMatlGmPerccAsArray(tagSet);//new double[tagSet.tagData.size()];

		dlogSet.matlTag=tag;
		dlogSet.matlName=name;
		dlogSet.matlFormula=formula;
		dlogSet.matlGmPerCC=gmPerCC;
		
		//CT params
		dlogSet.numAng=(int)(0.5*Math.PI*imageImp.getWidth());

		//Detector
		dlogSet.detFormula="Cs:1:I:1";
		dlogSet.detCM=.01;
		dlogSet.detGmPerCC=8.41;		
		scale16=false;
		
		return dlogSet;		
	}
	
	//*******************************************************************************

	private void getSelections(GenericDialog gd)
	{
		//GenericDialog.getNext... calls are required for macro recording
		//They depend on the ordering of the Dialog components
		//Rearranging the Dialog components breaks this code
		gd.resetCounters();
		bppSet.numAng = (int)gd.getNextNumber();
		String padOption = gd.getNextChoice();
		detPixCnt = (int) gd.getNextNumber();
		bppSet.pixSizeCM = pixelSize;
		bppSet.target = gd.getNextChoice();
		bppSet.kv = gd.getNextNumber();
		bppSet.ma = gd.getNextNumber();
		bppSet.nBins = (int)gd.getNextNumber();
		bppSet.minKV = gd.getNextNumber();
		bppSet.filter = gd.getNextChoice();
		bppSet.filterCM = gd.getNextNumber();
		bppSet.detFormula = gd.getNextString();
		bppSet.detCM = gd.getNextNumber();
		bppSet.detGmPerCC =  gd.getNextNumber();
		scale16 = gd.getNextBoolean();
		scaleFactor =  gd.getNextNumber();
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
		
		//Sort the element arrays
		Arrays.sort(targetSymb);
		Arrays.sort(filterSymb);
		
		//the original image width and height
		originalWidth =ip.getWidth();
		originalHeight =ip.getHeight();
		if(originalHeight != originalWidth)
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
		
		pixelSize = cal.pixelWidth;

		tagSet = mlt.loadTagFile(tagSetPath);
		if(tagSet==null)
		{
			IJ.error("The Materials tagSet failed to load\n"
					+ "Please locate or create \"DefaultMaterials.csv\"\n"
					+ "and place it in the plugins/DialogData folder");
			return;
		}
				
		//Read the saved dialog settings
		bppSet = (ParallelProjectors.BremParallelParams)ser.ReadSerializedObject(settingsPath);
		
		if(bppSet==null)
		{
			bppSet = getDialogDefaultSettings();
		}
		else // the DefaultMaterials.csv file may have been modified since previous plugin run
		{
			bppSet.matlFormula = mlt.getTagSetMatlFormulasAsArray(tagSet);
			bppSet.matlGmPerCC = mlt.getTagSetMatlGmPerccAsArray(tagSet);
			bppSet.matlName = mlt.getTagSetMatlNamesAsArray(tagSet);
			bppSet.matlTag = mlt.getTagSetMatlTagAsArray(tagSet);
		}

		if(DoDialog())
		{
			if(ValidateParams(bppSet))
			{
//				IJ.log("detCM="+bppSet.detCM);
//				IJ.log("detFormula="+bppSet.detFormula);
//				IJ.log("detGmPerCC="+bppSet.detGmPerCC);
//				IJ.log("filter="+bppSet.filter);
//				IJ.log("filterCM="+bppSet.filterCM);
//				IJ.log("filterGmPerCC="+bppSet.filterGmPerCC);
//				IJ.log("kv="+bppSet.kv);
//				IJ.log("ma="+bppSet.ma);
//				IJ.log("minKV="+bppSet.minKV);
//				IJ.log("nBins="+bppSet.nBins);
//				IJ.log("numAng="+bppSet.numAng);
//				IJ.log("pixSizeCM="+bppSet.pixSizeCM);
//				IJ.log("target="+bppSet.target);				
				DoRoutine(bppSet);
				ser.SaveObjectAsSerialized(bppSet, settingsPath);
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
	
	private boolean ValidateParams(ParallelProjectors.BremParallelParams bppSet)
	{
		//Test the formulas
		ArrayList<AtomData> formula;

		//Pre-screen the formulas for correctness
		formula = mmc.createFormulaList(bppSet.target);
		if(formula==null) {IJ.error(bppSet.target + " Is not a valid target material"); return false;}
		formula = mmc.createFormulaList(bppSet.filter);
		if(formula==null) {IJ.error(bppSet.filter + " Is not a valid filter material"); return false;}
		
		if(bppSet.matlFormula==null){IJ.error("Missing Formulas"); return false;}
		if(bppSet.matlGmPerCC==null){IJ.error("Missing Densities"); return false;}
		
		for(int i=1;i< bppSet.matlFormula.length;i++)
		{
			if(bppSet.matlGmPerCC[i] < 0){IJ.error("Material 1 Density " + bppSet.matlGmPerCC[i] + " Cannot be negative"); return false;}
			if(bppSet.matlFormula[i] == null){IJ.error("Missing Formula at item " + i); return false;}
			formula = mmc.createFormulaList(bppSet.matlFormula[i]);
			if(formula==null) {IJ.error(bppSet.matlFormula[i] + " Is not a valid  material"); return false;}			
		}
		
		formula = mmc.createFormulaList(bppSet.detFormula);
		if(formula==null) {IJ.error(bppSet.detFormula + " Is not a valid detector material"); return false;}
		
		//Test the numbers
		if(bppSet.kv < bppSet.minKV){IJ.error("Source KV " + bppSet.kv + " Must be greater than " + bppSet.minKV + "KV"); return false;}
		if(bppSet.kv <=0){IJ.error("Source KV " + bppSet.kv + " Must be greater than 0 KV"); return false;}
		if(bppSet.ma <=0){IJ.error("Source mA " + bppSet.ma + " Must be greater than 0 mA"); return false;}
		if(bppSet.nBins <=0){IJ.error("Bin Count " + bppSet.nBins + " Must be greater than 0"); return false;}
		if(bppSet.minKV > bppSet.kv){IJ.error("Source minMV " + bppSet.minKV + " Must be less than " + bppSet.kv + "KV"); return false;}
		
		if(bppSet.filterCM < 0){IJ.error("Filter Thickness " + bppSet.filterCM + " Cannot be negative"); return false;}
		if(bppSet.filterGmPerCC <= 0){IJ.error("Filter Density " + bppSet.filterGmPerCC + " Cannot be negative"); return false;}
		
		if(bppSet.numAng < 1){IJ.error("Number of angles " + bppSet.numAng + " Cannot be negative or zero"); return false;}
		if(bppSet.detCM <= 0){IJ.error("Detector Thickness " + bppSet.detCM + " Cannot be negative"); return false;}
		if(bppSet.detGmPerCC <= 0){IJ.error("Detector Densith " + bppSet.detCM + " Cannot be negative or zero"); return false;}
		
		return true;
	}
}
