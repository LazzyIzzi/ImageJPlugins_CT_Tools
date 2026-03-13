package CT_Tools;

import java.awt.Color;
import java.awt.Font;

import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

public class About_Tag_Tools implements PlugIn {
	@Override
	public void run(String arg) {
		final Color myColor = new Color(240, 230, 190);// slightly darker than buff
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		String dfiTxt = "Many of the CT_Tools plugins require inputting chemical formulas in an X:1:Y:1 format\n"
				+ "This can be tedious and repetitive. To make life easier, CT_Tools stores a list of materials\n"
				+ "in DefaultMaterials.csv in the ImageJ~plugins/DialogData folder. CT_Tools creates this file\n"
				+ "with data for all elements and a few materials when first run, and loads it when needed.\n"
				+ "The list contains a \"Tag\" , a material's name, formula and density.\r\n\r\n"
				+ "Tag Tools\n"
				+ "Materials Editor - edits the list of material tags,names,formulas and densities used for attenuation calculations\n"
				+ "Materials Editor Excel - edits the materials list using Excel of other .csv file editor\n"
				+ "Tag Image To MuLin - creates a MuLin image from a TagImage at the selected keV\r\n\r\n"
				+ "Notes\n"
				+ "* Do NOT delete any basic elemental data, bad things will happen.\n"
				+ "* Materials Editor checks your changes for correctness.\n"
				+ "* Materials Editor Excel opens the default .csv file handler and does no input checking so be careful.\n"
				+ "* If DefaultMaterials.csv is corrupted, delete it and run a plugin like \"Linear Attenuation\" to\n"
				+ "  recreate DefaultMaterials.csv\n"
				+ "* Keep a backup if you have made extensive changes.";
		//GenericDialog gd = new GenericDialog("About CT Tools");
		GenericDialog gd = GUI.newNonBlockingDialog("About Tag Tools");
		gd.addMessage(dfiTxt, myFont);
		gd.setBackground(myColor);
		gd.setIconImage(new ResourceReader().readImageFile("LazzyIzzi-32.png"));
		gd.hideCancelButton();
		gd.showDialog();
	}
	

}
