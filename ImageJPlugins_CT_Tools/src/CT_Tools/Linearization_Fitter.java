package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;

import ij.plugin.*;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;

import java.awt.*;
import java.awt.event.*;

import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
//import jhd.MuMassCalculator.*;
import jhd.Projection.*;
import tagTools.*;
//import tagTools.TagListImageTools.*;

/* 	Estimate "polynomial linearization" beam hardening coefficients from an uncorrected reconstructed slice.
 * Requires the reconstructed beam-hardened CT slice and a user-supplied tagged model image.
 * This allows use of pixel classifiers other than simple thresholding to create a tagged model image.
 * Plots the slice vs model attenuations and calls ImageJ's CurveFitter to find linearization coefficients.
 *  JHD 1/25/22
*/

/* Macro example for viewing plot vs energy
 *
for(keV=100;keV<251;keV+=5)
{
	run("Linearization Fitter", "ct=Al_CastingWithIronAndBrassPinsTagImage_ParBremSino-1_Recon-1 tag=Al_CastingWithIronAndBrassPinsTagImage.tif est.="+keV);
	kStr = d2s(keV,1);
	fitStr = "Fit Parameters "+kStr+"keV";
	close(fitStr);
	selectWindow("BeamHardening Plot");
	run("Duplicate...", "title=[BeamHardening Plot "+kStr+"keV]");
	selectWindow("BeamHardening Plot");
	close();	
}
run("Images to Stack", "name=BeamHardeningPlot title=BeamHardening use");

 */

public class Linearization_Fitter implements PlugIn , DialogListener ,ActionListener
{
	ParallelProjectors prj= new ParallelProjectors();
	
	GenericDialog gd;
	GenericDialogAddin gda;

	CurveFitter crvfit;
	ImagePlus dataImp,modelImp;
	ImageProcessor ip;
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	final Color buff = new Color(250,240,200);	
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Color errColor = new Color(255,100,0);
	final Color white = new Color(255,255,255);
	
	TagListTools.TagSet myTagSet;	
	TagListTools tagListTools = new TagListTools();
	TagListImageTools tagListImageTools = new TagListImageTools();
	
	String[] matlNames;

	int		matlIndex;	// the position of the material in the list
	float	low,high;
	float[] tauLUT;
	double	keV;
	String dataImageName;
	String modelImageName;
	String phiImageName;
	
	final int nViews = 18;

	//*****************************************************************

	@Override
	public void run(String arg)
	{

		if(IJ.versionLessThan("1.41k"))
		{
			IJ.error("ImageJ version 1.41k or better is required");
			return;
		}
		DoDialog();

	}

	//*****************************************************************
	NumericField kevNF;
	NumericField kevIncNF;
	ButtonField upBtnBF,downBtnBF;
	private void DoDialog()
	{
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		myTagSet = tagListTools.readTagSetFile(path);
		
		//Get names array from TagSet
		matlNames = new String[myTagSet.tagData.size()];
		int i=0;
		for(TagListTools.TagData td : myTagSet.tagData)
		{
			matlNames[i]= td.matlName;
			i++;
		}
				
		if(myTagSet!=null)
		{
			int winCnt = WindowManager.getImageCount();
			if(winCnt<2)
			{
				IJ.error("This plugin requires a CT slice and a Tagged model slice");
				return;
			}
			
			String[] winTitles = WindowManager.getImageTitles();
			String[] phiTitles = new String[winTitles.length + 1];
			phiTitles[0]="None";
			for(i=1;i<phiTitles.length;i++) phiTitles[i]=winTitles[i-1];
			
			gda = new GenericDialogAddin();
			//gd = new GenericDialog("Find Beam Hardening Corrections");
			gd= GUI.newNonBlockingDialog("Linearization Fitter with Porosity");
			gd.addDialogListener(this);
			//Materials
			//gd.setInsets(10,0,0);
			gd.addMessage("The CT Slice and Tag Image must be the same size\n"
					+ "with isotropic pixel size units in cm.\n"
					+ "The CT Slice value units must be linear attenuation (cm-1).\n"
					+ "Porosity image values must be between 0(solid) and 1(open pore).\n"
					+ "Click the\"+\" or \"-\" buttons to change energy for the best fit.\n"
					+ "At any time you can use the plot window's \"Data->Add Fit\"\n"
					+ "to view available fit curves.\n",myFont,Color.BLACK);
			
			gd.addChoice("CT Slice:", winTitles, winTitles[0]);
			gd.addChoice("Tag Image:", winTitles, winTitles[0]);
			gd.addChoice("Porosity Image:", phiTitles, phiTitles[0]);
								
			gd.addNumericField("Est. keV", 100);
			kevNF = gda.getNumericField(gd, null, "estKeV");
			SpinnerPanel sp = gda.new SpinnerPanel();
			TextField tf = (TextField)gd.getNumericFields().get(0);
			gd.addToSameRow();
			gd.addPanel(sp.addSpinner(tf,"estKev", 5.0));
					
			gd.addMessage("After clicking OK\n"
					+ "Do not close the \"Fit Parameters\" Window.\n"
					+ "Use the Apply Linearization plugin to select\n"
					+ "a \"Fit Parameters\" fit to apply to the CT slice's original sinogram",myFont,Color.BLACK);
			gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/Linearization.html");
			gd.setBackground(myColor);
			gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
			gd.showDialog();

			if(gd.wasOKed())
			{
				fitAttenuations();
				PlotWindow plotWin = (PlotWindow)WindowManager.getWindow("BeamHardening Plot");
				if(plotWin!=null) plotWin.setTitle("BeamHardening Plot " + keV +"keV");
				TextWindow txtWin = (TextWindow)WindowManager.getWindow("Fit Parameters");
				if(txtWin!=null) txtWin.setTitle("Fit Parameters "+ keV +"keV");			
			}
			else
			{
				PlotWindow plotWin = (PlotWindow)WindowManager.getWindow("BeamHardening Plot");
				if(plotWin!=null) plotWin.close();
				TextWindow txtWin = (TextWindow)WindowManager.getWindow("Fit Parameters");
				if(txtWin!=null) txtWin.close();			
			}
		}
		else return;		
	}

	//*****************************************************************
	
	private void getSelections()
	{
		gd.resetCounters();
		try
		{
			dataImageName = gd.getNextChoice();
			modelImageName   = gd.getNextChoice();
			phiImageName   = gd.getNextChoice();	
			keV=gd.getNextNumber();
		}
		catch (Exception e)
		{
			if(e instanceof NullPointerException)
			{
				IJ.showMessage("Error", "To record, the Macro Recorder must be open before\nlaunching the Linearization Fitter Plugin");
			}
			else
			{
				IJ.showMessage(e.getMessage());
				//e.printStackTrace();
			}
		}
	}

	private void fitAttenuations()
	{
		ImagePlus phiImp;

		getSelections();
		
		
		//Get the CTslice  and model data
		dataImp = WindowManager.getImage(dataImageName);
		modelImp = WindowManager.getImage(modelImageName);
		
		//create a default porosity image filled with zeros
		if(phiImageName.equals("None"))
		{
			phiImp = dataImp.duplicate();
			phiImp.getProcessor().multiply(0);
			phiImp.setTitle("porosity");
		}
		else
		{
			phiImp = WindowManager.getImage(phiImageName);			
		}

		if(!(	dataImp.getWidth()	== dataImp.getHeight() && 
				modelImp.getWidth()	==  modelImp.getHeight() && 
				phiImp.getWidth()	==  phiImp.getHeight() && 
				dataImp.getWidth()	== modelImp.getHeight() &&
				dataImp.getWidth()	== phiImp.getHeight()))
		{
			IJ.error("Selected images must be square and of equal size.");			
			return;
		}
		
		String dataUnit = dataImp.getCalibration().getUnit().toUpperCase();
		String modelUnit = modelImp.getCalibration().getUnit().toUpperCase();
		if(!dataUnit.equals("CM") || !modelUnit.equals("CM"))
		{
			IJ.error("Pixel units in both images must be cm");
			return;
		}
		
		//Projection comparison.
		ParallelProjectors parPrj = new ParallelProjectors();
		
		int width = dataImp.getWidth();
		float[] dataImg = (float[]) dataImp.getProcessor().getPixels();
		float[] dataSino = parPrj.imageToParallelSinogram(dataImg, width, width, nViews);
		
		//Don't step on the user's model image
		ImagePlus tagImp = modelImp.duplicate();
		//Convert tags to muLin
		float[] tagImg = (float[])tagImp.getProcessor().getPixels();		
		tagListImageTools.tagsToLinearAttn(tagImg, myTagSet, keV);
		
		//Multiply the tagImg by the porosity
		float[] phiImg = (float[])phiImp.getProcessor().getPixels();
		for(int i=0;i<tagImg.length;i++) tagImg[i] *= 1-phiImg[i];
		float[] modelSino = parPrj.imageToParallelSinogram(tagImg, width, width, nViews);

		//Sum the sinograms
		double pixSize = dataImp.getCalibration().pixelWidth;
		double[] dataProj = new double[width*nViews];
		double[] modelProj = new double[width*nViews];
		for(int i = 0;i< width*nViews;i++)
		{
			dataProj[i] += (double) dataSino[i]*pixSize;
			modelProj[i] += (double) modelSino[i]*pixSize;
		}
		
		//Plot the data
		Plot tauPlot;
		PlotWindow plotWin = (PlotWindow)WindowManager.getWindow("BeamHardening Plot");
		if(plotWin==null)
		{
			tauPlot = new Plot("BeamHardening Plot", "Data Attn.","Model Attn.");		
		   	tauPlot.setBackgroundColor(buff);
		   	tauPlot.setFontSize(14);
			tauPlot.addPoints(dataProj, modelProj, Plot.DOT);
			tauPlot.show();
			plotWin = (PlotWindow)WindowManager.getWindow("BeamHardening Plot");
		}
		tauPlot = plotWin.getPlot();
		tauPlot.setColor(Color.RED);
		tauPlot.setLineWidth(1.5f);		
		tauPlot.replace(0, "dot", dataProj, modelProj);
		tauPlot.setLimitsToFit(true);
		
		String str  =  "Data: " + dataImageName;
		str = str + "\nModel: " + modelImageName;
		str = str + "\nkeV: " + keV;
		tauPlot.setFont(Font.BOLD, 14f);
		tauPlot.setColor(Color.BLACK);
		tauPlot.addLabel(0.02, 0.1, str);		
		tauPlot.update();
		
		//Output the fit results table
		ResultsTable fitRT = ResultsTable.getResultsTable("Fit Parameters");
		if(fitRT==null) fitRT = new ResultsTable();
		fitRT.setPrecision(5);
		fitRT.deleteRows(0, fitRT.getCounter());
		String[] hdr = {"A","B","C","D","E","F","G","H"};

		double[] fitParams;
		int i;
		
		fitRT.incrementCounter();		
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.POLY6);
		fitParams = crvfit.getParams();
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.POLY6]);
		for(i=0;i< fitParams.length-1;i++)
		{
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());
		fitRT.addValue("Eeff", keV);		
		
	
		fitRT.incrementCounter();
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.POLY5);
		fitParams = crvfit.getParams();
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.POLY5]);
		for(i=0;i< fitParams.length-1;i++)
		{
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());
		fitRT.addValue("Eeff", keV);		

		fitRT.incrementCounter();
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.POLY4);
		fitParams = crvfit.getParams();
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.POLY4]);
		for(i=0;i< fitParams.length-1;i++)
		{
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());
		fitRT.addValue("Eeff", keV);		

		fitRT.incrementCounter();
		fitRT.addValue("Eeff", keV);		
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.POLY3);
		fitParams = crvfit.getParams();
		for(i=0;i< fitParams.length-1;i++)
		{
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.POLY3]);
	
		fitRT.incrementCounter();
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.POLY2);
		fitParams = crvfit.getParams();
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.POLY2]);
		for(i=0;i< fitParams.length-1;i++)
		{
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());
		fitRT.addValue("Eeff", keV);		

		fitRT.incrementCounter();
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.INV_RODBARD);
		fitParams = crvfit.getParams();
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.INV_RODBARD]);
		for(i=0;i< fitParams.length-1;i++)
		{				
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());
		fitRT.addValue("Eeff", keV);		
			
		fitRT.incrementCounter();
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.STRAIGHT_LINE);
		fitParams = crvfit.getParams();	
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.STRAIGHT_LINE]);
		for(i=0;i< fitParams.length-1;i++)
		{			
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());
		fitRT.addValue("Eeff", keV);		
		
		fitRT.show("Fit Parameters");
		//TextWindow rtWin = (TextWindow)WindowManager.getWindow("Fit Parameters");
		//int rtHeight = rtWin.getHeight();
		//rtWin.setSize(800,rtHeight);				
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = true;

		getSelections();
		if(e!=null)
		{
			Object src = e.getSource();
			if(src instanceof TextField)
			{
				TextField tf = (TextField)src;
				String name = tf.getName();
				switch(name)
				{
				case "estKev":
					keV = kevNF.getNumber();
					if(Double.isNaN(keV)) dialogOK=false;
					else if(keV<1 || keV > 1e9) dialogOK=false;
					break;
				}
				if(dialogOK==true)										
				{
					fitAttenuations();
				}
			}
		}
		return dialogOK;
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
	public void actionPerformed(ActionEvent e)
	{
		if(e!=null)
		{
			Object src = e.getSource();
			if(src instanceof Button)
			{
				Button theButton = (Button)src;
				switch(theButton.getName())
				{
				case "upBtn":
					kevNF.setNumber(kevNF.getNumber()+kevIncNF.getNumber());
					break;
				case "downBtn":
					kevNF.setNumber(kevNF.getNumber()-kevIncNF.getNumber());
					break;
				}
			}
			getSelections();
			fitAttenuations();
		}
		
	}

}
