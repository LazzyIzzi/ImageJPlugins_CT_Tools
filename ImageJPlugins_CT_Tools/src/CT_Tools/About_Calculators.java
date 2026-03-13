package CT_Tools;

import java.awt.Color;
import java.awt.Font;
import DocumentReader.*;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_Calculators implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		String txt = "X-ray Calculators\r\n\r\n"
				+ "Linear Attenuation - Calculates Linear attenuation coefficients \"MuLin\"\n"
				+ "     from the chemical formula and density at a selected X-ray energy \"keV\".\n"
				+ "     Enter a formula in the X:1:Y:1 format or select a formula from the \"Material List\"\n"
				+ "     menu. Use the \"Material List Filter\" to search the Materials List.\r\n\r\n"
				+ "Spectrum Plotter - Plots NIST mass attenuation \"MuMass\" data for a formula vs keV\n"
				+ "     Enter a formula and an energy range, select plot options and click \"Update\".\n"
				+ "     MuMass = MuLin/density."
				+ "     NIST data is used with permission.\r\n\r\n"
				+ "Lookup keV - returns one or more X-ray energies that would produce the observed\n"
				+ "     linear attenuation \"MuLin\" from a material formula and density.\r\n\r\n"
				+ "Lookup keV Ratio - returns one or more X-ray energies that would produce the\n"
				+ "     relative linear attenuations of two materials.  A warning will appear when the\n"
				+ "     MuLin contrast becomes unreliable, typically at high energies or with similar formulas\n"; 
		//GenericDialog gd = new GenericDialog("About X-ray Calculators");
		GenericDialog gd = GUI.newNonBlockingDialog("About X-ray Calculators");
		gd.addMessage(txt, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
	

}
