package CT_Tools;

import org.jtransforms.fft.FloatFFT_1D;
import org.jtransforms.fft.FloatFFT_2D;


/**
 * A set of methods that use Piotr Wendykier's JTransforms Library for Direct Fourier Inversion (DFI) tomographic reconstruction.
 * This class has no ImageJ dependencies. It uses semiBiCubic interpolation and a padFactor of 4 
 * to optimize DFI speed and accuracy.<br>
 * Download JTransforms-3.1-with-dependencies.jar to the plugins folder for the forward and inverse FTs<br>
 * @see <a href=
 *      "https://javadoc.io/doc/com.github.wendykierp/JTransforms/latest/index.html">JTransforms</a>
 * @author LazzyIzzi
 */
public class DFIutils {

	protected class RowColLUT {
		float row, col;
	}

	protected class SemiBicubicLUT {
		float row, col;
		float w0, w1, w2, w3, w4, w5, w6, w7;
	}

	class DFIparams {
		float[] paddedSino1, paddedSino2;
		// padding is done using ImageJ methods
		// before calling DFIrecon
		int paddedSinoWidth, padFactor;
		// SemiBicubicLUT should be created
		// before calling DFIrecon on a stack
		SemiBicubicLUT[] semiBicubicLUT;
		boolean showLUT, showPolarFT, showCartFT;
	}

	private class SemiBicubicWeights {
		float w0, w1, w2, w3, w4, w5, w6, w7;
	}

	//static final int COL = 0, ROW = 1;

	DebugUtils dbu = new DebugUtils();
	JTransformsUtils ftu = new JTransformsUtils();

	/**
	 * Tomographic reconstruction of two same-size pre-processed (e.g. padded etc.) x-ray parallel
	 * projection data.
	 * 
	 * @param dp A DFIparamsSimplified nested class containing the required DFI
	 *           parameters
	 * @return Two reconstructed images in JTransforms sequenced format. Use
	 *         JTransformsUtils.fftRealToFloat and fftImaginaryToFloat to separate the images.
	 */
	public float[] dfiRecon(DFIparams dp) {
		float[] jtReIm = dfiRecon(dp.paddedSino1, dp.paddedSino2, dp.paddedSinoWidth, dp.padFactor, dp.semiBicubicLUT,
				dp.showLUT, dp.showPolarFT, dp.showCartFT);
		return jtReIm;
	}

	/**
	 * Tomographic reconstruction of x-ray parallel projection data
	 * 
	 * @param padSino1       A 1D array of a 2D padded 0 to 180 degree axis-centered
	 *                       parallel projection sinogram
	 * @param padSino2       A 1D array of a second similar sinogram
	 * @param padSinoWidth   The width in pixels of the padded sinogram
	 * @param padFactor      The factor used to pad both sinograms with zeros
	 * @param interpMethod   Use "BiLinear" or "BiCubic" to obtain the Cartesian
	 *                       values from the polar data
	 * @param semiBicubicLut A pre-computed lookup table of Cartesian(x,y) for each
	 *                       polar(r,theta). Call DFIutil makeSemiBicubicLUT
	 *                       method to create the LUT before calling dfiRecon when
	 *                       reconstructing stacks of sinograms. Pass null to have
	 *                       dfiRecon create the LUT each time dfiRecon is called.
	 * @return Two reconstructed images in JTransforms sequenced format. Use
	 *         JTransformsUtils fftRealToFloat and fftImaginaryToFloat to separate the images.
	 */
	private float[] dfiRecon(float[] padSino1, float[] padSino2, int padSinoWidth, int padFactor,
			SemiBicubicLUT[] semiBicubicLut, boolean showLUT, boolean showPolarFT, boolean showCartFT) {
		int padSinoHeight = padSino1.length / padSinoWidth;
		int cartWidth = padSinoWidth / padFactor;
		int cartHeight = cartWidth;
		FloatFFT_1D fftDo = new FloatFFT_1D(padSinoWidth);
		FloatFFT_2D fft2dDo = new FloatFFT_2D(cartWidth, cartHeight);
		float[] jtReIm, rowData1, rowData2;
		// create the rowColLUT if not supplied by user
		if (semiBicubicLut == null) {
			semiBicubicLut = makeSemiBicubicLUT(padSinoWidth, padSinoHeight, padFactor);
		}
		if (showLUT == true) {
			dbu.showDebugImage("SemiBicubicLUT", (Object) semiBicubicLut, cartWidth, cartHeight);
		}

		float[] padSinoFTre = new float[padSinoWidth * padSinoHeight];
		float[] padSinoFTim = new float[padSinoWidth * padSinoHeight];

		for (int row = 0; row < padSinoHeight; row++) {
			rowData1 = getRow(padSino1, padSinoWidth, row);
			rowData2 = getRow(padSino2, padSinoWidth, row);
			jtReIm = ftu.reimToJTransformsComplex(rowData1, rowData2);

//			Computes 2D forward DFT of complex data leaving the result in a.
//			The data is stored in 1D array in row-major order. Complex numbers are stored
//			as two float values in sequence: the real and imaginary part,
//			i.e. the input array must be of size rows*2*columns.
			fftDo.complexForward(jtReIm);

			putRow(ftu.getJTransformsReal(jtReIm), padSinoFTre, padSinoWidth, row);
			putRow(ftu.getJTransformsImaginary(jtReIm), padSinoFTim, padSinoWidth, row);
		}

		if (showPolarFT == true) {
			dbu.showDebugImage("PadSinoFTre", (Object) padSinoFTre, padSinoWidth, padSinoHeight);
			dbu.showDebugImage("PadSinoFTim", (Object) padSinoFTim, padSinoWidth, padSinoHeight);
		}
		ftu.phaseShiftRows1D(padSinoFTre, padSinoWidth);
		ftu.phaseShiftRows1D(padSinoFTim, padSinoWidth);

		float[] cartReData = polarToCartesianSemiBicubic(padSinoFTre, padSinoWidth, padFactor, semiBicubicLut);
		float[] cartImData = polarToCartesianSemiBicubic(padSinoFTim, padSinoWidth, padFactor, semiBicubicLut);
		if (showCartFT == true) {
			dbu.showDebugImage("cartReData", (Object) cartReData, cartWidth, cartHeight);
			dbu.showDebugImage("cartImData", (Object) cartImData, cartWidth, cartHeight);
		}
		// Phase Shift
//		ftu.phaseShift2D(cartReData, cartWidth);
//		ftu.phaseShift2D(cartImData, cartWidth);
		ftu.phaseShift(cartReData, cartWidth,cartHeight,1);
		ftu.phaseShift(cartImData, cartWidth,cartHeight,1);

		// Inverse Transform
		jtReIm = ftu.reimToJTransformsComplex(cartReData, cartImData);

		fft2dDo.complexInverse(jtReIm, true);

		return jtReIm;

	}

	/**
	 * @return The text of the JTransforms License
	 */
	protected String getJTransformsLicense() {

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
	
	/**
	 * SemiBicubic Lookup table maker
	 * 
	 * @param polarWidth  The width of the padded polar image
	 * @param polarHeight The height of the padded polar image
	 * @param padFactor   The factor 2,4,6,8 used to pad the width of the original
	 *                    polar image with zeros
	 * @return A lookup table containing the polar pixel row, column in Cartesian
	 *         coordinates, and the semiBicubic interpolation weights for each polar
	 *         pixel
	 */
	protected SemiBicubicLUT[] makeSemiBicubicLUT(int polarWidth, int polarHeight, int padFactor) {
		RowColLUT[] rowColLUT = makeRowColLUT(polarWidth, polarHeight, padFactor);
		SemiBicubicWeights[] SemiBicubicWeights = makeSemiBicubicWeightsLUT(rowColLUT);
		SemiBicubicLUT[] SemiBicubicLUT = new SemiBicubicLUT[rowColLUT.length];

		for (int i = 0; i < rowColLUT.length; i++) {
			SemiBicubicLUT[i] = new SemiBicubicLUT();
			SemiBicubicLUT[i].col = rowColLUT[i].col;
			SemiBicubicLUT[i].row = rowColLUT[i].row;
			SemiBicubicLUT[i].w0 = SemiBicubicWeights[i].w0;
			SemiBicubicLUT[i].w1 = SemiBicubicWeights[i].w1;
			SemiBicubicLUT[i].w2 = SemiBicubicWeights[i].w2;
			SemiBicubicLUT[i].w3 = SemiBicubicWeights[i].w3;
			SemiBicubicLUT[i].w4 = SemiBicubicWeights[i].w4;
			SemiBicubicLUT[i].w5 = SemiBicubicWeights[i].w5;
			SemiBicubicLUT[i].w6 = SemiBicubicWeights[i].w6;
			SemiBicubicLUT[i].w7 = SemiBicubicWeights[i].w7;
		}
		return SemiBicubicLUT;
	}


	private float[] getRow(float[] data2D, int dataWidth, int row) {
		float[] rowData = new float[dataWidth];
		for (int i = 0; i < dataWidth; i++) {
			rowData[i] = data2D[i + row * dataWidth];
		}
		return rowData;
	}

	/**
	 * Creates a table of the column and row locations that map a polar FT to a
	 * Cartesian FT<br>
	 * DFIrecon uses these row and column locations to interpolate the polarFT
	 * values to Cartesian FT pixel values.
	 * 
	 * @param polarWidth  The width in pixels of the polar FT of a 0-180 degree zero
	 *                    padded parallel-beam sinogram.
	 * @param polarHeight The height in pixels of the polar FT of a 0-180 degree
	 *                    zero padded parallel-beam sinogram.
	 * @param padFactor   The factor used to pad the original sinogram, use 1 for no
	 *                    padding.
	 * @return A RowColLUT[] with column and row locations that map a polar FT to a
	 *         Cartesian FT.
	 */
	private RowColLUT[] makeRowColLUT(int polarWidth, int polarHeight, int padFactor) {
		int ii, jj;
		int halfPolarWidth = polarWidth / 2;

		int cartWidth = polarWidth / padFactor;
		int cartHeight = cartWidth;
		int cartHalfWidth = cartWidth / 2;
		int cartHalfHeight = cartHeight / 2;

		// Initialize the LUT with a negative number
		RowColLUT[] rowColLUT = new RowColLUT[cartWidth * cartHeight];
		for (int i = 0; i < rowColLUT.length; i++) {
			rowColLUT[i] = new RowColLUT();
			rowColLUT[i].row = -1;
			rowColLUT[i].col = -1;
		}

		float column, row;
		float rowScale = (polarHeight - 1) / 180.0f;
		for (int j = 1; j < cartHalfHeight; j++) // the vertical direction
		{
			for (int i = 1; i < cartHalfWidth; i++) // the horizontal direction
			{
				ii = i * padFactor;
				jj = j * padFactor;

				// find the sub-pixel row and column corresponding to the current Cartesian
				// point
				column = (float) Math.sqrt((double) ii * ii + jj * jj);
				row = (float) Math.toDegrees(Math.atan2((double) jj, (double) ii)) * rowScale;

				if (column < halfPolarWidth) {
					// upper left quadrant
					rowColLUT[i + j * cartWidth].col = column;
					rowColLUT[i + j * cartWidth].row = row;

					// generate the lower left quadrant by symmetry
					ii = j;
					jj = cartHeight - i;
					rowColLUT[ii + jj * cartWidth].col = polarWidth - column;
					rowColLUT[ii + jj * cartWidth].row = polarHeight / 2 + row;

					// generate the upper right quadrant by symmetry
					ii = cartWidth - j;
					jj = i;
					rowColLUT[ii + jj * cartWidth].col = column;
					rowColLUT[ii + jj * cartWidth].row = polarHeight / 2 + row;

					// generate the lower right quadrant by symmetry
					ii = cartWidth - i;
					jj = cartHeight - j;
					rowColLUT[ii + jj * cartWidth].col = polarWidth - column;
					rowColLUT[ii + jj * cartWidth].row = row;
				}
			}
		}

		// copy the 0 degree data
		for (int j = 0, i = 0; i < cartWidth; i++) // the horizontal direction
		{
			// the Cartesian points fall directly on the polar points
			rowColLUT[i + j * cartWidth].col = i * padFactor;
			rowColLUT[i + j * cartWidth].row = 0;
		}

		// copy the 90 degree data
		for (int i = 0, j = 1; j < cartHeight; j++) // the vertical direction
		{
			// again the Cartesian points fall directly on the polar points
			rowColLUT[i + j * cartWidth].col = j * padFactor;
			rowColLUT[i + j * cartWidth].row = polarHeight / 2;
		}
		return rowColLUT;
	}

	// This method rotates the reconstructed image about -0.1 degrees!!
	private SemiBicubicWeights[] makeSemiBicubicWeightsLUT(RowColLUT[] rowColLUT) {
		SemiBicubicWeights[] sbc = new SemiBicubicWeights[rowColLUT.length];
		float column, row;
		float x, A, B, C, D, B1, C1, w0, w1;
		int iCol, iRow;

		for (int i = 0; i < rowColLUT.length; i++) {
			column = rowColLUT[i].col;
			row = rowColLUT[i].row;
			iCol = (int) column;
			iRow = (int) row;
			x = column - iCol;
			w0 = iRow - row + 1; // the delta theta below
			w1 = row - iRow; // the delta theta above

			// weight calculation courtesy of Bengt Fornberg while he was at Exxon
			// https://www.colorado.edu/amath/bengt-fornberg-0
			B1 = x + 1.0f;
			C1 = B1 * x;
			D = x - 1.0f;
			C = (x - 2.f) * 0.5f;
			B = C * D;

			A = -B * x * .33333333f;
			B = B * B1;
			C = -C * C1;
			D = C1 * D * .16666667f;

			sbc[i] = new SemiBicubicWeights();
			sbc[i].w0 = w0 * A;
			sbc[i].w1 = w0 * B;
			sbc[i].w2 = w0 * C;
			sbc[i].w3 = w0 * D;

			sbc[i].w4 = w1 * A;
			sbc[i].w5 = w1 * B;
			sbc[i].w6 = w1 * C;
			sbc[i].w7 = w1 * D;
		}
		return sbc;
	}

	/**
	 * @param polarWidth  The width of the padded polar image
	 * @param polarHeight The height of the padded polar image
	 * @param padFactor   The factor 2,4,6,8 used to expand the width of the
	 *                    original polar image with zeros
	 * @return A Cartesian version of the polar FT
	 */
	private float[] polarToCartesianSemiBicubic(float[] polarData, int polDataWidth, int padFactor,
			SemiBicubicLUT[] sbcw) {
		int cartDim = polDataWidth / padFactor;
		float[] cartData = new float[cartDim * cartDim];
		float pixVal;
		int iCol, jRow;
		float col, row;

		for (int i = 1; i < cartData.length; i++) {
			col = sbcw[i].col;
			// out of range polar addresses are marked with -1
			if (col > -1) {
				row = sbcw[i].row;
				iCol = (int) col;
				jRow = (int) row;

				pixVal = polarData[iCol - 1 + jRow * polDataWidth] * sbcw[i].w0;
				pixVal += polarData[iCol + 0 + jRow * polDataWidth] * sbcw[i].w1;
				pixVal += polarData[iCol + 1 + jRow * polDataWidth] * sbcw[i].w2;
				pixVal += polarData[iCol + 2 + jRow * polDataWidth] * sbcw[i].w3;

				pixVal += polarData[iCol - 1 + (jRow + 1) * polDataWidth] * sbcw[i].w4;
				pixVal += polarData[iCol + 0 + (jRow + 1) * polDataWidth] * sbcw[i].w5;
				pixVal += polarData[iCol + 1 + (jRow + 1) * polDataWidth] * sbcw[i].w6;
				pixVal += polarData[iCol + 2 + (jRow + 1) * polDataWidth] * sbcw[i].w7;

				cartData[i] = pixVal;
			}

		}
		// SemiBicubic does not interpolate the DC term
		cartData[0] = polarData[0];
		// copy the 0 degree data gives no improvement
//		for (int ii = 0; ii < cartDim; ii++) // the horizontal direction
//		{
//			// the Cartesian points fall directly on the polar points
//			cartData[ii] = polarData[ii * padFactor];
//		}

		return cartData;
	}

	private void putRow(float[] rowData, float[] data2D, int dataWidth, int row) {
		for (int i = 0; i < dataWidth; i++) {
			data2D[i + row * dataWidth] = rowData[i];
		}
	}

}
