package CT_Tools;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.*;
import javax.imageio.ImageIO;
import ij.IJ;
import ij.ImagePlus;

public class ResourceReader {
	// Gtp40 helped

	public ResourceReader() {
	}

	// Method to read files in a folder
	/**Reads the 
	 * @return A list of file names in this jar file's /resources/documents folder
	 */
	public String[] getDocumentList() {
		ArrayList<String> names = new ArrayList<>();
		try {
			// Obtain the JAR file path
			URL jarPath = getClass().getProtectionDomain().getCodeSource().getLocation();
			String jarFilePath = jarPath.toURI().getPath();

			// Open the JAR file
			try (JarFile jarFile = new JarFile(jarFilePath)) {
				// List entries in the JAR file
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();

					// Check if the entry is in the desired resource folder
					if (name.startsWith("resources/documents") && !entry.isDirectory()) {
						name = name.substring(name.lastIndexOf("/") + 1);
						names.add(name);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return names.toArray(new String[names.size()]);
	}

//	private JarEntry getJarEntry(String path) {
//		// Obtain the JAR file path
//		URL jarPath = getClass().getProtectionDomain().getCodeSource().getLocation();
//		String jarFilePath = null;
//		JarEntry entry = null;
//		try {
//			jarFilePath = jarPath.toURI().getPath();
//		} catch (URISyntaxException e) {
//			IJ.log("jarPath.toURI().getPath() failed");
//			e.printStackTrace();
//			return entry;
//		}
//		IJ.log(jarFilePath);
//
//		// Open the JAR file
//		JarFile jarFile = null;
//		try {
//			jarFile = new JarFile(jarFilePath);
//		} catch (IOException e) {
//			IJ.log("jarFile = new JarFile(jarFilePath) failed");
//			e.printStackTrace();
//			return entry;
//		}
//		Enumeration<JarEntry> entries = jarFile.entries();
//
//		// List entries in the JAR file
//		while (entries.hasMoreElements()) {
//			entry = entries.nextElement();
//			String entryPath = entry.getName();
//			IJ.log("path="+path+ ", entryPath="+entryPath);
//			if (path.equals(entryPath)) {
//				IJ.log("MATCH path="+path+ ", entryPath="+entryPath);				
//				break;
//			}
//		}
//		try {
//			jarFile.close();
//		} catch (IOException e) {
//			IJ.log("jarFile.close() failed");
//			e.printStackTrace();
//			return entry;
//		}
//		IJ.log("getJarEntry Success="+entry.getName());
//		return entry;
//
//	}
	


	/**
	 * Method to read an image file<br>
	 * Use readImagePlusFile to return the image as an ImagePlus
	 * @param fileName the filename of the image
	 * @return an image or null if filename is not found
	 */
	public BufferedImage readImageFile(String fileName) {
		BufferedImage image = null;
		InputStream inputStream;

		inputStream = getClass().getResourceAsStream("/resources/images/" + fileName);
		if (inputStream != null) {
			try {
				image = ImageIO.read(inputStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return image;
	}

	
	/**Reads the names of image files in this jar file's resources/images folder
	 * @return A list of names
	 */
	public String[] getImageList() {
		ArrayList<String> names = new ArrayList<>();
		try {
			// Obtain the JAR file path
			URL jarPath = getClass().getProtectionDomain().getCodeSource().getLocation();
			String jarFilePath = jarPath.toURI().getPath();

			// Open the JAR file
			try (JarFile jarFile = new JarFile(jarFilePath)) {
				// List entries in the JAR file
				Enumeration<JarEntry> entries = jarFile.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					String name = entry.getName();

					// Check if the entry is in the desired resource folder
					if (name.startsWith("resources/images") && !entry.isDirectory()) {
						name = name.substring(name.lastIndexOf("/") + 1);
						names.add(name);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return names.toArray(new String[names.size()]);
	}
			
	/**Reads an image file from this resource<br>
	 * @param fileName the filename of the image
	 * @return An ImagePlus or null if filename is not found or the file type is not recognized
	 */
	public ImagePlus readImagePlusFile(String fileName) {
		BufferedImage buffImg = null;
		ImagePlus imp = null;
		InputStream inputStream;

		inputStream = getClass().getResourceAsStream("/resources/images/" + fileName);
		if (inputStream != null) {
			try {
				buffImg = ImageIO.read(inputStream);
				imp = new ImagePlus(fileName, buffImg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			IJ.log("inputStream is null");
		}
		try {
			inputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return imp;
	}

	/**Reads a text file from a this Java archive
	 * @param fileName
	 * @return The contents of the file as a string
	 */
	public String readTextFile(String fileName) {
		StringBuilder content = new StringBuilder();
		BufferedReader reader;

		InputStream inputStream = getClass().getResourceAsStream("/resources/documents/" + fileName);
		if (inputStream == null) {
			return null;
			// return "File Not Found";
		} else {
			try {
				reader = new BufferedReader(new InputStreamReader(inputStream));
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line).append("\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// IJ.log(content.toString());
		return content.toString();
	}
}
    	
     

	


