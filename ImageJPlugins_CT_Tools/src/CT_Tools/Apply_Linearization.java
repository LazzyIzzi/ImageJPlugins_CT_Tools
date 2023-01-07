package CT_Tools;

import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Font;
import java.awt.TextField;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.DialogListener;
import ij.measure.ResultsTable;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class Apply_Linearization implements PlugInFilter, DialogListener
{
	final int numFldDigits = 6;
	int numFldCols = numFldDigits + 4;	
	double[][] coeffs;	
	Vector<TextField> numTxtFldVector;
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	
	ImagePlus dataImp;
	ImageProcessor dataIp;
	ResultsTable fitRT;
		
	@Override
	public int setup(String arg, ImagePlus imp)
	{
		this.dataImp = imp;
		return DOES_32 + DOES_16;
	}

	@Override
	public void run(ImageProcessor ip)
	{
		this.dataIp = ip;
	
		
		//import the table values if the Correction results table is present
		String[] paramChoices;
		int rowCnt=0,colCnt=0;
		//fitRT = ResultsTable.getResultsTable("Fit Params");
		fitRT = ResultsTable.getActiveTable();
		if(fitRT!=null)
		{
			if(fitRT.getTitle().contains("Fit Params"))
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
			else
			{
				paramChoices = new String[1];
				paramChoices[0] = "Fit Params Table Absent";			
			}
		}		
		else
		{
			paramChoices = new String[1];
			paramChoices[0] = "Fit Params Table Absent";			
		}
		
		
		//Set up the Dialog			
		GenericDialog gd = new GenericDialog("Apply Sinogram Linearization");		
		gd.setInsets(10,0,0);
		if(fitRT==null)
		{
			gd.addChoice("Parameters", paramChoices, paramChoices[0]);			
		}
		else
		{
			gd.addChoice("Parameters", paramChoices, paramChoices[rowCnt-1]);			
		}
				
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

			double myDbl;
			switch(dataImp.getBitDepth())
			{
			case 32:
				float[] sinoData = (float[]) dataImp.getProcessor().getPixels();
				for(int i=0;i<sinoData.length;i++)
				{
					myDbl = sinoData[i];
					myDbl = A + 
							B*myDbl + 
							C*Math.pow(myDbl,2) + 
							D*Math.pow(myDbl,3) +
							E*Math.pow(myDbl,4) +
							F*Math.pow(myDbl,5) +
							G*Math.pow(myDbl,6);					
					sinoData[i] = (float)myDbl;;
				}
				break;
			case 16:
				short[] iSinoData = (short[]) dataImp.getProcessor().getPixels();
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
					iSinoData[i] = (short)(myDbl*6000);				
				}				
			}
		}
	}
	
	//*******************************************************************************************************

	@SuppressWarnings("unchecked")
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		if(e!=null)
		{
			if(e.getSource() instanceof Choice)
			{
				Choice choiceSrc = (Choice)e.getSource();
				int theItem = choiceSrc.getSelectedIndex();
				numTxtFldVector = gd.getNumericFields();
				//move the selected table row into the numeric fields
				int col = 0;
				for(TextField tf:numTxtFldVector)
				{
					double val;
					val = coeffs[col][theItem];
					String valStr = String.format("%" + numFldDigits + "." + (numFldDigits-1) +"f", val);
					tf.setText(valStr);		
					col++;
				}
			}
		}
		double A = gd.getNextNumber();
		double B = gd.getNextNumber();
		double C = gd.getNextNumber();
		double D = gd.getNextNumber();
		double E = gd.getNextNumber();
		double F = gd.getNextNumber();
		double G = gd.getNextNumber();

		return true;
	}	
}
