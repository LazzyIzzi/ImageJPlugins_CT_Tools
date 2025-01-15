package CT_Tools;

import ij.plugin.PlugIn;
import ij.text.TextWindow;

public class About_DFI_JTransforms implements PlugIn {
	@Override
	public void run(String arg) {		
		FFTutils ftu = new FFTutils();
		new TextWindow("JTransforms License", ftu.getJTransformsLicense(), 800, 700);		
	}

}
