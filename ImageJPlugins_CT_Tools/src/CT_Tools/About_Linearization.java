package CT_Tools;

import java.awt.Color;
import java.awt.Font;

import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_Linearization implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		

		String txt = "Definition\n"
				+ "Linearization is a method for correcting common beam hardening artifacts\n"
				+ "such as cupping and \"interossal\" opacity distortion. These occur when the\n"
				+ "specimen \"hardens\" the incident X-ray spectrum by preferentially absorbing\n"
				+ "the low energy portion of the X-ray beam as it passes through.\r\n"
				+ "Thicker or more dense regions of the specimen become less absorbing because they are\n"
				+ "sampled by a higher average energy X-ray beam. Beam pre-filtering is usually used\n"
				+ "to minimize beam hardening but significantly reduces incident x-ray intensity.\r\n\r\n"
				+ "Simple Linearization\n"
				+ "\"DFI JTransforms\" has a \"beam hardening\" slider that applies a weighted second order correction\n"
				+ "to the observed attenuations, corrected=(1-weight)*observed + weight*observed^2.\r\n"
				+ "This arbitrary weighting can substantially supress cupping but can produce incorrect attenuations.\r\n\r\n"
				+ "Model Based Linearization\n"
				+ "The Linearization plugins are a process designed to help optimize the accuracy of\n"
				+ "reconstructed images.\n"
				+ "The input image is a beam hardened reconstructed slice.*\n"
				+ "   1.Use \"MuLin Image to Parallel Sinogram\" to reproject the input image.\n"
				+ "   2.Use \"Material Tagger\" to segment the input slice into a materials \"TagImage\".\n"
				+ "      2a.If the input image is too beam hardened to segment, use DFI_JTransforms to\n"
				+ "         reconstruct the sinogram from step 1 using the 2nd order correction to\n"
				+ "         qualitatively correct the artifact. Go to step 2.\n"
				+ "   3.Use \"Linearization Fitter\" with the input slice and TagImages to obtain the\n"
				+ "     best linearization function and effective energy**\n"
				+ "   4.Use \"Apply Linearization\" to apply the function to the sinogram in step 1\n"
				+ "   5.Use \"DFI_JTransforms\" to reconstruct the linearized sinogram.\r\n\r\n"
				+ "Notes\n"
				+ " *Beam hardened images can be created either by a laboratory scanner or by scanning\n"
				+ "	  any TagImage with \"TagImage To Parallel Brems Sinogram\" configured to produce\n"
				+ "  beam hardening artifacts, followed by DFI_JTransforms reconstruction.\n"
				+ "**Effective energy \"Eeff\" is the monochromatic x-ray energy that\n"
				+ "   will produce the same attenuation as that measured with a given\r\n"
				+ "   polychromatic x-ray energy distribution." ;
		//GenericDialog gd = new GenericDialog("About Linearization");
		GenericDialog gd = GUI.newNonBlockingDialog("About Linearization");
		gd.addMessage(txt, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
}
