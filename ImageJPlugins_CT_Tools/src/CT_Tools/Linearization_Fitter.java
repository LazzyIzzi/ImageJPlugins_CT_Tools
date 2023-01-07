package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Plot;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;

import java.awt.*;


import jhd.MuMassCalculator.*;
import jhd.Projection.*;
import jhd.TagTools.*;

/* 	Estimate "polynomial linearization" beam hardening coefficients from an uncorrected reconstructed slice.
 * Requires the reconstructed beam-hardened CT slice and a user-supplied tagged model image.
 * This allows use of pixel classifiers other than simple thresholding to create a tagged model image.
 * Plots the slice vs model attenuations and calls ImageJ's CurveFitter to find linearization coefficients.
 *  JHD 1/25/22
*/

public class Linearization_Fitter implements PlugInFilter //, DialogListener, ActionListener
{
	MuMassCalculator mmc = new MuMassCalculator();
	ParallelProjectors prj= new ParallelProjectors();
	
	GenericDialog gd;
	Calibration dataCal;
	CurveFitter crvfit;
	ImagePlus dataImp,modelImp;
	ImageProcessor ip;
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	
	MatlListTools.TagSet myTagSet;	
	MatlListTools mlt = new MatlListTools();
	String[] matlNames;

	int		matlIndex;	// the position of the material in the list
	float	low,high;
	float[] tauLUT;
	double	keV;
	final int nViews = 18;

	//*****************************************************************

	@Override
	public int setup(String arg, ImagePlus imp)
	{
		this.dataImp = imp;
		return DOES_32;
	}

	//*****************************************************************

	@Override
	public void run(ImageProcessor ip)
	{
		this.ip = ip;
		
		if(ip.getWidth()!= ip.getHeight())
		{
			IJ.error("Image Must be square.");			
			return;
		}
		if(IJ.versionLessThan("1.41k")) return;
		dataCal = dataImp.getCalibration();
		if(!dataCal.getUnit().equals("cm"))
		{
			IJ.error("Pixel units must be cm");
			return;
		}

		DoDialog();

	}

	//*****************************************************************

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

			gd = new GenericDialog("Find Beam Hardening Corrections");
			//Materials
			gd.setInsets(10,0,0);
			gd.addMessage("This Plugin requires 2 same size square images\n"
					+ "with isotropic pixel sizes in cm units and pixel \n"
					+ "values in cm-1.",myFont,Color.BLACK);
			gd.addChoice("CT Slice:", winTitles, winTitles[0]);
			gd.addChoice("Tag Image:", winTitles, winTitles[0]);
					
			gd.addNumericField("Est. keV", 100);
			gd.addMessage("Click OK\n"
					+ "In the plot window, use \"Data->Add Fit\"\n"
					+ "to select the best fit.\n"
					+ "Use Apply Correction plugin to apply\n"
					+ "the correction to the CT slice's original sinogram",myFont,Color.BLACK);
			//gd.addDialogListener(this);
			gd.addHelp("https://lazzyizzi.github.io/Linearization.html");
			gd.setBackground(myColor);
			gd.showDialog();

			if(gd.wasOKed())
			{
				DoRoutine();
			}
		}
		else return;		
	}

	//*****************************************************************

	private void DoRoutine()
	{
		//Get the CTslice  and model data
		dataImp = WindowManager.getImage(gd.getNextChoice());
		modelImp = WindowManager.getImage(gd.getNextChoice());
		keV = gd.getNextNumber();
		if(!dataImp.getCalibration().equals(dataImp.getCalibration()))
		{
			IJ.error("The CT slice and the model do not have the same calibration.");
			return;
		}

		//We don't want to step on the users model image
		//ImagePlus tagImp = (ImagePlus) modelImp.clone();
		ImagePlus tagImp = modelImp.duplicate();
		//Convert tags to muLin
		float[] tagImg = (float[])tagImp.getProcessor().getPixels();		
		mlt.tagsToLinearAttn(tagImg, myTagSet, keV);


		int width = dataImp.getWidth();
		double pixSize = dataImp.getCalibration().pixelWidth;
		float[] dataImg = (float[]) dataImp.getProcessor().getPixels();

		//Sinogram comparison.
		ParallelProjectors parPrj = new ParallelProjectors();
		float[] dataSino = parPrj.imageToParallelSinogram(dataImg, width, width, nViews);
		float[] modelSino = parPrj.imageToParallelSinogram(tagImg, width, width, nViews);

		double[] dataProj = new double[width*nViews];
		double[] modelProj = new double[width*nViews];
		for(int i = 0;i< width*nViews;i++)
		{
			dataProj[i] += (double) dataSino[i]*pixSize;
			modelProj[i] += (double) modelSino[i]*pixSize;
		}

		Plot tauPlot = new Plot("Model vs Data for " + dataImp.getTitle() + " at " + keV +"keV",  "Data Proj","Model Proj");
		tauPlot.setColor(Color.RED);
		tauPlot.add("dot", dataProj, modelProj );
		tauPlot.show();
		
		String resultTitle = "Fit Params for " + dataImp.getTitle() + " at " + keV +"keV";

		ResultsTable fitRT = new ResultsTable();
		fitRT.setPrecision(5);
		//fitRT.show(resultTitle);

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

		fitRT.show(resultTitle);
	}
}
