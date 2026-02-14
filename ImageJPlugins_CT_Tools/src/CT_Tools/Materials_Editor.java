package CT_Tools;

import ij.IJ;
import ij.plugin.PlugIn;
import tagTools.TagListEditor;


public class Materials_Editor implements PlugIn {
	
	//The TagListEditor has no ImageJ dependencies

	@Override
	public void run(String arg)
	{
		String dir = IJ.getDirectory("plugins");
		String fileName = "DefaultMaterials.csv";
		String filePath = dir + "DialogData\\" + fileName;		
		//IJ.log(filePath);
		TagListEditor tle = new TagListEditor();
		tle.editMaterialsList(filePath);
	}
}
			



