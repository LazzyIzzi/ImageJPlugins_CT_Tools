package CT_Tools;


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.ImageListener;

import ij.gui.*;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.measure.Calibration;
import ij.plugin.ImageCalculator;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import jhd.ImageJAddins.GenericDialogAddin;
import jhd.ImageJAddins.GenericDialogAddin.*;

/**Reconstructs a stack of 0-180 degree parallel-beam scan 32bit  sinograms.<br>
 * If you have projections, read them into a stack and convert them to sinograms.<br>
 * using ImageJ's stack->reslice..<br>
 * 
 * @author LazzyIzzi
 *
 */

public class CT_Recon_ParallelBeam implements ActionListener, DialogListener, ImageListener, PlugIn
{
	
	//Pass recon parameters as a Class (Struct) so I don't mess up the arguments list
	//fixRays and sumRule options are set to "None" because they don't work properly.
	private class DialogSettings
	{
		String sumRuleChoice,extShapeChoice,extWidthChoice,cartFilterChoice,fixRingsChoice,fixRaysChoice,axisShiftChoice;
		int sumRuleIndex,extShapeIndex,extWidthIndex,cartFilterIndex,fixRingsIndex,fixRaysIndex,axisShiftIndex;
		double beamHardenWeight,cartCutoff,scaleFactor;
		int rotationAxis;
		boolean useScaleFactor;
	}
	
	class ReconStatusListener implements Runnable // J. Anderson 1999
	{
		Thread t;

		public ReconStatusListener ()
		{
			reconDoneFlag = false;
					
			t = new Thread(this);
			t.start();
		}

		public void reconDone()
		{
			IJ.showProgress(2);
			reconDoneFlag = true;
			try
			{
				//File File_to_Delete = new File ("Recon_Status.txt");
				File File_to_Delete = new File (statusFileLocation);
				File_to_Delete.delete();
			}
			catch (Exception de)
			{
			}
		}

		public void run()
		{
			while (!reconDoneFlag)
			{
				String fileLine;

				try
				{
					//FileReader FR = new FileReader (File_Name);
					FileReader fileReader = new FileReader (statusFileLocation);
					BufferedReader bufferedFileReader = new BufferedReader(fileReader);
					fileLine = bufferedFileReader.readLine();
					bufferedFileReader.close();
					if ((fileLine != null) & (! reconDoneFlag))
					{
						IJ.showStatus(fileLine);
						String[] strs = fileLine.split(" ");
						double slice = Double.parseDouble(strs[2]);
						double tot = Double.parseDouble(strs[4]);
						IJ.showProgress(slice/tot);
						
					}
				}
				catch (Exception fe)
				{
					//System.out.println(statusFileLocation + " does not exist");
				}
				try
				{
					//t.sleep(1000);
					Thread.sleep(1000);
				}
				catch (Exception se)
				{
				}
			}
		}

	}
	
	GenericDialog gd;
	ImagePlus imageImp;	
	String[] winTitles;
 
	//The names of the directory and files used for I/O
	String pluginDir,originalFileName,copyFileName;
	
	//The fully qualified file locations for the file required by Base_Recon.exe
	String originalFileLocation,copyFileLocation,sinoFileLocation;
	
	String specFileLocation,reconFileLocation,timeFileLocation;
	String statusFileLocation,exeFileLocation;
	
	//The command issued to Base_Recon.exe.
	//Since the filenames are reused probaply shoule be final String ...
	String reconCommand;
	//The flag to indicate the reconstruction processing is complete
	boolean reconDoneFlag = false;
	//Listens for the flag
	ReconStatusListener Recon_Status_Posts;
	
	//Dialog Component Fields, requires GenericDialogPlusLib.*;
	//Makes event handlers much easier to write
	ButtonField reconStackBF,reconSliceBF,refreshBF;	
	NumericField rotAxisNF,cartCutNF,scaleFactorNF;	
	ChoiceField sinoCF,axisShiftCF, cartFilterCF,extWidthCF,extShapeCF, fixRingsCF,fixRaysCF,sumRuleCF;		
	SliderField cartCutSF,beamHardenSF;
	CheckboxField useScaleFactorCBF;
	double scaleFactor = 6000;
	boolean useScaleFactor = false;

	
	//The background color for the GenericDialog just because I like it.
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	final Color errColor = new Color(255,100,0);
	final Color white = new Color(255,255,255);
	private boolean littleEndian = false;

	//*********************************************************************************************
	
	@Override
	public void actionPerformed(ActionEvent e)
	{

		Object src = e.getSource();
		Button btn = (Button)src;
		String name = btn.getName();
		switch(name)
		{
		case "refresh":
			if(IJ.isMacro()) return;
			winTitles =getImages();
			if(winTitles.length==0)
			{
				IJ.showMessage("There are no 16 or 32-Bit images present");
				return;
			}
			sinoCF.getChoice().removeAll();
			sinoCF.setChoices(winTitles);
			break;
		case "reconSliceBtn":
			//The reconSliceBtn button reconstructs the current slice
			String item = sinoCF.getChoice().getSelectedItem();
			if(item==null)
			{
				IJ.showMessage("Please open and select a 32-bit sinogram image.");
				return;
			}
			DialogSettings ds = getDialogSettings();
			imageImp=WindowManager.getImage(item);
			double maxVal = imageImp.getStatistics().max;
			//scaleFactor = scaleFactorNF.getNumber();
			switch (imageImp.getBitDepth())
			{
			case 32:
				if( maxVal > 5.46)
				{
					IJ.showMessage("Sinogram maximum value of "+maxVal+" is greater than 5.46 exceeding the reconstructors dynamic range.");
					return;
				}
				break;
			case 16:
				//if(maxVal> 32767)
				if(maxVal/ds.scaleFactor> 5.46)
				{
					//IJ.showMessage("Sinogram maximum value of "+maxVal+" is greater than 32767 exceeding the reconstructors dynamic range.");
					IJ.showMessage("Sinogram maximum scale value of "+maxVal+"/"+ds.scaleFactor + "=" + maxVal/ds.scaleFactor +"\nis greater than 5.46 exceeding the reconstructors dynamic range.");
					return;
				}
				break;
			}
			
			IJ.showStatus("Reconstructing");
			ImagePlus sliceImp = imageImp.crop("whole-slice");
			ImagePlus reconImp = reconstruct(sliceImp,ds);
					
			//if the image "TestImage.rcon" does not exist
			//Copy Properties and Calibration and convert from 1/pixel to 1/cm
			Calibration reconCal = reconImp.getCalibration();
			Calibration imageCal = imageImp.getCalibration();
			reconCal.pixelHeight = imageCal.pixelWidth;
			reconCal.pixelWidth = imageCal.pixelWidth;
			reconCal.setXUnit(imageCal.getXUnit());
			reconCal.setYUnit(imageCal.getXUnit());
			reconImp.repaintWindow();
			setImageProperties(imageImp,reconImp);
				
			//reconstruct() returns the reconImp titled "TestImage.rcon"		
			ImagePlus testImp = WindowManager.getImage("TestImage.rcon");
			if(testImp==null)
			{
				reconImp.getProcessor().multiply(1/reconCal.pixelWidth);
				reconImp.show();
			}
			else
			{
				testImp.setImage(reconImp);
				Roi roi = testImp.getRoi();
				testImp.setRoi(0,0,testImp.getWidth(),testImp.getHeight());
				testImp.getProcessor().multiply(1/reconCal.pixelWidth);
				testImp.setRoi(roi);
				ImageStatistics stats = testImp.getStatistics();
				testImp.getProcessor().setMinAndMax(stats.min, stats.max);
				testImp.updateAndDraw();						
			}		
			break;
		}		
	}
		
	//*********************************************************************************************

	private byte [] bytesForShorts(short [] value)  // J. Anderson 1999
	{
		byte Array [] = new byte [value.length *2];

		for (int Index = 0; Index < value.length; Index++)
		{
			Array [(Index * 2) + 1] = (byte) ((value[Index] >> 8) & 0xff);
			Array [Index * 2] = (byte) (value[Index] & 0xff);
		}
		return Array;
	}
	
	//*********************************************************************************************

	private boolean callBaseRecon() // adapted from J. Anderson 1999
	{
		boolean ok = true;
		try
		{
			Runtime r = Runtime.getRuntime();
			Process p = null;
			p = r.exec(reconCommand);
			Recon_Status_Posts = new ReconStatusListener ();
			p.waitFor();
			Recon_Status_Posts.reconDone();
		}
		catch (Exception pe)
		{
			Recon_Status_Posts.reconDone();
			ok=false;
		}
		return ok;
	}

	//*********************************************************************************************

	/**Creates a scaled 16-bit copy of a 32-bit image. Image properties are not copied.
	 * @param destDir an existing directory to save the 16-bit copy
	 * @param srcTitle the title of the source 32-bit image
	 * @param destTitle the title of the destination 16-bit image
	 * @return an ImagePlus reference to the copied image.
	 */
	private ImagePlus convertTo16bitTifFile(String destDir, ImagePlus sinoImp, String destTitle)
	{		
		ImagePlus copy16bitImp = IJ.createImage(destTitle, sinoImp.getWidth(), sinoImp.getHeight(), sinoImp.getNSlices(), 16);
		IJ.run(copy16bitImp, "Set...", "value=6000 stack");
		ImageCalculator.run(copy16bitImp, sinoImp, "Multiply stack");
		copy16bitImp.setCalibration(sinoImp.getCalibration());
		IJ.saveAs(copy16bitImp, "Tiff", destDir+destTitle);
		return copy16bitImp;
	}

	//*********************************************************************************************

	private void deleteTempFiles()
	{
		File file = new File(copyFileLocation);
		if(file.exists()) file.delete();
		file = new File(sinoFileLocation);
		if(file.exists()) file.delete();
		file = new File(specFileLocation);
		if(file.exists()) file.delete();
		file = new File(reconFileLocation);
		if(file.exists()) file.delete();
		file = new File(timeFileLocation);
		if(file.exists()) file.delete();				
		file = new File(statusFileLocation);
		if(file.exists()) file.delete();				
	}
	
	//*********************************************************************************************
	
	@Override
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e)
	{
		boolean dlogOK = true;
		if(e!=null)
		{
			//getDialogSettings(gd);
			imageImp = WindowManager.getImage(sinoCF.getChoice().getSelectedItem());

			Object src = e.getSource();
			if(src instanceof Choice)
			{
				Choice theChoice = (Choice)src;
				String choiceName = theChoice.getName();
				switch (choiceName)
				{
				case "sinogram":
					IJ.runMacro("selectWindow(\"" + imageImp.getTitle() + "\")");
					rotAxisNF.setNumber(imageImp.getWidth()/2);
					setScaleFactorFromSinoProperty(imageImp);
					useScaleFactorCBF.getCheckBox().setState(useScaleFactor);
					scaleFactorNF.setNumber(scaleFactor);
					break;
				}
			}
			
			//sliders generate textfield events but we don't need to check values
			//because they are forced to be between 0-1
			else if(src instanceof TextField)
			{
				TextField tf = (TextField)src;

				String textFieldName = tf.getName();
				switch(textFieldName)
				{
				case "rotAxis":				
					double rotAxis = rotAxisNF.getNumber();
					if(Double.isNaN(rotAxis) ||rotAxis <0 || rotAxis> imageImp.getWidth())
					{
						reconSliceBF.getButton().setEnabled(false);
						dlogOK = false;
					}
					else reconSliceBF.getButton().setEnabled(true);
					break;
				case "scaleFactor":
					double scaleFactor = scaleFactorNF.getNumber();
					if(Double.isNaN(scaleFactor)|| scaleFactor<1)
						{
							dlogOK = false;
							reconSliceBF.getButton().setEnabled(false);
							dlogOK = false;
						}
						else reconSliceBF.getButton().setEnabled(true);
					break;
				}
				if(!dlogOK) tf.setBackground(errColor);
				else tf.setBackground(white);
			}
		}
		//dialogItemChanged is called once with a null argument when
		//OK is clicked. W
		getDialogSettings(gd);
		
		return dlogOK;
	}

	//*********************************************************************************************

	private void displayReconImage(ImagePlus reconImp)
	{

		//Copy Sinogram Calibration
		Calibration reconCal = reconImp.getCalibration();
		Calibration imageCal = imageImp.getCalibration();
		reconCal.pixelHeight = imageCal.pixelWidth;
		reconCal.pixelWidth = imageCal.pixelWidth;
		reconCal.setXUnit(imageCal.getXUnit());
		reconCal.setYUnit(imageCal.getXUnit());
		
		//Copy sinogram properties and append the recon properties
		setImageProperties(imageImp,reconImp);
		
		//Rescale from per pixel to per cm
		ImageProcessor reconIp = reconImp.getProcessor();
		for(int i=1;i<=reconImp.getNSlices();i++) 
		{
			reconImp.setSlice(i);
			reconIp.multiply(1/reconCal.pixelWidth);
		}

        ImageStatistics stats = reconImp.getStatistics();
		reconIp.setMinAndMax(stats.min, stats.max);
		
		//The readReconImage names the reconImp "TestImage.rcon"
		//Rename by adding _Recon to the sinogram image title
		String imgTitle = imageImp.getTitle();
		if(imgTitle.endsWith(".tif"))
		{
			imgTitle = imgTitle.replace(".tif", "_Recon.tif");
		}
		else
		{
			imgTitle = imgTitle+"_Recon";
		}
		
		imgTitle = WindowManager.getUniqueName(imgTitle);
		reconImp.setTitle(imgTitle.replace(".tif", "_Recon.tif"));
		reconImp.show();		

	}
	
	//*********************************************************************************************

	public void floatToArray (byte [] Array, int First_Byte, float Float_Value) // J. Anderson 1999
	{
		Array[First_Byte+3] = (byte) ((Float.floatToIntBits(Float_Value) >> 24) & 0xff);
		Array[First_Byte+2] = (byte) ((Float.floatToIntBits(Float_Value) >> 16) & 0xff);
		Array[First_Byte+1] = (byte) ((Float.floatToIntBits(Float_Value) >> 8) & 0xff);
		Array[First_Byte] = (byte) (Float.floatToIntBits(Float_Value) & 0xff);
	}
	
	//*********************************************************************************************

	private DialogSettings getDialogSettings()
	{
		//This method is not macro recordable
		//called from event handlers
		DialogSettings ds = new DialogSettings();
		ds.rotationAxis = (int) rotAxisNF.getNumber();
		ds.axisShiftChoice = axisShiftCF.getChoice().getSelectedItem();
		ds.axisShiftIndex = axisShiftCF.getChoice().getSelectedIndex();
		String text = cartCutSF.getTextField().getText();
		ds.cartCutoff = Double.parseDouble(text);
		ds.cartFilterChoice = cartFilterCF.getChoice().getSelectedItem();
		ds.cartFilterIndex = cartFilterCF.getChoice().getSelectedIndex();
		ds.extWidthChoice = extWidthCF.getChoice().getSelectedItem();
		ds.extWidthIndex = extWidthCF.getChoice().getSelectedIndex();
		ds.extShapeChoice = extShapeCF.getChoice().getSelectedItem();
		ds.extShapeIndex = extShapeCF.getChoice().getSelectedIndex();
		ds.fixRingsChoice = fixRingsCF.getChoice().getSelectedItem();
		ds.fixRingsIndex = fixRingsCF.getChoice().getSelectedIndex();
		ds.fixRaysChoice = "Don't Fix";	
		ds.fixRaysIndex = 0;	
		ds.sumRuleChoice = "None";
		ds.sumRuleIndex = 0;
		text=beamHardenSF.getTextField().getText();
		ds.beamHardenWeight = Double.parseDouble(text);
		ds.useScaleFactor = useScaleFactorCBF.getCheckBox().getState();
		ds.scaleFactor = scaleFactorNF.getNumber();

		return ds;
	}

	//*********************************************************************************************

	private DialogSettings getDialogSettings(GenericDialog gd)
	{
		//This method provided for macro recording
		//Reordering or adding items in the dialog may break this code
		DialogSettings ds = new DialogSettings();
		String imgTitle = gd.getNextChoice(); //the image data is passed separately
		ds.rotationAxis = (int)gd.getNextNumber();
		ds.axisShiftChoice = gd.getNextChoice();
		ds.cartFilterChoice = gd.getNextChoice();
		ds.extWidthChoice = gd.getNextChoice();
		ds.extShapeChoice = gd.getNextChoice();
		ds.fixRingsChoice =gd.getNextChoice();
		ds.fixRaysChoice = "Don't Fix";	
		ds.sumRuleChoice = "None";
		ds.cartCutoff = gd.getNextNumber();
		ds.beamHardenWeight = gd.getNextNumber();
		ds.scaleFactor = gd.getNextNumber();
		ds.useScaleFactor = gd.getNextBoolean();
		
		gd.resetCounters();
		int imgIndex = gd.getNextChoiceIndex();
		ds.axisShiftIndex = gd.getNextChoiceIndex();
		ds.cartFilterIndex = gd.getNextChoiceIndex();
		ds.extWidthIndex = gd.getNextChoiceIndex();
		ds.extShapeIndex = gd.getNextChoiceIndex();
		ds.fixRingsIndex = gd.getNextChoiceIndex();
		ds.fixRaysIndex = 0;
		ds.sumRuleIndex = 0;

		return ds;
	}
	
	//*********************************************************************************************
	
	private String[] getImages()
	{
		ArrayList<String> winTitles = new ArrayList<String>();
		String[] titleArr;
		
		String[] titles = WindowManager.getImageTitles();
		for(String title : titles)
		{
			ImagePlus imp = WindowManager.getImage(title);
			int bitDepth=imp.getBitDepth();
			if(bitDepth == 32 || bitDepth == 16)
				winTitles.add(imp.getTitle());
		}
		titleArr = winTitles.toArray(new String[winTitles.size()]);
		return titleArr;
	}

	//*********************************************************************************************

	@Override
	public void imageClosed(ImagePlus imp) {
		if(IJ.isMacro()) return;
		String[] titles =getImages();
		sinoCF.getChoice().removeAll();
		sinoCF.setChoices(titles);
	}

	//*********************************************************************************************

	@Override
	public void imageOpened(ImagePlus imp) {
		if(IJ.isMacro()) return;
		if(imp.getTitle().equals("TestImage.rcon")) return;
		String[] titles =getImages();
		sinoCF.getChoice().removeAll();
		sinoCF.setChoices(titles);
	}
		
	//*********************************************************************************************
	
	@Override
	public void imageUpdated(ImagePlus imp) {
		if(IJ.isMacro()) return;
		if(imp.getTitle().startsWith("DUP")) return;
		if(imp.getBitDepth()==32)
		{
			FileInfo fi=imp.getOriginalFileInfo();
			if(fi!=null)
			{
				try{sinoCF.getChoice().remove(fi.fileName);}
				catch (Exception e) {}
				
				sinoCF.getChoice().add(imp.getTitle());
			}
			else
			{
				sinoCF.getChoice().add(imp.getTitle());			
			}
		}
	}
		
	//*********************************************************************************************

	public void intToArray (byte [] Array, int First_Byte, int Int_Value) // J. Anderson 1999
	{
		Array[First_Byte+3] = (byte) ((Int_Value >> 24) & 0xff);
		Array[First_Byte+2] = (byte) ((Int_Value >> 16) & 0xff);
		Array[First_Byte+1] = (byte) ((Int_Value >> 8) & 0xff);
		Array[First_Byte] = (byte) (Int_Value & 0xff);
	}
	
	//*********************************************************************************************

	private ImagePlus load(String directory, String name) 
	{
		ImagePlus imp;

		FileInfo fi = new FileInfo(); 
		try 
		{
			fi = readHeader( directory+name);
		}
		catch (IOException e)
		{
			IJ.log("Recon Reader: Header: "+ e.getMessage());
		}
		fi.fileName = name;
		fi.directory = directory;
		fi.fileFormat = FileInfo.RAW;
		FileOpener fo = new FileOpener(fi);  
		imp = fo.openImage();
		return imp; 
	}
	
	//*********************************************************************************************

	private float readFloat(DataInputStream input) throws IOException 
	{
		if (!littleEndian) return input.readFloat();  
		int orig = readInt(input);
		return (Float.intBitsToFloat(orig));
	}

	//*********************************************************************************************
	
	private FileInfo readHeader( String hdrfile ) throws IOException 
	{
		FileInputStream filein = new FileInputStream (hdrfile);
		DataInputStream input = new DataInputStream (filein);
		FileInfo fi = new FileInfo();

		this.littleEndian = true;     
		
		int	key = readInt (input);				// key: 'RECO'
		if (key != 1380270927)
		{
			this.littleEndian = false;
		}
		float	pixSize = readFloat(input);			// Size of pixels in microns 
		int		nviews = readInt (input);			// Number of Slices
		char 	dataFormat = (char) input.readByte();			// data format, Byte, Short, Float
		char	pad1 = (char) input.readByte();			// reserved alignment bytes
		char	pad2 = (char) input.readByte();			// reserved alignment bytes
		char	pad3 = (char) input.readByte();			// reserved alignment bytes
		int		xsize = readInt (input);
		int		ysize = readInt (input);		// Dimensions of image 
		
		fi.intelByteOrder = this.littleEndian;   
		fi.width = ysize;
		fi.height = xsize;
		fi.nImages = nviews;

		fi.unit = new String ("cm"); 
		fi.pixelWidth = (double) pixSize;
		fi.pixelHeight = (double) pixSize;
		fi.pixelDepth = (double) pixSize;
		fi.offset = 184;

		input.close();
		filein.close();
    
		if (dataFormat == 'B')
			fi.fileType = FileInfo.GRAY8; 			// DT_UNSIGNED_CHAR 
		if (dataFormat == 'F')
			fi.fileType = FileInfo.GRAY32_FLOAT; 		// DT_FLOAT 
		
		return (fi);
	}

	//*********************************************************************************************

	private int readInt(DataInputStream input) throws IOException 
	{
		if (!littleEndian) return input.readInt(); 
		byte b1 = input.readByte();
		byte b2 = input.readByte();
		byte b3 = input.readByte();
		byte b4 = input.readByte();
		return ( (((b4 & 0xff) << 24) | ((b3 & 0xff) << 16) | ((b2 & 0xff) << 8) | (b1 & 0xff)) );
	}

	//*********************************************************************************************
	//readRecon Adapted from a Plugin by Guy Williams, gbw1000@wbic.cam.ac.uk 23/9/99**************
	//*********************************************************************************************
	private ImagePlus readRecon(String directory, String name) 
	{
		if ((name == null) || (name == "")) return null;
		else
		{
			ImagePlus imp = load(directory, name);
			return imp;
		}
	} 
 
	//*********************************************************************************************

	private short readShort(DataInputStream input) throws IOException
	{
		if (!littleEndian) return input.readShort(); 
		byte b1 = input.readByte();
		byte b2 = input.readByte();
		return ( (short) (((b2 & 0xff) << 8) | (b1 & 0xff)) );
	}

	//*********************************************************************************************

	/**Reconstructs an ImagePlus sinogram or sinogram stack<br>
	 * Constructs the necessary files, calls Base_Recon.exe
	 * @param sinoImp the imagePlus sinogram or sinogram stack
	 * @return the reconstructed slice(s)
	 */
	private ImagePlus reconstruct(ImagePlus sinoImp, DialogSettings ds)
	{
		//Base_Recon.exe is a version of the BNL NSLS X2B reconstruction kit.
		//designed for batch reconstruction using file driven IO.
		imageImp = WindowManager.getImage(sinoCF.getChoice().getSelectedItem());

		String imgTitle = imageImp.getTitle();

		pluginDir = IJ.getDirectory("plugins");
		String startupDir = IJ.getDirectory("startup");
		copyFileLocation = pluginDir + "copySino.tif";
		sinoFileLocation = pluginDir + "sinoImage.sino";		//location to write the 16bit sinogram in .sino format
		specFileLocation = pluginDir + "specText.txt";			//location to write the text .spec file
		reconFileLocation = pluginDir + "TestImage.rcon";		//location for Base_Recon.exe to write the temporary reconstructed image in .rcon format
		timeFileLocation = pluginDir + "timeText.txt";			//location for Base_Recon.exe to write text time file in .time format
		statusFileLocation = startupDir + "Recon_Status.txt";	//Base_Recon.exe writes its status in the imageJ home directory
		exeFileLocation = pluginDir + "Base_Recon.exe";			//The required location for BaseRecon.exe

		reconCommand = "\"" + exeFileLocation+"\" " + "\"" + specFileLocation + "\" \"" + timeFileLocation + "\"";

		//Convert the 32bit sinogram to 16bit unsigned
		//We used 16-bit unsigned at X2B to save disk space.
		//The detectors never had more than 16-bit output.
		ImagePlus copyImp;
		if(sinoImp.getBitDepth()==32)
		{
			IJ.showStatus("Converting "+imgTitle+" to 16-bit");
			copyImp = convertTo16bitTifFile(pluginDir, sinoImp, "copySino.tif");
			if(copyImp == null)
			{
				IJ.showMessage("Copying to 16-bits");
				//deleteTempFiles();
				return null;
			}
		}
		else
		{
			copyImp=sinoImp.duplicate();
			IJ.saveAs(copyImp, "Tiff", pluginDir + "copySino.tif");
		}

		//IJ.showStatus("Writing Spec File");
		boolean ok = writeSpecFile(copyImp,ds);
		if(!ok)
		{
			IJ.showMessage("Error Writing Spec File");
			//deleteTempFiles();
			return null;
		}

		IJ.showStatus("Writing Sinogram File");
		if(!writeSinoFile(copyImp))
		{
			IJ.showMessage("Error Writing Sinogram File");
			//deleteTempFiles();
			return null;
		}

		IJ.showStatus("Loading Reconstruction");
		if(!callBaseRecon())
		{
			IJ.showMessage("Error Reconstructing");
			//deleteTempFiles();
			return null;
		}
		IJ.showStatus("Done");

		return  readRecon(pluginDir , "TestImage.rcon");

		//deleteTempFiles();
	}

	//*********************************************************************************************
	@Override
	public void run(String arg)
	{
		if(IJ.versionLessThan("1.53u"))// GenericDialog.resetCounters()
		{
			IJ.showMessage("ImageJ version 1.53u or better required.");
			return;
		}
		//Check that Base_Recon.exe is in the plugins directory
		String exeFilePath = IJ.getDirectory("plugins") + "Base_Recon.exe";
		File exeFile = new File(exeFilePath);
		if(!exeFile.exists())
		{
			IJ.showMessage("Reconstruction executable \"Base_Recon.exe\" not found\n"
					+ "in the ImageJ plugins folder");
			return;
		}

		//get a lit of open 32bit or 16bit images
		winTitles =getImages();
		if(winTitles.length==0)
		{
			IJ.showMessage("There are no 32bit or 16bit images present");
			return;
		}
		//Select the current one
		imageImp = WindowManager.getCurrentImage();
		//set the scale factor from the image ScaleFactor property
		//if no ScaleFactor property set defaults
		setScaleFactorFromSinoProperty(imageImp);

		//Track the Image selection menu
		ImagePlus.addImageListener(this);	

		//The dialog Choice items
		String reconDlogTitle = "FFT Parallel-Ray Reconstruction";
		String[] axisShift = {"- Half Pixel","- 4th Pixel","- 8th Pixel","- 16th Pixel","- 32nd Pixel","None","+ 32nd Pixel","+ 16th Pixel","+ 8th Pixel","+ 4th Pixel","+ Half Pixel",};
		String[] cartFilt = {"None","Rectangular","Hanning","Welch","Parzen"};
		String[] extWidth = {"None","To Half of Data","To 4th of Data","To 8th of Data","To 16th of Data"};
		String[] extShape = {"Half Sine","Linear","Half Gaussian","Exponential","1 over r","1 over r sq"};
		String[] fixRings = {"Don't Fix","with 1x3 median","that are 2x Avg","that are 4x Avg","that are 6x Avg","that are 8x Avg","that are 10x Avg"};
		Font myFont = new Font(Font.DIALOG, Font.BOLD, 12);
		Font warningFont = new Font(Font.DIALOG, Font.ITALIC+Font.BOLD, 12);

		//Load the DialogPlus methods for event handlers
		GenericDialogAddin gda = new GenericDialogAddin();

		gd = GUI.newNonBlockingDialog(reconDlogTitle);
		//gd = new GenericDialog(reconDlogTitle);
		gd.addDialogListener(this);
		gd.addMessage("Tomographic Reconstruction of parallel ray sinograms"
				+ "\n using direct Fourier inversion(Flannery et.al. Science 1987)",myFont);
		gd.setInsets(0, 20, 0);
		gd.addMessage("Opacity values for 32Bit images must be < 5.46"
				+ "\nand for 16Bit images maxOpacity/ScaleFactor must be < 5.46."
				+ "\nRescaling by other than the original scaleFactor"
				+ "\nproduces incorrect attenuations.",warningFont);

		gd.addChoice("Data_Source", winTitles, imageImp.getTitle());
		sinoCF = gda.getChoiceField(gd, null, "sinogram");
		gd.setInsets(0, 110, 0);
		gd.addButton("Refresh", this);
		refreshBF = gda.getButtonField(gd, "refresh");

		gd.addNumericField("Rotation_Axis:", imageImp.getWidth()/2,0);
		rotAxisNF = gda.getNumericField (gd, null, "rotAxis");

		gd.addChoice("Shift_Axis:", axisShift, axisShift[5]);
		axisShiftCF = gda.getChoiceField(gd, null, "axisShift");

		gd.addSlider("Cartesian_Cutoff:", 0, 1, 1, 0.1);
		cartCutSF = gda.getSliderField(gd, null, null, "cartCut");

		gd.addChoice("Cartesian_Filter:", cartFilt, cartFilt[0]);
		cartFilterCF = gda.getChoiceField(gd, null, "cartFilter");

		gd.addChoice("Extension_Width:", extWidth, extWidth[0]);
		extWidthCF = gda.getChoiceField(gd, null, "extWidth");

		gd.addChoice("Extension_Shape:", extShape, extShape[1]);
		extShapeCF = gda.getChoiceField(gd, null, "extShape");

		gd.addChoice("Fix_severe_rings:", fixRings, fixRings[0]);
		fixRingsCF = gda.getChoiceField(gd, null, "fixRings");

		gd.addSlider("Beam_Harden:", 0, 1, 0, .01);
		beamHardenSF = gda.getSliderField(gd, null, null, "beamHarden");
		
		gd.addMessage("If a sinogram has a ScaleFactor property it is automatically imported.");
		gd.setInsets(0, 20, 0);
		gd.addCheckbox("Use_Scale_Factor", useScaleFactor);
		useScaleFactorCBF = gda.getCheckboxField(gd, "useScaleFactor");
		gd.addNumericField("Scale_factor", scaleFactor);
		scaleFactorNF = gda.getNumericField (gd, null, "scaleFactor");
		

		gd.setInsets(5, 187, 0);
		gd.addButton("Reconstruct Test Slice", this);
		reconSliceBF = gda.getButtonField(gd, "reconSliceBtn");

		gd.setBackground(myColor);

		gd.setOKLabel("OK(reconstruct all)");

		gd.addHelp("https://lazzyizzi.github.io/CT_ReconPages/CtRecon.html");		

		gd.showDialog();

		if(gd.wasCanceled())
		{
			ImagePlus.removeImageListener(this);
			return;
		}
		else 
		{
			String item = sinoCF.getChoice().getSelectedItem();
			if(item!=null)
			{
				//If test image is open close it
				ImagePlus testImp = WindowManager.getImage("TestImage.rcon");
				if(testImp!=null)
				{
					testImp.changes=false;
					testImp.close();
				}
				imageImp=WindowManager.getImage(item);								
				double maxVal = imageImp.getStatistics().max;
				DialogSettings ds = getDialogSettings();
				switch (imageImp.getBitDepth())
				{
				case 32:
					if( maxVal > 5.46)
					{
						IJ.showMessage("Sinogram maximum value of "+maxVal+" is greater than 5.46 exceeding the reconstructors dynamic range.");
						return;
					}
					break;
				case 16:
					if(maxVal/ds.scaleFactor> 5.46)
					{
						IJ.showMessage("Sinogram maximum scale value of "+maxVal+"/"+ds.scaleFactor + "=" + maxVal/ds.scaleFactor +"\nis greater than 5.46 exceeding the reconstructors dynamic range.");
						return;
					}
					break;
				}
				
				ImagePlus reconImp = reconstruct(imageImp,ds);				
				displayReconImage(reconImp);
				ImagePlus.removeImageListener(this);
			}
		}
		return;
	}
	
	//*********************************************************************************************

	private void setImageProperties(ImagePlus srcImp, ImagePlus destImp)
	{
		Properties srcProps = srcImp.getImageProperties();
		ArrayList<String> destProps = new ArrayList<String>();
		if(srcProps!=null)
		{
			//Copy the projector properties
			Enumeration<Object> srcKey = srcProps.keys();
			Enumeration<Object> srcElem = srcProps.elements();
			while(srcKey.hasMoreElements())
			{
				destProps.add((String)srcKey.nextElement());
				destProps.add((String)srcElem.nextElement());
			}
		}
		//Add the Reconstruction Properties
		DialogSettings ds = getDialogSettings();
		destProps.add("Rotation Axis");
		destProps.add(String.valueOf(ds.rotationAxis));   
		destProps.add("Axis Shift");
		destProps.add(ds.axisShiftChoice);
		destProps.add("Cartesian Filter");
		destProps.add(ds.cartFilterChoice);
		destProps.add("Cartesian Cutoff");
		destProps.add(String.valueOf(ds.cartCutoff));			
		destProps.add("Extension Shape");
		destProps.add(ds.extShapeChoice);
		destProps.add("Extension Width");
		destProps.add(ds.extWidthChoice);
		destProps.add("Fix Rays");
		destProps.add(ds.fixRaysChoice);
		destProps.add("Fix Rings");
		destProps.add(ds.fixRingsChoice);
		destProps.add("SumRule");
		destProps.add(ds.sumRuleChoice);			
		destProps.add("BeamHardenWeight");			
		destProps.add(String.valueOf(ds.beamHardenWeight));

		String[] destPropsArr = destProps.toArray(new String[destProps.size()]);

		destImp.setProperties(destPropsArr);
	}
  
	//*********************************************************************************************
	//Write temporary sinogram file in CT_Recon .sino format, J. Anderson 1999
	private boolean writeSinoFile(ImagePlus copyImp)	
	{
		boolean ok = true;
		try
		{
			FileOutputStream sinoFileStream = new FileOutputStream(sinoFileLocation);
			
			int sinoWidth = copyImp.getWidth();
			int sinoHeight = copyImp.getHeight();
			ImageStack sinoStack = copyImp.getStack();
			int sinoSliceCnt = sinoStack.getSize();

			int headerOffset = 100;
			byte sinoHdrArray [] = new byte [headerOffset];
			intToArray(sinoHdrArray, 0, sinoSliceCnt);									// overwrite as needed
			intToArray(sinoHdrArray, 4, sinoWidth);										// overwrite as needed
			intToArray(sinoHdrArray, 8, 1);												// overwrite as needed
			intToArray(sinoHdrArray, 12, sinoHeight);									// overwrite as needed
			floatToArray(sinoHdrArray, 20, (float)copyImp.getCalibration().pixelWidth);	// overwrite as needed
			sinoFileStream.write(sinoHdrArray, 0,  sinoHdrArray.length);
			for (int slice = 1; slice <= sinoSliceCnt; slice++)
			{
				sinoFileStream.write(bytesForShorts((short [])sinoStack.getProcessor(slice).getPixels()), 0,sinoWidth * sinoHeight * 2);
				IJ.showProgress((double)slice/(double)sinoSliceCnt);
			}
			sinoFileStream.close();

		}
		catch (IOException e) 
		{
			IJ.error("An error occured while writing the file. " + sinoFileLocation);
			return false;
		}
		return ok;
	}
  
	//*********************************************************************************************

	private boolean writeSpecFile(ImagePlus copyImp, DialogSettings ds)
	{
		boolean ok = true;
		
		//Setup some values
		boolean sinoByteOrder = true;
		int sinoHeight = copyImp.getHeight();
		int sinoWidth = copyImp.getWidth();
		int sinoCount = copyImp.getStack().getSize();
		
		Calibration sinoCal = copyImp.getCalibration();
		FileInfo sinoInfo = copyImp.getOriginalFileInfo();
		try
		{
			FileWriter specWriter = new FileWriter(specFileLocation);
	
			//Descriptions of the sinogram data
			specWriter.write("Orig File Name: " + copyFileLocation +"\n");
			specWriter.write("Sino File Name: " + sinoFileLocation + "\n");
			specWriter.write("Spec File Name: " + specFileLocation + "\n");
			specWriter.write("Recon File Name: " + reconFileLocation + "\n");
			if (sinoByteOrder) specWriter.write("intelByteOrder: " + 1 + "'" + sinoByteOrder + "\n");
			else specWriter.write("intelByteOrder: " + 0 + "'" + sinoByteOrder + "\n");			
			specWriter.write("width: " + sinoWidth + "\n");
			specWriter.write("height: " + sinoHeight + "\n");
			specWriter.write("nImages: " + sinoCount + "\n");
			specWriter.write("unit: " + sinoCal.getUnit() + "\n");
			specWriter.write("pixelWidth: " + sinoCal.pixelWidth + "\n");
			specWriter.write("pixelHeight: " + sinoCal.pixelHeight + "\n");
			specWriter.write("pixelDepth: " + sinoCal.pixelDepth + "\n");
			specWriter.write("offset: " + sinoInfo.offset + "\n");
			specWriter.write("fileType: " + sinoInfo.fileType + "\n");

			//The user's reconstruction selections
			specWriter.write("Recon Method: 0'Direct Fourier\n");
			specWriter.write("Use Sum Rule:" + " " + ds.sumRuleIndex + "'" + ds.sumRuleChoice + "\n");
			specWriter.write("Left: 0" + "\n");
			specWriter.write("Right: " + sinoWidth + "\n");
			specWriter.write("Top: 0" + "\n");
			specWriter.write("Bottom: 0" + "\n");
			specWriter.write("Rotation Axis:" + " " + ds.rotationAxis + "\n");
			specWriter.write("Axis Position:" + " 1'Center\n");
			specWriter.write("Extension Shape:" + " " + ds.extShapeIndex + "'" + ds.extShapeChoice + "\n");
			specWriter.write("Cartesian Filter:" + " " + ds.cartFilterIndex  + "'" + ds.cartFilterChoice + "\n");
			specWriter.write("Interpolation:" + " 1'SemiBiCubic\n");//SemiBiCubic for optimal results, hard coded for this plugin
			specWriter.write("Pad Factor:" + " 2'2\n"); //Option 2 uses a pad factor of 4 for optimal results, hard coded for this plugin
//			The Kellogg ring supressor algorithm does not perform well
			specWriter.write("Ring Suppress:" + " 0'false\n");			
			specWriter.write("Fix severe rings:" + " " + ds.fixRingsIndex + "'" + ds.fixRingsChoice + "\n");
			if (ds.beamHardenWeight == 0.0) specWriter.write("Beam Harden:" + " 0'false\n");
			else specWriter.write("Beam Harden:" + " 1'true\n)");			
			specWriter.write("Fix severe rays:" + " " + ds.fixRaysIndex + "'" + ds.fixRaysChoice + "\n");			
			//Disabled for plugin, users can use imageJ tools to threshold the sinogram
			specWriter.write("Threshold Sino:" + " 0'false\n");			
			specWriter.write("Cartesian Cutoff:" + " " + ds.cartCutoff+ "\n");
			specWriter.write("Extension Width:" + " " + ds.extWidthIndex  + "'" + ds.extWidthChoice + "\n");
			if(ds.useScaleFactor) specWriter.write("Divide Sino by:" + scaleFactorNF.getNumber() + "\n");
			else specWriter.write("Divide Sino by: 6000" + "\n");
			specWriter.write("Shift Axis:" + " " + ds.axisShiftIndex + "'" + ds.axisShiftChoice + "\n");
			
			specWriter.write("Rotate Recon (deg):" + "\n");//No Rotation, hard coded for this plugin
			specWriter.write("weight (0-1):" + " " + ds.beamHardenWeight+ "\n");
			specWriter.close();
		}
		catch(IOException e)
		{
			IJ.log("An error occurred when creating " + specFileLocation);
			ok=false;
		}				
	return ok;	
	}

	//*********************************************************************************************

	private void setScaleFactorFromSinoProperty(ImagePlus imp)
	{
		String scaleFactorStr = imp.getProp("ScaleFactor");
		useScaleFactor = false;
		if(scaleFactorStr!=null) 
		{
			if(GenericDialogAddin.isNumeric(scaleFactorStr))
			{
				scaleFactor = Double.parseDouble(scaleFactorStr);
				useScaleFactor=true;
			}
		}
	}

}
