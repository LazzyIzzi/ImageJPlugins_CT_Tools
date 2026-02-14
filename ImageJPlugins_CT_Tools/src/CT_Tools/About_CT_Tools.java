package CT_Tools;

import java.awt.Color;
import java.awt.Font;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_CT_Tools implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		String dfiTxt = "CT_Tools is a suite of plugins for routing X-ray calculations\r\n\r\n"
				+"Calculators\r\n\r\n"
				+ "Linear Attenuation - Calculates MuLin using formula, density, and keV\n"
				+ "Spectrum Plotter - Plots MuMass NIST data for a formula vs keV\n"
				+ "Lookup MuLin - returns keV(s) given a MuLin, formula, density\n"
				+ "Lookup Ratio - returns keV(s) given a pair of  MuLin, formula, density\r\n\r\n"
				+ "Workflow* (sequence of steps for model-based linearization of a beam hardened CT slice)\r\n\r\n"
				+ "1. Scanner Setup - Optimise scan time, beam-hardening for a given source, filter, and detector\n"
				+ "2. Material Tagger** - builds a \"TagImage\" model by segmenting the CT slice into known components\n"
				+ "3. Tag Image To Parallel Brems Sinogram - CT scans the model using source, filter, and detector settings\n"
				+ "4. Reconstruct - uses DFI to reconstruct the output of step 3\n"
				+ "5. Linearization Fitter - finds beam-hardening solutions by comparing the image to its model reconstruction at selected energies\n"
				+ "6. Apply Linearization - corrects the image sinogram using the selected solution\n"
				+ "7. Reconstruct Linearized - reconstructs the corrected sinogram\r\n\r\n"
				+ "*CT slices must be in attenuation units of CM-1 and pixel size in CM\n"
				+ "**For severely beam-hardened CT slices, re-project the CT slice using Projectors->\"MuLin Image To Parallel_Sinogram\"\n"
				+ " followed by DFI JTransform's BH corrector to suppress the cupping artifact\r\n\r\n"
				+ "Tools\r\n\r\n"
				+ "Materials Editor - edits the list of material tags,names,formulas and densities used for attenuation calculations\n"
				+ "Materials Editor Excel - edits the materials list using Excel of other .csv file editor\n"
				+ "Tag Image To Fan Brems Sinogram - Fan-beam CT scans the model using source, filter, and detector settings\n"
				+ "Tag Image To Parallel Brems Sinogram - same as Workflow item 3\n"
				+ "Tag Image To MuLin - converts a tag image the linear attenuation at the selected keV\n"
				+ "Attenuation Error - creates a difference image from the model slice and corrected slice at the selected keV\n"
				+ "Attenuation To Effective Energy - computes an image of the effective energy of each component in the corrected slice\n "
				+ "Reconstructors\r\n\r\n"
				+ "DFI JTransforms - reconstructs parallel beam sinograms using direct Fourier inversion";
		

			//String license = new JTransformsUtilsDFI().getJTransformsLicenseDFI();
		GenericDialog gd = new GenericDialog("About CT Tools");
		gd.addMessage(dfiTxt, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
	

}
