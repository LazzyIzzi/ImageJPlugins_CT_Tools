package CT_Tools;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Arrays;
//import java.util.Vector;

import ij.*;
import ij.plugin.*;
import ij.text.TextPanel;
import ij.text.TextWindow;
import ij.gui.*;
import ij.measure.ResultsTable;
import jhd.Serialize.Serializer;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;
import jhd.MuMassCalculator.*;

import tagTools.*;
import tagTools.TagListTools.TagSet;


public class Scanner_Setup implements PlugIn, DialogListener, ActionListener
//public class Beam_Hardening_Estimator implements PlugIn, ActionListener
{
	private class TauData
	{
		double[] path;
		double[] tau;
	}
	private class XrayMetrics
	{
		double sampleTauDet,sampleMuLin;
		//double[] sampleEff;
		double thinSampleTauDet;
		double thinSampleMuLin;
		//double[] thinSampleEff;
		double[] sampleMevEff;
		double[] beamHardening;
		double photonUse,filterTrans,detectorAbs;
	}
	private class XraySpectra
	{			
		//The source
		int size;
		double[] src,kevList;		
		//The Scanner absorbances
		double[] filterTau,sampleTau,thinSampleTau,detTau;
		//The Spectra, Some currently unnecessary spectra commented out
		//double[] srcNoFiltDet;
		double[] srcFilt,srcFiltDet;
		//double[] srcNoFiltSamp;
		double[] srcFiltSamp,srcFiltThinSamp,srcFiltSampDet,srcFiltThinSampDet;
	}
	
	//A bunch of constants
	final String mySettingsTitle = "Scanner_Setup";
	final String dialogTitle = "Scanner Setup";
	final String spectPlotTitle = "X-ray Spectra";
	final String tauPlotTitle = "Attenuation vs Thickness";
	final String metricsResultsTitle = "Scanner Setup Results";
	
	// A factor to estimate the thickness of the thinnest part of the sample
	// Since most detectors are ~1000 pixels .001 is a about a one pixel path
	// paths less than 1 pixel will be dominated by partial voxel effects
	static final double gThin = .001;
	
	//Does the attenuation calculations
	MuMassCalculator mmc= new MuMassCalculator();
	
	//A serializable class used to store the dialog settings between calls
	MuMassCalculator.BeamHardenParams bhSet = new MuMassCalculator.BeamHardenParams();
	
	//The class used to serialize (save) the users selections
	Serializer ser = new Serializer();
	
	//A class to manage materials lists organized as CSV tag files
	TagListTools mlt=new TagListTools();
	
	//A tagSet consists of a String tagHdr[4] and an ArrayList of tag data
	TagSet matlTagSet;
	TagSet elementTagSet;
	
	//Used to convert the tagData ArrayList to the separate arrays needed by GenericDialog
	String[] matlName,elementName;
	String[] matlFormula,elementFormula;	
	double[] matlGmPerCC,elementGmPerCC;

	String[] filteredMatlName;
	String[] filteredMatlFormula,filteredElementFormula;	
	double[] filteredMatlGmPerCC;

	String dir = IJ.getDirectory("plugins"); 
	String settingsPath = dir+ "DialogSettings" + File.separator + mySettingsTitle + ".ser";
	String[] elementSymb = Arrays.copyOf(mmc.getAtomSymbols(),mmc.getAtomSymbols().length);
		
	final int plotWidth=600,plotHeight=300;//275;
	
	final Color buff = new Color(250,240,200);
	
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Color errColor = new Color(255,100,0);
	final Color white = new Color(255,255,255);
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	boolean isMacro=false;
	GenericDialog gd;
	
	public void actionPerformed(ActionEvent e)
	{
		if(e!=null)
		{
			switch(e.getActionCommand())
			{
			case "Update Plot":
				XraySpectra xrs = getXraySpectra();
				Plot xrsPlot = prepareSpectraPlot(xrs);
				showUpdatePlot(xrsPlot);

				XrayMetrics xrm = getXrayMetrics(xrs);			
				ResultsTable xrmRT = prepareMetricsResultsTable(xrm);
				xrmRT.show(metricsResultsTitle);
				Window win = WindowManager.getWindow(metricsResultsTitle);
				//Scroll to current result
				TextPanel txtPnl = (TextPanel)win.getComponent(0);
				txtPnl.showRow(txtPnl.getLineCount());

				TauData td= getTauData();
				Plot tauPlot = prepareTauPlot(td);
				showUpdatePlot(tauPlot);
				
				arrangeWindows();
			}
		}
	}
	
	//*********************************************************************************/

	private void arrangeWindows()
	{
//		final String spectPlotTitle = "X-ray Spectra";
//		final String tauPlotTitle = "Attenuation vs Thickness";	
//		final String metricsResultsTitle = "BH Workbench Results";
		int dlogW = gd.getSize().width;
		int dlogH = gd.getSize().height;
		int dlogL = gd.getLocation().x;
		int dlogT = gd.getLocation().y;
		Window win;
		// the tau plot to right of dialog top
		win = WindowManager.getWindow(tauPlotTitle);
		win.setLocation(dlogL+dlogW,dlogT);
		// the spectrum plot to right of dialog middle
		win = WindowManager.getWindow(spectPlotTitle);
		win.setLocation(dlogL+dlogW,dlogT + dlogH/2);
		// the spectrum plot to right of dialog middle
		win = WindowManager.getWindow(metricsResultsTitle);
		win.setLocation(dlogL,dlogT + dlogH);
		win.setSize(dlogW+plotWidth,plotHeight);
	}
	
	//*********************************************************************************/
	
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
				case "sampleChoice":
					int index = sampleCF.getChoice().getSelectedIndex();
					sampFormulaSF.getTextField().setText(filteredMatlFormula[index]);
					sampGmPerCCNF.setNumber(filteredMatlGmPerCC[index]);
					break;
				case "detectorChoice":
					index = detectorCF.getChoice().getSelectedIndex();
					detFormulaSF.getTextField().setText(filteredMatlFormula[index]);
					detGmPerCCNF.setNumber(filteredMatlGmPerCC[index]);
					break;
				}
			}
			else if(src instanceof TextField)
			{
				TextField tf = (TextField)src;
				String name = tf.getName();
				String filterStr = tf.getText();
				TagSet filteredTagData;
				String numStr;
				switch(name)
				{
				case "sampleFilter":
					filteredTagData = mlt.filterTagData(matlTagSet, filterStr);
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
					sampleCF.getChoice().setVisible(false);
					sampleCF.getChoice().removeAll();
					sampleCF.setChoices(filteredMatlName);
					sampleCF.getChoice().setVisible(true);
					if(filteredMatlName.length>0)
					{
						sampleCF.getChoice().select(0);
						sampFormulaSF.getTextField().setText(filteredMatlFormula[0]);
						sampGmPerCCNF.setNumber(filteredMatlGmPerCC[0]);
					}
					break;
				case "detectorFilter":
					filteredTagData = mlt.filterTagData(matlTagSet, filterStr);
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
					detectorCF.getChoice().setVisible(false);
					detectorCF.getChoice().removeAll();
					detectorCF.setChoices(filteredMatlName);
					detectorCF.getChoice().setVisible(true);
					if(filteredMatlName.length>0)
					{
						detectorCF.getChoice().select(0);
						detFormulaSF.getTextField().setText(filteredMatlFormula[0]);
						detGmPerCCNF.setNumber(filteredMatlGmPerCC[0]);
					}
					break;
				case "detectorFormula":
				case "sampleFormula":
					if(mmc.getMevArray(tf.getText())==null) dialogOK = false;
					break;
				case "filterThickness":
					numStr = tf.getText();
					if(!isNumeric(numStr)) dialogOK=false;
					else
					{
						double num = Double.valueOf(numStr);
						if(num<0) dialogOK=false;
					}
					break;
				default:
					//all of the others are numeric
					numStr = tf.getText();
					if(!isNumeric(numStr)) dialogOK=false;
					else
					{
						double num = Double.valueOf(numStr);
						if(num<=0) dialogOK=false;
					}
					break;
				}
				updateBtnBF.getButton().setEnabled(dialogOK);
				if(dialogOK==false) tf.setBackground(errColor);										
				else tf.setBackground(white);
			}
		}

		getSelections();
		return dialogOK;		
	}
	
	//***************************************************************************************
	ChoiceField sampleCF,detectorCF;
	StringField sampFiltSF,detFiltSF,sampFormulaSF,detFormulaSF;
	NumericField KVNF,maNF,filterThickNF,sampGmPerCCNF,detGmPerCCNF,detThicknessNF;
	ButtonField updateBtnBF;
	
	public void doDialog()
	{
		String[] plotChoices = {"keV","Angstroms"};
		GenericDialogAddin gda = new GenericDialogAddin();

		gd = GUI.newNonBlockingDialog(dialogTitle);
		gd.addDialogListener(this);

		//X-ray Source
		gd.setInsets(10,0,0);
		gd.addMessage("X-ray Source________________",myFont,Color.BLACK);
		gd.addChoice("Target",elementSymb,bhSet.target);
		gd.addNumericField("KV", bhSet.kv);
		KVNF = gda.getNumericField(gd, null, "KV");
		gd.addNumericField("mA", bhSet.ma);
		maNF = gda.getNumericField(gd, null, "ma");

		//Filter
		gd.setInsets(10,0,0);
		gd.addMessage("Source Filter________________",myFont,Color.BLACK);
		gd.addChoice("Filter_Material",elementSymb,bhSet.filter);
		gd.addNumericField("Filter_Thickness(cm)", bhSet.filterCM);
		filterThickNF = gda.getNumericField(gd, null, "filterThickness");

		//Sample
		gd.setInsets(10,0,0);
		gd.addMessage("Sample____________________",myFont,Color.BLACK);
		gd.addStringField("Sample_Name_Filter", "");
		sampFiltSF = gda.getStringField(gd, null, "sampleFilter");

		gd.addChoice("Sample_Name_Choices:", matlName, matlName[0]);
		sampleCF = gda.getChoiceField(gd, null, "sampleChoice");		
		gd.addStringField("Sample_Formula", bhSet.matlFormula);
		sampFormulaSF = gda.getStringField(gd, null, "sampleFormula");

		
		gd.addNumericField("Sample_Thickness(cm)", bhSet.matlCM);
		gd.addNumericField("Sample_Density(gm/cc)", bhSet.matlGmPerCC);
		sampGmPerCCNF = gda.getNumericField(gd, null, "sampleGmPerCC");

		//Detector
		gd.setInsets(10,0,0);
		gd.addMessage("Detector___________________",myFont,Color.BLACK);
		gd.addStringField("Detector_Name_Filter", "");
		detFiltSF = gda.getStringField(gd, null, "detectorFilter");
		gd.addChoice("Detector_Name_Choices:", matlName, matlName[0]);
		detectorCF = gda.getChoiceField(gd, null, "detectorChoice");
		gd.addStringField("Detector_Formula", bhSet.detFormula);
		detFormulaSF = gda.getStringField(gd, null, "detectorFormula");
		gd.addNumericField("Detector_Thickness(cm)", bhSet.detCM);
		
		gd.addNumericField("Detector_Density(gm/cc)", bhSet.detGmPerCC);
		detGmPerCCNF = gda.getNumericField(gd, null, "detGmPerCC");
		
		//Plot Range
		gd.setInsets(10,0,0);
		gd.addMessage("Plot Range_______________",myFont,Color.BLACK);
		gd.addNumericField("Plot_Minimum(keV)", bhSet.kvMin);
		gd.addNumericField("Plot_Inc(keV)", bhSet.kvInc);
		gd.setInsets(0,100,0);
		gd.addRadioButtonGroup(null, plotChoices, 2, 1, bhSet.plotChoice);

		gd.setInsets(20,80,10);
		gd.addButton("Update Plot", this);
		updateBtnBF = gda.getButtonField(gd, "updateBtn");
		gd.setAlwaysOnTop(false);

		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/CtScannerSetup.html");
				
		gd.setBackground(myColor);
		gd.showDialog();
	}

	//***************************************************************************************
	
	private void doRoutine()
	{
		//Getting dialog settings and displaying results are event driven.
		//See actionPerformed()		
		if (gd.wasCanceled())
		{
			PlotWindow plotWin = (PlotWindow)WindowManager.getWindow(tauPlotTitle);			
			if(plotWin!=null)plotWin.close();
			
			plotWin = (PlotWindow)WindowManager.getWindow(spectPlotTitle);
			if(plotWin!=null)plotWin.close();
			
			TextWindow txtWin = (TextWindow)WindowManager.getWindow(metricsResultsTitle);
			if(txtWin!=null)txtWin.close();
//			IJ.selectWindow(metricsResultsTitle);
//			IJ.run("Close");
			return;
		}
		else if(gd.wasOKed())
		{
			getSelections();
			ser.SaveObjectAsSerialized(bhSet, settingsPath);
			if(isMacro==false)
			{

				XraySpectra xrs = getXraySpectra();
				Plot xrsPlot = prepareSpectraPlot(xrs);
				showUpdatePlot(xrsPlot);

				XrayMetrics xrm = getXrayMetrics(xrs);			
				ResultsTable xrmRT = prepareMetricsResultsTable(xrm);
				xrmRT.show(metricsResultsTitle);

				TauData td= getTauData();
				Plot tauPlot = prepareTauPlot(td);
				showUpdatePlot(tauPlot);

				arrangeWindows();
			}
			else
			{
				String str  =  " :src=" + bhSet.target + "," + bhSet.kv + "kV," + bhSet.ma + "ma";
				str = str + ":filter=" + bhSet.filter + "," + bhSet.filterCM + "cm";//+ bhSet.filterGmPerCC + "gm/cc";

				XraySpectra xrs = getXraySpectra();
				Plot xrsPlot = prepareSpectraPlot(xrs);
				xrsPlot.show();
				ImagePlus imp = xrsPlot.getImagePlus();
				imp.setTitle(spectPlotTitle+str);
				
				//xrsPlot.setFrozen(true);
				XrayMetrics xrm = getXrayMetrics(xrs);			
				ResultsTable xrmRT = prepareMetricsResultsTable(xrm);
				xrmRT.show(metricsResultsTitle);


				TauData td= getTauData();
				Plot tauPlot = prepareTauPlot(td);
				tauPlot.show();
				imp = tauPlot.getImagePlus();
				imp.setTitle(tauPlotTitle+str);

			}
			return;
		}	
	}
			
	//***************************************************************************************

	private void getSelections()
	{
		boolean dialogOK=true;
		
		gd.resetCounters();
		bhSet.target = gd.getNextChoice();
		bhSet.filter = gd.getNextChoice();		
		bhSet.filterGmPerCC = mmc.getAtomGmPerCC(bhSet.filter);
		bhSet.kv = gd.getNextNumber();
		bhSet.ma = gd.getNextNumber();
		bhSet.filterCM = gd.getNextNumber();
		bhSet.matlCM = gd.getNextNumber();
		bhSet.matlGmPerCC = gd.getNextNumber();
		bhSet.detCM = gd.getNextNumber();
		bhSet.detGmPerCC = gd.getNextNumber();
		bhSet.kvMin = gd.getNextNumber();		
		bhSet.kvInc = gd.getNextNumber();
		@SuppressWarnings("unused")
		String dumStr = gd.getNextString();
		bhSet.matlFormula = gd.getNextString();
		dumStr = gd.getNextString();
		bhSet.detFormula = gd.getNextString();
		bhSet.plotChoice = gd.getNextRadioButton();			
	}

	//***************************************************************************************
	//This method calculates tau vs thickness
	private TauData getTauData()
	{
		final double pathStep = 0.1;
		int size = (int)Math.ceil((bhSet.matlCM)/pathStep) + 1;
		TauData td = new TauData();
		td.tau = new double[size];
		td.path = new double[size];

		double src;//The source
		double filterTau,sampleTau,detTau;//The Scanner absorbances
		double srcFilt,srcFiltDet,srcFiltSamp,srcFiltSampDet;//The Spectra
		double srcFiltDetIntg,srcFiltSampDetIntg;//Integration
		double keV,meV;
		double path=0;
		for( int i = 0; i < size; i++)
		{
			srcFiltDetIntg = 0;
			srcFiltSampDetIntg =0;
			for(keV = bhSet.kv; keV >= bhSet.kvMin; keV-= bhSet.kvInc)
			{
				//The Source Intensity at meV
				meV = keV / 1000;
				src = mmc.spectrumKramers(bhSet.kv, bhSet.ma, bhSet.target, meV);	

				//The component attenuations
				filterTau = mmc.getMuMass(bhSet.filter, meV, "TotAttn") * bhSet.filterCM * bhSet.filterGmPerCC;           
				detTau = mmc.getMuMass(bhSet.detFormula, meV, "TotAttn") * bhSet.detCM * bhSet.detGmPerCC;
				sampleTau = mmc.getMuMass(bhSet.matlFormula, meV, "TotAttn") * path * bhSet.matlGmPerCC;

				//The intensities
				srcFilt = src * Math.exp(-filterTau);            //The filtered source
				srcFiltDet = srcFilt * (1 - Math.exp(-detTau));  //The filtered source detected
				srcFiltSamp = srcFilt * Math.exp(-sampleTau);    //The source attenuated by the bhSet.filter and the sample
				srcFiltSampDet = srcFiltSamp * (1 - Math.exp(-detTau));  //The source attenuated by the bhSet.filter and the sample detected

				//Integrate
				srcFiltDetIntg += srcFiltDet;
				srcFiltSampDetIntg += srcFiltSampDet;
			}
			td.tau[i] = -Math.log(srcFiltSampDetIntg / srcFiltDetIntg);
			td.path[i] = path;
			path += pathStep;
		}
		return td;
	}
		
	//***************************************************************************************
	
//    /**Java TextField should have a method like this!!
//	 * @param textFieldVector A vector containing a generic dialog's text fields
//	 * @param textFieldName The name of the field 
//	 * @return the index of the field
//	 */
//	private int getTextFieldIndex(Vector<TextField> textFieldVector, String textFieldName)
//	{
//		int index=-1, cnt = 0;
//		for(TextField tf: textFieldVector)
//		{
//			String name = tf.getName();
//			if(name.equals(textFieldName))
//			{
//				index = cnt;
//				break;				
//			}
//			else cnt++;
//		}
//		return index;
//	}
//	
	//***************************************************************************************
	
	private XrayMetrics getXrayMetrics(XraySpectra xrs)
	{
		XrayMetrics xrm = new XrayMetrics();
		//Integration
		double srcIntg=0;
		//double srcNoFiltDetIntg=0;
		double srcFiltIntg=0;
		double srcFiltDetIntg=0;
		//double srcNoFiltSampIntg=0;
		//double srcFiltSampIntg=0;
		//double srcFiltThinSampIntg=0;
		double srcFiltSampDetIntg=0;
		double srcFiltThinSampDetIntg=0;

		for(int i=0;i<xrs.size;i++)
		{
			srcIntg += xrs.src[i]*bhSet.kvInc;
			srcFiltIntg += xrs.srcFilt[i] * bhSet.kvInc;
			srcFiltDetIntg += xrs.srcFiltDet[i] * bhSet.kvInc;
			//srcNoFiltSampIntg +=srcNoFiltSamp[i] *  bhSet.kvInc;//Currently unused
			//srcFiltSampIntg += srcFiltSamp[i] * bhSet.kvInc;//Currently unused
			//srcFiltThinSampIntg += srcFiltThinSamp[i] * bhSet.kvInc;//Currently unused
			srcFiltSampDetIntg += xrs.srcFiltSampDet[i] * bhSet.kvInc;
			srcFiltThinSampDetIntg += xrs.srcFiltThinSampDet[i] * bhSet.kvInc;
		}
		xrm.sampleTauDet = -Math.log(srcFiltSampDetIntg / srcFiltDetIntg);
		xrm.sampleMuLin = xrm.sampleTauDet / bhSet.matlCM;
		xrm.sampleMevEff = mmc.getMeVfromMuLin(bhSet.matlFormula, xrm.sampleMuLin, bhSet.matlGmPerCC,"TotAttn");
		
		int effLen = xrm.sampleMevEff.length;
		
		xrm.thinSampleTauDet = -Math.log(srcFiltThinSampDetIntg / srcFiltDetIntg);
		xrm.thinSampleMuLin = xrm.thinSampleTauDet / (gThin * bhSet.matlCM);
		double[] thinSampleEff = mmc.getMeVfromMuLin(bhSet.matlFormula, xrm.thinSampleMuLin, bhSet.matlGmPerCC,"TotAttn");
		int thinEffLen = thinSampleEff.length;
		
		
		if(xrm.sampleMevEff!=null && thinSampleEff!=null && thinEffLen==effLen)
		{			
			xrm.beamHardening = new double[effLen];
			for(int j=0;j<effLen;j++)
			{					
				double drift =xrm.sampleMevEff[j] -thinSampleEff[j];
				xrm.beamHardening[j] = drift/xrm.sampleMevEff[j]*100;
			}
		}
		else
		{
			xrm.beamHardening = new double[1];
			xrm.beamHardening[0]=Double.NaN;
		}
		xrm.detectorAbs = srcFiltDetIntg / srcFiltIntg;
		xrm.filterTrans = srcFiltIntg / srcIntg;
		xrm.photonUse = xrm.detectorAbs*xrm.filterTrans;

		return xrm;
	}
	
	//***************************************************************************************
	
	private XraySpectra getXraySpectra()
	{
		XraySpectra xrs = new XraySpectra();
		int size = (int)((bhSet.kv-bhSet.kvMin)/bhSet.kvInc) +1;
		xrs.size = size;
		//The source
		xrs.src = new double[size];
		xrs.kevList = new double[size];
		
		//The Scanner absorbances
		xrs.filterTau = new double[size];
		xrs.sampleTau = new double[size];
		xrs.thinSampleTau = new double[size];
		xrs.detTau = new double[size];
				
		//The Spectra, Some currently unnecessary spectra commented out
		//xrs.srcNoFiltDet = new double[size];
		xrs.srcFilt = new double[size];
		xrs.srcFiltDet = new double[size];
		//xrs.srcNoFiltSamp = new double[size];
		xrs.srcFiltSamp = new double[size];
		xrs.srcFiltThinSamp = new double[size];
		xrs.srcFiltSampDet = new double[size];
		xrs.srcFiltThinSampDet = new double[size];

		double meV;
		int i=0;
		//for(double keV = bhSet.kvMin; keV <= bhSet.kv; keV+= bhSet.kvInc)
		for(double keV = bhSet.kv; keV >= bhSet.kvMin; keV-= bhSet.kvInc)
		{
			//The Source Spectrum
			meV = keV / 1000;
			xrs.kevList[i] = keV;			
			xrs.src[i] = mmc.spectrumKramers(bhSet.kv, bhSet.ma, bhSet.target, meV);//get the source continuum intensity spectrum			

            //The component attenuations
			xrs.filterTau[i] = mmc.getMuMass(bhSet.filter, meV, "TotAttn") * bhSet.filterCM * bhSet.filterGmPerCC;           
			xrs.detTau[i] = mmc.getMuMass(bhSet.detFormula, meV, "TotAttn") * bhSet.detCM * bhSet.detGmPerCC;
			xrs.sampleTau[i] = mmc.getMuMass(bhSet.matlFormula, meV, "TotAttn") * bhSet.matlCM * bhSet.matlGmPerCC;
			xrs.thinSampleTau[i] = mmc.getMuMass(bhSet.matlFormula, meV, "TotAttn") * gThin * bhSet.matlCM * bhSet.matlGmPerCC; // 0.001 = approximately 1 pixel
            
            //The intensity spectra
            //srcNoFiltDet[i] = src[i] * (1 - Math.exp(-detTau[i]))    //The unfiltered source Currently unused
			xrs.srcFilt[i] = xrs.src[i] * Math.exp(-xrs.filterTau[i]);            //The filtered source
			xrs.srcFiltDet[i] = xrs.srcFilt[i] * (1 - Math.exp(-xrs.detTau[i]));  //The filtered source detected
            //srcNoFiltSamp[i] = src[i] * Math.exp(-sampleTau[i])      //The source attenuated by only the sample Currently unused
			xrs.srcFiltSamp[i] = xrs.srcFilt[i] * Math.exp(-xrs.sampleTau[i]);    //The source attenuated by the bhSet.filter and the sample
			xrs.srcFiltThinSamp[i] = xrs.srcFilt[i] * Math.exp(-xrs.thinSampleTau[i]);    //The source attenuated by the bhSet.filter and a arbitrary very thin part of the sample
			xrs.srcFiltSampDet[i] = xrs.srcFiltSamp[i] * (1 - Math.exp(-xrs.detTau[i]));  //The source attenuated by the bhSet.filter and the sample detected
			xrs.srcFiltThinSampDet[i] = xrs.srcFiltThinSamp[i] * (1 - Math.exp(-xrs.detTau[i])); //The source attenuated by the bhSet.filter and the thin sample detected

 			i++;
		}		
		return xrs;		
	}
	
	//***************************************************************************************
	
	//***************************************************************************************
	//Called if ReadSerializedObject fails
	private void initializeSettings()
	{
		bhSet = new MuMassCalculator.BeamHardenParams();
		//X-ray Source
		bhSet.target = "W";
		bhSet.kv = 160;
		bhSet.ma = 100;

		//Filter
		bhSet.filter = "Cu";
		bhSet.filterCM = 0.01;
		bhSet.filterGmPerCC = mmc.getAtomGmPerCC(bhSet.filter);

		//Sample
		bhSet.matlFormula = matlTagSet.tagData.get(0).matlFormula;//"Ca:1:C:1:O:3";
		bhSet.matlCM = 3;
		bhSet.matlGmPerCC = matlTagSet.tagData.get(0).matlGmPerCC;//2.71;

		//Detector
		bhSet.detFormula = "Cs:1:I:1";
		bhSet.detCM = 0.01;;
		bhSet.detGmPerCC = 4.51;
		
		//Plot Range
		bhSet.kvMin = 10;
		bhSet.kvInc = 1;
		bhSet.plotChoice = "keV";
	}
	
	//***************************************************************************************
	
	private ResultsTable prepareMetricsResultsTable(XrayMetrics xrm)
	{
		ResultsTable xrmTable;
		xrmTable = ResultsTable.getResultsTable(metricsResultsTitle);
		if(xrmTable==null)
		{
			xrmTable=new ResultsTable();
			xrmTable.setPrecision(3);
		}
		xrmTable.incrementCounter();
		xrmTable.addValue("Sample", bhSet.matlFormula);
		xrmTable.addValue("S CM", bhSet.matlCM);
		xrmTable.addValue("S gm/cc", bhSet.matlGmPerCC);
		xrmTable.addValue("S Tau", xrm.sampleTauDet);
		if(xrm.beamHardening!=null)
		{
			for(int j=0;j<xrm.beamHardening.length;j++)
			{
				xrmTable.addValue("BH%" + j,xrm.beamHardening[j]);
				xrmTable.addValue("Eeff (keV) " + j, xrm.sampleMevEff[j]*1000);
			}
		}
		xrmTable.addValue("Photon Use%",xrm.photonUse*100);
		xrmTable.addValue("Source kv", bhSet.kv);
		xrmTable.addValue("Src ma", bhSet.ma);
		xrmTable.addValue("Src Anode", bhSet.target);

		xrmTable.addValue("Filter", bhSet.filter);
		xrmTable.addValue("F CM", bhSet.filterCM);
		xrmTable.addValue("F gm/cc", bhSet.filterGmPerCC);
		xrmTable.addValue("F Trans%", xrm.filterTrans*100);

		xrmTable.addValue("Detector", bhSet.detFormula);
		xrmTable.addValue("Det CM", bhSet.detCM);
		xrmTable.addValue("Det gm/cc", bhSet.detGmPerCC);
		xrmTable.addValue("Det Absorp.", xrm.detectorAbs);

		return xrmTable;

	}

	//***************************************************************************************

	private Plot prepareSpectraPlot(XraySpectra xrs)
	{
		Plot xrsPlot = null;

		//Plot the results
		String legend = "Source Counts\nFiltered\nSample Trans\nFiltered Detected (Io)\nSample Detected (I)";

		String str  =  "src=" + bhSet.target + "," + bhSet.kv + "kV," + bhSet.ma + "ma";
		str = str + "\nfilter=" + bhSet.filter + "," + bhSet.filterCM + "cm,"+ bhSet.filterGmPerCC + "gm/cc ";
		str = str + "\nsample=" +bhSet.matlFormula + "," + bhSet.matlCM + "cm,"+ bhSet.matlGmPerCC + "gm/cc";
		str = str + "\ndet=" + bhSet.detFormula+ bhSet.detCM + "cm,"+ bhSet.detGmPerCC + "gm/cc";
		String xAxisTitle = "keV";

		//convert from energy to wavelength if requested
		if(bhSet.plotChoice == "Angstroms")
		{
			for(int i=0;i<xrs.kevList.length;i++)
			{
				xrs.kevList[i] = 12.41/xrs.kevList[i];
			}
			xAxisTitle = "Wavelength(Angstroms)";
		}

		//Find min and max MuMass for the source spectrum
		double countsMax = xrs.src[0];
		double countsMin = xrs.src[0];
		for(int i=1;i<xrs.src.length;i++)
		{
			if(countsMax < xrs.src[i]) countsMax = xrs.src[i];
			if(countsMin > xrs.src[i]) countsMin = xrs.src[i];
		}

		xrsPlot = new Plot(spectPlotTitle,xAxisTitle,"Counts");	
		xrsPlot.setSize(plotWidth, plotHeight);			
		xrsPlot.setBackgroundColor(buff);
		xrsPlot.setLogScaleY();
		xrsPlot.setFontSize(12);

		//Source Intensity
		xrsPlot.setLineWidth(2);
		xrsPlot.setColor(Color.blue);
		xrsPlot.addPoints(xrs.kevList,xrs.src,Plot.LINE);

		// intensity after bhSet.filter
		xrsPlot.setColor(Color.red);
		xrsPlot.addPoints(xrs.kevList,xrs.srcFilt,Plot.LINE);

		// intensity after sample
		xrsPlot.setColor(Color.green);
		xrsPlot.addPoints(xrs.kevList,xrs.srcFiltSamp,Plot.LINE);
		//plot.addPoints(kevList,srcFiltDet,Plot.LINE);

		// detected intensity after bhSet.filter i.e. Io
		xrsPlot.setColor(Color.gray);
		xrsPlot.addPoints(xrs.kevList,xrs.srcFiltDet,Plot.LINE);

		// detected intensity
		xrsPlot.setColor(Color.BLACK);
		xrsPlot.addPoints(xrs.kevList,xrs.srcFiltSampDet,Plot.LINE);
		xrsPlot.setLimits(xrs.kevList[0], xrs.kevList[xrs.kevList.length-1], countsMin, countsMax);

		xrsPlot.addLegend(legend);
		xrsPlot.setLegend(legend, Plot.TOP_RIGHT);
		xrsPlot.addLabel(0.02, 0.1, str);
		return xrsPlot;

	}
	
	//***************************************************************************************

	private Plot prepareTauPlot(TauData td)
    {
    	//Find min and max MuMass for the source spectrum
    	Plot tauPlot;

    	double countsMax = td.tau[0];
    	double countsMin = td.tau[0];
    	for(int i=1;i<td.tau.length;i++)
    	{
    		if(countsMax < td.tau[i]) countsMax = td.tau[i];
    		if(countsMin > td.tau[i]) countsMin = td.tau[i];
    	}

    	//Plot the results
        String legend = bhSet.matlFormula + ", " + bhSet.target + ", " + bhSet.kv +"KV" + ", " + bhSet.ma + "mA" + ", " + bhSet.filterCM + "cm "+ bhSet.filter;

    	tauPlot = new Plot(tauPlotTitle,"Path(cm)","Attenuation");
    	tauPlot.setSize(plotWidth, plotHeight);			
    	tauPlot.setBackgroundColor(buff);
    	tauPlot.setFontSize(14);
    	tauPlot.setLineWidth(2);
    	tauPlot.setColor(Color.blue);
    	tauPlot.addPoints(td.path,td.tau,Plot.LINE);
    	tauPlot.addLegend(legend);
    	return tauPlot;
    }
	
	//***************************************************************************************
	
	//***********************************************************************
	/**Loads the materials list, builds the materials arrays, builds the BHparams list*/
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
		String matlFilePath = dir + "DialogData\\DefaultMaterials.csv";
		
		matlTagSet = mlt.readTagSetFile(matlFilePath);
		if(matlTagSet==null)
		{
			IJ.showMessage("DefaultMaterials.csv failed to load");
			return;
		}

		
		//Get names array from TagSet
		matlName = mlt.getTagSetMatlNamesAsArray(matlTagSet);// new String[matlTagSet.tagData.size()];
		matlFormula = mlt.getTagSetMatlFormulasAsArray(matlTagSet); //new String[matlTagSet.tagData.size()];
		matlGmPerCC = mlt.getTagSetMatlGmPerccAsArray(matlTagSet); //  new double[matlTagSet.tagData.size()];

		filteredMatlName=matlName;
		filteredMatlFormula=matlFormula;
		filteredMatlGmPerCC=matlGmPerCC;
		Arrays.sort(elementSymb);

		//Read the saved dialog settings
		bhSet = (MuMassCalculator.BeamHardenParams)ser.ReadSerializedObject(settingsPath);
		if(bhSet==null)
		{
			initializeSettings();
		}
		doDialog();
		doRoutine();
	}
	
	//***************************************************************************************
	
	private void showUpdatePlot(Plot plot)
	{
		PlotWindow plotWin;
		plotWin = (PlotWindow)WindowManager.getWindow(plot.getTitle());
		if(plotWin==null)
		{
			plot.show();
		}
		else plotWin.drawPlot(plot);		
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
