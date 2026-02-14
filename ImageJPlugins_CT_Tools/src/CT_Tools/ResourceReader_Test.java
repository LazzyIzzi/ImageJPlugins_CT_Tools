package CT_Tools;

import java.awt.image.BufferedImage;

import ij.IJ;
import ij.ImagePlus;
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

//		ImagePlus imp = dr.readImagePlusFile("LazzyIzzi-32.png");
//
//		//ImagePlus imp = new ImagePlus("LazzyIzzi-32.png", bi);
//
//		imp.show();

		String[] docNames = dr.getDocumentList();
		for (int i = 0; i < docNames.length; i++) {
			IJ.log(docNames[i]);
		}

		String[] imgNames = dr.getImageList();
		for (int i = 0; i < imgNames.length; i++) {
			IJ.log(imgNames[i]);
		}
		
		IJ.open("https://lazzyizzi.github.io/CT_ReconImages/CtRecon/CT_Recon_Montage.png");
	}

}
