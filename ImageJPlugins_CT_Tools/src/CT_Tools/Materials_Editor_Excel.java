package CT_Tools;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
//import javax.swing.JOptionPane;

import ij.IJ;
import ij.plugin.PlugIn;
import tagTools.TagListTools;

public class Materials_Editor_Excel implements PlugIn {

	public Materials_Editor_Excel() {
	}

	@Override
	public void run(String arg) {
		String dir = IJ.getDirectory("plugins");
		String path = dir + "DialogData\\DefaultMaterials.csv";
		Path thePath = Paths.get(path);

		// If DefaultMaterials.csv does not exist, create it
		if (Files.exists(thePath) == false) {
			TagListTools tlt = new TagListTools();
			tlt.readTagSetFile(path);
		}
		// ignore the returned tagSet and open the file with the
		// default .csv file editor
		try {
			Desktop.getDesktop().open(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
