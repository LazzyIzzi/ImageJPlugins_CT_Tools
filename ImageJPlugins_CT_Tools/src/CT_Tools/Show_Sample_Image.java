package CT_Tools;

import java.awt.Color;
import java.awt.image.BufferedImage;

import DocumentReader.DocumentReader;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Show_Sample_Image implements PlugIn {

	public Show_Sample_Image() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(String arg) {
		final Color myColor = new Color(240,230,190);//slightly darker than buff
		ResourceReader dr = new ResourceReader();
		String[] imgNames=dr.getImageList();
		//ImagePlus iconImp = dr.readImagePlusFile("LazzyIzzi-32.png");
		if(imgNames == null) {
			IJ.error("There are no sample images");
			return;			
		}
		
		GenericDialog gd = new GenericDialog("Show Sample Image");
		gd.addChoice("Select an Image", imgNames, imgNames[0]);
		gd.setBackground(myColor);
		//gd.setIconImage(iconImp.getImage());
		gd.showDialog();
		
		if(gd.wasOKed()) {
			String fileName = gd.getNextChoice();
			ResourceReader r = new ResourceReader();
			ImagePlus imp = r.readImagePlusFile(fileName);
			imp.show();			
		}				
	}
}
