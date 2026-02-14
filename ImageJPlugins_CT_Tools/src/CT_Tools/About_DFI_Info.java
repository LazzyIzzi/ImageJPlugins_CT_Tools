package CT_Tools;

import java.awt.Color;
import java.awt.Font;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_DFI_Info implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		String license = new JTransformsUtilsDFI().getJTransformsLicenseDFI();
		GenericDialog gd = new GenericDialog("JTransforms License");
		gd.addMessage(license, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
}
