package CT_Tools;

import CT_Tools.DFIutils.*;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/**ImageJ based methods to display the DFI plugin's intermediate images 
 * @author LazzyIzzi
 *
 */
public class DebugUtils {

	public void showDebugImage(String title,Object image, int width, int height) {
		if (image instanceof RowColLUT[]) {
			RowColLUT[] data = (RowColLUT[]) image;

			ImagePlus imp = IJ.createImage("title", width, height, 2, 32);
			ImageStack stk = imp.getStack();

			int k = 0;
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < width; j++) {
					stk.getProcessor(1).putPixelValue(i, j, data[k].row);
					stk.getProcessor(2).putPixelValue(i, j, data[k].col);
					k++;
				}
			}
			stk.setSliceLabel("Row", 1);
			stk.setSliceLabel("Column", 2);
			imp.show();
			IJ.run(imp, "Enhance Contrast", "saturated=0.35");
		}

		else if (image instanceof SemiBicubicLUT[]) {
			SemiBicubicLUT[] data = (SemiBicubicLUT[]) image;

			ImagePlus sbcImp = IJ.createImage(title, width, height, 10, 32);
			int k = 0;
			ImageStack stk = sbcImp.getStack();
			for (int i = 0; i < width; i++) {
				for (int j = 0; j < width; j++) {
					stk.getProcessor(1).putPixelValue(i, j, data[k].row);
					stk.getProcessor(2).putPixelValue(i, j, data[k].col);
					stk.getProcessor(3).putPixelValue(i, j, data[k].w0);
					stk.getProcessor(4).putPixelValue(i, j, data[k].w1);
					stk.getProcessor(5).putPixelValue(i, j, data[k].w2);
					stk.getProcessor(6).putPixelValue(i, j, data[k].w3);
					stk.getProcessor(7).putPixelValue(i, j, data[k].w4);
					stk.getProcessor(8).putPixelValue(i, j, data[k].w5);
					stk.getProcessor(9).putPixelValue(i, j, data[k].w6);
					stk.getProcessor(10).putPixelValue(i, j, data[k].w7);
					k++;
				}
			}
			stk.setSliceLabel("Row", 1);
			stk.setSliceLabel("Column", 2);
			stk.setSliceLabel("W0", 3);
			stk.setSliceLabel("W1", 4);
			stk.setSliceLabel("W2", 5);
			stk.setSliceLabel("W3", 6);
			stk.setSliceLabel("W4", 7);
			stk.setSliceLabel("W5", 8);
			stk.setSliceLabel("W6", 9);
			stk.setSliceLabel("W7", 10);
			sbcImp.show();
			IJ.run(sbcImp, "Enhance Contrast", "saturated=0.35");
		}

		else if (image instanceof float[]) {
			float[] data = (float[]) image;

			ImagePlus imp = IJ.createImage(title, width, height, 1, 32);
				imp.getProcessor().setPixels(data);
				imp.show();
				IJ.run(imp, "Enhance Contrast", "saturated=0.35");
		}
		
	}
	
	public void showDebugStack(String title, Object[] image, int width, int height, int depth) {
		if (image[0] instanceof float[]) {
			float[] data;
			ImagePlus imp = IJ.createImage(title, width, height, depth, 32);
			ImageStack stk = imp.getStack();
			ImageProcessor ip;

			for (int k = 0; k < depth; k++) {
				data = (float[]) image[k];
				ip = stk.getProcessor(k + 1);
				//ip.setPixels(data); // fails, returns all zeros
				
				for (int j = 0; j < height; j++) { //works
					for (int i = 0; i < width; i++) {
						ip.putPixelValue(i, j, data[i + j * width]);
					}
				}
			}
			imp.show();
		}
	}

}

