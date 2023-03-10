package CT_Tools;

import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
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
	final int numFldDigits = 6;
	int numFldCols = numFldDigits + 4;
	String[] paramChoices;
	double[][] coeffs;
	Object sinoData;
	boolean useScaleFactor;
	double scaleFactor = 6000;
	
	Vector<TextField> numTxtFldVector;
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	
	ImagePlus dataImp;
	ImageProcessor dataIp;
	ResultsTable fitRT;
	GenericDialog gd;
	GenericDialogAddin gda = new GenericDialogAddin();
	ChoiceField sinoCF,resultTableCF, fitCF;
	CheckboxField createNewCBF,useScaleFactorCBF;
	NumericField scaleFactorNF;

	@Override
	public void run(String arg)
	{		
		String[] imageTitles = WindowManager.getImageTitles();
		String[] nonImageTitles = WindowManager.getNonImageTitles();
		
		//Filter nonImageTitles by looking at headings
		ArrayList<String> titleList = new ArrayList<>();
		for(String title :nonImageTitles)
		{
			ResultsTable rt = ResultsTable.getResultsTable(title);
			if(rt!=null)
			{
				if(rt.getTitle().contains("Fit Parameters"))
				//if(rt.columnExists("Fit"))
				{
					titleList.add(title);
				}
			}
		}		
		String[] resultTableTitles = titleList.toArray(new String[titleList.size()]);		
		
		//Remove RGB images from the images list
		titleList.clear();
		for(String title : imageTitles)
		{
			if(WindowManager.getImage(title).getBitDepth() != 24)
			{
				titleList.add(title);
			}
		}
		imageTitles = titleList.toArray(new String[titleList.size()]);

		if(resultTableTitles.length==0|| imageTitles.length ==0)
		{
			IJ.error("This plugin requires at least one image\n "
					+ "and one \"Fit Parameters\" window.");
			return;
		}
		
		handleResultTableChoice(resultTableTitles[0]);
		if(WindowManager.getImage(imageTitles[0]).getBitDepth() == 32) useScaleFactor = false;
		else useScaleFactor = true;

		gda=new GenericDialogAddin();
		gd = new GenericDialog("Apply Sinogram Linearization");
		
		gd.addChoice("Sinogram Choice", imageTitles, imageTitles[0]);
		sinoCF = gda.getChoiceField(gd, null, "sinoChoice");
		gd.addChoice("Results Choice", resultTableTitles, resultTableTitles[0]);
		resultTableCF = gda.getChoiceField(gd, null, "resultsTable");
		
		gd.setInsets(10,0,0);
		gd.addChoice("Fit Choice", paramChoices, paramChoices[fitRT.getCounter()-1]);
		fitCF = gda.getChoiceField(gd, null, "fit");
						
		gd.addDialogListener(this);
		gd.addMessage("Select Fit or enter the beam hardening\n"
				+ "polynomial coefficients.",myFont,Color.BLACK);
		if(fitRT!=null)
		{
			gd.addNumericField("A0 or A", coeffs[0][5],numFldDigits,numFldCols,null);
			gd.addNumericField("A1 or B", coeffs[1][5],numFldDigits,numFldCols,null);
		}
		else
		{
			gd.addNumericField("A0 or A", 0,numFldDigits,numFldCols,null);
			gd.addNumericField("A1 or B", 0,numFldDigits,numFldCols,null);	
		}
		gd.addNumericField("A2 or C", 0,numFldDigits,numFldCols,null);
		gd.addNumericField("A3 or D", 0,numFldDigits,numFldCols,null);
		gd.addNumericField("A4 or E", 0,numFldDigits,numFldCols,null);
		gd.addNumericField("A5 or F", 0,numFldDigits,numFldCols,null);
		gd.addNumericField("A6 or G", 0,numFldDigits,numFldCols,null);
		gd.addCheckbox("Use_Scale Factor", useScaleFactor);
		useScaleFactorCBF = gda.getCheckboxField(gd, "useScaleFactor");
		gd.addNumericField("Scale_Factor", scaleFactor);
		scaleFactorNF = gda.getNumericField(gd, null, "scaleFactor");
		gd.addCheckbox("Create new Image", true);
		createNewCBF = gda.getCheckboxField(gd, "createNew");
		gd.addMessage("Click OK to apply them\n"
				+ "to your sinogram",myFont,Color.BLACK);
		gd.addHelp("https://lazzyizzi.github.io/Linearization.html");
		gd.setBackground(myColor);
		gd.showDialog();
		

		if(gd.wasCanceled())
		{
			return;
		}
		
		else //Apply the polynomial correction to the sinogram
		{
			double A = gd.getNextNumber();
			double B = gd.getNextNumber();
			double C = gd.getNextNumber();
			double D = gd.getNextNumber();
			double E = gd.getNextNumber();
			double F = gd.getNextNumber();
			double G = gd.getNextNumber();
			useScaleFactor = gd.getNextBoolean();
			if(useScaleFactor) scaleFactor = gd.getNextNumber();
			else scaleFactor = 1;
			boolean createNew = gd.getNextBoolean();
			
			if(createNew)
			{
				
				ImagePlus dataImage =  WindowManager.getImage(sinoCF.getChoice().getSelectedItem());
				String copyTitle = dataImage.getTitle();
				
				Choice fitChoice = fitCF.getChoice();
				int itemCount = fitChoice.getItemCount();
				int itemIndex = fitChoice.getSelectedIndex();
				int fitOrder =itemCount - itemIndex;				
				copyTitle += ("_Ord"+fitOrder);				
				ImagePlus copyImage = dataImage.duplicate();
				copyImage.setTitle(copyTitle);
				copyImage.show();
				sinoData = copyImage.getProcessor().getPixels();
			}
			else
			{
				sinoData = getSinoData(sinoCF.getChoice().getSelectedItem());
			}
					
			double myDbl;
			if(sinoData instanceof float[])
			{
				float[] fData = (float[]) sinoData;
				for(int i=0;i<fData.length;i++)
				{
					myDbl = fData[i]/scaleFactor;
					myDbl = A + 
							B*myDbl + 
							C*Math.pow(myDbl,2) + 
							D*Math.pow(myDbl,3) +
							E*Math.pow(myDbl,4) +
							F*Math.pow(myDbl,5) +
							G*Math.pow(myDbl,6);					
					fData[i] = (float)(myDbl*scaleFactor);
				}				
			}
			else if (sinoData instanceof int[])
			{
				int[] iSinoData = (int[]) sinoData;
				for(int i=0;i<iSinoData.length;i++)
				{
					myDbl = (double)iSinoData[i]/scaleFactor;
					myDbl = A +
							B*myDbl +
							C*Math.pow(myDbl,2) + 
							D*Math.pow(myDbl,3) +
							E*Math.pow(myDbl,4) +
							F*Math.pow(myDbl,5) +
							G*Math.pow(myDbl,6);
					iSinoData[i] = (int)(myDbl*scaleFactor);				
				}								
			}
			else if (sinoData instanceof short[])
			{
				short[] iSinoData = (short[]) sinoData;
				for(int i=0;i<iSinoData.length;i++)
				{
					myDbl = (double)iSinoData[i]/6000;
					myDbl = A +
							B*myDbl +
							C*Math.pow(myDbl,2) + 
							D*Math.pow(myDbl,3) +
							E*Math.pow(myDbl,4) +
							F*Math.pow(myDbl,5) +
							G*Math.pow(myDbl,6);
					iSinoData[i] = (short)(myDbl*scaleFactor);				
				}								
			}
			else if (sinoData instanceof byte[])
			{
				byte[] iSinoData = (byte[]) sinoData;
				for(int i=0;i<iSinoData.length;i++)
				{
					myDbl = (double)iSinoData[i]/6000;
					myDbl = A +
							B*myDbl +
							C*Math.pow(myDbl,2) + 
							D*Math.pow(myDbl,3) +
							E*Math.pow(myDbl,4) +
							F*Math.pow(myDbl,5) +
							G*Math.pow(myDbl,6);
					iSinoData[i] = (byte)(myDbl*scaleFactor);				
				}								
			}
		}
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
//					String title = sinoCF.getChoice().getSelectedItem();
//					if(WindowManager.getImage(title).getBitDepth() == 32) useScaleFactor = false;
//					else  useScaleFactor = true;
//					useScaleFactorCBF.getCheckBox().setState(useScaleFactor);
					break;
				case "fit":
					handleFitChoice(fitCF.getChoice().getSelectedIndex());
					break;
				case "resultsTable":
					handleResultTableChoice(resultTableCF.getChoice().getSelectedItem());
					handleFitChoice(fitCF.getChoice().getSelectedIndex());
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
		return dialogOK;
	}
	
	@SuppressWarnings("unchecked")
	private void handleFitChoice(int colIndex)
	{
		numTxtFldVector = gd.getNumericFields();
		TextField[] tfArr = numTxtFldVector.toArray(new TextField[numTxtFldVector.size()]);
		//tfArr.length-1 so we don't write R^2 in the scaleFactor box 
		for(int i=0;i<tfArr.length-1;i++)
		{
			tfArr[i].setText(Double.toString(coeffs[i][colIndex]));
		}
//		int col=0;
//		for(TextField tf:numTxtFldVector)
//		{
//			tf.setText(Double.toString(coeffs[col][colIndex]));
//			col++;
//		}		
	}
	
	private void handleResultTableChoice(String resultTableTitle)
	{
		//import the table values if the Correction results table is present
		int rowCnt=0,colCnt=0;

		//fitRT = ResultsTable.getResultsTable("Fit Params");
		fitRT = ResultsTable.getResultsTable(resultTableTitle);
		if(fitRT!=null)
		{
			colCnt = fitRT.getLastColumn();
			rowCnt = fitRT.getCounter();
			coeffs = new double[colCnt][rowCnt];
			int row,col=0;
			for(row=0;row<rowCnt;row++)
			{
				for(col=0;col<colCnt;col++)
				{
					coeffs[col][row] = fitRT.getValueAsDouble(col+1, row);
				}
			}
			String[] colHdr = fitRT.getHeadings();			
			paramChoices = fitRT.getColumnAsStrings(colHdr[0]);
		}
	}
	
	private Object getSinoData(String imageTitle)
	{
		return  WindowManager.getImage(imageTitle).getProcessor().getPixels();
	}


}
