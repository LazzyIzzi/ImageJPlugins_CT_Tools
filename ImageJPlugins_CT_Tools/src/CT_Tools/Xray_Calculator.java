package CT_Tools;
/*
 * An example plugin that calls several of the functions available in MuMassCalc_J8Lib.jar
 * MuMassCalc_J8Lib.jar is derived from the NIST XCOM web application used to calculate 
 * photon cross sections for scattering, photoelectric absorption and pair production,
 * as well as total attenuation coefficients, for any element, compound (Z less than o equal to 100),
 * at energies from 1 keV to 100 GeV.
 * See: https://github.com/LazzyIzzi/MuMassCalculator
 * 
 * This plugin supports interactive calculation of interpolated linear(cm-1) and mass attenuation (cm2/gm)
 * from a user-supplied simplified chemical formula, density, and the photon energy. Calculated values 
 * are posted to a results window.
 * 
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import ij.plugin.*;
import ij.text.TextPanel;
import ij.gui.*;
import ij.*;
import ij.measure.*;

import jhd.MuMassCalculator.*;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.TagTools.*;
import jhd.TagTools.MatlListTools.TagSet;
import gray.AtomData.*;


	public class Xray_Calculator implements PlugIn, ActionListener, DialogListener
{

	MuMassCalculator mmc= new MuMassCalculator();
	MatlListTools mlt=new MatlListTools();
	MatlListTools.TagSet tagSet;
	String[] matlName;
	String[] matlFormula;
	double[] matlGmPerCC;

	String[] filteredMatlName;
	String[] filteredMatlFormula;
	double[] filteredMatlGmPerCC;

	final String myDialogTitle = "Xray Calculator";
	final String resultsTitle = "Xray Calculator Results";
	final int calcW=650,calcH=220;
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Color errColor = new Color(255,100,0);
	final Color white = new Color(255,255,255);
	final Font myFont = new Font(Font.DIALOG, Font.ITALIC+Font.BOLD, 14);
	
	GenericDialog gd;
	GenericDialogAddin gda;
	
	int dlogW,dlogH,dlogL,dlogT;
	boolean useTabDensity;
	
	//***********************************************************************************/

	protected class DialogSettings
	{
		String formulaName,formula;
		double gmPerCC;
		double meV;
		double minMeV;
		double maxMeV;
	}
	DialogSettings ds = new DialogSettings();
	
	//***********************************************************************************/
	
	@Override
	public void run(String arg)
	{
		if(IJ.versionLessThan("1.53u"))
		{
			IJ.showMessage("Newer ImageJ Version Required", "Update ImageJ to version 1.53u or better to run this plugin");
			return;
		}
		
		//Location of the default materials list
		String dir = IJ.getDirectory("plugins");
		String defaultFilePath = dir + "DialogData\\DefaultMaterials.csv";
		
		tagSet = mlt.loadTagFile(defaultFilePath);
		//Get arrays from TagSet
		matlName = mlt.getTagSetMatlNamesAsArray(tagSet);
		matlFormula = mlt.getTagSetMatlFormulasAsArray(tagSet);
		matlGmPerCC = mlt.getTagSetMatlGmPerccAsArray(tagSet);
		
		filteredMatlName=matlName;
		filteredMatlFormula=matlFormula;
		filteredMatlGmPerCC=matlGmPerCC;


		DoDialog();
		DoRoutine();		
	}

	//*****************************************************************************
	StringField matlNameSF,filterSF,formulaSF;
	ChoiceField matlNameCF;
	NumericField gmPerCCNF,kevNF;
	CheckboxField useTabDensCF;
	ButtonField getKevBF;

	private void DoDialog()
	{
		gda = new GenericDialogAddin();
		gd =  GUI.newNonBlockingDialog(myDialogTitle);
		gd.addDialogListener(this);
		
		double theKeV = 100.0;
		
		gd.setInsets(0, 0, 0);
		gd.addMessage("Compute total linear absorption(cm-1)\n"
				+ "and mass attenuations(cm2/gm)",myFont);
		gd.addCheckbox("Import_tabulated_densities", true);
		useTabDensCF = gda.getCheckboxField(gd, "useTabDensity");
		
		gd.setInsets(5, 0, 0);
		gd.addMessage("Formula Format\n"
				+ "Atom1:Count1:Atom2:Count2 etc.", myFont);
		gd.addStringField("Material_List_Filter",  "");
		filterSF = gda.getStringField(gd, null, "filter");
		gd.addChoice("Material_List", matlName, matlName[0]);
		matlNameCF = gda.getChoiceField(gd, null, "materialChoice");
		gd.addStringField("Material_Name",  matlFormula[0],18);
		matlNameSF = gda.getStringField(gd, null, "matlName");
		
		gd.addStringField("Formula",  matlFormula[0],18);
		formulaSF = gda.getStringField(gd, null, "formula");
		
		gd.addNumericField("Density", matlGmPerCC[0],4,8,"gm/cc");
		gmPerCCNF = gda.getNumericField2(gd, null,null, "gmPerCC");
		
		gd.addNumericField("keV", theKeV);
		kevNF = gda.getNumericField(gd, null, "kev");
		
		gd.setInsets(10, 120, 0);
		gd.addButton("Calculate", this);
		getKevBF=gda.getButtonField(gd, "getKev");
		gd.addMessage("_____________________________",myFont);
		gd.addHelp("https://lazzyizzi.github.io/XrayCalculator.html");
		gd.setBackground(myColor);		
		gd.showDialog();
		
	}
	
	//*********************************************************************************/

	private void DoRoutine()
	{
		if (gd.wasCanceled())
		{
			ResultsTable rt;
			rt = ResultsTable.getResultsTable("Absorption Edges(keV)");
			if(rt!=null)
			{			
				IJ.selectWindow("Absorption Edges(keV)");
				IJ.run("Close");
			}
			rt = ResultsTable.getResultsTable(resultsTitle);
			if(rt!=null)
			{			
				IJ.selectWindow(resultsTitle);
				IJ.run("Close");
			}
			return;
		}

		else if(gd.wasOKed())
		{
			//MMCsettings mmcSet = getSelections();
			getSelections();
			UpdateResults();
		
		}
	}

	//*********************************************************************************/
	
	private boolean ValidateParams()
	{
		boolean paramsOK = true;
		double[] mevList = mmc.getMevArray(ds.formula);
		if(mevList==null)
		{
			IJ.showMessage("Error", ds.formula + " Bad Formula, Element or count missing");
			paramsOK = false;
		}
		if(ds.meV < 0.001 || ds.meV > 100000)
		{
			IJ.showMessage("Error:  keV Out of range  1 < keV < 100,000,000");
			paramsOK = false;
		}		
		return paramsOK;		
	}

	//*********************************************************************************/
	

	private void getSelections()
	{
		gd.resetCounters();
			try {
				@SuppressWarnings("unused")
				String filterStr = gd.getNextString();
				ds.formulaName = gd.getNextString();
				ds.formula = gd.getNextString();
				ds.gmPerCC = gd.getNextNumber();
				ds.meV =  gd.getNextNumber()/1000;
				useTabDensity = gd.getNextBoolean();
			} catch (Exception e) {
				IJ.showMessage("Error", "To record, the Macro Recorder must be open before\nlaunching the X-ray Calculator Plugin");
			}
	}

	//*********************************************************************************/
	
	private void UpdateResults()
	{
		dlogW = gd.getSize().width;
		dlogH = gd.getSize().height;
		dlogL = gd.getLocation().x;
		dlogT = gd.getLocation().y;
		ResultsTable rt = ResultsTable.getResultsTable(resultsTitle);
		if(rt==null)
		{
			rt=new ResultsTable();
			rt.setPrecision(5);
		}
		
		try
		{
			String[] mmTypes =mmc.getMuMassTypes();
			ArrayList<AtomData> fl = mmc.createFormulaList(ds.formula);
			
			double myKeV = ds.meV*1000;
			if(fl != null && myKeV> 0.001 && myKeV < 100000)
			{
				rt.incrementCounter();
				rt.addValue("Name", ds.formulaName);
				rt.addValue("Formula", ds.formula);
				rt.addValue("gm/cc", ds.gmPerCC);
				rt.addValue("keV", myKeV);
				try
				{
					rt.addValue("Linear Attn cm"+(char)0x207b+(char)0x0b9, ds.gmPerCC * mmc.getMuMass(ds.formula, myKeV/1000, mmTypes[0]));
					for(int i=0;i<mmTypes.length;i++)
					{
						double muMass = mmc.getMuMass(ds.formula, myKeV/1000, mmTypes[i]); 				
						rt.addValue(mmTypes[i], muMass);
					}
					rt.show(resultsTitle);
					Window win = WindowManager.getWindow(resultsTitle);
					//if dialog is open, window follows the dialog box
					if(dlogL>0) win.setLocation(dlogL+dlogW,dlogT);
					//Scroll to current result
					TextPanel txtPnl = (TextPanel)win.getComponent(0);
					txtPnl.showRow(txtPnl.getLineCount());
				}
				catch(Exception e1)
				{
					//do Nothing
				} 
			}
		}
		catch(Exception e1)
		{
			//do Nothing
		}
	}
	
	//*********************************************************************************/

	public void actionPerformed(ActionEvent theEvent)
	{
		getSelections();
		String cmd = theEvent.getActionCommand();
		switch(cmd)
		{
		case "Calculate":
			if (ValidateParams())
				UpdateResults();
			break;
		}
	}

	//*********************************************************************************/
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dialogOK = true; //getSelections();;

		if(e!=null)
		{
			Object src = e.getSource();			
			if(src instanceof Choice)
			{
				Choice choice = (Choice)src;
				switch(choice.getName())
				{
				case "materialChoice":
					int index =  matlNameCF.getChoice().getSelectedIndex();
					matlNameSF.getTextField().setText(filteredMatlName[index]);
					formulaSF.getTextField().setText(filteredMatlFormula[index]);
					if(useTabDensity)
					{
						gmPerCCNF.setNumber(filteredMatlGmPerCC[index]);
					}
					dialogOK=true;
					break;
				}
			}
			else if(src instanceof TextField)
			{					
				TextField tf = (TextField) e.getSource();
				String name = tf.getName();
				String filterStr = tf.getText();
				switch(name)
				{
				case "filter":
					TagSet filteredTagData = mlt.filterTagData(tagSet, filterStr);
					if(filterStr.equals(""))
					{
						//copy the original arrays into the filtered arrays
						filteredMatlName = matlName;
						filteredMatlFormula = matlFormula;
						filteredMatlGmPerCC =matlGmPerCC;
					}
					else
					{
						filteredMatlName = mlt.getTagSetMatlNamesAsArray(filteredTagData);
						filteredMatlFormula = mlt.getTagSetMatlFormulasAsArray(filteredTagData);
						filteredMatlGmPerCC =mlt.getTagSetMatlGmPerccAsArray(filteredTagData);
					}
					matlNameCF.getChoice().setVisible(false);
					matlNameCF.getChoice().removeAll();
					matlNameCF.setChoices(filteredMatlName);
					matlNameCF.getChoice().setVisible(true);
					if(filteredMatlName.length>0)
					{
						matlNameCF.getChoice().select(0);
						matlNameSF.getTextField().setText(filteredMatlName[0]);
						formulaSF.getTextField().setText(filteredMatlFormula[0]);
						if(useTabDensity)
						{
							gmPerCCNF.setNumber(filteredMatlGmPerCC[0]);
						}
					}
					break;
				case "gmPerCC":
					//all of the others are numeric
					String numStr = tf.getText();
					if(!isNumeric(numStr)) dialogOK=false;
					else
					{
						double num = Double.valueOf(numStr);
						if(num<=0) dialogOK=false;
					}
					break;
				case "kev":
					String keVStr = tf.getText();
					if(!isNumeric(keVStr)) dialogOK=false;
					else
					{
						double keV = Double.valueOf(keVStr);
						if(keV<1 || keV > 1e9) dialogOK=false;
					}
					break;
				case "formula":
					if(mmc.getMevArray(tf.getText())==null) dialogOK = false;
					break;
					
				}
				getKevBF.getButton().setEnabled(dialogOK);
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

}
