package CT_Tools;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.NewImage;
import ij.plugin.CanvasResizer;
import ij.process.ImageProcessor;

/**
 * Custom ImageJ methods to prepare the sinogram(s) for reconstruction
 * by padding.  Axis shifting and profile extension are supported.
 * @author LazzyIzzi
 *
 */
public class SinogramUtils {
	/**
	 * Calls ImageJ's Translate to shift the position of the sinogram center of
	 * rotation
	 * 
	 * @param sinoImp   A sinogram image
	 * @param axisShift Move axis of rotation "-" left "+" to the right
	 * @return the execution time for the method
	 */
	public double axisShift(ImagePlus sinoImp, float axisShift) {
		long start = System.nanoTime();
		sinoImp.getProcessor().setInterpolationMethod(ImageProcessor.BICUBIC);
		int nSlices = sinoImp.getNSlices();

		if (sinoImp.hasImageStack()) {
			ImageStack stk = sinoImp.getStack();
			for (int slice = 1; slice <= nSlices; slice++) {
				ImageProcessor ip = stk.getProcessor(slice);
				ip.setInterpolationMethod(ImageProcessor.BICUBIC);
				ip.translate(axisShift, 0);
			}
			// sinoImp.updateAndDraw();

		} else {
			ImageProcessor ip = sinoImp.getProcessor();
			ip.translate(axisShift, 0);
			// sinoImp.updateAndDraw();
		}

		long end = System.nanoTime();
		return (end - start) / 1e9;
	}

	/**
	 * Apply a second order beam hardening correction to x-ray sinogram projection
	 * data
	 * @param sinoImp             The original, unmodified sinogram
	 * @param beamHardeningFactor the corrected pixel value p = (1-bhf)*p + bhf*p<sup>2</sup>
	 * @return the execution time for the method
	 */
	public double applyBeamHardeningCorrection(ImagePlus sinoImp, float beamHardeningFactor) {
		// Apply the beam hardening correction to the sinogram before padding
		long start = System.nanoTime();
		int sinoSliceCnt = sinoImp.getNSlices();

		if (beamHardeningFactor > 0) {
			IJ.showStatus("Applying Beam Hardening Correction");
			for (int slice = 1; slice <= sinoSliceCnt; slice++) {
				IJ.showProgress(slice, sinoSliceCnt);
				sinoImp.setSlice(slice);
				float[] pixels = (float[]) sinoImp.getProcessor().getPixels();
				for (int i = 0; i < pixels.length; i++) {
					pixels[i] = pixels[i] * (1 - beamHardeningFactor)
							+ beamHardeningFactor * (float) Math.pow(pixels[i], 2);
				}
			}
		}
		long end = System.nanoTime();
		return (end - start) / 1e9;
	}

	/**Uses ImageJ's resizer to pad the sinogram to the left and right with zeros
	 * @param sinoImp   A sinogram image, may be BH corrected and extended
	 * @param padFactor padFactor*the sinogram width gives the new width
	 * @return the execution time for the method
	 */
	public double padSinogram(ImagePlus sinoImp, int padFactor) {
		long start = System.nanoTime();
		int width = sinoImp.getWidth();
		int newWidth = width * padFactor;
		int widthOffset = (width * (padFactor - 1)) / 2;
		int height = sinoImp.getHeight();
		int heightOffset = 0;

		CanvasResizer resizer = new CanvasResizer();
		IJ.showStatus("Padding Sinogram(s)");
		if (sinoImp.hasImageStack()) {
			ImageStack stk = sinoImp.getStack();
			stk = resizer.expandStack(stk, newWidth, height, widthOffset, heightOffset);
			sinoImp.setStack(stk);
		} else {
			ImageProcessor ip = sinoImp.getProcessor();
			ip = resizer.expandImage(ip, newWidth, height, widthOffset, heightOffset);
			sinoImp.setProcessor(ip);
		}
		long end = System.nanoTime();
		return (end - start) / 1e9;
	}

	/**
	 * Linearly extends the left and right edges of a sinogram to zero where a portion of the specimen has rotated out of the field of view.
	 * Call this method after beam hardening(if needed) and before padding.
	 * 
	 * @param sinoImp        A sinogram image
	 * @param extensionWidth the width of the extension
	 * @return the execution time for the method
	 */
	public double profileExtend(ImagePlus sinoImp, int extensionWidth) {
		long start = System.nanoTime();
		int sinoWidth = sinoImp.getWidth();
		int sinoHeight = sinoImp.getHeight();
		int heightOffset = 0;
		int extendedSinoWidth = sinoWidth + 2 * extensionWidth;
		int widthOffset = extensionWidth;
		int sinoSliceCount = sinoImp.getNSlices();

		float[] ext = getLinearExtension(extensionWidth);

		// get the edge pixel values for all slices BEFORE changing canvas size
		float[][] leftEdgePix = new float[sinoSliceCount][sinoHeight];
		float[][] rightEdgePix = new float[sinoSliceCount][sinoHeight];
		for (int slice = 1; slice <= sinoSliceCount; slice++) {
			for (int row = 0; row < sinoHeight; row++) {
				leftEdgePix[slice - 1][row] = sinoImp.getStack().getProcessor(slice).getPixelValue(0, row);
				rightEdgePix[slice - 1][row] = sinoImp.getStack().getProcessor(slice).getPixelValue(sinoWidth - 1, row);
			}
		}

		// Expand the image to hold the extensions
		CanvasResizer resizer = new CanvasResizer();
		IJ.showStatus("Padding Sinogram(s)");
		if (sinoImp.hasImageStack()) {
			ImageStack stk = sinoImp.getStack();
			stk = resizer.expandStack(stk, extendedSinoWidth, sinoHeight, widthOffset, heightOffset);
			sinoImp.setStack(stk);
		} else {
			ImageProcessor ip = sinoImp.getProcessor();
			ip = resizer.expandImage(ip, extendedSinoWidth, sinoHeight, widthOffset, heightOffset);
			sinoImp.setProcessor(ip);
		}

		// Apply the left and right extensions each slice
		for (int slice = 1; slice <= sinoSliceCount; slice++) {
			// Apply the right extension
			int rightCol = sinoWidth + extensionWidth;
			for (int row = 0; row < sinoHeight; row++) {
				for (int i = 0; i < extensionWidth; i++) {
					sinoImp.getStack().getProcessor(slice).putPixelValue(rightCol + i, row,
							rightEdgePix[slice - 1][row] * ext[i]);
				}
			}
			// Apply the left extension
			for (int row = 0; row < sinoHeight; row++) {
				for (int i = 0; i < extensionWidth; i++) {
					sinoImp.getStack().getProcessor(slice).putPixelValue(i, row,
							leftEdgePix[slice - 1][row] * ext[extensionWidth - 1 - i]);
				}
			}
		}
		long end = System.nanoTime();
		return (end - start) / 1e9;
	}
	
	/**A custom image duplicator to avoid unwanted Macro Recorder entries
	 * @param srcImp The ImagePlus to be duplicated
	 * @return A duplicate of the srcImp
	 */
	public ImagePlus duplicate(ImagePlus srcImp) {
		
		int w = srcImp.getWidth();
		int h = srcImp.getHeight();
		int sliceCnt = srcImp.getNSlices();
		int bitDepth = srcImp.getBitDepth();
		String title = srcImp.getTitle();
		ImagePlus copyImp = null;
		
		switch (bitDepth)
		{
		case 8:
			copyImp = NewImage.createByteImage(title + "_copy", w, h, sliceCnt, NewImage.FILL_BLACK);
			break;
		case 16:
			copyImp = NewImage.createShortImage(title + "_copy", w, h, sliceCnt, NewImage.FILL_BLACK);
			break;
		case 24:
			copyImp = NewImage.createRGBImage(title + "_copy", w, h, sliceCnt, NewImage.FILL_BLACK);
			break;
		case 32:
			copyImp = NewImage.createFloatImage(title + "_copy", w, h, sliceCnt,NewImage.FILL_BLACK);
			break;
		default:
			copyImp = NewImage.createFloatImage(title + "_copy", w, h, sliceCnt,NewImage.FILL_BLACK);
			break;
			
		}
			
		ImageStack srcStk =srcImp.getStack();
		ImageStack copyStk =copyImp.getStack();
		Object srcPix, destPix;
		int pixCnt=srcStk.getProcessor(1).getPixelCount();
		
		for(int slice=1;slice<=sliceCnt;slice++) {
			srcPix = srcStk.getPixels(slice);
			destPix = copyStk.getPixels(slice);						
			System.arraycopy(srcPix, 0, destPix, 0, pixCnt);
		}
		
		copyImp.setProperties(srcImp.getPropertiesAsArray());
		copyImp.setCalibration(srcImp.getCalibration());
		int[] dim = srcImp.getDimensions();
		copyImp.setDimensions(dim[2], dim[3], dim[4]);
		
		return copyImp;
	}


	/**
	 * builds a simple linear 1 to 0 ramp
	 * 
	 * @param extensionWidth the width of the ramp
	 * @return a simple linear ramp
	 */
	private float[] getLinearExtension(int extensionWidth) {
		float[] extension = new float[extensionWidth];
		float val = 1.0f;
		double inc = 1.0 / extensionWidth;

		for (int i = 0; i < extensionWidth; i++) {
			val -= inc;
			if (val < 0)
				val = 0;
			extension[i] = val;
		}
		return extension;
	}

}
