package CT_Tools;

//import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import ij.IJ;
//import ij.ImagePlus;
import ij.plugin.PlugIn;

public class ResourceReader_Test implements PlugIn {

	public ResourceReader_Test() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(String arg) {

		ResourceReader dr = new ResourceReader();
		String str;
		str = dr.readTextFile("DefaultMaterials.csv");
		IJ.log(str);


		String[] docNames = dr.getDocumentList();
		for (int i = 0; i < docNames.length; i++) {
			IJ.log(docNames[i]);
		}

		String[] imgNames = dr.getImageList();
		for (int i = 0; i < imgNames.length; i++) {
			IJ.log(imgNames[i]);
		}
		
		IJ.open("https://lazzyizzi.github.io/ExampleImages/DryBereaSandstone_512_slice.tif");
		
			try {
				URL url = new URL("\"https://lazzyizzi.github.io/ExampleImages");
				URLConnection urlc = url.openConnection();
				Object content = urlc.getContent();
				IJ.log(content.getClass().toString());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
