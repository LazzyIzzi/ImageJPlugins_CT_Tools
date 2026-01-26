package tagTools;

import java.awt.Dimension;
import java.util.HashSet;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

//import ij.IJ;
import jhd.MuMassCalculator.MuMassCalculator;
import tagTools.TagListTools.*;

/**Applies tag properties to images*/
public class TagListImageTools {

	public TagListImageTools() {
		// TODO Auto-generated constructor stub
	}
	TagListTools mt = new TagListTools();
	MuMassCalculator mmc = new MuMassCalculator();
	
	/**Scans an float image for unique pixel tag values
	 * @param imageData
	 * @return a list of integer tag values used in the image
	 */
	public int[] getUniqueTags(float[] imageData) {
		// from https://www.javatpoint.com/find-unique-elements-in-array-java

		HashSet<Float> hashset = new HashSet<>();
		for (int i = 0; i < imageData.length; i++) {
			if (!hashset.contains(imageData[i])) {
				hashset.add(imageData[i]);
			}
		}

		// Convert hash to float array
		Float[] tagList = hashset.toArray(new Float[hashset.size()]);
		int[] tagArr = new int[hashset.size()];
		int i = 0;
		// Convert to primitive array
		for (Float fObj : tagList) {
			tagArr[i] = fObj.intValue();
			i++;
		}

		// print hash set that contains distinct element
		return tagArr;
	}
	
	/**Computes the effective x-ray energy at each image pixel using <br>
	 * the reconstructed linear attenuation and known composition(tag image)<br>
	 * Images must be the same size.
	 * 
	 * @param dataImagePixels  a 1D array of pixels from a beam hardened reconstructed image
	 * @param tagImagePixels a 1D array of pixels from a model "tag" image
	 * @return a 1D array of effective x-ray energies.
	 */
	public float[] muLinandTagsToMevImage(float[] dataImagePixels, float[] tagImagePixels, TagSet tagSet) {
		if (dataImagePixels.length != tagImagePixels.length) {
			return null;
			
		}
		// Set up the progress bar
		JPanel fldPanel = new JPanel();
		JFrame frame = new JFrame("Tags To MeV Image");
		JProgressBar prgBar = new JProgressBar(0, tagImagePixels.length);

		frame.setSize(400, 100);
		frame.setLocationRelativeTo(null);

		prgBar.setPreferredSize(new Dimension(350, 50));
		prgBar.setValue(0);
		prgBar.setStringPainted(true);
		fldPanel.add(prgBar);

		frame.add(fldPanel);
		frame.setVisible(true);

		// get the tag info as arrays
		String[] matlFormulas = mt.getTagSetMatlFormulasAsArray(tagSet);
		double[] matlGmPerCC = mt.getTagSetMatlGmPerccAsArray(tagSet);
		int[] tags = mt.getTagSetMatlTagAsArray(tagSet);

		// find the largest tag value
		int maxTag = -1;
		for (int i = 0; i < tags.length; i++) {
			if (tags[i] > maxTag)
				maxTag = tags[i];
		}

		// Build look up tables
		String[] formulaLUT = new String[maxTag + 1];
		double[] gmPerCcLUT = new double[maxTag + 1];
		for (int i = 0; i < tags.length; i++) {
			formulaLUT[tags[i]] = matlFormulas[i];
			gmPerCcLUT[tags[i]] = matlGmPerCC[i];
		}

		String formula;
		double gmPerCC;
		float[] effPix = new float[tagImagePixels.length];
		double[] effArr;

		for (int i = 0; i < tagImagePixels.length; i++) {
			prgBar.setValue(i);
			if (tagImagePixels[i] != 0)// 0 is empty space
			{
				formula = formulaLUT[(int) tagImagePixels[i]];
				gmPerCC = gmPerCcLUT[(int) tagImagePixels[i]];
				if (formula != null) {
					effArr = mmc.getMeVfromMuLin(formula, dataImagePixels[i], gmPerCC, "TotAttn");
					// take the first effective energy solution.
					// This may cause issues if absorption edges are present near the effective
					// energy
					if (effArr != null)
						effPix[i] = (float) effArr[0];
					else
						effPix[i] = 0;
				} else
					effPix[i] = 0;
			}
		}
		frame.dispose();
		return effPix;
	}

	/**Converts in-place a tag image to linear attenuation at a selected energy	 * 
	 * @param imageData a 1D array of a tag image
	 * @param tagSet    a class contain tagID, Name, Formula and density
	 * @param keV       the x-ray energy
	 * @return true if successfully converted.
	 */
	public boolean tagsToLinearAttn(float[] imageData, TagSet tagSet, double keV) {
		// tagsToMuLin(pixels,myTags,keV);
		int[] tagArr = getUniqueTags(imageData);

		// check if tagArr is bigger than the tagList
		// if it is, it is probably not a tag image
		if (tagArr.length > tagSet.tagData.size()) {
			JOptionPane.showMessageDialog(null, "There are more tags in the input image than\n" + "there are tags in the materials list.");
			return false;
		}

		// Find the position of each tag in the TagData list
		int[] tagIndex = new int[tagArr.length];
		for (int j = 0; j < tagArr.length; j++) {
			tagIndex[j] = -1;// -1 indicates a match was not found, zero is a valid tag index
			int i = 0;
			for (TagData td : tagSet.tagData) {
				if (tagArr[j] == td.matlTag) {
					tagIndex[j] = i;
				}
				i++;
			}
		}

		// Get biggest matlTag
		int maxTag = Integer.MIN_VALUE;
		for (int i = 0; i < tagArr.length; i++) {
			if (tagArr[i] > maxTag)
				maxTag = tagArr[i];
		}

		// Create an array to hold the muMass values
		float[] muMassArr = new float[maxTag + 1];

		// Set the muMass values for each tag
		for (int i = 0; i < tagIndex.length; i++) {
			if (tagIndex[i] >= 0) {
				String formula = tagSet.tagData.get(tagIndex[i]).matlFormula;
				double gmPerCC = tagSet.tagData.get(tagIndex[i]).matlGmPerCC;
				double muLin = mmc.getMuMass(formula, keV / 1000, "TotAttn") * gmPerCC;
				mmc.getFormulaWeight(formula);
				muMassArr[tagArr[i]] = (float) muLin;
				// System.out.println("formula="+ formula+", gmPerCC =" + gmPerCC + ",
				// muLin="+muLin);
			} else {
				JOptionPane.showMessageDialog(null, "Tag " + tagArr[i] + " was not found in the materials list");
			}
		}

		for (int i = 0; i < imageData.length; i++) {
			try {
				imageData[i] = muMassArr[(int) imageData[i]];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}

}
