package CT_Tools;

import java.awt.Color;
import java.awt.Font;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import jhd.ImageJAddins.GenericDialogAddin;
import ij.gui.GenericDialog;

public class Set_Image_ScaleFactor implements PlugIn {

	final Color myColor = new Color(240,230,190);//slightly darker than buff
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	@Override
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		
		GenericDialog gd = new GenericDialog("Set Scale Factor");
		gd.addMessage("Set/Edit " + imp.getTitle() + "'s ScaleFactor property."
				+ "\nEnter blank or non-number to remove.",myFont);
		double scaleFactor = getScaleFactorFromImageProperty(imp);
		
		gd.addStringField("ScaleFactor", Double.toString(scaleFactor));
		gd.setBackground(myColor);		
		gd.showDialog();
		
		if(gd.wasOKed())
		{
			String scaleFactorStr = gd.getNextString();
			if(isNumeric(scaleFactorStr))
			{
				scaleFactor = Double.parseDouble(scaleFactorStr);
				imp.setProp("ScaleFactor", scaleFactor);
				String	msg = imp.getTitle() +" ScaleFactor property set to " + scaleFactor + ".";
				if(IJ.isMacro()) IJ.log(msg);
				else
				{
					msg = imp.getTitle() + " ScaleFactor property set to " + scaleFactor + "."
							+ "\n use Image->ShowInfo or Ctrl-i to view.";
					IJ.showMessage(msg);
				}
			}
			else
			{
				imp.setProp("ScaleFactor", null);
				String msg = "ScaleFactor property removed from "+ imp.getTitle();
				if(IJ.isMacro()) IJ.log(msg);
				else IJ.showMessage(msg);
			}
		}
	}
	
	private double getScaleFactorFromImageProperty(ImagePlus imp)
	{
		double scaleFactor=1;
		String scaleFactorStr = imp.getProp("ScaleFactor");
		if(scaleFactorStr!=null) 
		{
			if(GenericDialogAddin.isNumeric(scaleFactorStr))
			{
				scaleFactor = Double.parseDouble(scaleFactorStr);
			}
		}
		return scaleFactor;
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
