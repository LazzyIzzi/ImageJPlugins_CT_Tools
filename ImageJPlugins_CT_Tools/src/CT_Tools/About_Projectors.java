package CT_Tools;

import java.awt.Color;
import java.awt.Font;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_Projectors implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		String txt = "This set of four individual quantitative plugins vary from simple monochromatic\n"
				+ " parallel projection to polychromatic fan beam projection.\n"
				+ " All operate on single images or image stacks.\r\n"
				+ "\r\n"
				+ "MuLinSlice_To_Parallel_Sinogram \n"
				+ "   Creates a monochromatic parallel beam sinogram from a 2D linear attenuation image.\r\n"
				+ "MuLinSlice_To_Fan_Sinogram \n"
				+ "   Creates a monochromatic fan beam sinogram from a 2D linear attenuation image.\r\n"
				+ "TagSlice_To_Parallel_Sinogram \n"
				+ "   Creates a bremsstrahlung parallel beam sinogram from a 2D \"tag\" image.\r\n"
				+ "TagSlice_To_Fan_Sinogram\n"
				+ "   Creates a bremsstrahlung fan beam sinogram from a 2D \"tag\" image.\r\n\r\n"
				+ "General\r\n"
				+ "1. The input image must be 32-bit. The plugin will not load otherwise.\r\n"
				+ "2. Input image pixel width and height must be equal and in \"cm\" units\n"
				+ "    Rectangular images will cause the Pad Option to default to \"Circumscribed\".\r\n"
				+ "3. PolyChromatic CTscans require \"Tagged\" images.\r\n"
				+ "4. The suggested views are the minimum required for correct sampling.\n"
				+ "    Lower views will reduce the resolution of slices reconstructed from the sinogram.\n"
				+ "    Higher views will not improve S/N in slices reconstructed using the \"DFI_JTransforms\" plugin.\r\n"
				+ "5. Fan beam plugins calculate the correct detector size and source-to sample distance from\n"
				+ "    the source-to-detector and magnification and display it FYI in the dialog.\r\n"
				+ "6. TagSlice projectors can import values from Scanner Setup.";
		//GenericDialog gd = new GenericDialog("About X-ray Calculators");
		GenericDialog gd = GUI.newNonBlockingDialog("About Projectors");
		gd.addMessage(txt, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
	

}
