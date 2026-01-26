package tagTools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JOptionPane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

/**TagSet definition and assorted getters and setters*/
public class TagListTools {
	/**Default Constructor*/
	public TagListTools() {
	}
	/**Contains a TagSet column header and Column values*/
	public class TagData {
		public int matlTag;
		public String matlName;
		public String matlFormula;
		public double matlGmPerCC;

		// Default constructor
		public TagData() {
		}

		public TagData(int matlTag, String matlName, String matlFormula, double matlGmPerCC) {
			this.matlTag = matlTag;
			this.matlName = matlName;
			this.matlFormula = matlFormula;
			this.matlGmPerCC = matlGmPerCC;
		}
	};

	/**Contains a TagSet column header*/
	public class TagHdr {
		public String[] colHdr;

		// Default constructor
		public TagHdr() {
		};

		public TagHdr(String[] colHdr) {
			this.colHdr = colHdr;
		}

	}

	/**Contains a TagSet column data*/
	public class TagSet {
		public TagHdr tagHdr;
		public ArrayList<TagData> tagData;;

		// Default constructor
		public TagSet() {
		}

		public TagSet(TagHdr tagHdr, ArrayList<TagData> tagData) {
			this.tagHdr = tagHdr;
			this.tagData = tagData;
		}
	}


	/**Filters a TagSet by  Name
	 * @param tagSet     the tagSet to be filtered
	 * @param nameFilter return data with material names that begin with this string
	 * @return A TagSet of material names that begin with the nameFilter
	 *         string
	 */
	public TagSet filterTagData(TagSet tagSet, String nameFilter) {
		ArrayList<TagData> filteredTagData = new ArrayList<TagData>();
		TagHdr filteredTagHdr = new TagHdr(tagSet.tagHdr.colHdr);
		String filterUC = nameFilter.toUpperCase();
		String matlNameUC;
		for (TagData tagData : tagSet.tagData) {
			matlNameUC = tagData.matlName.toUpperCase();
			if (matlNameUC.startsWith(filterUC, 0)) {
				filteredTagData
						.add(new TagData(tagData.matlTag, tagData.matlName, tagData.matlFormula, tagData.matlGmPerCC));
			}
		}
		TagSet filteredTagSet = new TagSet(filteredTagHdr, filteredTagData);

		return filteredTagSet;
	}

	/**Gets the formula for a particular tag from a TagSet
	 * @param theTag
	 * @param tagSet
	 * @return the formula
	 */
	public String getTagMatlFormula(int theTag, TagSet tagSet) {
		String formula = null;
		String[] formulas = getTagSetMatlFormulasAsArray(tagSet);
		int[] tags = getTagSetMatlTagAsArray(tagSet);
		for (int i = 0; i < tags.length; i++) {
			if (tags[i] == theTag) {
				formula = formulas[i];
				break;
			}
		}
		return formula;
	}

	/**Gets the density for a particular tag from a TagSet
	 * @param theTag
	 * @param tagSet
	 * @return the density
	 */
	public double getTagMatlGmPerCC(int theTag, TagSet tagSet) {
		double gmPerCC = -1;
		double[] densities = getTagSetMatlGmPerccAsArray(tagSet);
		int[] tags = getTagSetMatlTagAsArray(tagSet);
		for (int i = 0; i < tags.length; i++) {
			if (tags[i] == theTag) {
				gmPerCC = densities[i];
				break;
			}
		}
		return gmPerCC;
	}

	/**Gets the name for a particular tag from a TagSet
	 * @param theTag
	 * @param tagSet
	 * @return the name
	 */
	public String getTagMatlName(int theTag, TagSet tagSet) {
		String name = null;
		String[] names = getTagSetMatlNamesAsArray(tagSet);
		int[] tags = getTagSetMatlTagAsArray(tagSet);
		for (int i = 0; i < tags.length; i++) {
			if (tags[i] == theTag) {
				name = names[i];
				break;
			}
		}
		return name;
	}

	/**Converts the TagSet Formula column to an Array
	 * @param tagSet A materials List TagSet
	 * @return the TagSet Formulas as a String array
	 */
	public String[] getTagSetMatlFormulasAsArray(TagSet tagSet) {
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);
		String[] formulas = new String[tagSet.tagData.size()];
		for (int i = 0; i < tagData.length; i++) {
			formulas[i] = tagData[i].matlFormula;
		}
		return formulas;
	}

	/**Converts the TagSet Density column to an Array
	 * @param tagSet A materials List TagSet
	 * @return the TagSet Densities as a double array
	 */
	public double[] getTagSetMatlGmPerccAsArray(TagSet tagSet) {
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);
		double[] density = new double[tagSet.tagData.size()];
		for (int i = 0; i < tagData.length; i++) {
			density[i] = tagData[i].matlGmPerCC;
		}
		return density;
	}

	/**Converts the TagSet Names column to an Array
	 * @param tagSet
	 * @return the TagSet Material Names as a String array
	 */
	public String[] getTagSetMatlNamesAsArray(TagSet tagSet) {
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);
		String[] matlNames = new String[tagSet.tagData.size()];
		for (int i = 0; i < tagData.length; i++) {
			matlNames[i] = tagData[i].matlName;
		}
		return matlNames;
		}
	
	/**Converts the TagSet Tag column to an Array
	 * @param tagSet
	 * @return the TagSet Tag number as an int array
	 */
	public int[] getTagSetMatlTagAsArray(TagSet tagSet) {
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);
		int[] tags = new int[tagSet.tagData.size()];
		for (int i = 0; i < tagData.length; i++) {
			tags[i] = tagData[i].matlTag;
		}
		return tags;
	}

	/**Loads a TagSet from an existing DefaultMaterials.csv or
	 * creates and loads a new one.
	 * @return a TagSet or null if the method fails.
	 */
	public TagSet readTagSetFile(String path) {
		boolean fileOk = true;
		String matlStr;
		TagSet tagSet = null;
		Path thePath = Paths.get(path);

		if (Files.exists(thePath) == false) {
			try {
				Path parent = thePath.getParent();
				Files.createDirectories(parent);
				Files.createFile(thePath);
				byte[] bytes = null;
				
				//Get DefaultMaterials.csv from the tagTools Resources 
				ClassLoader cl = tagTools.TagListTools.class.getClassLoader();
				URL url = cl.getResource("tagTools/DefaultMaterials.csv");
				Path resPath = null;
				try {
					resPath = Paths.get(url.toURI());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				try {
					bytes = Files.readAllBytes(resPath);
				} catch (IOException e) {
					e.printStackTrace();
				}
				matlStr = new String(bytes);
				//brute force just in case the resource is compromised
//				matlStr = "Tag,Name,X:1:Y:2 Formula,gm/cc\n"
//						+ "0,EmptySpace,H,0\n"
//						+ "1,Hydrogen,H,1.0E-4\n"
//						+ "2,Helium,HE,2.0E-4\n"
//						+ "3,Lithium,LI,0.535\n"
//						+ "4,Beryllium,BE,1.848\n"
//						+ "5,Boron,B,2.46\n"
//						+ "6,Carbon,C,2.26\n"
//						+ "7,Nitrogen,N,0.0013\n"
//						+ "8,Oxygen,O,0.0014\n"
//						+ "9,Fluorine,F,0.0017\n"
//						+ "10,Neon,NE,9.0E-4\n"
//						+ "11,Sodium,NA,0.968\n"
//						+ "12,Magnesium,MG,1.738\n"
//						+ "13,Aluminum,AL,2.7\n"
//						+ "14,Silicon,SI,2.33\n"
//						+ "15,Phosphorus,P,1.823\n"
//						+ "16,Sulfur,S,1.96\n"
//						+ "17,Chlorine,CL,0.0032\n"
//						+ "18,Argon,AR,0.0018\n"
//						+ "19,Potassium,K,0.856\n"
//						+ "20,Calcium,CA,1.55\n"
//						+ "21,Scandium,SC,2.985\n"
//						+ "22,Titanium,TI,4.507\n"
//						+ "23,Vanadium,V,6.11\n"
//						+ "24,Chromium,CR,7.14\n"
//						+ "25,Manganese,MN,7.47\n"
//						+ "26,Iron,FE,7.874\n"
//						+ "27,Cobalt,CO,8.9\n"
//						+ "28,Nickel,NI,8.908\n"
//						+ "29,Copper,CU,8.92\n"
//						+ "30,Zinc,ZN,7.14\n"
//						+ "31,Gallium,GA,5.904\n"
//						+ "32,Germanium,GE,5.323\n"
//						+ "33,Arsenic,AS,5.727\n"
//						+ "34,Selenium,SE,4.819\n"
//						+ "35,Bromine,BR,3.12\n"
//						+ "36,Krypton,KR,0.0037\n"
//						+ "37,Rubidium,RB,1.532\n"
//						+ "38,Strontium,SR,2.63\n"
//						+ "39,Yttrium,Y,4.472\n"
//						+ "40,Zirconium,ZR,6.511\n"
//						+ "41,Niobium,NB,8.57\n"
//						+ "42,Molybdenum,MO,10.28\n"
//						+ "43,Technetium,TC,11.5\n"
//						+ "44,Ruthenuim,RU,12.37\n"
//						+ "45,Rhodium,RH,12.45\n"
//						+ "46,Palladium,PD,12.023\n"
//						+ "47,Silver,AG,10.49\n"
//						+ "48,Cadmium,CD,8.65\n"
//						+ "49,Indium,IN,7.31\n"
//						+ "50,Tin,SN,7.31\n"
//						+ "51,Antimony,SB,6.697\n"
//						+ "52,Telurium,TE,6.24\n"
//						+ "53,Iodine,I,4.94\n"
//						+ "54,Xenon,XE,0.0059\n"
//						+ "55,Cesium,CS,1.879\n"
//						+ "56,Barium,BA,3.51\n"
//						+ "57,Lanthanum,LA,6.146\n"
//						+ "58,Cerium,CE,6.689\n"
//						+ "59,Praseodymium,PR,6.64\n"
//						+ "60,Neodymium,ND,7.01\n"
//						+ "61,Promethium,PM,7.264\n"
//						+ "62,Samarium,SM,7.353\n"
//						+ "63,Europium,EU,5.244\n"
//						+ "64,Gadolinium,GD,7.901\n"
//						+ "65,Terbium,TB,8.219\n"
//						+ "66,Dysprosium,DY,8.551\n"
//						+ "67,Holmium,HO,8.795\n"
//						+ "68,Erbium,ER,9.066\n"
//						+ "69,Thulium,TM,9.321\n"
//						+ "70,Ytterbium,YB,6.57\n"
//						+ "71,Lutetium,LU,9.841\n"
//						+ "72,Hafnium,HF,13.31\n"
//						+ "73,Tantalum,TA,16.65\n"
//						+ "74,Tungsten,W,19.25\n"
//						+ "75,Rhenium,RE,21.02\n"
//						+ "76,Osmium,OS,22.59\n"
//						+ "77,Iridium,IR,22.56\n"
//						+ "78,Platinum,PT,21.09\n"
//						+ "79,Gold,AU,19.3\n"
//						+ "80,Mercury,HG,13.534\n"
//						+ "81,Thallium,TL,11.85\n"
//						+ "82,Lead,PB,11.34\n"
//						+ "83,Bismuth,BI,9.78\n"
//						+ "84,Polonium,PO,9.196\n"
//						+ "86,Radon,RN,0.0097\n"
//						+ "88,Radium,RA,5\n"
//						+ "89,Actinium,AC,10.07\n"
//						+ "90,Thorium,TH,11.724\n"
//						+ "91,Protactinium,PA,15.37\n"
//						+ "92,Uranium,U,19.05\n"
//						+ "93,Neptunium,NP,20.45\n"
//						+ "94,Plutonium,PU,19.816\n"
//						+ "96,Curium,CM,13.51\n"
//						+ "97,Berkelium,BK,14.78\n"
//						+ "98,Californium,CF,15.1\n"
//						+ "101,Albite,Na:1:Al:1:Si:3:O:8,2.62\n"
//						+ "102,Alumina,Al:2:O:3,3.7\n"
//						+ "103,Anatase,Ti:1:O:2,3.9\n"
//						+ "104,Ankerite,Fe:1:Ca:1:C:2:O:6,3\n"
//						+ "105,Anorthite,Ca:1:Si:2:Al:2:O:8,2.76\n"
//						+ "106,Anthracite,C:1,1.5\n"
//						+ "107,Apatite,Ca:5:P:3:O:12:Cl:0.33:F:0.33:O:0.33:H:0.33,3.2\n"
//						+ "108,Aragonite,Ca:1:C:1:O:3,2.95\n"
//						+ "109,Barite,Ba:1:S:1:O:4,4.5\n"
//						+ "110,Bromodecane,Br:1:C:10:H:21,1.07\n"
//						+ "111,Calcite,Ca:1:C:1:O:3,2.71\n"
//						+ "112,Chlorite,Fe:2:Mg:2:Al:4:Si:2:O:18:H:8,3\n"
//						+ "113,CsI,Cs:1:I:1,4.51\n"
//						+ "114,decane,C:10:H:22,0.73\n"
//						+ "115,di-iododecane,I:2:C:10:H:20,2.254\n"
//						+ "116,Dolomite,Ca:1:Mg:1:C:3:O:6,2.87\n"
//						+ "117,Galena,Pb:1:S:1,7.58\n"
//						+ "118,Hematite,Fe:1:O:1,5.26\n"
//						+ "119,Illite,K:1.5:Al:5.5:Si:6.5:O:24:H:4,2.8\n"
//						+ "120,iododecane,I:1:C:10:H:21,1.254\n"
//						+ "121,Kaolinite,Al:2:Si:2:O:9:H:4,2.6\n"
//						+ "122,KBr,K:1:Br:1,2.75\n"
//						+ "123,KCl,K:1:Cl:1,1.984\n"
//						+ "124,K-feldspar,K:1:Al:1:Si:3:O:8,2.56\n"
//						+ "125,KI,K:1:I:1,3.13\n"
//						+ "126,Lithium,Li:1,0.534\n"
//						+ "127,MagnesiumCarbonate,Mg:1:C:1:O:3,2.958\n"
//						+ "128,MagnesiumOxide,Mg:1:O:1,3.58\n"
//						+ "129,Muscovite,K:1:Al:3:Si:3:O:12:H:2,2.8\n"
//						+ "130,NaBr,Na:1:Br:1,3.203\n"
//						+ "131,NaCl,Na:1:Cl:1,2.165\n"
//						+ "132,NaI,Na:1:I:1,3.667\n"
//						+ "133,PE,C:1:H:2,0.95\n"
//						+ "134,PEEK,C:19:O:3:H:12,1.3\n"
//						+ "135,PotassiumCarbonate,K:2:C:1:O:3,2.428\n"
//						+ "136,PotassiumMonoxide,K:2:O:1,2.32\n"
//						+ "137,Pyrite,Fe:1:S:2,4.93\n"
//						+ "138,Quartz,Si:1:O:2,2.65\n"
//						+ "139,Siderite,Fe:1:C:1:O:3,3.96\n"
//						+ "140,SS,Fe:1:Cr:1,7.53\n"
//						+ "141,Talc,Mg:3:Si:4:O:12:H:2,2.75\n"
//						+ "142,Water,H:2:O:1,1\n"
//						+ "143,BGO,BI:4:GE:3:O:12,7.13\n"
//						+ "144,Gadox,GD:2:S:1:O:2,7.32\n"
//						+ "145,Cadmium Tungstate,CD:1:W:1:O:4,7.9\n"
//						+ "150,SheppLoganSkull,CA:9:P:6:O:8:H:2,0.4\n"
//						+ "151,SheppLoganSkull2,CA:9:P:6:O:8:H:2,2\n"
//						+ "152,SheppLoganDark,H:2:O:1,1\n"
//						+ "153,SheppLoganOverlap,H:2:O:1,1.01\n"
//						+ "154,SheppLoganMedium,H:2:O:1,1.02\n"
//						+ "155,SheppLoganLight,H:2:O:1,1.03\n"
//						+ "156,SheppLoganWhite,H:2:O:1,1.04";
				stringToFile(matlStr, path);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage());
				e.printStackTrace();
				fileOk = false;
			}
		}

		if (fileOk) {

			matlStr = fileToString(path);

			// Split materials at newline
			String[] rowArr = matlStr.split("\n");
			// copy the first four items into the tagSet header
			String[] hdrItems = rowArr[0].split(",");

			if (hdrItems[0].equals("Tag")) {

				try {
					TagHdr tagHdr = new TagHdr(hdrItems);
					ArrayList<TagData> tagData = new ArrayList<TagData>();
					// copy the remaining rows into the tagSet
					for (int i = 1; i < rowArr.length; i++) {
						String[] rowItems = rowArr[i].split(",");
						tagData.add(new TagData(Integer.parseInt(rowItems[0]), rowItems[1], rowItems[2],
								Double.parseDouble(rowItems[3])));
					}
					tagSet = new TagSet(tagHdr, tagData);
				} catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(null, e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return tagSet;
	}

	/**Changes the Formula associated with a Tag
	 * @param theTag change the formula of this tag
	 * @param newFormula
	 * @param tagSet change this tagSet
	 */
	public void setTagMatlFormula(int theTag, String newFormula, TagSet tagSet) {
		int index = getTagIndex(theTag,tagSet);		
		tagSet.tagData.get(index).matlFormula = newFormula;
	}

	/**Changes the Density associated with a Tag
	 * @param theTag change the formula of this tag
	 * @param gmPerCC
	 * @param tagSet change this tagSet
	 */
	public void setTagMatlGmPerCC(int theTag, double gmPerCC, TagSet tagSet) {
		int index = getTagIndex(theTag,tagSet);		
		tagSet.tagData.get(index).matlGmPerCC = gmPerCC;
	}

	/**Changes the Name associated with a Tag
	 * @param theTag change the Name for this tag
	 * @param newName
	 * @param tagSet change this tagSet
	 */
	public void setTagMatlName(int theTag, String newName, TagSet tagSet) {
		int index = getTagIndex(theTag,tagSet);		
		tagSet.tagData.get(index).matlName = newName;
	}
	
	/**Finds the location (row) of a tag in the list
	 * @param theTag 
	 * @param tagSet search this tagSet
	 * @return
	 */
	public int getTagIndex(int theTag, TagSet tagSet) {
		int index = -1;
		int[] tags = getTagSetMatlTagAsArray(tagSet);
		for (int i = 0; i < tags.length; i++) {
			if (tags[i] == theTag) {
				index = i;
				break;
			}
		}
		return index;
	}

	/**Writes a tagSet to ImageJ plugins/DialogData/DefaultMaterials.csv
	 * @param tagSet
	 * @param path
	 * @return false if write fails
	 */
 	public boolean writeTagSetFile(TagSet tagSet, String path) {
		String tagStr = tagSetToString(tagSet);
		boolean result = false;
		if (tagStr != null) {
			result = stringToFile(tagStr, path);
		}
		return result;
	}

	private String fileToString(String path) {
		String content = null;
		Path thePath = Paths.get(path);
		try {
			byte[] bytes = Files.readAllBytes(thePath);
			content = new String(bytes);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			e.printStackTrace();
		}
		return content;
	}

	private boolean stringToFile(String strData, String path) {
		Path thePath = Paths.get(path);
		byte[] bytes = strData.getBytes(StandardCharsets.UTF_8);

		try {
			Files.write(thePath, bytes);
			return true;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	private String tagSetToString(TagSet tagSet) {
		String outStr = null;
		if (tagSet != null) {
			// get the column headings
			String[] hdr = tagSet.tagHdr.colHdr;
			// concatenate the table header into a string
			outStr = hdr[0] + ',' + hdr[1] + ',' + hdr[2] + ',' + hdr[3] + "\n";

			// concatenate the table data
			for (TagData td : tagSet.tagData) {
				outStr = outStr + td.matlTag + ',' + td.matlName + ',' + td.matlFormula + ',' + td.matlGmPerCC + '\n';
			}
		}
		return outStr;
	}

}
