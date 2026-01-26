package CT_Tools;
/*
 * An example plugin that calls several of the functions available in MuMassCalc_J8Lib.jar
 * MuMassCalc_J8Lib.jar is derived from the NIST XCOM web application used to calculate 
 * photon cross sections for scattering, photoelectric absorption and pair production,
 * as well as total attenuation coefficients, for any element, compound (Z less than o equal to 100),
 * at energies from 1 keV to 100 GeV.
 * See: https://github.com/LazzyIzzi/MuMassCalculator
 * 
 * This plugin supports two operations:
 * 1. Interactive calculation of interpolated attenuation cross-sections in units of cm2/gm from a
 * user-supplied simplified chemical formula and the photon energy. Calculated values 
 * are posted to a results window.
 * 
 * 2.Plotting of tabulated absorption spectra for selected cross-sections for the user supplied
 * formula and energy range.  Optionally the absorption edge energies for the atoms in the formula
 * are reported in a separate results table.
 * 
 */

import java.awt.*;
import java.awt.event.*;

import ij.plugin.*;
import ij.text.TextPanel;
import ij.gui.*;
import ij.*;
import ij.measure.*;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.MuMassCalculator.*;
import tagTools.*;
import tagTools.TagListTools.TagSet;

public class Xray_Lookup_Ratio implements PlugIn, ActionListener, DialogListener
{

	final String myDialogTitle = "X-Ray Lookup Ratio";
	final String resultsTitle = "KeV Ratio Solutions";
	final int calcW=850,calcH=230;
	//final Color buff = new Color(250,240,200);
	final Font myFont = new Font(Font.DIALOG, Font.ITALIC+Font.BOLD, 14);
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Color errColor = new Color(255,100,0);
	final Color white = new Color(255,255,255);
	
	MuMassCalculator mmc= new MuMassCalculator();
	TagListTools mlt=new TagListTools();
	TagSet tagSet;
	String[] matlName;
	String[] matlFormula;
	double[] matlGmPerCC;
	int[] matlTag;

	String[] filteredMatlNames1,filteredMatlNames2;
	String[] filteredFormulas1,filteredFormulas2;
	double[] filteredGmPerCC1,filteredGmPerCC2;
	

	GenericDialog gd;
	int dlogW,dlogH,dlogL,dlogT;
	boolean isMacro = false;
	boolean useTabDensity;

	private class DialogSettings
	{
		String formulaName1;
		String formulaR1;
		double attnR1;
		double gmPerCCR1;

		String formulaName2;
		String formulaR2;
		double attnR2;
		double gmPerCCR2;
	}	
	DialogSettings ds = new DialogSettings();

	//*********************************************************************************/

	@Override
	public void run(String arg)
	{
		if(IJ.versionLessThan("1.53u"))
		{
			IJ.showMessage("Newer ImageJ Version Required", "Update ImageJ to version 1.53u or better to run this plugin");
			return;
		}
		if(Macro.getOptions()!=null) isMacro=true;

		//Location of the default materials list
		String dir = IJ.getDirectory("plugins");
		String defaultFilePath = dir + "DialogData\\DefaultMaterials.csv";

		//Initialize master list of material data
		tagSet = mlt.readTagSetFile(defaultFilePath);
		matlName = mlt.getTagSetMatlNamesAsArray(tagSet);
		matlFormula = mlt.getTagSetMatlFormulasAsArray(tagSet);
		matlGmPerCC = mlt.getTagSetMatlGmPerccAsArray(tagSet);
		matlTag = mlt.getTagSetMatlTagAsArray(tagSet);
		//Initialize filtered copies
		filteredMatlNames1 = matlName;
		filteredMatlNames2 = matlName;
		filteredFormulas1 = matlFormula;
		filteredFormulas2 = matlFormula;
		filteredGmPerCC1 = matlGmPerCC;
		filteredGmPerCC2 = matlGmPerCC;
		
		DoDialog();
		DoRoutine();		
	}

	//*********************************************************************************/

	private void InitSettings()
	{	
		ds.formulaR1= matlFormula[3];
		ds.attnR1= 0.46;
		ds.gmPerCCR1 = matlGmPerCC[3];
		ds.formulaName1= matlName[3];

		ds.formulaR2= matlFormula[6];
		ds.attnR2 = 0.49;
		ds.gmPerCCR2 = matlGmPerCC[6];
		ds.formulaName2= matlName[6];
	}

	//*********************************************************************************/
	// The fields to hold the generic Dialog components
	StringField name1SF,name2SF,filter1SF,filter2SF,formula1SF,formula2SF;
	ChoiceField matlName1CF,matlName2CF;
	NumericField gmPerCC1NF,brightness1NF,gmPerCC2NF,brightness2NF;
	CheckboxField useTabDensCF;
	ButtonField getKevBF;

	public void DoDialog()
	{

		InitSettings();

		//Use GenericDialogAddin methods for dialog component event handler
		GenericDialogAddin gda = new GenericDialogAddin();
		
		gd =  GUI.newNonBlockingDialog(myDialogTitle);
		gd.addMessage("Get Energy from relative brightness (linear attenuation ratio)",myFont);
		gd.addMessage("Formula Format: "
				+ "Atom1:Count1:Atom2:Count2 etc.", myFont);
		
		gd.addCheckbox("Import tabulated densities", true);
		useTabDensCF = gda.getCheckboxField(gd, "useTabDens");

		//First selection block
		gd.setInsets(5, 0, 0);
		gd.addStringField("Filter_Material_1_Names", "");
		filter1SF = gda.getStringField(gd, null, "filter1");

		gd.setInsets(0, 0, 5);
		gd.addChoice("Material_1_Names", matlName, ds.formulaName1);
		matlName1CF = gda.getChoiceField(gd, null, "materialChoice1");

		gd.addStringField("Material_1_Name: ", ds.formulaName1,26);
		name1SF = gda.getStringField(gd, null, "name1");

		gd.addStringField("Formula_1: ", ds.formulaR1,26);
		formula1SF = gda.getStringField(gd, null, "formula1");

		gd.addNumericField("Density_1 observed for Formula_1", ds.gmPerCCR1,4,8,"gm/cc");
		gmPerCC1NF = gda.getNumericField2(gd, null, null, "gmPerCC1");

		gd.addNumericField("Brightness_1 observed for Formula_1", ds.attnR1);
		brightness1NF = gda.getNumericField(gd, null, "brightness1");

		gd.setInsets(0, 0, 5);
		gd.addMessage("______________________________________________");;
		//Second selection block
		gd.setInsets(5, 0, 0);
		gd.addStringField("Filter_Material_2_Names", "");
		filter2SF = gda.getStringField(gd, null, "filter2");

		gd.setInsets(0, 0, 5);
		gd.addChoice("Material_2_Names", matlName, ds.formulaName2);
		matlName2CF = gda.getChoiceField(gd, null, "materialChoice2");

		gd.addStringField("Material_2_Name: ", ds.formulaName2,26);
		name2SF = gda.getStringField(gd, null, "name2");

		gd.addStringField("Formula_2: ", ds.formulaR2,26);
		formula2SF = gda.getStringField(gd, null, "formula2");

//		gd.addNumericField("Density_2 observed for Formula_2",  ds.gmPerCCR2);
//		gmPerCC2NF = gda.getNumericField(gd, null, "gmPerCC2");
		gd.addNumericField("Density_2 observed for Formula_2", ds.gmPerCCR2,4,8,"gm/cc");
		gmPerCC2NF = gda.getNumericField2(gd, null, null, "gmPerCC2");

		gd.addNumericField("Brightness_2 observed for Formula_2", ds.attnR2);
		brightness2NF = gda.getNumericField(gd, null, "brightness2");

		gd.setInsets(20, 200, 0);
		gd.addButton("Get keV from ratio", this);
		getKevBF = gda.getButtonField(gd, "getKeV");
		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/XrayLookupRatio.html");		
		gd.addDialogListener(this);
		gd.setBackground(myColor);
		gd.addMessage("Edit tabulated density to match lab measurement");

		gd.showDialog();
	}

	//*********************************************************************************/

	private void DoRoutine()
	{
		dlogW = gd.getSize().width;
		dlogH = gd.getSize().height;
		dlogL = gd.getLocation().x;
		dlogT = gd.getLocation().y;

		if (gd.wasCanceled())
		{
			ResultsTable rt;
			rt = ResultsTable.getResultsTable(resultsTitle);
			if(rt!=null)
			{			
				IJ.selectWindow(resultsTitle);
				IJ.run("Close");
			}
			return;
		}
		if (gd.wasOKed())
		{
			getSelections();
			if(ds!=null)
			{
				double[] ratioResult = mmc.getMeVfromMuLinRatio(ds.formulaR1, ds.attnR1, ds.gmPerCCR1, ds.formulaR2, ds.attnR2, ds.gmPerCCR2, "TotAttn");
				if(ratioResult!=null)
				{
					for(int i=0;i<ratioResult.length;i++)
					{
						double slope = getMuLinRatioSlope(ratioResult[i],ds.formulaR1, ds.attnR1, ds.gmPerCCR1, ds.formulaR2, ds.attnR2, ds.gmPerCCR2);
						//require that dMuLinRatio/dE slope be sufficiently sensitive
						if(Math.abs(slope)<5)
						{
							String  str = "Low brightness ratio sensitivity Warning!"
									+ "\nThe linear attenuation ratio of " + ds.formulaR1 + " to "+ ds.formulaR2
									+ "\nis a weak function of energy at " + String.format("%.2f" + "KeV", ratioResult[i]*1000)
									+ "\nThe reported energy may be inaccurate and sensitive to noise in the"
									+ "\nattenuation measurements "+ ds.attnR1 + " and " + ds.attnR2 + "cm-1";
							if(isMacro)
							{
								IJ.log(str);
								IJ.log("   ");
							}
							else
							{
								IJ.showMessage("Warning",str);
							}
						}
					}
				}
				ResultsTable rt = updateResultsTable(ratioResult);
				rt.show(resultsTitle);
				Window win = WindowManager.getWindow(resultsTitle);
				if(!isMacro)
				{
					win.setLocation(dlogL+dlogW,dlogT);
					win.setSize(calcW,dlogH);					
				}
				else
				{
					//win.setLocation(0,0);
					win.setSize(calcW,calcH);											
				}
				//Scroll to current result
				TextPanel txtPnl = (TextPanel)win.getComponent(0);
				txtPnl.showRow(txtPnl.getLineCount());
			}
		}
	}

	//*********************************************************************************/
	
	private void getSelections()
	{
		gd.resetCounters();
		useTabDensity = gd.getNextBoolean();
		String filterStr1 = gd.getNextString(); 
		ds.formulaName1 = gd.getNextString();
		ds.formulaR1 = gd.getNextString();
		ds.gmPerCCR1 = gd.getNextNumber();
		ds.attnR1 = gd.getNextNumber();

		String filterStr2 = gd.getNextString(); 
		ds.formulaName2 = gd.getNextString();
		ds.formulaR2 = gd.getNextString();
		ds.gmPerCCR2 = gd.getNextNumber();
		ds.attnR2 = gd.getNextNumber();
	}

	//*********************************************************************************/

	private ResultsTable updateResultsTable(double[] ratioResult)
	{
		ResultsTable rt = ResultsTable.getResultsTable(resultsTitle);
		if(rt==null || rt.getCounter()==0)
		{
			rt=new ResultsTable();
			rt.setPrecision(5);
			rt.addValue("Name 1","");
			rt.addValue("Formula 1","");
			rt.addValue("gm/cc 1","");
			rt.addValue("Brightness 1","");
			rt.addValue("Name 2","");
			rt.addValue("Formula 2","");
			rt.addValue("gm/cc 2","");
			rt.addValue("Brightness 2","");
			rt.deleteRow(0);
		}

		rt.incrementCounter();
		if(ratioResult!=null)
		{
			for(int i=0;i<ratioResult.length;i++)
			{
				rt.addValue("KeV " + i , ratioResult[i]*1000);
			}			
		}
		else
		{
			rt.addValue("KeV 0", "No Solution");
		}

		rt.addValue("Name 1", ds.formulaName1);
		rt.addValue("Formula 1", ds.formulaR1);
		rt.addValue("Brightness 1", ds.attnR1);
		rt.addValue("gm/cc 1", ds.gmPerCCR1);
		rt.addValue("Name 2", ds.formulaName2);
		rt.addValue("Formula 2", ds.formulaR2);
		rt.addValue("Brightness 2", ds.attnR2);
		rt.addValue("gm/cc 2", ds.gmPerCCR2);
		return rt;
	}

	//*********************************************************************************/

	/**
	 * @param meV the photon energy in MeV
	 * @param formula1 the chemical formula for component 1 as Atom1:Count1:Atom2:Count2... e.g. Ca:1:C:1:O:3 for calcium carbonate CaCO3
	 * @param attn1	The observed linear attenuation for formula1
	 * @param gmPerCC1 The observed density in gm/cc for formula1
	 * @param formula2
	 * @param attn2
	 * @param gmPerCC2
	 * @return The slope of the line dMuLinRatio/dE Low slopes indicate loss of accuracy
	 */
	private double getMuLinRatioSlope(double meV, String formula1, double attn1, double gmPerCC1, String formula2, double attn2, double gmPerCC2)
	{

		double slope=0, eHi, eLo, muLin1Hi,muLin1Lo,muLin2Hi,muLin2Lo,muLinRatioHi,muLinRatioLo;
		final double energyWindow=.01;

		try
		{
			eHi =(1+energyWindow)*meV;
			eLo =(1-energyWindow)*meV;

			muLin1Hi = mmc.getMuMass(formula1, eHi, "TotAttn")*gmPerCC1;
			muLin1Lo = mmc.getMuMass(formula1, eLo, "TotAttn")*gmPerCC1;

			muLin2Hi = mmc.getMuMass(formula2, eHi, "TotAttn")*gmPerCC2;
			muLin2Lo = mmc.getMuMass(formula2, eLo, "TotAttn")*gmPerCC2;

			muLinRatioHi = muLin1Hi/muLin2Hi;
			muLinRatioLo = muLin1Lo/muLin2Lo;

			slope=(muLinRatioLo-muLinRatioHi)/(eLo-eHi);
			return slope;
		}
		catch (Exception e)
		{
			return 0.0;
		}		
	}

	//*********************************************************************************/

	public void actionPerformed(ActionEvent theEvent)
	{
		dlogW = gd.getSize().width;
		dlogH = gd.getSize().height;
		dlogL = gd.getLocation().x;
		dlogT = gd.getLocation().y;
		//getSelections();
		getSelections();//		if(getSelections())
		{
			String cmd = theEvent.getActionCommand();
			switch(cmd)
			{
			case "Get keV from ratio":
				double[] result2 = mmc.getMeVfromMuLinRatio(ds.formulaR1, ds.attnR1, ds.gmPerCCR1, ds.formulaR2, ds.attnR2, ds.gmPerCCR2, "TotAttn");
				if(result2!=null)
				{
					for(int i=0;i<result2.length;i++)
					{
						double slope = getMuLinRatioSlope(result2[i],ds.formulaR1, ds.attnR1, ds.gmPerCCR1, ds.formulaR2, ds.attnR2, ds.gmPerCCR2);
						//require that dMuLinRatio/dE slope be sufficiently sensitive
						if(Math.abs(slope)<5)
						{
							String  str = "Low brightness ratio sensitivity Warning!"
									+ "\nThe linear attenuation ratio of " + ds.formulaR1 + " to "+ ds.formulaR2
									+ "\nis a weak function of energy at " + String.format("%.2f" + "KeV", result2[i]*1000)
									+ "\nThe reported energy may be inaccurate and sensitive to noise in the"
									+ "\nattenuation measurements "+ ds.attnR1 + " and " + ds.attnR2 + "cm-1"
									+ "\nRecommend altering brightness least significant digits and observing the keV change";
							IJ.showMessage("Warning",str);
						}
					}
				}
				ResultsTable rt = updateResultsTable(result2);
				rt.show(resultsTitle);									//					}
				Window win = WindowManager.getWindow(resultsTitle);
				win.setLocation(dlogL+dlogW,dlogT);
				win.setSize(calcW,dlogH);					
				//Scroll to current result
				TextPanel txtPnl = (TextPanel)win.getComponent(0);
				txtPnl.showRow(txtPnl.getLineCount());
				break;
			}
		}
	}

	//*********************************************************************************/

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		//boolean	dlogOK = getSelections();
		boolean	dialogOK = true;

		if(e!=null)
		{
			if(e.getSource() instanceof Choice)
			{
				Choice choice = (Choice)e.getSource();
				String name = choice.getName();
				switch(name)
				{
				case "materialChoice1":
					int index;
					index =  matlName1CF.getChoice().getSelectedIndex();				
					name1SF.getTextField().setText(filteredMatlNames1[index]);
					formula1SF.getTextField().setText(filteredFormulas1[index]);
					if(useTabDensity)
					{
						gmPerCC1NF.setNumber(filteredGmPerCC1[index]);
					}
					break;
				case "materialChoice2":
					index = matlName2CF.getChoice().getSelectedIndex();		
					name2SF.getTextField().setText(filteredMatlNames2[index]);
					formula2SF.getTextField().setText(filteredFormulas2[index]);
					if(useTabDensity)
					{
						gmPerCC2NF.setNumber(filteredGmPerCC2[index]);
					}
					break;
				}
			}
			else if(e.getSource() instanceof TextField)
			{					
				TextField tf = (TextField) e.getSource();
				String name = tf.getName();					
				String filterStr = tf.getText();
				switch(name)
				{
				case "filter1":
					TagSet filteredTagData1 = mlt.filterTagData(tagSet, filterStr);
					if(filterStr.equals(""))
					{
						//copy the original arrays into the filtered arrays
						filteredMatlNames1 = matlName;
						filteredFormulas1 = matlFormula;
						filteredGmPerCC1 =matlGmPerCC;
					}
					else
					{
						filteredMatlNames1 = mlt.getTagSetMatlNamesAsArray(filteredTagData1);
						filteredFormulas1 = mlt.getTagSetMatlFormulasAsArray(filteredTagData1);
						filteredGmPerCC1 =mlt.getTagSetMatlGmPerccAsArray(filteredTagData1);
					}
					matlName1CF.getChoice().setVisible(false);
					matlName1CF.getChoice().removeAll();
					matlName1CF.setChoices(filteredMatlNames1);
					matlName1CF.getChoice().setVisible(true);
					if(filteredMatlNames1.length>0)
					{
						matlName1CF.getChoice().select(0);
						name1SF.getTextField().setText(filteredMatlNames1[0]);
						formula1SF.getTextField().setText(filteredFormulas1[0]);
						if(useTabDensity)
						{
							gmPerCC1NF.setNumber(filteredGmPerCC1[0]);
						}
					}
					break;
				case "filter2":
					TagSet filteredTagData2 = mlt.filterTagData(tagSet, filterStr);
					if(filterStr.equals(""))
					{
						//copy the original arrays into the filtered arrays
						filteredMatlNames2 = matlName;
						filteredFormulas2 = matlFormula;
						filteredGmPerCC2 =matlGmPerCC;
					}
					else
					{
						filteredMatlNames2 = mlt.getTagSetMatlNamesAsArray(filteredTagData2);
						filteredFormulas2 = mlt.getTagSetMatlFormulasAsArray(filteredTagData2);
						filteredGmPerCC2 =mlt.getTagSetMatlGmPerccAsArray(filteredTagData2);
					}
					matlName2CF.getChoice().setVisible(false);
					matlName2CF.getChoice().removeAll();
					matlName2CF.setChoices(filteredMatlNames2);
					matlName2CF.getChoice().setVisible(true);
					if(filteredMatlNames2.length>0)
					{
						matlName2CF.getChoice().select(0);
						name2SF.getTextField().setText(filteredMatlNames2[0]);
						formula2SF.getTextField().setText(filteredFormulas2[0]);
						if(useTabDensity)
						{
							gmPerCC2NF.setNumber(filteredGmPerCC2[0]);
						}
					}
					break;
				case "gmPerCC1":
				case "gmPerCC2":
				case "brightness1":
				case "brightness2":
					//all of the others are numeric
					String numStr = tf.getText();
					if(!isNumeric(numStr)) dialogOK=false;
					else
					{
						double num = Double.valueOf(numStr);
						if(num<=0) dialogOK=false;
					}
					break;
				case "formula1":
				case "formula2":
					if(mmc.getMevArray(tf.getText())==null) dialogOK = false;
					break;
				}
				getKevBF.getButton().setEnabled(dialogOK);
				if(dialogOK==false) tf.setBackground(errColor);										
				else tf.setBackground(white);
			}				
		}
		//GenericDialog OK button calls dialogItemChanged with a null event
		//Macro recording requires calls to GenericDialog.getNext...() methods
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

}
