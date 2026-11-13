package jhd.CT_Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**A Wrapper class for object serialization*/
public class Serializer
{
	/**Saves an object in serialized (binary) format
	 * @param theObject Reference to a serializable object
	 * @param path An absolute file save path. File name should end in .ser
	 * @return True if successful
	 */
	public boolean SaveObjectAsSerialized(Object theObject, String path)
	{
		File objFile = new File(path);
		if(!objFile.exists())			
		{
			//Create a path for the output file
			objFile = new File(path);
			objFile.getParentFile().mkdirs();
		}

		try
		{
			FileOutputStream fileOut = new FileOutputStream(path);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(theObject);
			out.close();
			fileOut.close();
			//System.out.printf("Serialized data is saved in " + path);
		}
		catch (IOException i)
		{
			i.printStackTrace();
			return false;
		}

		return true;
	}

	//*********************************************************************************

	/** Restores an object from serialized (binary) format
	 * @param path An absolute file save path. File name should end in .ser
	 * @return The object or null if failed to read.
	 */
	public Object ReadSerializedObject(String path)
	{

		Object theObject = null;
		
		File objFile = new File(path);
		if(objFile.exists())
		{
			try
			{
				FileInputStream fileIn = new FileInputStream(path);
				ObjectInputStream in = new ObjectInputStream(fileIn);
				theObject = in.readObject();
				in.close();
				fileIn.close();
			}
			catch (IOException i)
			{
				i.printStackTrace();
			}
			catch (ClassNotFoundException c)
			{
				System.out.println("DialogSettings class not found");
				c.printStackTrace();
			}
		}
		return theObject;
	}
}
