package CT_Tools;

import java.awt.Color;
import java.awt.Font;

import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_Scanner_Setup implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		

		String txt = "Scanner Setup is a simplified 1D model of a conventional X-ray CT scanner.\n"
				+ "It helps you setup the X-ray source, pre-filter, and detector to obtain optimal\n"
				+ "reconstructed images. A poor setup can result in excessive scan times and\n"
				+ "noisy images with significant artifacts and incorrect attenuations\r\n\r\n"
				+ "Components:\n"
				+ "Source - uses the Kramers equation to model the continuum spectrum of a conventional\n"
				+ "         X-ray source. Characteristic emissions and source geometry are neglected.\n"
				+ "Filter - is downstream of the source and is used to reduce the low energy portion\n"
				+ "         of the source spectrum.  It pre-hardens the spectrum making it more monochromatic.\n"
				+ "         Pre-hardened sources produce more consistent specimen attenuation.\n"
				+ "Specimen - is downstream of the filter and absorbs X-rays in proportion to its composition\n"
				+ "         and thickness.  It also further hardens the X-ray spectrum.\n"
				+ "Detector - measures the transmitted X-ray intensity. It is modeled by the X-ray absorption\n"
				+ "         of the detector composition and density.\r\n\r\n"
				+  "Using Scanner Setup\n"
				+ "An optimal \"Attenuation vs Thickness\" plot should be approximately linear.\n"
				+ "The \"X-ray Spectra\" plot shows the x-ray intensity spectral distribution at key points along the\n"
				+ "source to detector path. The \"Filtered Detected Io\" and \"Sample Detected I\" distributions\n"
				+ "should have similar shapes and extents.\n"
				+ "The results window reports setup metrics.\n"
				+ "Sample absorbance \"S Tau\" should be about 2 or slightly less. Increasing KV, filter \n"
				+ "thickness, or filter atomic number usually reduces Tau.\n"
				+ "Beam Hardening \"BH%\" should be less than 20 for correctable beam hardening artifact.\n"
				+ "\"Photon Use%\" is the fraction of filtered x-rays that reach the detector. Low values\n"
				+ "indicate long exposure time for actual measurements.\r\n\r\n"
				+"Important Notes\n"
				+ "1. The X-ray source is noiseless. Increasing the source current will increase the output intensity\n"
				+ "   but it is always a 64 bit floating point number. \"KV\" is the controlling setting, \"Target\"\n"
				+ "   and \"ma\" have no effect but are included for future use.\n"
				+ "2. The detector is also noiseless but is sensitive to the incident X-ray energy. Thin\n"
				+ "   low atomic number detectors are less sensitive to high energy X-rays. \n"
				+ "3. Only total attenuation is measured. X-rays arising from scatter, fluorescense or other processes\n"
				+ "   are neglected.\n"
				+ "4. Scanner Setup settings can be imported into TagImage projectors.\r\n\r\n"
				+ "Disclaimer\n"
				+ "Scanner Setup is a manual optimization of experimental parameters. It is intended to\n"
				+ "provide guidance for setting up a laboratory CT scanner. Real scanners vary in \n"
				+ "design and noise characteristics. Scanner Setup is not intended for medical use";
		
		//GenericDialog gd = new GenericDialog("About Scanner Setup");
		GenericDialog gd = GUI.newNonBlockingDialog("About Scanner Setup");
		gd.addMessage(txt, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
}
