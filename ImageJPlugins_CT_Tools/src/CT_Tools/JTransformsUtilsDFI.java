package CT_Tools;

/**
 * A set of methods to convert real imaginary data to and from JTransforms sequenced format.
 * This class has no ImageJ dependencies.
 * @see <a href=
 *      "https://javadoc.io/doc/com.github.wendykierp/JTransforms/latest/index.html">JTransforms</a>
 * @author LazzyIzzi
 */
public class JTransformsUtilsDFI {

	/**
	 * Extracts the imaginary part of JTransform sequenced data
	 * 
	 * @param jtReIm the sequenced data
	 * @return the real part as a float array
	 */
	public float[] getJTransformsImaginary(float[] jtReIm) {
		float[] imaginary = new float[jtReIm.length / 2];
		for (int i = 0; i < jtReIm.length / 2; i++)
			imaginary[i] = jtReIm[2 * i + 1];
		return imaginary;
	}

	/**
	 * Extracts the real part of JTransform sequenced data
	 * 
	 * @param jtReIm the sequenced data
	 * @return the real part as a float array
	 */
	public float[] getJTransformsReal(float[] jtReIm) {
		float[] real = new float[jtReIm.length / 2];
		for (int i = 0; i < jtReIm.length / 2; i++)
			real[i] = jtReIm[2 * i];
		return real;
	}
		
	/**
	 * Applies as 3D phase shift to a 3D image
	 * 
	 * @param data  the 2D data to phase shift
	 * @param width the number of columns in the 3D data
	 * @param height the number of rows in the 3D data
	 * @param depth the number of slices in the 3D data
	 */
	public void phaseShift(float[] imageData, int width, int height, int depth) {
		double phase, val = -1.0;
		int i, j, k, home;

		for (k = 0; k < depth; k++) {
			for (j = 0; j < height; j++) {
				for (i = 0; i < width; i++) {
					phase = Math.pow(val, (double) (i + j + k));
					home = i + j * width + k * width * height;
					imageData[home] *= phase;
				}
			}
		}
	}
	
	/**
	 * Applies as 1D phase shift to each row of a 2D polar image
	 * 
	 * @param imageData  the 2D data to phase shift
	 * @param width the number of columns in the 2D data
	 */
	public void phaseShiftRows1D(float[] imageData, int width) {
		int height;
		int i, j;

		height = imageData.length / width;
		for (j = 0; j < height; j++) {
			for (i = 1; i < width; i += 2) {
				imageData[i + j * width] *= -1;
			}
		}
	}


	/**
	 * Convert real array to JTranforms sequenced array with zeros in the imaginary
	 * part
	 * 
	 * @param re The real data in a 1D array
	 * @return A sequenced array in JTransforms format with zeros in the imaginary
	 *         part
	 */
	public float[] realToJTransformsComplex(float[] re) {
		float[] jtReIm = new float[re.length * 2];
		for (int i = 0; i < re.length; i++) {
			jtReIm[2 * i] = re[i];
			jtReIm[2 * i + 1] = 0;
		}
		return jtReIm;
	}

	/**
	 * Convert separate real and imaginary arrays to JTranforms sequenced array.
	 * 
	 * @param real      The real data in a 1D array
	 * @param imaginary The imaginary data in a 1D array
	 * @return A sequenced array in JTransforms format
	 */
	public float[] reimToJTransformsComplex(float[] real, float[] imaginary) {
		float[] jtReIm = new float[real.length * 2];
		for (int i = 0; i < real.length; i++) {
			jtReIm[2 * i] = real[i];
			jtReIm[2 * i + 1] = imaginary[i];
		}
		return jtReIm;
	}
	
	/**
	 * @return The text of the JTransforms License
	 */
	public String getJTransformsLicenseDFI() {

		String dfiTxt = "DFI_JTransforms is an ImageJ plugin for reconstructing parallel-beam\r\n"
				+ "(0-180deg) sinograms into tomographic slices using Direct Fourier Inversion\r\n"
				+ "as described by Flannery et al. Science 18 Sep 1987 Vol 237, Issue 4821 pp.\r\n"
				+ "1439-1444, DOI: 10.1126/science.237.4821.1439\r\n"
				+ "https://www.science.org/doi/10.1126/science.237.4821.1439\r\n"
				+ "JTransforms-3.1-with-dependencies.jar is used for the forward and inverse FTs\r\n"
				+ "and must be in your plugins folder for this plugin to work.\r\n"
				+ "See: https://github.com/wendykierp/JTransforms\r\n"
				+ "The JTransforms license is shown below\r\n"
				+ "LazzyIzzi 1-15-2025\r\n\r\n";

		String txt = "JTransforms FFT library\r\n\r\n" + "JTransforms\r\n"
				+ "Copyright (c) 2007 onward, Piotr Wendykier\r\n" + "All rights reserved.\r\n" + "\r\n"
				+ "Redistribution and use in source and binary forms, with or without\r\n"
				+ "modification, are permitted provided that the following conditions are met:\r\n" + "\r\n"
				+ "1. Redistributions of source code must retain the above copyright notice, this\r\n"
				+ "   list of conditions and the following disclaimer. \r\n"
				+ "2. Redistributions in binary form must reproduce the above copyright notice,\r\n"
				+ "   this list of conditions and the following disclaimer in the documentation\r\n"
				+ "   and/or other materials provided with the distribution.\r\n" + "\r\n"
				+ "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND\r\n"
				+ "ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\r\n"
				+ "WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\r\n"
				+ "DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR\r\n"
				+ "ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES\r\n"
				+ "(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;\r\n"
				+ "LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND\r\n"
				+ "ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\r\n"
				+ "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS\r\n"
				+ "SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
		return dfiTxt + txt;
	}

}
