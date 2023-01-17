package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;
//import ij.gui.GenericDialog;
//import ij.gui.Plot;
import ij.plugin.*;
import ij.process.ImageProcessor;
import ij.text.TextWindow;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;

import java.awt.*;
import java.awt.event.*;

import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.MuMassCalculator.*;
import jhd.Projection.*;
import jhd.TagTools.*;

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
	MuMassCalculator mmc = new MuMassCalculator();
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
	
	MatlListTools.TagSet myTagSet;	
	MatlListTools mlt = new MatlListTools();
	String[] matlNames;

	int		matlIndex;	// the position of the material in the list
	float	low,high;
	float[] tauLUT;
	double	keV;
	String dataImageName;
	String modelImageName;
	
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
	private void DoDialog()
	{
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
			int winCnt = WindowManager.getImageCount();
			if(winCnt<2)
			{
				IJ.error("This plugin requires a CT slice and a Tagged model slice");
				return;
			}
			
			String[] winTitles = WindowManager.getImageTitles();
			gda = new GenericDialogAddin();
			//gd = new GenericDialog("Find Beam Hardening Corrections");
			gd= GUI.newNonBlockingDialog("Find Beam Hardening Corrections");
			gd.addDialogListener(this);
			//Materials
			//gd.setInsets(10,0,0);
			gd.addMessage("The CT Slice and Tag Image must be the same size\n"
					+ "with isotropic pixel units in cm.\n"
					+ "The CT Slice values must be linear attenuation (cm-1).",myFont,Color.BLACK);
			gd.addChoice("CT Slice:", winTitles, winTitles[0]);
			gd.addChoice("Tag Image:", winTitles, winTitles[0]);
					
			gd.addNumericField("Est. keV", 100);
			kevNF = gda.getNumericField(gd, null, "estKeV");
			gd.setInsets(0, 75, 0);
			gd.addButton("Update",this);
			gd.addMessage("Click OK\n"
					+ "In the plot window, use \"Data->Add Fit\"\n"
					+ "to select the best fit.\n"
					+ "Use Apply Linearization plugin to apply\n"
					+ "the correction to the CT slice's original sinogram",myFont,Color.BLACK);
			gd.addHelp("https://lazzyizzi.github.io/Linearization.html");
			gd.setBackground(myColor);
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
		dataImageName = gd.getNextChoice();
		modelImageName   = gd.getNextChoice();
		keV=gd.getNextNumber();
	}

	private void fitAttenuations()
	{
		//Get the CTslice  and model data
		getSelections();
		dataImp = WindowManager.getImage(dataImageName);
		modelImp = WindowManager.getImage(modelImageName);

		if(!(dataImp.getWidth()== dataImp.getHeight() && modelImp.getWidth() ==  modelImp.getHeight() && dataImp.getWidth()== modelImp.getHeight()))
		{
			IJ.error("Tag and reconstructed images must be square and of equal size.");			
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
		mlt.tagsToLinearAttn(tagImg, myTagSet, keV);
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
		String[] hdr = {"A","B","C","D","E","F","G","H"};

		double[] fitParams;
		int i;
		
		fitRT.deleteRows(0, fitRT.getCounter());

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

		fitRT.incrementCounter();
		crvfit = new CurveFitter(dataProj,modelProj);
		crvfit.doFit(CurveFitter.POLY3);
		fitParams = crvfit.getParams();
		fitRT.addValue("Fit", CurveFitter.fitList[CurveFitter.POLY3]);
		for(i=0;i< fitParams.length-1;i++)
		{
			fitRT.addValue(hdr[i], fitParams[i]);
		}
		fitRT.addValue("R^2", crvfit.getRSquared());

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
		
		fitRT.show("Fit Parameters");
		TextWindow rtWin = (TextWindow)WindowManager.getWindow("Fit Parameters");
		int rtHeight = rtWin.getHeight();
		rtWin.setSize(800,rtHeight);				
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = true;
		if(e!=null)
		{
			Object src = e.getSource();
			if(src instanceof TextField)
			{
				TextField tf = (TextField)src;
				String name = tf.getName();
				switch(name)
				{
				case "estKeV":
					keV = kevNF.getNumber();
					if(Double.isNaN(keV)) dialogOK=false;
					else if(keV<1 || keV > 1e9) dialogOK=false;
					break;
				}
				if(dialogOK==false) tf.setBackground(errColor);										
				else tf.setBackground(white);
			}
		}
		getSelections();
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
	public void actionPerformed(ActionEvent e) {
		getSelections();
		fitAttenuations();
		
	}

}
