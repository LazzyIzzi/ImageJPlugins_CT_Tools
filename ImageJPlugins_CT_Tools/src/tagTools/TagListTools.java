package tagTools;

import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JOptionPane;

import CT_Tools.ResourceReader;

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
		//Path thePath = Path.of(path);
		byte[] bytes = null;
		Path resourcePath = null;
		
		//if DefaultMaterials.csv is not present in the plugins/DialogData folder
		//Create a new copy from the resources
		if (Files.exists(thePath) == false) {
			try {
				//the parent is the plugins/DialogData folder
				Path parent = thePath.getParent();
				//create the DialogData folder if it does not exist
				Files.createDirectories(parent);
				//create the DefaultMaterials.csv file 
				Files.createFile(thePath);				
				matlStr = new ResourceReader().readTextFile("DefaultMaterials.csv");
//				IJ.log(matlStr);
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
