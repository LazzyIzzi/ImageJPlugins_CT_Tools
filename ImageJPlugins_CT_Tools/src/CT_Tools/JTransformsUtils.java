package CT_Tools;

import java.io.File;

//import org.jtransforms.fft.FloatFFT_1D;

import ij.IJ;

/**
 * A set of methods to convert complex data to and from JTransforms 1D sequenced
 * format and support for basic operations in the frequency domain. This class
 * does not do the FFTs and has no ImageJ dependencies.
 * 
 * @see <a href=
 *      "https://javadoc.io/doc/com.github.wendykierp/JTransforms/latest/index.html">JTransforms</a>
 * @author LazzyIzzi
 */
/**
 * @author LazzyIzzi
 *
 */
public class JTransformsUtils {
	
	public boolean JtransformsJarPresent() {
		boolean result = false;
		String jtFilePath = IJ.getDirectory("plugins") + "JTransforms-3.1-with-dependencies.jar";
		File jtFile = new File(jtFilePath);
		if (jtFile.exists()) {
			result = true;
		}
		return result;
	}
	
	public String getJtransformsJarErrorMsg() {
		String msg = "Please download JTransforms-3.1-with-dependencies.jar\n"
				+ "to the ImageJ/plugins folder and restart ImageJ\n"
				+ "https://github.com/wendykierp/JTransforms.";
		return msg;
	}

	/**
	 * Extracts the imaginary part of 1D JTransform sequenced data
	 * 
	 * @param jtReIm the 1D JTransform real,imaginary sequenced data
	 * @return the imaginary part as a 1D float array
	 */
	public float[] getJTransformsImaginary(float[] jtReIm) {
		float[] imaginary = new float[jtReIm.length / 2];
		for (int i = 0; i < jtReIm.length / 2; i++)
			imaginary[i] = jtReIm[2 * i + 1];
		return imaginary;
	}

	/**
	 * Extracts the real part of 1D JTransform sequenced data
	 * 
	 * @param jtReIm the 1D JTransform real,imaginary sequenced data
	 * @return the real part as a 1D float array
	 */
	public float[] getJTransformsReal(float[] jtReIm) {
		float[] real = new float[jtReIm.length / 2];
		for (int i = 0; i < jtReIm.length / 2; i++)
			real[i] = jtReIm[2 * i];
		return real;
	}

	/**
	 * In-place correlation of target with itself returned in the target array
	 * 
	 * @param jtReImTarget A 1D JTransforms real-imaginary sequenced array of image
	 *                     data to be modified
	 */
	public void jtransformsAutoCorrelate(float[] jtReImTarget) {
		if (jtReImTarget.length != jtReImTarget.length) {
			throw new IllegalArgumentException("Target and Sample arrays must be the same size.");
		} else {
			int reIndex, imIndex;
			double A, B, C, D;
			for (int i = 0; i < jtReImTarget.length / 2; i++) {
				reIndex = 2 * i;
				imIndex = reIndex + 1;
				A = jtReImTarget[reIndex];
				B = jtReImTarget[imIndex];
				C = jtReImTarget[reIndex];
				D = jtReImTarget[imIndex];
				jtReImTarget[reIndex] = (float) (A * C + B * D);
				jtReImTarget[imIndex] = (float) (B * C - A * D);
			}
		}
	}

	/**
	 * In-place convolution of target with sample returned in the target array
	 * 
	 * @param jtReImTarget A 1D JTransforms real-imaginary sequenced array of image
	 *                     data to be modified
	 * @param jtReImSample A 1D JTransforms real-imaginary sequenced array of image
	 *                     data to modify the target
	 */
	public void jtransformsConvolve(float[] jtReImTarget, float[] jtReImSample) {
		if (jtReImTarget.length != jtReImSample.length) {
			throw new IllegalArgumentException("Target and Sample arrays must be the same size.");
		} else {
			int reIndex, imIndex;
			double A, B, C, D;
			for (int i = 0; i < jtReImTarget.length / 2; i++) {
				reIndex = 2 * i;
				imIndex = reIndex + 1;
				A = jtReImTarget[reIndex];
				B = jtReImTarget[imIndex];
				C = jtReImSample[reIndex];
				D = jtReImSample[imIndex];
				jtReImTarget[reIndex] = (float) (A * C - B * D);
				jtReImTarget[imIndex] = (float) (A * D + B * C);
			}
		}
	}

	/**
	 * In-place correlation of target with sample returned in the target array
	 * 
	 * @param jtReImTarget A 1D JTransforms real-imaginary sequenced array of image
	 *                     data to be modified
	 * @param jtReImSample A 1D JTransforms real-imaginary sequenced array of image
	 *                     data to modify the target
	 */
	public void jtransformsCorrelate(float[] jtReImTarget, float[] jtReImSample) {
		if (jtReImTarget.length != jtReImSample.length) {
			throw new IllegalArgumentException("Target and Sample arrays must be the same size.");
		} else {
			int reIndex, imIndex;
			double A, B, C, D;
			for (int i = 0; i < jtReImTarget.length / 2; i++) {
				reIndex = 2 * i;
				imIndex = reIndex + 1;
				A = jtReImTarget[reIndex];
				B = jtReImTarget[imIndex];
				C = jtReImSample[reIndex];
				D = jtReImSample[imIndex];
				jtReImTarget[reIndex] = (float) (A * C + B * D);
				jtReImTarget[imIndex] = (float) (B * C - A * D);
			}
		}
	}

	/**
	 * In-place deconvolution of target with sample returned in the target array
	 * 
	 * @param jtReImTarget A 1D JTransforms real-imaginary sequenced array of image
	 *                     data to be modified
	 * @param jtReImSample A 1D JTransforms real-imaginary sequenced array of image
	 *                     data to modify the target
	 */
	public void jtransformsDeconvolve(float[] jtReImTarget, float[] jtReImSample) {
		if (jtReImTarget.length != jtReImSample.length) {
			throw new IllegalArgumentException("Target and Sample arrays must be the same size.");
		} else {

			// double cutoff = IJ.getNumber("Cutoff", 1e-6);
			final double cutoff = 1e-10;
			int reIndex, imIndex;
			double A, B, C, D, X;
			for (int i = 0; i < jtReImTarget.length / 2; i++) {
				reIndex = 2 * i;
				imIndex = reIndex + 1;
				A = jtReImTarget[reIndex];
				B = jtReImTarget[imIndex];
				C = jtReImSample[reIndex];
				D = jtReImSample[imIndex];
				X = C * C + D * D;
				// if (X == 0.0f) {
				// Limit noise caused by dividing by small number
				// i.e. don't amplify weak frequencies
				if (X <= cutoff) {
					jtReImTarget[reIndex] = 0.0f;
					jtReImTarget[imIndex] = 0.0f;
				} else {
					jtReImTarget[reIndex] = (float) ((A * C + B * D) / X);
					jtReImTarget[imIndex] = (float) ((B * C - A * D) / X);
				}
			}
		}
	}

	/**
	 * Extracts the imaginary part of JTransform sequenced data
	 * 
	 * @param jtReIm  the real-imaginary sequenced data
	 * @param nSlices The z, depth etc. AKA number of ImageJ stack slices
	 * @return the imaginary part as an array of float arrays where the first index
	 *         points to the slice and the second index points to the pixels in that
	 *         slice.
	 */
	public float[][] jtransformsImaginaryToFloat3D(float[] jtReIm, int nSlices) {
		int pixelCnt = jtReIm.length / nSlices / 2;
		float[][] stackData = new float[nSlices][pixelCnt];
		int offset;

		for (int slice = 0; slice < nSlices; slice++) {
			for (int i = 0; i < pixelCnt; i++) {
				offset = 2 * (i + slice * pixelCnt);
				stackData[slice][i] = jtReIm[offset + 1];
			}
		}
		return stackData;
	}

	/**
	 * Converts JTransforms sequenced data from magnitude-phase to real-imaginary
	 * Set depth=1 for 2D image. Set nSlices=1 and height=1 for a 1D image
	 * 
	 * @param jtReIm  1D JTransforms sequenced magnitude-phase array
	 * @param cols    the width of the original source image
	 * @param rows    the height of the original source image
	 * @param nSlices the depth of the original source image
	 */
	public void jtransformsMagPhToReIm(float[] jtReIm, int nSlices, int rows, int cols) {
		int i, j, k, home;
		float magVal, phVal;

		for (k = 0; k < nSlices; k++) {
			for (j = 0; j < rows; j++) {
				for (i = 0; i < cols; i++) {
					home = 2 * (i + j * cols + k * cols * rows);
					magVal = jtReIm[home];
					phVal = jtReIm[home + 1];
					jtReIm[home] = (float) (magVal * Math.cos(phVal));
					jtReIm[home + 1] = (float) (magVal * Math.cos(Math.PI / 2 - phVal));
				}
			}
		}
	}

	/**
	 * In-place sets real = 1 and imaginary = 0
	 * 
	 * @param jtReImTarget A 1D jJTransforms real-imaginary sequenced array of image
	 *                     data to be modified
	 * @param jtReImSample A 1D jJTransforms real-imaginary sequenced array of image
	 *                     data to modify the target
	 */
	public void jtransformsNoOp(float[] jtReImTarget, float[] jtReImSample) {
		if (jtReImTarget.length != jtReImSample.length) {
			throw new IllegalArgumentException("Target and Sample arrays must be the same size.");
		} else {
			int reIndex, imIndex;
			for (int i = 0; i < jtReImTarget.length / 2; i++) {
				reIndex = 2 * i;
				imIndex = reIndex + 1;
				jtReImTarget[reIndex] = 1;
				jtReImTarget[imIndex] = 0;
				jtReImSample[reIndex] = 1;
				jtReImSample[imIndex] = 0;
			}
		}
	}

	/**
	 * Applies a phase shift to a JTransforms Complex array.<br>
	 * Set depth=1 for 2D image. Set depth=1 and height=1 for a 1D image
	 * 
	 * @param jtReIm a 1D array the 2D JTransforms sequence data
	 * @param depth  the number of slices in the 3D data
	 * @param height the number of rows in the 3D data
	 * @param width  the number of columns in the 3D data
	 */
	public void jtransformsPhaseShift(float[] jtReIm, int depth, int height, int width) {
		double phase;
		int i, j, k, home;

		for (k = 0; k < depth; k++) {
			for (j = 0; j < height; j++) {
				for (i = 0; i < width; i++) {
					home = 2 * (i + j * width + k * width * height);
					phase = Math.pow(-1.0, (double) (i + j + k));
					jtReIm[home] *= phase;
					jtReIm[home + 1] *= phase;
				}
			}
		}
	}

	/**
	 * Extracts the real part of JTransform sequenced data
	 * 
	 * @param jtReIm  the sequenced data
	 * @param nSlices The z, depth etc. AKA number of stack slices
	 * @return the real part as an array of float arrays where the first index
	 *         points to the slice and the second index points to the pixels in that
	 *         slice. For Example<br>
	 * 
	 *         <pre>
	 *         float[][] pixelsReInv = jtu.jtransformsRealToFloat3D(jtReIm, nSlices);
	 *         for (int slice = 0; slice < nSlices; slice++) {
	 *         	reInvImp.getStack().setPixels(pixelsReInv[slice], slice + 1);
	 *         }
	 *         </pre>
	 */
	public float[][] jtransformsRealToFloat3D(float[] jtReIm, int nSlices) {
		int pixelCnt = jtReIm.length / nSlices / 2;
		float[][] stackData = new float[nSlices][pixelCnt];
		int offset;

		for (int slice = 0; slice < nSlices; slice++) {
			for (int i = 0; i < pixelCnt; i++) {
				offset = 2 * (i + slice * pixelCnt);
				stackData[slice][i] = jtReIm[offset];
			}
		}
		return stackData;
	}

	/**
	 * Converts JTransforms sequenced data from real-imaginary to magnitude-phase
	 * Set depth=1 for 2D image. Set depth=1 and height=1 for a 1D image
	 * 
	 * @param jtReIm  JTransforms sequenced real-imaginary array
	 * @param cols    the width of the source image
	 * @param rows    the height of the source image
	 * @param nSlices the depth of the source image
	 */
	public void jtransformsReImToMagPh(float[] jtReIm, int nSlices, int rows, int cols) {
		int i, j, k, home;
		float reVal, imVal;

		for (k = 0; k < nSlices; k++) {
			for (j = 0; j < rows; j++) {
				for (i = 0; i < cols; i++) {
					home = 2 * (i + j * cols + k * cols * rows);
					reVal = jtReIm[home];
					imVal = jtReIm[home + 1];
					jtReIm[home] = (float) Math.sqrt(reVal * reVal + imVal * imVal);
					jtReIm[home + 1] = (float) Math.atan2((double) imVal, (double) reVal);
				}
			}
		}
	}

	/**
	 * In-place template matching of target with sample returned in the target array
	 * 
	 * @param jtReImTarget   A 1D jJTransforms real-imaginary sequenced array of
	 *                       image data to be modified
	 * @param jtReImTemplate A 1D jJTransforms real-imaginary sequenced array of
	 *                       image data to match in the target
	 */
	public void jtransformsTemplateMatch(float[] jtReImTarget, float[] jtReImTemplate) {
		if (jtReImTarget.length != jtReImTemplate.length) {
			throw new IllegalArgumentException("Target and Sample arrays must be the same size.");
		} else {
			int reIndex, imIndex;
			double A, B, C, D;
			for (int i = 0; i < jtReImTarget.length / 2; i++) {
				reIndex = 2 * i;
				imIndex = reIndex + 1;
				A = jtReImTarget[reIndex];
				B = jtReImTarget[imIndex];
				C = jtReImTemplate[reIndex];
				D = -jtReImTemplate[imIndex];
				jtReImTarget[reIndex] = (float) (A * C - B * D);
				jtReImTarget[imIndex] = (float) (A * D + B * C);
			}
		}
	}

//
////	/**
////	 * Converts In-Place magnitude-phase to real-imaginary
////	 * Set depth=1 for 2D image. Set depth=1 and height=1 for a 1D image
////	 * 
////	 * @param magnitude  magnitude FFT
////	 * @param phase  JTransforms sequenced magnitude-phase array
////	 * @param cols    the width of the source image
////	 * @param rows    the height of the source image
////	 * @param nSlices the depth of the source image
////	 */
////	public void magPhToReIm(float[] magnitude, float[] phase, int nSlices, int rows, int cols) {
////		int i, j, k, home;
////		float magVal, phVal;
////
////		for (k = 0; k < nSlices; k++) {
////			for (j = 0; j < rows; j++) {
////				for (i = 0; i < cols; i++) {
////					home = i + j * cols + k * cols * rows;
////					magVal = magnitude[home];
////					phVal = phase[home];
////					magnitude[home] = (float) (magVal * Math.cos(phVal));
////					phase[home] = (float) (magVal * Math.cos(Math.PI / 2 - phVal));
////				}
////			}
////		}
////	}

	/**
	 * Converts In-Place FFT magnitude-phase to real-imaginary.<br>
	 * Requires 32-bit (float) data with the same width, height and number of
	 * slices..<br>
	 * ImageJ Example:<br>
	 * Object[] magnitude = magnitudeImp.getStack().getImageArray();<br>
	 * Object[] phase = phaseImp.getStack().getImageArray();<br>
	 * magPhToReIm(magnitude,phase,magnitudeImp.getNSlices();<br>
	 * 
	 * @param magnitude the magnitude part of an FFT
	 * @param phase     the phase part of an FFT
	 * @param nSlices   the depth of both source images.
	 */
	public void magPhToReIm(Object[] magnitude, Object[] phase, int nSlices) {
		float magVal, phVal;
		float[] magData;
		float[] phData;

		if (magnitude[0] instanceof float[] && phase[0] instanceof float[]) {
			for (int k = 0; k < nSlices; k++) {
				magData = (float[]) magnitude[k];
				phData = (float[]) phase[k];

				for (int i = 0; i < magData.length; i++) {
					magVal = magData[i];
					phVal = phData[i];
					magData[i] = (float) (magVal * Math.cos(phVal));
					phData[i] = (float) (magVal * Math.cos(Math.PI / 2 - phVal));
				}
			}
		}
	}

	/**
	 * Applies a phase shift to an image.<br>Set depth=1 for 2D image. Set depth=1 and
	 * height=1 for a 1D image
	 * 
	 * @param imageData the 2D data to phase shift
	 * @param width     the number of columns in the 3D data
	 * @param height    the number of rows in the 3D data
	 * @param depth     the number of slices in the 3D data
	 */
	public void phaseShift(float[] imageData, int width, int height, int depth) {
		double phase;
		int i, j, k, home;

		for (k = 0; k < depth; k++) {
			for (j = 0; j < height; j++) {
				for (i = 0; i < width; i++) {
					phase = Math.pow(-1, (double) (i + j + k));
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
	
	/**In-place phase shifting of a array of 1D slice arrays 
	 * @param imageArray as returned by ImageJ imp.getStack.getImageArray()
	 * @param width the width (columns) of the image in pixels
	 * @param height the height (rows) of the image in pixels
	 * @param depth the depth (nSlices) of the image
	 */
	public void stackPhaseShift(Object[] imageArray, int width, int height, int depth) {
		if (imageArray[0] instanceof float[]) {
			for (int k = 0; k < depth; k++) {
				float[] sliceData = (float[]) imageArray[k];
				for (int j = 0; j < height; j++) {
					for (int i = 0; i < width; i++) {
						sliceData[i + j * width] *= Math.pow(-1, (double) (i + j + k));
					}
				}
			}
		}
	}



//	/**
//	 * Applies as 1D phase shift to each row of a 2D polar image
//	 * 
//	 * @param imageData  the 2D data to phase shift
//	 * @param width the number of columns in the 2D data
//	 */
//	public void phaseShiftRows1D(float[] imageData, int width) {
//		int height;
//		int i, j;
//
//		height = imageData.length / width;
//		for (j = 0; j < height; j++) {
//			for (i = 1; i < width; i += 2) {
//				imageData[i + j * width] *= -1;
//			}
//		}
//	}

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
	 * Converts In-Place FFT real-imaginary to magnitude-phase.<br>
	 * Requires 32-bit (float) data with the same width, height and number of
	 * slices..<br>
	 * ImageJ Example:<br>
	 * Object[] real = realImp.getStack().getImageArray();<br>
	 * Object[] imaginary = imaginaryImp.getStack().getImageArray();<br>
	 * reImToMagPh(real,imaginary,realImp.getNSlices();<br>
	 * 
	 * @param real      the real part of an FFT
	 * @param imaginary the imaginary part of an FFT
	 * @param nSlices   the depth of both source images.
	 */
	public void reImToMagPh(Object[] real, Object[] imaginary, int nSlices) {
		float reVal, imVal;
		float[] reData;
		float[] imData;

		if (real[0] instanceof float[] && imaginary[0] instanceof float[]) {
			for (int k = 0; k < nSlices; k++) {
				reData = (float[]) real[k];
				imData = (float[]) imaginary[k];

				for (int i = 0; i < reData.length; i++) {
					reVal = reData[i];
					imVal = imData[i];
					// Put magnitude data in real array
					reData[i] = (float) Math.sqrt(reVal * reVal + imVal * imVal);
					// Put phase data in imaginary array
					imData[i] = (float) Math.atan2((double) imVal, (double) reVal);
				}
			}
		}
	}

////	/**
////	 * Applies as 3D phase shift to a 3D image
////	 * 
////	 * @param data The data from 32-Bit imp.getStack().getImageArray();
////	 * @param The width of the image in pixels
////	 * @param The height of the image in pixels
////	 * @param The depth, z direction etc.  of the image in pixels
////	 */
////	public void stackPhaseShift(Object[] data, int width, int height, int nSlices) {
////		if (data[0] instanceof float[]) {
////			float[] sliceData1,sliceData2;
////			boolean isOdd=false;
////
////			if (Math.floorMod(nSlices, 2) > 0) {
////				isOdd=true;
////				nSlices-=1;
////			}
////
////			for (int slice = 0; slice < nSlices; slice += 2) {
////				sliceData1 = (float[]) data[slice];
////				sliceData2 = (float[]) data[slice+1];
////
////				// do the first slice
////				for (int j = 0; j < height; j += 2) {
////					for (int i = 1; i < width; i += 2) {
////						sliceData1[i + j * width] *= -1;
////					}
////				}
////				for (int j = 1; j < height; j += 2) {
////					for (int i = 0; i < width; i += 2) {
////						sliceData1[i + j * width] *= -1;
////					}
////				}
////				// then the next slice
////				for (int j = 0; j < height; j += 2) {
////					for (int i = 0; i < width; i += 2) {
////						sliceData2[i + j * width] *= -1;
////
////					}
////				}
////				for (int j = 1; j < height; j += 2) {
////					for (int i = 1; i < width; i += 2) {
////						sliceData2[i + j * width] *= -1;
////					}
////				}
////			}
////			if(isOdd) {
////				// do the last slice
////				sliceData1 = (float[]) data[nSlices];
////				for (int j = 0; j < height; j += 2) {
////					for (int i = 1; i < width; i += 2) {
////						sliceData1[i + j * width] *= -1;
////					}
////				}
////				for (int j = 1; j < height; j += 2) {
////					for (int i = 0; i < width; i += 2) {
////						sliceData1[i + j * width] *= -1;
////					}
////				}				
////			}
////		} else {
////			throw new IllegalArgumentException("argument not from a 32-bit ImageStack");
////		}
////	}

	/**
	 * Converts a real array to a 1D JTransforms sequenced array with zeros in the
	 * imaginary part
	 * 
	 * @param real    The real data from imp.getStack().getImageArray();
	 * @param nSlices the depth of the source images.
	 * @return A sequenced array in JTransforms format with zeros in the imaginary
	 *         part
	 * @throws IllegalArgumentException if Object[] is not an array of float arrays
	 */
	public float[] stackRealToJTransformsComplex(Object[] real, int nSlices) {
		float[] jtReIm = null;
		float[] sliceData;
		int offset;

		if (real[0] instanceof float[]) {
			sliceData = (float[]) real[0];
			int pixelCnt = sliceData.length;
			jtReIm = new float[2 * pixelCnt * nSlices];
			for (int slice = 0; slice < nSlices; slice++) {
				sliceData = (float[]) real[slice];
				for (int i = 0; i < pixelCnt; i++) {
					offset = 2 * (i + slice * pixelCnt);
					jtReIm[offset] = sliceData[i];
					jtReIm[offset + 1] = 0;
				}
			}
		} else {
			throw new IllegalArgumentException("argument not from a 32-bit ImageStack");
		}
		return jtReIm;
	}

	/**
	 * Converts real and imaginary Object[] arrays to JTransforms sequenced
	 * array.<br>
	 * Each Object[i] must be instance of float[].
	 * 
	 * @param re      The real data returned from, for example, a call to ImagePlus
	 *                imp.getStack().getImageArray();
	 * @param im      The imaginary data from imp.getStack().getImageArray();
	 * @param nSlices the depth of both source images.
	 * @return A sequenced array in JTransforms format.
	 * @throws IllegalArgumentException if Object[] is not an array of float arrays
	 */
	public float[] stackReImToJTransformsComplex(Object[] re, Object[] im, int nSlices) {
		float[] jtReIm = null;
		float[] reData;
		float[] imData;
		int offset;

		if (re[0] instanceof float[]) {
			reData = (float[]) re[0];
			imData = (float[]) im[0];
			int pixelCnt = reData.length;
			jtReIm = new float[2 * pixelCnt * nSlices];
			for (int slice = 0; slice < nSlices; slice++) {
				reData = (float[]) re[slice];
				imData = (float[]) im[slice];
				for (int i = 0; i < pixelCnt; i++) {
					offset = 2 * (i + slice * pixelCnt);
					jtReIm[offset] = reData[i];
					jtReIm[offset + 1] = imData[i];
				}
			}
		} else {
			throw new IllegalArgumentException("argument not from a 32-bit ImageStack");
		}
		return jtReIm;
	}

	/**
	 * @return The text of the JTransforms License
	 */
	protected String getJTransformsLicense() {

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
		return txt;
	}

}
