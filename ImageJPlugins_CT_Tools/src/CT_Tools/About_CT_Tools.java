package CT_Tools;

import java.awt.Color;
import java.awt.Font;

import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_CT_Tools implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		String dfiTxt = "CT_Tools is a suite of plugins for routine X-ray calculations\r\n\r\n"
				+ "Definitions\n"
				+ "MuLin = linear attenuation coefficient of a material.(cm-1)\n"
				+ "MuMass = mass attenuation coefficient of a chemicla formula (cm^2/gm).\n"
				+ "MuLin = MuMass*(material gmPerCC)\n"
				+ "TagImage - a map of the materials in a CT slice using a unique number tag for each material.\n"
				+ "CT slices must be in MuLin with centimeter pixel units\n"
				+ "Effective energy \"Eeff\" is the monochromatic x-ray energy that\n"
				+ "	       will produce the same attenuation as that measured with a given\n"
				+ "	       polychromatic x-ray energy distribution.\r\n\r\n"
				
				+"Calculators\n"
				+ "Linear Attenuation - calculates MuLin using formula, density, and keV\n"
				+ "Spectrum Plotter - plots MuMass NIST data for a formula vs keV\n"
				+ "Lookup keV - returns keV(s) given a MuLin, formula, density\n"
				+ "Lookup keV Ratio - returns keV(s) given a contrasting pair of MuLin, formula, and density\n"
				+ "Scanner Setup - select the source, filter, and detector of a real or virtual CT scanner to optimize\n"
				+ "                scan efficiency, signal-to-noise, and beam-hardening\r\n\r\n"

				+ "Linearization\n"
				+ "a sequence of steps for model-based linearization of a beam hardened CT slice.\n"
				+ "that identifies the effective energy that minimizes MuLin errors.\n"
				+ "1. Re-project - creates a sinogram of the input slice.\n"
				+ "2. Material Tagger - builds a TagImage model by segmenting the CT slice into known materials\n"
				+ "3. Linearization Fitter - finds beam-hardening solutions by comparing the image to its TagImage at selected energies\n"
				+ "4. Apply Linearization - corrects the image sinogram from step 1 using the selected solution\n"
				+ "5. Reconstruct - reconstructs the corrected sinogram\r\n\r\n"	

				+ "Tag Tools\n"
				+ "Materials Editor - edits the list of material tags,names,formulas and densities used for attenuation calculations\n"
				+ "Materials Editor Excel - edits the materials list using Excel of other .csv file editor\n"
				+ "Tag Image To MuLin - creates a MuLin image from a TagImage at the selected keV\r\n\r\n"

				+ "Scanners\n"
				+ "MuLin Image To Parallel Sinogram - creates a parallel-beam sinogram of a MuLin image.\n"
				+ "MuLin Image To Fan Sinogram - creates a fan-beam sinogram of a MuLin image.\n"
				+ "Tag Image To Parallel Brems Sinogram - creates a parallel-beam sinogram using source, filter, and detector settings\n"
				+ "Tag Image To Fan Brems Sinogram - creates a fan-beam sinogram using source, filter, and detector settings\r\n\r\n"

				+ "Reconstructors\n"
				+ "DFI JTransforms - reconstructs parallel beam sinograms using direct Fourier inversion.\n"
				+ "   Plugin can correct for axis error, cupping artifact, and out-of-frame data loss.\r\n\r\n"

				+ "Experimental\n"
				+ "Attenuation Error - creates a difference image of a CT slice and its TagImage at the selected keV.\n"
				+ "Attenuation To Effective Energy - computes an image of the effective energy of each material in the corrected slice.";
		//GenericDialog gd = new GenericDialog("About CT Tools");
		GenericDialog gd = GUI.newNonBlockingDialog("About CT Tools");
		gd.addMessage(dfiTxt, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
	

}
