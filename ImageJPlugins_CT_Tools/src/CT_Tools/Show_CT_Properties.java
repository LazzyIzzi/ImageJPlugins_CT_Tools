package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

public class Show_CT_Properties implements PlugIn
{
	final String[] projProps = {"Geometry","Source","Source KV","Source mA","Source Target","Filter","Filter(cm)","Min keV",
			"Bins","Source To Detector","Magnification","Detector","Detector(cm)","Detector(gm/cc)","ScaleFactor"};
	final String[] reconProps = {"Rotation Axis","Axis Shift","Cartesian Filter","Cartesian Cutoff",
			"Extension Shape","Fix Rings","BeamHardenWeight"}; // Not used "Fix Rays","SumRule"
	final String[] linearizationProps = {"Linearization","LinearizationA","LinearizationB","LinearizationC",
			"LinearizationD","LinearizationE","LinearizationF","LinearizationG","LinearizationR","LinearizationEff"};

	ImagePlus imp;
	@Override
	public void run(String arg)
	{
		imp = IJ.getImage();
		if(imp.getProp(projProps[0])!=null) CT_PropertiesToTable(projProps, "Projection Properties");
		if(imp.getProp(reconProps[0])!=null) CT_PropertiesToTable(reconProps, "CT_Recon Properties");
		if(imp.getProp(linearizationProps[0])!=null) CT_PropertiesToTable(linearizationProps, "Linearization Properties");
	}

	//*******************************************************************************

	private void CT_PropertiesToTable(String[] propList, String propTitle)
	{
		ResultsTable propRT = ResultsTable.getResultsTable(propTitle);
		if(propRT==null) propRT = new ResultsTable();

		propRT.setPrecision(4);
		propRT.incrementCounter();
		propRT.addValue("Name", imp.getTitle());
		for(String propLabel : propList)
		{
			String propValStr = imp.getProp(propLabel);
			if(propValStr==null) propRT.addValue(propLabel, "NA");
			else if(isNumeric(propValStr)) propRT.addValue(propLabel, Double.parseDouble(propValStr));
			else propRT.addValue(propLabel, propValStr);
		}
		propRT.show(propTitle);
	}

	//*******************************************************************************

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
