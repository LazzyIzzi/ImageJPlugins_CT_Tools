package CT_Tools;

import ij.IJ;
import ij.Macro;
import ij.plugin.PlugIn;
import jhd.TagTools.*;

import java.io.File;

import javax.swing.JFileChooser;

public class Materials_Editor implements PlugIn {

	@Override
	public void run(String arg)
	{
		String  filePath = Macro.getOptions();
		if(filePath==null)
		{
			String dir = IJ.getDirectory("plugins");
			filePath = dir + "DialogData\\DefaultMaterials.csv";
			File file = new File(filePath);

			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setSelectedFile(file);

			int returnVal = fc.showOpenDialog(fc);

			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				filePath = fc.getSelectedFile().getPath();
			}
			else return;
		}
		
		MatlListTools mlt = new MatlListTools();
		MatlListTools.TagSet myTags;
		myTags = mlt.loadTagFile(filePath);
		if(myTags!=null)
		{
			int last = filePath.lastIndexOf('\\')+1;
			String fileName =filePath.substring(last);
			String fileDir = filePath.substring(0, last);
			mlt.editTagFile(fileName, myTags);
			File file = new File(fileDir+"TagWriter.txt");
			file.delete();
		}
		else
		{
			IJ.error(filePath + " Not Found");
		}
	}
	//
	////		String dir = IJ.getDirectory("plugins");
	////		 filePath = dir + "DialogData\\DefaultMaterials.csv";
	//		File file = new File(filePath);
	//
	//		JFileChooser fc = new JFileChooser();
	//		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	//		fc.setSelectedFile(file);
	//
	//		int returnVal = fc.showOpenDialog(fc);
	//
	//		if(returnVal == JFileChooser.APPROVE_OPTION)
	//		{
	//			filePath = fc.getSelectedFile().getPath();
	//			myTags = mlt.loadTagFile(filePath);
	//			int last = filePath.lastIndexOf('\\')+1;
	//			String fileName =filePath.substring(last);
	//			mlt.editTagFile(fileName, myTags);
	//		}
}
			



