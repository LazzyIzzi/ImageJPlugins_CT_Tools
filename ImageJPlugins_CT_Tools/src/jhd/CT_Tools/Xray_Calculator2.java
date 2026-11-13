package jhd.CT_Tools;
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
 * This version replaces the Choice menu with a JComboBox.  This change simplifies coding of the selection of
 * tag list items. It runs OK with blocking and non-blocking Dialogs but macros recorded by the non-blocking
 * version do not run. The issue is apparently caused by the JComboBox waiting for user input.
 * JHD 10-26-26
 */
 

import java.awt.*;
import java.awt.event.*;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboPopup;

import ij.plugin.*;
import ij.text.TextPanel;
import ij.gui.*;
import ij.*;
import ij.measure.*;

import jhd.MuMass.XrayCalculator;
import tagTools.TagListTools;
import tagTools.TagListTools.TagSet;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;

public class Xray_Calculator2 implements PlugIn, ActionListener, DialogListener, ItemListener {

	protected class DialogSettings {
		String formulaName, formula;
		double gmPerCC;
		double meV;
		double minMeV;
		double maxMeV;
	}
	DialogSettings ds = new DialogSettings();

	private static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	XrayCalculator xrc = new XrayCalculator();
	TagListTools mlt = new TagListTools();
	TagSet tagSet;
	String[] matlNames;
	String[] matlFormulas;
	double[] matlGmPerCCs;
	String[] filteredMatlName;
	int dlogW, dlogH, dlogL, dlogT;
	boolean useTabDensity;

	final String myDialogTitle = "Xray Calculator";
	final String resultsTitle = "Xray Calculator Results";
	final int calcW = 650, calcH = 220;
	final Color myColor = new Color(240, 230, 190);// slightly darker than buff
	final Color errColor = new Color(255, 100, 0);

	final Color white = new Color(255, 255, 255);
	final Font myFont = new Font(Font.DIALOG, Font.ITALIC + Font.BOLD, 14);

	GenericDialog gd;
	GenericDialogAddin gda;
	StringField matlNameSF, filterSF, formulaSF;
	NumericField gmPerCCNF, kevNF, comboBoxNF;
	CheckboxField useTabDensCBF;
	ButtonField getKevBF;

	public void actionPerformed(ActionEvent theEvent) {
		getSelections();
		String cmd = theEvent.getActionCommand();
		switch (cmd) {
		case "Calculate":
			UpdateResults();
			break;
		}
	}

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		boolean dialogOK = true;
		getSelections();

		if (e != null) {
			Object src = e.getSource();
			if (src instanceof TextField) {
				TextField tf = (TextField) e.getSource();
				String name = tf.getName();
				switch (name) {
				case "gmPerCC":
					// all of the others are numeric
					String numStr = tf.getText();
					if (!isNumeric(numStr))
						dialogOK = false;
					else {
						double num = Double.valueOf(numStr);
						if (num < 0)
							dialogOK = false;
					}
					break;
				case "kev":
					String keVStr = tf.getText();
					if (!isNumeric(keVStr))
						dialogOK = false;
					else {
						double keV = Double.valueOf(keVStr);
						if (keV < 1 || keV > 1e9)
							dialogOK = false;
					}
					break;
				case "formula":
					if (xrc.validateFormula(tf.getText()) == false)
						dialogOK = false;
					break;
				}
				getKevBF.getButton().setEnabled(dialogOK);
				if (dialogOK == false)
					tf.setBackground(errColor);
				else
					tf.setBackground(white);

			}
		}

//		getSelections();
		return dialogOK;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e != null) {

			Object src = e.getSource();
			if (src instanceof JComboBox) {
				@SuppressWarnings("rawtypes")
				JComboBox cb = (JComboBox) src;

				if (cb.getName().equals("tagList")) {
					int index = cb.getSelectedIndex();
					matlNameSF.getTextField().setText(matlNames[index]);
					formulaSF.getTextField().setText(matlFormulas[index]);
					if (useTabDensCBF.getCheckBox().getState() == true) {
						gmPerCCNF.setNumber(matlGmPerCCs[index]);
					}
				}

			} else {
			}
		}
	}

	@Override
	public void run(String arg) {
		if (IJ.versionLessThan("1.53u")) {
			IJ.showMessage("Newer ImageJ Version Required",
					"Update ImageJ to version 1.53u or newer to run this plugin");
			return;
		}

		// Location of the default materials list
		String dir = IJ.getDirectory("plugins");
		String defaultFilePath = dir + "DialogData\\DefaultMaterials.csv";
		tagSet = mlt.readTagSetFile(defaultFilePath);
		if (tagSet == null) {
			IJ.error("Unable to load/create plugins/DialogData/DefaultMaterials.csv");
			return;
		}
		matlNames = mlt.getTagSetMatlNamesAsArray(tagSet);
		matlFormulas = mlt.getTagSetMatlFormulasAsArray(tagSet);
		matlGmPerCCs = mlt.getTagSetMatlGmPerccAsArray(tagSet);


		DoDialog();
		if(gd.wasOKed())
			DoRoutine();
	}

	private void DoDialog() {
		gda = new GenericDialogAddin();
		gd = GUI.newNonBlockingDialog(myDialogTitle);
		// newNonBlockingDialog does not support JComboBox
//		gd = new GenericDialog(myDialogTitle);
		gd.addDialogListener(this);

		double theKeV = 100.0;

		gd.setInsets(0, 0, 0);
		gd.addMessage("Compute total linear absorption(cm-1)\n" + "and mass attenuations(cm2/gm)", myFont);

		gd.setInsets(5, 0, 0);
		gd.addMessage("Formula Format\n" + "Atom1:Count1:Atom2:Count2 etc.", myFont);

		gd.addCheckbox("Import_tabulated_densities", true);
		useTabDensCBF = gda.getCheckboxField(gd, "useTabDensity");

		// Setup JComboBox
		JComboBox<String> jcb = new JComboBox<>();
		jcb.setName("tagList");
		for (String name : matlNames) {
			jcb.addItem(name);
		}
		jcb.setBackground(Color.WHITE);
		jcb.setPreferredSize(new Dimension(150, 25));
		// jcb.setFont(myFont);
		// Magic words to make the JComboBox background stay white
		Object child = jcb.getAccessibleContext().getAccessibleChild(0);
		BasicComboPopup popup = (BasicComboPopup) child;
		@SuppressWarnings("rawtypes")
		JList list = popup.getList();
		list.setSelectionBackground(Color.WHITE);
		jcb.addItemListener(this);

		// put the JComboBox in a panel so gd.add puts it in a new row
		// otherwise it is installed in the same row as the previous item.
		Panel panel = new Panel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
		Label label = new Label("Available Tags");
		panel.add(label);
		panel.add(jcb);
		gd.addPanel(panel);

		gd.addStringField("Material_Name", matlFormulas[0], 18);
		matlNameSF = gda.getStringField(gd, null, "matlName");

		gd.addStringField("Formula", matlFormulas[0], 18);
		formulaSF = gda.getStringField(gd, null, "formula");

		gd.addNumericField("Density", matlGmPerCCs[0], 4, 8, "gm/cc");
		gmPerCCNF = gda.getNumericField2(gd, null, null, "gmPerCC");

		gd.addNumericField("keV", theKeV);
		kevNF = gda.getNumericField(gd, null, "kev");

		gd.setInsets(10, 120, 0);
		gd.addButton("Calculate", this);
		getKevBF = gda.getButtonField(gd, "getKev");
		gd.addMessage("_____________________________", myFont);
		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/XrayCalculator.html");
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.setOKLabel("Close");

		gd.showDialog();

	}

	private void DoRoutine() {
		if (gd.wasCanceled()) {
			ResultsTable rt;
			rt = ResultsTable.getResultsTable("Absorption Edges(keV)");
			if (rt != null) {
				IJ.selectWindow("Absorption Edges(keV)");
				IJ.run("Close");
			}
			rt = ResultsTable.getResultsTable(resultsTitle);
			if (rt != null) {
				IJ.selectWindow(resultsTitle);
				IJ.run("Close");
			}
			return;
		}

		else if (gd.wasOKed()) {
			// MMCsettings mmcSet = getSelections();
			getSelections();
			UpdateResults();

		}
	}

	private void getSelections() {
		gd.resetCounters();
		try {
			//String str = gd.getNextChoice();
			ds.formulaName = gd.getNextString();
			ds.formula = gd.getNextString();
			ds.gmPerCC = gd.getNextNumber();
			ds.meV = gd.getNextNumber() / 1000;
			useTabDensity = gd.getNextBoolean();
		} catch (Exception e) {
			IJ.showMessage("Error",
					"To record, the Macro Recorder must be open before\nlaunching the X-ray Calculator Plugin");
		}
	}

	private void UpdateResults() {
		dlogW = gd.getSize().width;
		dlogH = gd.getSize().height;
		dlogL = gd.getLocation().x;
		dlogT = gd.getLocation().y;
		ResultsTable rt = ResultsTable.getResultsTable(resultsTitle);
		if (rt == null) {
			rt = new ResultsTable();
			rt.setPrecision(5);
		}

		try {
			String[] mmTypes = xrc.getMuMassTypes();
			double myKeV = ds.meV * 1000;
			rt.incrementCounter();
			rt.addValue("Name", ds.formulaName);
			rt.addValue("Formula", ds.formula);
			rt.addValue("gm/cc", ds.gmPerCC);
			rt.addValue("keV", myKeV);
			try {
				rt.addValue("Linear Attn cm" + (char) 0x207b + (char) 0x0b9,
						ds.gmPerCC * xrc.getFormulaMuMass(ds.formula, myKeV / 1000, mmTypes[0]));
				for (int i = 0; i < mmTypes.length; i++) {
					double muMass = xrc.getFormulaMuMass(ds.formula, myKeV / 1000, mmTypes[i]);
					rt.addValue(mmTypes[i], muMass);
				}
				rt.show(resultsTitle);
				Window win = WindowManager.getWindow(resultsTitle);
				// if dialog is open, window follows the dialog box
				if (dlogL > 0)
					win.setLocation(dlogL + dlogW, dlogT);
				// Scroll to current result
				TextPanel txtPnl = (TextPanel) win.getComponent(0);
				txtPnl.showRow(txtPnl.getLineCount());
			} catch (Exception e1) {
				// do Nothing
			}

		} catch (Exception e1) {
			// do Nothing
		}
	}

}
