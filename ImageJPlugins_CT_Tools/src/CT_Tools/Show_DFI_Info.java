package CT_Tools;

import java.awt.Color;
import java.awt.Font;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class Show_DFI_Info implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		DFIutils dfiu = new DFIutils();
		//JTransformsUtils jtu = new JTransformsUtils();
		String license = dfiu.getJTransformsLicense();
		GenericDialog gd = new GenericDialog("JTransforms License");
		gd.addMessage(license, myFont);
		gd.setBackground(myColor);
		gd.hideCancelButton();
		gd.showDialog();
	}
}
