package CT_Tools;

import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
//import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;

public class Apply_Linearization implements PlugIn, DialogListener
{
	
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	
	ImagePlus dataImp;
	
	ResultsTable fitRT;
	final int numFldDigits = 6;
	int numFldCols = numFldDigits + 4;
	
	GenericDialog gd;
	double A,B,C,D,E,F,G,rSqr,keV;
	String[] paramHdr = {"A","B","C","D","E","F","G"};
	String paramsTableChoice, fitChoice;
	boolean useScaleFactor,createNewImage;
	double scaleFactor = 6000;	//default scale factor

		
	GenericDialogAddin gda = new GenericDialogAddin();	
	ChoiceField sinoCF,resultTableCF, fitCF;
	CheckboxField createNewCBF,useScaleFactorCBF;
	NumericField scaleFactorNF;
	NumericField[] coefNFs;
	
	//*********************************************************************************************
	private void getSettings()
	{
		A = gd.getNextNumber();
		B = gd.getNextNumber();
		C = gd.getNextNumber();
		D = gd.getNextNumber();
		E = gd.getNextNumber();
		F = gd.getNextNumber();
		G = gd.getNextNumber();
		paramsTableChoice = gd.getNextChoice();
		fitChoice = gd.getNextChoice();
		useScaleFactor = gd.getNextBoolean();
		createNewImage = gd.getNextBoolean();
		scaleFactor = gd.getNextNumber();
	}
	
	//*********************************************************************************************
	private void applyFit()
	{
		Object[] sinoData;
		double[] coeffArr = new double[7];
		
		coeffArr[0] = A;
		coeffArr[1] = B;
		coeffArr[2] = C;
		coeffArr[3] = D;
		coeffArr[4] = E;
		coeffArr[5] = F;
		coeffArr[6] = G;
				
		if(useScaleFactor==false) scaleFactor = 1;
		
		Choice fitChoice = fitCF.getChoice();
		ImagePlus dataImage =  WindowManager.getImage(sinoCF.getChoice().getSelectedItem());
		int nSlices = dataImage.getNSlices();
		if(createNewImage)
		{				
			String copyTitle = dataImage.getTitle();
			
			copyTitle +=  ("-" + fitChoice.getSelectedItem());				
			ImagePlus copyImage = dataImage.duplicate();
			copyImage.setTitle(copyTitle);
			copyImage.setProp("Linearization", fitChoice.getSelectedItem());
			copyImage.setProp("LinearizationA", coeffArr[0]);				
			copyImage.setProp("LinearizationB", coeffArr[1]);				
			copyImage.setProp("LinearizationC", coeffArr[2]);				
			copyImage.setProp("LinearizationD", coeffArr[3]);				
			copyImage.setProp("LinearizationE", coeffArr[4]);				
			copyImage.setProp("LinearizationF", coeffArr[5]);				
			copyImage.setProp("LinearizationG", coeffArr[6]);				
			copyImage.setProp("LinearizationR", rSqr);				
			copyImage.setProp("LinearizationEff", keV);				

			copyImage.show();
			sinoData = copyImage.getStack().getImageArray();
		}
		else
		{
			sinoData =  dataImage.getStack().getImageArray();
			dataImage.setTitle(dataImage.getTitle() + "-" + fitChoice.getSelectedItem());
		}
		
		double myDbl;
		int i,slice;
		//long start = System.nanoTime();

		String fitStr = fitChoice.getSelectedItem();
		switch(fitStr)
		{
		case "Inverse Rodbard":
			if(sinoData[0] instanceof float[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					float[] fData = (float[]) sinoData[slice];
					for( i=0;i<fData.length;i++)
					{
						myDbl = fData[i]/scaleFactor;
						myDbl = invRodbardCalc(myDbl,coeffArr);
						fData[i] = (float)(myDbl*scaleFactor);
					}
				}
			}
			else if(sinoData[0] instanceof int[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					int[] fData = (int[]) sinoData[slice];
					for( i=0;i<fData.length;i++)
					{
						myDbl = fData[i]/scaleFactor;
						myDbl = invRodbardCalc(myDbl,coeffArr);
						if(myDbl<0) myDbl=0;
						fData[i] = (int)(myDbl*scaleFactor);
					}
				}
			}
			else if(sinoData[0] instanceof short[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					short[] fData = (short[]) sinoData[slice];
					for( i=0;i<fData.length;i++)
					{						
						myDbl = fData[i]/scaleFactor;
						myDbl = invRodbardCalc(myDbl,coeffArr);
						if(myDbl<0) myDbl=0;
						fData[i] = (short)(myDbl*scaleFactor);
					}	
				}
			}
			else if(sinoData[0] instanceof byte[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					byte[] fData = (byte[]) sinoData[slice];
					for( i=0;i<fData.length;i++)
					{
						myDbl = fData[i]/scaleFactor;
						myDbl = invRodbardCalc(myDbl,coeffArr);
						if(myDbl<0) myDbl=0;
						fData[i] = (byte)(myDbl*scaleFactor);
					}
				}
			}
			break;
		default:
			if(sinoData[0] instanceof float[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					float[] fData = (float[]) sinoData[slice];
					for( i=0;i<fData.length;i++)
					{
						myDbl = fData[i]/scaleFactor;
						myDbl = polyCalc(myDbl,coeffArr);
						fData[i] = (float)(myDbl*scaleFactor);
					}
				}
			}
			else if (sinoData[0] instanceof int[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					int[] iSinoData = (int[]) sinoData[slice];
					for( i=0;i<iSinoData.length;i++)
					{
						myDbl = (double)iSinoData[i]/scaleFactor;
						myDbl = polyCalc(myDbl,coeffArr);
						if(myDbl<0) myDbl=0;
						iSinoData[i] = (int)(myDbl*scaleFactor);				
					}
				}
			}
			else if (sinoData[0] instanceof short[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					short[] iSinoData = (short[]) sinoData[slice];
					for( i=0;i<iSinoData.length;i++)
					{
						myDbl = (double)iSinoData[i]/scaleFactor;
						myDbl = polyCalc(myDbl,coeffArr);
						if(myDbl<0) myDbl=0;
						iSinoData[i] = (short)(myDbl*scaleFactor);				
					}
				}
			}
			else if (sinoData[0] instanceof byte[])
			{
				for(slice=0;slice<nSlices;slice++)
				{
					byte[] iSinoData = (byte[]) sinoData[slice];
					for( i=0;i<iSinoData.length;i++)
					{
						myDbl = (double)iSinoData[i]/scaleFactor;
						myDbl = polyCalc(myDbl,coeffArr);
						if(myDbl<0) myDbl=0;
						iSinoData[i] = (byte)(myDbl*scaleFactor);				
					}
				}
			}
			break;
		}
		//long end = System.nanoTime();
		//Surprise! This implementation was about 10-20% faster than with local variables
		//System.out.println("Linearization6 = "+(end-start) + "nsec" );
	}

	//*******************************************************************************************************

	private double invRodbardCalc(double val, double[] coeffArr)
	{	//Formula: y = c*((x-a)/(d-x))^(1/b)
		return coeffArr[2]*Math.pow((val-coeffArr[0])/(coeffArr[3]-val),1/coeffArr[1]);		
	}
	
	//*******************************************************************************************************

	private double polyCalc(double val, double[] coeffArr)
	{
		return coeffArr[0] + coeffArr[1]*val + coeffArr[2]*Math.pow(val,2) + 
				coeffArr[3]*Math.pow(val,3) + coeffArr[4]*Math.pow(val,4) +
				coeffArr[5]*Math.pow(val,5) + coeffArr[6]*Math.pow(val,6);		
	}

	//*******************************************************************************************************

	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = true;
		if(e!=null)
		{
			Object src = e.getSource();
			if(src instanceof Choice)
			{
				Choice choice = (Choice)src;
				String name = choice.getName();
				switch(name)
				{
				case "sinoChoice":
					String title = sinoCF.getChoice().getSelectedItem();
					dataImp = WindowManager.getImage(title);
					setScaleFactorFromSinoProperty(dataImp);
					scaleFactorNF.setNumber(scaleFactor);
					useScaleFactorCBF.getCheckBox().setState(useScaleFactor);
					break;
				case "fit":
					handleFitChoice();
					break;
				case "resultsTable":
					handleResultTableChoice(resultTableCF.getChoice().getSelectedItem());
					handleFitChoice();
					break;
				}
			}
			if(src instanceof Checkbox)
			{
				Checkbox cb = (Checkbox)src;
				String name = cb.getName();
				switch(name)
				{
				case "useScaleFactor":
					if(useScaleFactorCBF.getCheckBox().getState())
					{
						scaleFactor = scaleFactorNF.getNumber();
						if(Double.isNaN(scaleFactor)) dialogOK = false;
					}
					break;
				}
			}
		}
		getSettings();
		return dialogOK;
	}
	
	//*******************************************************************************************************

	private String[] getFitTableTitles(String[] nonImageTitles)
	{
		//Filter nonImageTitles by looking at headings
		ArrayList<String> titleList = new ArrayList<>();
		for(String title :nonImageTitles)
		{
			ResultsTable rt = ResultsTable.getResultsTable(title);
			if(rt!=null)
			{
				if(rt.getTitle().contains("Fit Parameters"))
				{
					titleList.add(title);
				}
			}
		}		
		return  titleList.toArray(new String[titleList.size()]);		
	}

	//*********************************************************************************************	

	private String[] getImageTitles(String[] imageTitles)
	{
		//Remove RGB images from the images list
		ArrayList<String> titleList = new ArrayList<>();
		for(String title : imageTitles)
		{
			if(WindowManager.getImage(title).getBitDepth() != 24)
			{
				titleList.add(title);
			}
		}
		return titleList.toArray(new String[titleList.size()]);
	}

	//*********************************************************************************************

	private void handleFitChoice()
	{
		int row = fitCF.getChoice().getSelectedIndex();
		//import the table coefficients by row
		for(int i=0;i<paramHdr.length;i++)
		{
			coefNFs[i].setNumber(fitRT.getValue(paramHdr[i], row));
		}
		rSqr = fitRT.getValue("R^2", row);
		keV = fitRT.getValue("Eeff", row);
	}
	
	//*********************************************************************************************

	private void handleResultTableChoice(String resultTableTitle)
	{
		fitRT = ResultsTable.getResultsTable(resultTableTitle);
		if(fitRT!=null)
		{
			//Users may have deleted Fit Parameters rows
			//rebuild the fitChoice menu from the "Fit" column 0		
			fitCF.setChoices(fitRT.getColumnAsStrings(fitRT.getColumnHeading(0)));
			//Set the default choice to the last row in the table
			fitCF.getChoice().select(fitRT.getCounter()-1);
			//import the table coefficients by row
			for(int i=0;i<paramHdr.length;i++)
			{
				coefNFs[i].setNumber(fitRT.getValue(paramHdr[i], fitRT.getCounter()-1));
			}			
			rSqr = fitRT.getValue("R^2", fitRT.getCounter()-1);
			keV = fitRT.getValue("Eeff", fitRT.getCounter()-1);
		}
	}

	//*********************************************************************************************

	@Override
	public void run(String arg)
	{		
		String[] fitTableTitles = getFitTableTitles( WindowManager.getNonImageTitles())	;		
		String[] imageTitles = getImageTitles(WindowManager.getImageTitles());
				
		if(fitTableTitles.length==0|| imageTitles.length ==0)
		{
			IJ.error("This plugin requires at least one grayscale image\n "
					+ "and one \"Fit Parameters\" window.");
			return;
		}
				
		fitRT = ResultsTable.getResultsTable(fitTableTitles[0]);
		setScaleFactorFromSinoProperty(WindowManager.getImage(imageTitles[0]));

		gda=new GenericDialogAddin();
		gd = new GenericDialog("Apply Sinogram Linearization");
		gd.addDialogListener(this);
		
		gd.addChoice("Sinogram Choice", imageTitles, imageTitles[0]);
		sinoCF = gda.getChoiceField(gd, null, "sinoChoice");
		gd.addChoice("Results Choice", fitTableTitles, fitTableTitles[0]);
		resultTableCF = gda.getChoiceField(gd, null, "resultsTable");
		
		gd.setInsets(10,0,0);
		//Default to last item in fitChoices List  "Straight Line"
		int defaultRowIndex = fitRT.getCounter()-1;
		String[] fitChoices = fitRT.getColumnAsStrings("Fit");
		gd.addChoice("Fit Choice", fitChoices,  fitChoices[defaultRowIndex]);
		fitCF = gda.getChoiceField(gd, null, "fit");
						
		gd.addMessage("Select Fit or enter the beam hardening\n"
				+ "polynomial coefficients.",myFont,Color.BLACK);
		
		coefNFs = new NumericField[paramHdr.length];
		for(int i=0;i<paramHdr.length;i++)
		{
			gd.addNumericField(paramHdr[i], fitRT.getValue(paramHdr[i], defaultRowIndex),numFldDigits,numFldCols,null);
			coefNFs[i] = gda.getNumericField(gd, null, "coef"+i);
		}

		rSqr = fitRT.getValue("R^2", defaultRowIndex);
		
		gd.addCheckbox("Use_Scale Factor", useScaleFactor);
		useScaleFactorCBF = gda.getCheckboxField(gd, "useScaleFactor");
		gd.addNumericField("Scale_Factor", scaleFactor);
		
		scaleFactorNF = gda.getNumericField(gd, null, "scaleFactor");
		gd.addCheckbox("Create new Image", true);
		createNewCBF = gda.getCheckboxField(gd, "createNew");
		gd.addMessage("Click OK to apply them\n"
				+ "to your sinogram",myFont,Color.BLACK);
		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/Linearization.html");
		gd.setBackground(myColor);
		gd.showDialog();
		

		if(gd.wasCanceled())
		{
			return;
		}
		else
		{
			getSettings();
			applyFit();
			IJ.showStatus("Apply Linearization Done");
		}
	}
	
	//*******************************************************************************************************

	private void setScaleFactorFromSinoProperty(ImagePlus imp)
	{
		String scaleFactorStr = imp.getProp("ScaleFactor");
				useScaleFactor=false;			
		if(scaleFactorStr!=null) 
		{
			if(GenericDialogAddin.isNumeric(scaleFactorStr))
			{
				scaleFactor = Double.parseDouble(scaleFactorStr);
				useScaleFactor=true;
			}
		}
	}
}
