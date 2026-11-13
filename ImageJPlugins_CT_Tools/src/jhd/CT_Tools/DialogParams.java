package jhd.CT_Tools;

/**A class to hold settings from CT_Tools Dialogs
 * 
 */
public class DialogParams {

	/**BeamHardenParams is a serializable class used to store and share X-ray imaging parameters 
	 * used by beam hardening plugins such as Scanner_Setup and the Bremsstrahlung projectors.
	 * Stored parameters expedite both the input and the sharing of parameters.
	 */
		public static class BeamHardenParams implements java.io.Serializable
		{
			private static final long serialVersionUID = 1L;
			/**The conventional X-ray source anode (target) material*/
			public String target;;
			/**The X-ray source accelerating potential, the upper limit on the X-ray energy*/
			public double kv;
			/**The X-ray source beam current*/
			public double ma;
			/**Divide the source spectrum into nBins discrete energies*/
			public double kvInc;
			/**The lower limit on the X-ray energy*/
			public double kvMin;
			//Filter
			/**The filter material, typically Al, Cu, Sn, Mo*/
			public String filter;

			/**The filter thickness*/
			public double filterCM;
			/**The filter density*/
			public double filterGmPerCC;
			/**The image pixel size in CM. the code works in CGS units*/
			public double pixSizeCM;

			/**compound formula. Must be in Atom1:Count1:Atom2:Count2 format, for example Ca:1:C:1:O:3 for CaCO3 Calcite*/
			public String matlFormula;

			/**compound density in gm/cc*/
			public double matlGmPerCC;
			/**matl thickness(cm)*/
			public double matlCM;
			//Detector
			/**The detector scintillator  formula, Must be in Atom1:Count1:Atom2:Count2 format, For example Cs:1:I:1 for CsI Cesium Iodide*/
			public String detFormula;

			/**The detector scintillator screen or detector element thickness in CM*/
			public double detCM;
			/**The detector scintillator screen or detector element density in gm/cc*/
			public double detGmPerCC;
			public String plotChoice;
			
			/**Constructor for this serializable parameter block*/ 
			public BeamHardenParams(){}
		}
	}


