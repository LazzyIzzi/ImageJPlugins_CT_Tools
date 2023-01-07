package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import java.util.Enumeration;
import java.util.Properties;

public class Show_Image_Properties implements PlugIn
{
	ImagePlus imp;
	@Override
	public void run(String arg)
	{
		imp = IJ.getImage();
		ResultsTable rt = ResultsTable.getResultsTable("Image Properties");
		if(rt==null) rt = new ResultsTable();
		
		rt.setPrecision(3);
		rt.incrementCounter();
		rt.addValue("Name", imp.getTitle());
		
		Properties props = imp.getImageProperties();		
		Enumeration<Object> keys = props.keys();
		Enumeration<Object> elements = props.elements();
		
		while(keys.hasMoreElements())
		{
			rt.addValue((String)keys.nextElement(), (String)elements.nextElement());
		}		
		rt.show("Image Properties");
	}
}
