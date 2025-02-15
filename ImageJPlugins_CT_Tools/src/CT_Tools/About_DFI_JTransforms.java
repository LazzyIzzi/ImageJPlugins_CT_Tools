package CT_Tools;

//import DFI_JTransforms_Beta2.JTransformsUtils;
import ij.plugin.PlugIn;
import ij.text.TextWindow;

public class About_DFI_JTransforms implements PlugIn {
	@Override
	public void run(String arg) {		
		//FFTutils ftu = new FFTutils();
		JTransformsUtils jtu =  new JTransformsUtils();
		//new TextWindow("JTransforms License", jtu.getJTransformsLicense(), 800, 700);		
	}

}
