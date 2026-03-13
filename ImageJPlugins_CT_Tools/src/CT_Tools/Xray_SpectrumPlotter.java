package CT_Tools;
/*
 * An example plugin that calls several of the functions available in MuMassCalc_J8Lib.jar
 * MuMassCalc_J8Lib.jar is derived from the NIST XCOM web application used to calculate
 * photon cross sections for scattering, photoelectric absorption and pair production,
 * as well as total attenuation coefficients, for any element, compound (Z less than o equal to 100),
 * at energies from 1 keV to 100 GeV.
 * See: https://github.com/LazzyIzzi/MuMassCalculator
 *
 * This plugin supports plotting of tabulated absorption spectra for selected cross-sections for the user supplied
 * formula and energy range.  Optionally the absorption edge energies for the atoms in the formula
 * are reported in a separate results table.
 *
 * 	The update plot button or OK button creates or updates the existing Plot window "Mass Attenuation" and Absorption edges results table
 * 	and places them to the right of the dialog.(done)
 * 	The OK button is macro recordable.
 *
 * 	Run from a macro creates a new window for each call and renames it to Mass Attenuation + formulaName
 * The OK button
 */

import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
import java.util.ArrayList;
//import java.util.Vector;

import DocumentReader.DocumentReader;
import gray.AtomData.AtomData;
import ij.IJ;
import ij.ImagePlus;
//import ij.ImagePlus;
//import ij.Macro;
import ij.WindowManager;
import ij.gui.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.text.TextPanel;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.MuMassCalculator.MuMassCalculator;
import tagTools.TagListTools;
import tagTools.TagListTools.TagSet;

public class Xray_SpectrumPlotter implements PlugIn, DialogListener, ActionListener {
	// class used to hold the dialog settings.
	private class DlogSettings {
		String formula;
		String formulaName;
		double gmPerCC;
		double minMeV;
		double maxMeV;
		boolean plotMeVLogScale;
		boolean[] muMassSelections;
		boolean plotMuMassLogScale;
		boolean reportEdges;
		String muType;
	}

	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	final Font myFont = new Font(Font.DIALOG, Font.ITALIC + Font.BOLD, 14);
	// The background color for the GenericDialog just because I like it.
	final Color myColor = new Color(240, 230, 190);// slightly darker than buff
	final Color errColor = new Color(255, 100, 0);
	final Color white = new Color(255, 255, 255);

	DlogSettings ds = new DlogSettings();
	MuMassCalculator mmc = new MuMassCalculator();
	// Allocate array for muMass type checkBoxes, see getSelections()

	// ds.muMassSelections = new boolean[mmc.getMuMassTypes().length];

	String[] mmTypes;// holds a list of cross-section types Total, Compton etc.
	String[] edgeNames;// holds a list of absorption edge labels, K, L1, L2 etc.
	// For loading the defaultMaterials.csv file into arrays
	TagListTools mlt = new TagListTools();
	TagSet tagSet;
	String[] matlNames;
	String[] matlFormulas;
	double[] matlGmPerCC;
	String[] filteredMatlNames;
	String[] filteredFormulas;

	double[] filteredGmPerCC;
	// For the Dialog
	GenericDialog gd;
	int dlogW, dlogH, dlogL, dlogT;

	String myDialogTitle = "Mu Mass Spectrum";
	// For the plots
	String plotTitle = "Mass Attenuation";
	PlotWindow gPlotWindow;
	final int plotW = 625, plotH = 350, resultW = 650, resultH = 160;
	final Color buff = new Color(250, 240, 200);

	Color[] lineColors = new Color[6];
	StringField filterSF, formulaSF, matlNameSF;
	ChoiceField matlNameCF;
	ButtonField updateBF, editBtnBF;
	NumericField minKeVNF, maxKeVNF, gmPerCCNF;

	RadioButtonField muChoiceRBF;

	@Override
	public void actionPerformed(ActionEvent theEvent) {
		getSelections();
		String cmd = theEvent.getActionCommand();
		switch (cmd) {
		case "Update": {
			updatePlot();
			if (ds.reportEdges) {
				updateEdges();
			}
		}
			break;
		}
	}

	@Override
	// @SuppressWarnings("unchecked")
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		boolean dialogOK = true;
		if (e != null) {
			Object src = e.getSource();
			if (src instanceof Choice) {
				int index = matlNameCF.getChoice().getSelectedIndex();
				formulaSF.getTextField().setText(filteredFormulas[index]);
				matlNameSF.getTextField().setText(filteredMatlNames[index]);
				ds.formulaName = filteredMatlNames[index];
				ds.formula = filteredFormulas[index];
				ds.gmPerCC = filteredGmPerCC[index];
			} else if (src instanceof TextField) {
				TextField tf = (TextField) e.getSource();
				String name = tf.getName();
				String filterStr = tf.getText();
				double minKev, maxKev;
				switch (name) {
				case "filter":
					TagSet filteredTagData = mlt.filterTagData(tagSet, filterStr);
					if (filterStr.equals("")) {
						// copy the original arrays into the filtered arrays
						filteredMatlNames = matlNames;
						filteredFormulas = matlFormulas;
						filteredGmPerCC = matlGmPerCC;
					} else {
						filteredMatlNames = mlt.getTagSetMatlNamesAsArray(filteredTagData);
						filteredFormulas = mlt.getTagSetMatlFormulasAsArray(filteredTagData);
						filteredGmPerCC = mlt.getTagSetMatlGmPerccAsArray(filteredTagData);
					}
					matlNameCF.getChoice().setVisible(false);
					matlNameCF.getChoice().removeAll();
					matlNameCF.setChoices(filteredMatlNames);
					matlNameCF.getChoice().setVisible(true);
					if (filteredMatlNames.length > 0) {
						matlNameCF.getChoice().select(0);
						matlNameSF.getTextField().setText(filteredMatlNames[0]);
						formulaSF.getTextField().setText(filteredFormulas[0]);
						gmPerCCNF.setNumber(filteredGmPerCC[0]);
					}
					break;
				case "gmPerCC":
					// all of the others are numeric
					String numStr = tf.getText();
					if (!isNumeric(numStr)) {
						dialogOK = false;
					} else {
						// Zero density makes material transparent
						double num = Double.valueOf(numStr);
						if (num < 0)
							dialogOK = false;
					}
					break;

				case "formula":
					String formula = formulaSF.getTextField().getText();
					double[] mevList = mmc.getMevArray(formula);
					if (mevList == null) {
						dialogOK = false;
						updateBF.getButton().setEnabled(false);
					} else
						updateBF.getButton().setEnabled(true);
					break;
				case "minKeV":
					String minStr = tf.getText();
					maxKev = maxKeVNF.getNumber();
					if (!isNumeric(minStr) || Double.isNaN(maxKev))
						dialogOK = false;
					else {
						minKev = Double.valueOf(minStr);
						if (minKev < 1 || minKev > maxKev || minKev > 1e9)
							dialogOK = false;
					}
					break;
				case "maxKeV":
					String maxStr = tf.getText();
					minKev = minKeVNF.getNumber();
					if (!isNumeric(maxStr) || Double.isNaN(minKev))
						dialogOK = false;
					else {
						maxKev = Double.valueOf(maxStr);
						if (maxKev > 1e9 || maxKev < minKev || maxKev < 1)
							dialogOK = false;
					}
					break;
				}
				if (!dialogOK) {
					tf.setBackground(errColor);
					updateBF.getButton().setEnabled(false);
				} else {
					tf.setBackground(white);
					updateBF.getButton().setEnabled(true);
				}
			}
		}
		return dialogOK;
	}

	@Override
	public void run(String arg) {
		// Plots will be drawn in separate cascaded windows when this plugin is called
		// by a macro
		// IJ.debugMode=true;

		if (IJ.versionLessThan("1.53u")) {
			IJ.showMessage("Newer ImageJ Version Required",
					"Update ImageJ to version 1.53u or better to run this plugin");
			return;
		}

		// Get the tagSet data as arrays
		String dir = IJ.getDirectory("plugins");
		String defaultFilePath = dir + "DialogData\\DefaultMaterials.csv";
		tagSet = mlt.readTagSetFile(defaultFilePath);
		matlNames = mlt.getTagSetMatlNamesAsArray(tagSet);
		matlFormulas = mlt.getTagSetMatlFormulasAsArray(tagSet);
		matlGmPerCC = mlt.getTagSetMatlGmPerccAsArray(tagSet);

		// Allocate array for muMass type checkBoxes, see getSelections()
		ds.muMassSelections = new boolean[mmc.getMuMassTypes().length];
		mmTypes = mmc.getMuMassTypes();
		edgeNames = mmc.getabsEdgeNames();

		// give each muMassType a distinct color
		lineColors[0] = Color.black;
		lineColors[1] = Color.blue;
		lineColors[2] = Color.red;
		lineColors[3] = Color.green;
		lineColors[4] = Color.cyan;
		lineColors[5] = Color.magenta;

		DoDialog();
		DoRoutine();
	}

	private void DoDialog() {

		gd = GUI.newNonBlockingDialog(myDialogTitle);
		// gd = new GenericDialog(myDialogTitle);
		GenericDialogAddin gda = new GenericDialogAddin();

		gd.addDialogListener(this);
		gd.setInsets(0, 0, 0);
		gd.addMessage("Plot tabulated mass absorption\n" + "cross-sections in cm2/gm", myFont);

		gd.setInsets(5, 0, 0);
		gd.addMessage("Formula Format\n" + "Atom1:Count1:Atom2:Count2 etc.", myFont);

		gd.addStringField("Material_List_Filter", "");
		filterSF = gda.getStringField(gd, null, "filter");

		gd.addChoice("Material List", matlNames, matlNames[0]);
		matlNameCF = gda.getChoiceField(gd, null, "matlName");

		gd.addStringField("Material Name: ", matlNames[0], 18);
		matlNameSF = gda.getStringField(gd, null, "matlName");

		gd.addStringField("Formula: ", matlFormulas[0], 18);
		formulaSF = gda.getStringField(gd, null, "formula");

		gd.addNumericField("gmPerCC: ", matlGmPerCC[0], 18);
		gmPerCCNF = gda.getNumericField(gd, null, "gmPerCC");

		gd.addMessage("Energy Range", myFont);
		gd.addNumericField("Min keV", 1, 12);
		minKeVNF = gda.getNumericField(gd, null, "minKeV");
		gd.addNumericField("Max keV", 100000000, 12);
		maxKeVNF = gda.getNumericField(gd, null, "maxKeV");
		gd.addCheckbox("Plot_keV Log scale", true);
		gd.addCheckbox("Plot_cm2/gm on Log scale", true);
		gd.addCheckbox("List absorption edge energies", false);
		gd.addMessage("Cross-section", myFont);
		String[] muMassTypes = mmc.getMuMassTypes();
		boolean[] state = {true,true,true,true,false,false};
		gd.addCheckboxGroup(2, 3, muMassTypes, state);

		gd.addMessage("Attenuation values", myFont);
		String[] items = { "MuMass", "MuLin" };
		gd.addRadioButtonGroup(null, items, 1, 2, items[0]);
		gd.addButton("Update", this);
		updateBF = gda.getButtonField(gd, "updateBtn");

		gd.addMessage("_____________________________", myFont);
		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/XraySpectrumPlotter.html");
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));

		gd.showDialog();
	}

	private void DoRoutine() {
		if (gd.wasCanceled()) {
			if (gPlotWindow != null)
				gPlotWindow.close();
			ResultsTable rt = ResultsTable.getResultsTable("Absorption Edges(keV)");
			if (rt != null) {
				IJ.selectWindow("Absorption Edges(keV)");
				IJ.run("Close");
			}
			return;
		}

		else if (gd.wasOKed()) {
			getSelections();
			if (ds != null) {
				if (IJ.isMacro())// macros draw plots in separate new windows
				{
					drawPlot();
					if (ds.reportEdges) {
						drawEdges();
					}
				} else// create/update plot and result windows to the right of dialog
				{
					updatePlot();
					if (ds.reportEdges) {
						updateEdges();
					}
				}
			}
		}
	}

	private void drawEdges() {
		ResultsTable rt = prepareAbsEdgeResults();
		rt.show("Absorption Edges(keV)");
		Window win = WindowManager.getWindow("Absorption Edges(keV)");
		// Scroll to current result
		TextPanel txtPnl = (TextPanel) win.getComponent(0);
		txtPnl.showRow(txtPnl.getLineCount());
	}

	private void drawPlot() {
		Plot newPlot = preparePlot();
		if (newPlot != null) {
			String title = newPlot.getTitle();
			newPlot.show();
			ImagePlus imp = newPlot.getImagePlus();
			imp.setTitle(title + "_" + ds.formulaName);
			newPlot.setFrozen(true);
		}
	}

	private void getSelections() {
		gd.resetCounters();
		try {
			@SuppressWarnings("unused")
			String filterStr = gd.getNextString(); // dummy read for choice
			ds.formulaName = gd.getNextString();
			ds.formula = gd.getNextString();
			ds.gmPerCC = gd.getNextNumber();
			ds.minMeV = gd.getNextNumber() / 1000;
			ds.maxMeV = gd.getNextNumber() / 1000;
			ds.plotMeVLogScale = gd.getNextBoolean();
			ds.plotMuMassLogScale = gd.getNextBoolean();
			ds.reportEdges = gd.getNextBoolean();
			for (int i = 0; i < ds.muMassSelections.length; i++) {
				ds.muMassSelections[i] = gd.getNextBoolean();
			}
			ds.muType = gd.getNextRadioButton();

		} catch (Exception e) {
			// e.printStackTrace();
			IJ.showMessage("Error",
					"To record, the Macro Recorder must be open before\nlaunching the X-ray Calculator Plugin");
		}
	}

	/**creates or updates an absorption edge results table from the dialog formula
	 * @return a reference to a ResultsTable
	 */
	private ResultsTable prepareAbsEdgeResults() {
		ResultsTable rt = ResultsTable.getResultsTable("Absorption Edges(keV)");
		if (rt == null) {
			rt = new ResultsTable();
			rt.setPrecision(4);
		}
		ArrayList<AtomData> fl = mmc.createFormulaList(ds.formula);
		if (fl != null) {
			for (int i = 0; i < fl.size(); i++) {
				boolean atomFound = false;
				String theAtom = mmc.getAtomSymbol(fl, i);
				String atm = theAtom;
				// Check if the atom already listed i the results table
				for (int j = 0; j < rt.size(); j++) {
					if (atm.toUpperCase().equals(rt.getStringValue("Name", j).toUpperCase())) {
						atomFound = true;
						break;
					}
				}
				// if the atom is not in the table get its edges and add them to the table
				if (!atomFound) {
					rt.incrementCounter();
					rt.addValue("Name", theAtom);
					for (String edge : edgeNames) {
						double edgeMev = mmc.getAtomAbsEdge(theAtom, edge);
						if (edgeMev > 0) {
							rt.addValue(edge, edgeMev * 1000);
						}
					}
				}
			}
			return rt;
		} else {
			return null;
		}

	}

	/**
	 * Prepares a plot using the dialog settings
	 * 
	 * @return reference to a Plot
	 */
	private Plot preparePlot() {
		Plot newPlot;

		int i, j, cnt;

		// Get the tabulated energies associated with atoms in the formula
		double[] meVArr = mmc.getMevArray(ds.formula, ds.minMeV, ds.maxMeV);

		if (meVArr == null || meVArr.length == 0) {
			IJ.showMessage("There is no tabulated data for " + ds.formula + " between " + ds.minMeV * 1000 + " and "
					+ ds.maxMeV * 1000 + " keV" + "\nPlease select a wider range to plot.");
			return null;
		}

		// count the number of muMassTypes selected++++++++++++++++++++++++++
		for (i = 0, cnt = 0; i < ds.muMassSelections.length; i++) {
			if (ds.muMassSelections[i] == true)
				cnt++;
		}

		// create a 2D array to hold each spectrum
		double[][] muMass = new double[cnt][meVArr.length];

		// Get the mass attenuation coefficients of the formula++++++++++++++
		// for each checked flag and for each meV
		String legend = "";
		for (i = 0, cnt = 0; i < ds.muMassSelections.length; i++) {
			if (ds.muMassSelections[i]) {
				for (j = 0; j < meVArr.length; j++) {
					muMass[cnt][j] = mmc.getMuMass(ds.formula, meVArr[j], mmTypes[i]);
				}
				legend += mmTypes[i] + "\t";
				cnt++;
			}
		}
		// Plot the spectra+++++++++++++++++++++++++++++++++++++++++++++++++++
		// If the user closes the plot window but the gPlotWindow is not set to null
		// Convert MeV to keV
		double[] keVArr = new double[meVArr.length];
		for (i = 0; i < meVArr.length; i++) {
			keVArr[i] = meVArr[i] * 1000;
		}

		// convert MuMass to MuLin
		double scale = 1.0d;
		String muUnit = "cm2/gm";
		if (ds.muType.equals("MuLin")) {
			scale = ds.gmPerCC;
			muUnit = ("cm-1");
			for (j = 0; j < cnt; j++) {
				for (i = 0; i < meVArr.length; i++) {
					muMass[j][i] *= scale;
				}
			}
		}
		newPlot = new Plot(plotTitle, "keV", muUnit);
		newPlot.setFont(Font.BOLD, 16);

		// add the spectra to the plot
		// the circles are used to show the positions of the tabulated NIST data
		for (i = 0, cnt = 0; i < ds.muMassSelections.length; i++) {
			if (ds.muMassSelections[i]) {
				newPlot.setColor(lineColors[i]);
				newPlot.addPoints(keVArr, muMass[cnt], Plot.CONNECTED_CIRCLES);
				cnt++;
			}
		}

		newPlot.setLineWidth(1);
		if (ds.plotMeVLogScale) {
			newPlot.setLogScaleX();
		}
		if (ds.plotMuMassLogScale) {
			newPlot.setLogScaleY();
		}
		newPlot.addLegend(legend, "top-right");
		newPlot.setLimitsToFit(true);
		newPlot.setColor(Color.BLACK);
		newPlot.addLabel(0, 0.9, ds.formulaName + "\n" + ds.formula);
		newPlot.setSize(plotW, plotH);
		newPlot.setBackgroundColor(buff);
		return newPlot;
	}

	// *********************************************************************************/

	private void updateEdges() {
		ResultsTable rt = prepareAbsEdgeResults();
		rt.show("Absorption Edges(keV)");
		Point plotLoc = gPlotWindow.getLocation();
		int plotHeight = gPlotWindow.getHeight();

		Window win = WindowManager.getWindow("Absorption Edges(keV)");
		win.setLocation(plotLoc.x, plotLoc.y + plotHeight);
		// Scroll to current result
		TextPanel txtPnl = (TextPanel) win.getComponent(0);
		txtPnl.showRow(txtPnl.getLineCount());

	}

	// *********************************************************************************/
	// *********************************************************************************/

//	private boolean validateParams()
//	{
//		boolean paramsOK = true;
//		double[] mevList = mmc.getMevArray(ds.formula);
//		if(mevList==null)
//		{
//			IJ.showMessage("Error", ds.formula + " Bad Formula, Element or count missing");
//			paramsOK = false;
//		}
//		
//		if(paramsOK)
//		{
//			updateBF.getButton().setEnabled(true);			
//		}
//		else
//		{
//			updateBF.getButton().setEnabled(false);						
//		}
//
//		return paramsOK;
//	}

	// *********************************************************************************/

	private void updatePlot() {
		dlogW = gd.getSize().width;
		dlogH = gd.getSize().height;
		dlogL = gd.getLocation().x;
		dlogT = gd.getLocation().y;

		Plot plot = preparePlot();
		gPlotWindow = (PlotWindow) WindowManager.getWindow(plotTitle);
		if (gPlotWindow == null) {
			gPlotWindow = plot.show();
		} else {
			gPlotWindow.drawPlot(plot);
		}
		gPlotWindow.setLocation(dlogL + dlogW, dlogT);
	}

}
