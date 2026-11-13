package jhd.CT_Tools;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
//import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Show_Sample_Image implements PlugIn {

	final Color myColor = new Color(240, 230, 190, 255);// slightly darker than buff

	Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
	public Show_Sample_Image() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(String arg) {
		String[] files = { "Al_CastingWithIronAndBrassPinsTagImage-512.tif", "DryBereaSandstone_512_slice.tif",
				"DryBereaSandstone_512_4TagImage_slice.tif", "Shepp-Logan Phantom256.tif",
				"Shepp-Logan Phantom256_TagImage.tif" };

		try {
			URL url = new URL("https://github.com/");
			URLConnection connection = url.openConnection();
			connection.connect();
		} catch (IOException e) {
			IJ.error("This plugin requires an internet connection");
			return;
		}

		GenericDialog gd = new GenericDialog("Read On-Line Example Image"); // gd.addMessage("Internet connection
																			// required",myFont);
		gd.addChoice("Image:", files, files[0]);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.showDialog();

		if (gd.wasOKed()) {
			IJ.open("https://lazzyizzi.github.io/ExampleImages/" + gd.getNextChoice());

		}
	}
}
