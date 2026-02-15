package CT_Tools;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Show_Sample_Image implements PlugIn {

	public Show_Sample_Image() {
		// TODO Auto-generated constructor stub
	}
	
	final Color myColor = new Color(240,230,190,255);//slightly darker than buff
	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);

	@Override
	public void run(String arg) {
		String[] files = { "Al_CastingWithIronAndBrassPinsTagImage-512.tif", "DryBereaSandstone_512_slice.tif",
				"Shepp-Logan Phantom256.tif" };
		
        try {
            @SuppressWarnings("deprecation")
			URL url = new URL("https://github.com/");
            URLConnection connection = url.openConnection();
            connection.connect();
        } catch (IOException e) {
            IJ.error("This plugin requires an internet connection");
            return;
        }
		

		GenericDialog gd = new GenericDialog("Read On-Line Example Image");
		//gd.addMessage("Internet connection required",myFont);
		gd.addChoice("Image:", files, files[0]);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.showDialog();

		if (gd.wasOKed()) {
			IJ.open("https://lazzyizzi.github.io/ExampleImages/" + gd.getNextChoice());

		}
	}		


//	@Override
//	public void run(String arg) {
//		final Color myColor = new Color(240,230,190);//slightly darker than buff
//		ResourceReader dr = new ResourceReader();
//		String[] imgNames=dr.getImageList();
//		//ImagePlus iconImp = dr.readImagePlusFile("LazzyIzzi-32.png");
//		if(imgNames == null) {
//			IJ.error("There are no sample images");
//			return;			
//		}
//		
//		GenericDialog gd = new GenericDialog("Show Sample Image");
//		gd.addChoice("Select an Image", imgNames, imgNames[0]);
//		gd.setBackground(myColor);
//		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
//		gd.showDialog();
//		
//		if(gd.wasOKed()) {
//			String fileName = gd.getNextChoice();
//			ResourceReader r = new ResourceReader();
//			ImagePlus imp = r.readImagePlusFile(fileName);
//			imp.show();			
//		}				
//	}
}
