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
import tagTools.TagListTools;
import tagTools.TagListTools.TagData;
import tagTools.TagListTools.TagSet;

public class Lookup_keV implements PlugIn, ActionListener, DialogListener {

	private class DialogSettings {
		String formulaName;
		String formula;
		double gmPerCC;
		double attn;
	}
	
	private static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	String myDialogTitle = "Lookup keV";
	String resultsTitle = "keV Solutions";
	// The default width and height of the keV solutions table
	final int calcW = 850, calcH = 230;

	// class used to hold the dialog settings.
	MuMassCalculator mmc = new MuMassCalculator();
	TagListTools mlt = new TagListTools();
	GenericDialog gd = GUI.newNonBlockingDialog(myDialogTitle);
	GenericDialogAddin gda = new GenericDialogAddin();
	final Color myColor = new Color(240, 230, 190);// slightly darker than buff
	final Color errColor = new Color(255, 100, 0);
	final Color white = new Color(255, 255, 255);
	final Font myFont = new Font(Font.DIALOG, Font.ITALIC + Font.BOLD, 14);
	DialogSettings ds = new DialogSettings();
		
	TagSet tagSet;
	String[] matlName, filteredMatlName;
	String[] matlFormula, filteredMatlFormula;
	double[] matlGmPerCC, filteredMatlGmPerCC;
	
	int dlogW, dlogH, dlogL, dlogT;
	boolean isMacro = false;
	boolean useTabDensity;
	
	StringField matlNameSF, filterSF, formulaSF;
	ChoiceField matlNameCF;
	NumericField gmPerCCNF, muLinNF;
	ButtonField getKevBF;

	public void actionPerformed(ActionEvent theEvent) {
		dlogW = gd.getSize().width;
		dlogH = gd.getSize().height;
		dlogL = gd.getLocation().x;
		dlogT = gd.getLocation().y;
		getSelections();
		String cmd = theEvent.getActionCommand();
		switch (cmd) {
		case "Get keV":
			double[] mevResult = mmc.getMeVfromMuLin(ds.formula, ds.attn, ds.gmPerCC, "TotAttn");
			ResultsTable rt = prepareResults(mevResult);
			rt.show(resultsTitle);
			Window win = WindowManager.getWindow(resultsTitle);
			win.setLocation(dlogL + dlogW, dlogT);
			win.setSize(calcW, dlogH);
			// Scroll to current result
			TextPanel txtPnl = (TextPanel) win.getComponent(0);
			txtPnl.showRow(txtPnl.getLineCount());
			break;
		}
	}

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		boolean dialogOK = true;

		if (e != null) {
			getSelections();
			Object src = e.getSource();
			if (src instanceof Choice) {
				Choice choice = (Choice) src;
				String name = choice.getName();
				switch (name) {
				case "matlNameChoice":
					int index = matlNameCF.getChoice().getSelectedIndex();
					formulaSF.getTextField().setText(filteredMatlFormula[index]);
					matlNameSF.getTextField().setText(filteredMatlName[index]);
					if (useTabDensity) {
						gmPerCCNF.setNumber(filteredMatlGmPerCC[index]);
					}
					break;
				}
			} else if (src instanceof TextField) {
				TextField tf = (TextField) src;
				String name = tf.getName();
				String filterStr = tf.getText();
				switch (name) {
				case "filter":
					TagSet filteredTagData = mlt.filterTagData(tagSet, filterStr);
					if (filterStr.equals("")) {
						// copy the original arrays into the filtered arrays
						filteredMatlName = matlName;
						filteredMatlFormula = matlFormula;
						filteredMatlGmPerCC = matlGmPerCC;
					} else {
						filteredMatlName = mlt.getTagSetMatlNamesAsArray(filteredTagData);
						filteredMatlFormula = mlt.getTagSetMatlFormulasAsArray(filteredTagData);
						filteredMatlGmPerCC = mlt.getTagSetMatlGmPerccAsArray(filteredTagData);
					}
					matlNameCF.getChoice().setVisible(false);
					matlNameCF.getChoice().removeAll();
					matlNameCF.setChoices(filteredMatlName);
					matlNameCF.getChoice().setVisible(true);
					if (filteredMatlName.length > 0) {
						matlNameCF.getChoice().select(0);
						matlNameSF.getTextField().setText(filteredMatlName[0]);
						formulaSF.getTextField().setText(filteredMatlFormula[0]);
						if (useTabDensity) {
							gmPerCCNF.setNumber(filteredMatlGmPerCC[0]);
						}
					}
					break;
				case "formula":
					if (mmc.getMevArray(tf.getText()) == null)
						dialogOK = false;
					break;
				case "density":
				case "muLin":
					// all of the others are numeric
					String numStr = tf.getText();
					if (!isNumeric(numStr))
						dialogOK = false;
					else {
						double num = Double.valueOf(numStr);
						if (num <= 0)
							dialogOK = false;
					}
					break;
				}
				getKevBF.getButton().setEnabled(dialogOK);
				if (dialogOK == false)
					tf.setBackground(errColor);
				else
					tf.setBackground(white);
			}
		}
		getSelections();
		return dialogOK;
	}

	public void DoDialog() {

		InitSettings();

		gd.addMessage("Get Energy from single component linear attenuation(cm-1)", myFont);
		gd.addMessage("Formula Format: " + "Atom1:Count1:Atom2:Count2 etc.", myFont);
		gd.addCheckbox("Import_tabulated_densities", true);

		// First selection block
		gd.setInsets(5, 0, 0);
		gd.addStringField("Search Names", "");
		filterSF = gda.getStringField(gd, null, "filter");

		gd.addChoice("Material Names", filteredMatlName, filteredMatlName[0]); // myTags.matlName[0] is column header
																				// title
		matlNameCF = gda.getChoiceField(gd, null, "matlNameChoice");

		gd.addStringField("Material_Name", filteredMatlName[0]);
		matlNameSF = gda.getStringField(gd, null, "matlName");

		gd.addStringField("Formula: ", ds.formula, 26);
		formulaSF = gda.getStringField(gd, null, "formula");
		gd.addNumericField("Density:", ds.gmPerCC, 4, 8, "gm/cc");
		gmPerCCNF = gda.getNumericField2(gd, null, null, "density");
		gd.addNumericField("Observed_cm-1", ds.attn);
		muLinNF = gda.getNumericField(gd, null, "muLin");
		gd.setInsets(0, 250, 0);
		gd.addButton("Get keV", this);
		getKevBF = gda.getButtonField(gd, "getKev");

		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/XrayLookupMuLin.html");
		gd.addDialogListener(this);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.showDialog();

	}

	@Override
	public void run(String arg) {
		if (Macro.getOptions() != null)
			isMacro = true;

		if (IJ.versionLessThan("1.53u")) {
			IJ.showMessage("Newer ImageJ Version Required",
					"Update ImageJ to version 1.53u or better to run this plugin");
			return;
		}

		// Location of the default materials list
		String dir = IJ.getDirectory("plugins");
		String defaultFilePath = dir + "DialogData\\DefaultMaterials.csv";
//		tagSet = mlt.loadTagFile(defaultFilePath);

		tagSet = mlt.readTagSetFile(defaultFilePath);
		if (tagSet == null) {
			IJ.error("Unable to load/create plugins/DialogData/DefaultMaterials.csv");
			return;
		}
		// Get names array from TagSet
		matlName = new String[tagSet.tagData.size()];
		matlFormula = new String[tagSet.tagData.size()];
		matlGmPerCC = new double[tagSet.tagData.size()];
		int i = 0;
		for (TagData td : tagSet.tagData) {
			matlName[i] = td.matlName;
			matlFormula[i] = td.matlFormula;
			matlGmPerCC[i] = td.matlGmPerCC;
			i++;
		}

		filteredMatlName = matlName;
		filteredMatlFormula = matlFormula;
		filteredMatlGmPerCC = matlGmPerCC;
		DoDialog();
		DoRoutine();
	}

	private void DoRoutine() {
		dlogW = gd.getSize().width;
		dlogH = gd.getSize().height;
		dlogL = gd.getLocation().x;
		dlogT = gd.getLocation().y;

		if (gd.wasCanceled()) {
			ResultsTable rt;
			rt = ResultsTable.getResultsTable(resultsTitle);
			if (rt != null) {
				IJ.selectWindow(resultsTitle);
				IJ.run("Close");
			}
			return;
		} else if (gd.wasOKed()) {
			getSelections();
			if (ds != null) {
//				if (ValidateParams())
//				{
				double[] meVresult = mmc.getMeVfromMuLin(ds.formula, ds.attn, ds.gmPerCC, "TotAttn");
				ResultsTable rt = prepareResults(meVresult);
				rt.show(resultsTitle);
				Window win = WindowManager.getWindow(resultsTitle);
				if (!isMacro) {
					win.setLocation(dlogL + dlogW, dlogT);
					win.setSize(calcW, dlogH);
				}
				// Scroll to current result
				TextPanel txtPnl = (TextPanel) win.getComponent(0);
				txtPnl.showRow(txtPnl.getLineCount());
			}
//			}
		}
	}

	private void getSelections() {
		gd.resetCounters();
		try {
			ds.formulaName = gd.getNextString();// dummy read
			ds.formulaName = gd.getNextChoice();// dummy read
			ds.formulaName = gd.getNextString();
			ds.formula = gd.getNextString();
			ds.gmPerCC = gd.getNextNumber();
			ds.attn = gd.getNextNumber();
			useTabDensity = gd.getNextBoolean();
		} catch (Exception e) {
			IJ.showMessage("Error",
					"To record, the Macro Recorder must be open before\nlaunching the X-ray Calculator Plugin");
		}
	}

	private void InitSettings() {
		ds.formulaName = filteredMatlName[0];
		ds.formula = filteredMatlFormula[0];
		ds.attn = 10;
		ds.gmPerCC = filteredMatlGmPerCC[0];
	}

	private ResultsTable prepareResults(double[] result) {
		String muLinStr = "MuLin cm" + (char) 0x207b + (char) 0x0b9;

		ResultsTable rt = ResultsTable.getResultsTable(resultsTitle);
		if (rt == null || rt.getCounter() == 0) {
			rt = new ResultsTable();
			rt.setPrecision(5);
			rt.addValue("Name", "");
			rt.addValue("Formula", "");
			rt.addValue("gm/cc", "");
			rt.addValue(muLinStr, "");
			rt.deleteRow(0);
		}

		rt.incrementCounter();
		if (result != null) {
			for (int i = 0; i < result.length; i++) {
				rt.addValue("keV " + i, result[i] * 1000);
			}
		} else {
			rt.addValue("keV 0", "No Solution");
		}
		rt.addValue("Name", ds.formulaName);
		rt.addValue("Formula", ds.formula);
		rt.addValue(muLinStr, ds.attn);
		rt.addValue("gm/cc", ds.gmPerCC);

		return rt;
	}

}
