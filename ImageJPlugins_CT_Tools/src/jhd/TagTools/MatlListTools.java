package jhd.TagTools;

import ij.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.awt.event.ActionEvent;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import jhd.MuMassCalculator.*;
import jhd.TagTools.MatlListTools.TagData;

/**Class with methods and nested class for viewing and editing Materials.csv files
 * @author John
 *
 */
@SuppressWarnings("serial")
public class MatlListTools  extends JFrame 
{
	private JPanel contentPanel;
	private JTable table;
	private JButton btnClose;
	private JButton btnAddRow;
	private JButton btnInsertRow;
	private JButton btnDeleteRow;
	private JButton btnRevert;
	private JButton btnNewFile;
	private JButton btnSaveAs;
	
	
	Font myFont = new Font("Tahoma", Font.BOLD, 12);	
	String[] tblHdr = {"Tag", "Name", "Formula", "Density"};
	Object[][] tagObject;
	MuMassCalculator mmc = new MuMassCalculator();
	
	public class TagHdr
	{
		public TagHdr(String[] colHdr)
		{
			this.colHdr=colHdr;
		}
		public String[] colHdr;
		
		
	}

	public class TagData
	{
		public TagData(int matlTag, String matlName, String matlFormula, double matlGmPerCC)
		{
			super();
			this.matlTag = matlTag;
			this.matlName = matlName;
			this.matlFormula = matlFormula;
			this.matlGmPerCC = matlGmPerCC;
		}
		public int matlTag;
		public String matlName;
		public String matlFormula;
		public double matlGmPerCC;
	}
	
	public static class TagSet
	{
		public TagSet(TagHdr tagHdr, ArrayList<TagData> tagData)
		{
			this.tagHdr = tagHdr;
			this.tagData = tagData;
		}
		public TagSet() {};
		public TagHdr tagHdr;
		public ArrayList<TagData> tagData;
	}
	
	
	//Keep a list of JFrames opened by this
	ArrayList<JFrame> frames = new ArrayList<JFrame>();
	
	//*********************************************************************************************
	
	/**
	 * @param tagSet the tagSet to be filtered
	 * @param nameFilter return data with material names that begin with this string
	 * @return A filtered tagSet
	 */
	public TagSet filterTagData(TagSet tagSet, String nameFilter)
	{
		ArrayList<TagData> filteredTagData = new ArrayList<TagData>();
		TagHdr  filteredTagHdr = new TagHdr(tagSet.tagHdr.colHdr);
		String filterUC = nameFilter.toUpperCase();
		String matlNameUC;
		for(TagData tagData : tagSet.tagData)
		{
			matlNameUC = tagData.matlName.toUpperCase();
			if(matlNameUC.startsWith(filterUC, 0))
			{
				filteredTagData.add(new TagData(tagData.matlTag,tagData.matlName,tagData.matlFormula,tagData.matlGmPerCC));
			}
		}
		TagSet filteredTagSet = new TagSet(filteredTagHdr,filteredTagData);		
		
		return filteredTagSet;
	}
	
	public String[] getTagSetMatlFormulasAsArray(TagSet tagSet)
	{
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);		
		String[] formulas = new String[tagSet.tagData.size()];
		for(int i=0;i<tagData.length;i++)
		{
			formulas[i] = tagData[i].matlFormula;
		}
		return formulas;
	}

	public String[] getTagSetMatlNamesAsArray(TagSet tagSet)
	{
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);		
		String[] matlNames = new String[tagSet.tagData.size()];
		for(int i=0;i<tagData.length;i++)
		{
			matlNames[i] = tagData[i].matlName;
		}
		return matlNames;
	}
	
	public double[] getTagSetMatlGmPerccAsArray(TagSet tagSet)
	{
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);		
		double[] density = new double[tagSet.tagData.size()];
		for(int i=0;i<tagData.length;i++)
		{
			density[i] = tagData[i].matlGmPerCC;
		}
		return density;
	}

	public int[] getTagSetMatlTagAsArray(TagSet tagSet)
	{
		TagData[] tagData = tagSet.tagData.toArray(new TagData[tagSet.tagData.size()]);		
		int[] tag = new int[tagSet.tagData.size()];
		for(int i=0;i<tagData.length;i++)
		{
			tag[i] = tagData[i].matlTag;
		}
		return tag;
	}

	/**
	 * @param path The path to the Materials file
	 * @return a tagSet used to map image components to a selected composition and density.
	 */
	public TagSet loadTagFile(String path)
	{

		TagSet tagSet = null;
		String matlStr = IJ.openAsString(path);

		if(matlStr.equals("Error: file not found"))
		{
			path = IJ.getFilePath("Dummy path");
		}

		//Open the file and read the data
		if(path!=null)
		{
			matlStr = IJ.openAsString(path);
			//Split materials at newline
			String[] rowArr = matlStr.split("\n");
			String[] hdrItems = rowArr[0].split(",");
			
			if(hdrItems[0].equals("Tag"))
			{
				TagHdr  tagHdr = new TagHdr(hdrItems);
				ArrayList<TagData> tagData = new ArrayList<TagData>();
				for(int i=1;i<rowArr.length;i++)
				{
					String[] rowItems = rowArr[i].split(",");
					tagData.add(new TagData(Integer.parseInt(rowItems[0]),rowItems[1],rowItems[2],Double.parseDouble(rowItems[3])));
				}
				tagSet = new TagSet(tagHdr,tagData);
			}
		}
		return tagSet;	
	}
	
	//********************************************************************************************
	
	public boolean tagsToLinearAttn(float[] imageData, MatlListTools.TagSet tagSet, double keV)
	{		
		//tagsToMuLin(pixels,myTags,keV);
		int[] tagArr = getUniqueTags(imageData);
		
		//check if tagArr is bigger than the tagList
		//if it is, it is probably not a tag image
		if(tagArr.length > tagSet.tagData.size())
		{
			IJ.showMessage("Error", "There are more tags in the input image than\n"
					+ "there are tags in the materials list.");
			return false;
		}
		
		// Find the position of each tag in the TagData list
		int[] tagIndex = new int[tagArr.length];
		for(int j=0;j<tagArr.length;j++)
		{
			tagIndex[j]=-1;//-1 indicates a match was not found, zero is a valid tag index
			int i=0;
			for(TagData td :  tagSet.tagData)
			{
				if(tagArr[j]==td.matlTag)
				{
					tagIndex[j] = i;
				}
				i++;
			}
		}
				
		//Get biggest matlTag
		int  maxTag = Integer.MIN_VALUE;
		for(int i = 0 ; i<tagArr.length;i++)
		{
			if(tagArr[i] > maxTag) maxTag = tagArr[i];
		}
		
		//Create an array to hold the muMass values
		float[] muMassArr = new float[maxTag+1];
		
		//Set the muMass values for each tag
		for(int i=0;i<tagIndex.length;i++)
		{
			if(tagIndex[i]>=0)
			{
				String 	formula= tagSet.tagData.get(tagIndex[i]).matlFormula;	
				double gmPerCC = tagSet.tagData.get(tagIndex[i]).matlGmPerCC;
				double muLin = mmc.getMuMass(formula, keV/1000, "TotAttn")*gmPerCC;			
				muMassArr[tagArr[i]]= (float) muLin;
				System.out.println("formula="+ formula+", gmPerCC =" + gmPerCC + ", muLin="+muLin);
			}
			else
			{
				IJ.log("Tag " +tagArr[i]+ " was not found in the materials list");
			}
		}
		
		for(int i=0;i<imageData.length;i++)
		{
			try {
				imageData[i] = muMassArr[(int)imageData[i]];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	//********************************************************************************************

	/**Scans an float image for unique pixel tag values
	 * @param imageData
	 * @return a list of integer tag values used in the image
	 */
	private int[] getUniqueTags( float[] imageData)
	{
		// from https://www.javatpoint.com/find-unique-elements-in-array-java

		HashSet<Float> hashset = new HashSet<>();   
		for (int i = 0; i < imageData.length; i++)   
		{   
			if (!hashset.contains(imageData[i]))   
			{   
				hashset.add(imageData[i]);   
			}   
		}

		//Convert hash to float array
		Float[] tagList = hashset.toArray(new Float[hashset.size()]);
		int[] tagArr = new int[hashset.size()];
		int i=0;
		//Convert to primitive array
		for(Float fObj : tagList)
		{
			tagArr[i]=fObj.intValue();
			i++;
		}

		//print hash set that contains distinct element  
		return tagArr;
	}
		
	//*********************************************************************************************

	/**Opens a JFrame Materials editor window
	 * @param title The window title
	 * @param tagSet The tagSet to be edited
	 */
	public void editTagFile(String title, TagSet tagSet)
	{
		if(tagSet!=null)
		{
			initEditorUI();
			initEditorEventHandlers();
			tagSetToTable(tagSet);
			setVisible(true);
			setTitle(title);
			WindowManager.addWindow(MatlListTools.this);
			frames.add(MatlListTools.this);
		}	
	}
	
	//The background color for the GenericDialog just because I like it.
	final Color myColor = new Color(240,230,190);//slightly darker than buff
	//*********************************************************************************************
	/**Create the frame.*/
	public void initEditorUI()
	{
		setIconImage(Toolkit.getDefaultToolkit().getImage(MatlListTools.class.getResource("/jhd/TagTools/LazzyIzzi-32.png")));
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 541, 465);
		setFont(myFont);
		//setLocation(200,200);
		setLocationRelativeTo(null);//Move to center of screen
		
		contentPanel = new JPanel();
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
		contentPanel.setLayout(null);
		contentPanel.setBackground(myColor);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 10, 500, 300);
		contentPanel.add(scrollPane);
		
		table = new JTable();		
		scrollPane.setViewportView(table);
		
		table.setFont(myFont);
		table.getTableHeader().setFont(myFont);
	    table.setAutoCreateRowSorter(true); // sorting of the rows on a particular column
		
		btnAddRow = new JButton("Add Row");
		btnAddRow.setToolTipText("Add a new row to the bottom of the table.");
		btnAddRow.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnAddRow.setBounds(10, 331, 120, 23);
		contentPanel.add(btnAddRow);
		
		btnInsertRow = new JButton("Insert Row");
		btnInsertRow.setToolTipText("Insert a new row above the selected row");
		btnInsertRow.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnInsertRow.setBounds(140, 331, 120, 23);
		contentPanel.add(btnInsertRow);
		
		btnDeleteRow = new JButton("Delete Row");
		btnDeleteRow.setToolTipText("Delete the currently selected row.");
		btnDeleteRow.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnDeleteRow.setBounds(270, 331, 120, 23);
		contentPanel.add(btnDeleteRow);
		
		btnSaveAs = new JButton("Save As");
		btnSaveAs.setToolTipText("Save a copy of the materials list.");
		btnSaveAs.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnSaveAs.setBounds(10, 361, 120, 23);
		contentPanel.add(btnSaveAs);
		
		btnRevert = new JButton("Revert");
		btnRevert.setToolTipText("Restore the current file to its original list.");
		btnRevert.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnRevert.setBounds(140, 361, 120, 23);
		contentPanel.add(btnRevert);
		
		btnClose = new JButton("Close");
		btnClose.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnClose.setBounds(421, 386, 89, 34);
		contentPanel.add(btnClose);
		
		btnNewFile = new JButton("Read File");
		btnNewFile.setToolTipText("Read another materials list.");
		btnNewFile.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnNewFile.setBounds(270, 361, 120, 23);
		contentPanel.add(btnNewFile);
	}
	
	//*********************************************************************************************
	
	private void initEditorEventHandlers()
	{
		btnAddRow.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				DefaultTableModel tm = (DefaultTableModel)table.getModel();
				//tm.addRow(new Object[] {null,null,null,null});
				try {
					tm.addRow(new Object[] {null,null,null,null});
		            int lastIndex =table.getRowCount()-1;
		            table.changeSelection(lastIndex, 0,false,false);
				} catch (Exception e1)
				{
					e1.printStackTrace();
				}
			}
		});
		
		//------------------------------------------------------------
		
		btnInsertRow.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int tableRow = table.getSelectedRow();
				
				DefaultTableModel tm = (DefaultTableModel)table.getModel();
				tm.fireTableDataChanged();
					try {
					int modelRow = table.convertRowIndexToModel(tableRow);
					//Model row is correct but the row is always added at row 0???
//					IJ.log("Model Row=" + modelRow);
//					IJ.log("Table Row=" + tableRow);
					tm.insertRow(tableRow,new Object[] {null,null,null,null});
					
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(btnInsertRow, "A row selection is required.\nThe new row will appear above the selection.");
				}
			}
		});

		//------------------------------------------------------------
		
		btnDeleteRow.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int[] selectedRows = table.getSelectedRows();
				DefaultTableModel tm = (DefaultTableModel)table.getModel();
				try
				{
					for(int i = selectedRows.length-1; i>=0;i--)
					{
						int j = table.convertRowIndexToModel(selectedRows[i]);
						tm.removeRow(j);
					}
				}
				catch (Exception e1)
				{
					JOptionPane.showMessageDialog(btnDeleteRow, "A row selection is required.");
				}
			}
		});

		//------------------------------------------------------------
		
		btnRevert.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				DefaultTableModel tm = (DefaultTableModel)table.getModel();
				try {
					tm.setDataVector(tagObject,tblHdr);
					table.getColumnModel().getColumn(0).setPreferredWidth(20);
					table.getColumnModel().getColumn(1).setPreferredWidth(120);
					table.getColumnModel().getColumn(2).setPreferredWidth(220);				
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		//------------------------------------------------------------
		
		btnNewFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				//String path = IJ.getFilePath(" View Materials file");
				String dir = IJ.getDirectory("plugins");
				//String dirPath = dir + "DialogData";
				//File dirFile = new File(dirPath);
				
				String filePath = dir + "DialogData\\DefaultMaterials.csv";
				//System.out.println(filePath);
				File file = new File(filePath);
				
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				fc.setSelectedFile(file);
				
				int returnVal = fc.showOpenDialog(fc);
					
				if(returnVal == JFileChooser.APPROVE_OPTION)
				{
					filePath = fc.getSelectedFile().getPath();
					//System.out.println(filePath);
					TagSet tagSet = loadTagFile(filePath);
					tagSetToTable(tagSet);
					int last = filePath.lastIndexOf('\\')+1;
					String fileName =filePath.substring(last);
					setTitle(fileName);
				}
			}
		});

		//------------------------------------------------------------
		
		btnSaveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e)
			{
				TagSet tagSet = tableToTagSet();
				
				if(tagSet!=null)
				{
					String tagStr,nameStr,formulaStr,densityStr;
					tagStr = table.getColumnName(0);
					nameStr = table.getColumnName(1);
					formulaStr = table.getColumnName(2);
					densityStr = table.getColumnName(3);
					tagStr = tagStr + ',' + nameStr +',' + formulaStr + ',' + densityStr +'\n';

					for(TagData td : tagSet.tagData)
					{
						tagStr = tagStr + td.matlTag +',' + td.matlName + ',' + td.matlFormula + ',' + td.matlGmPerCC + '\n';						
					}
					
					String dir = IJ.getDirectory("plugins");					
					String filePath = dir + "DialogData\\DefaultMaterials.csv";
					//System.out.println(filePath);
					File file = new File(filePath);					
					JFileChooser fc = new JFileChooser();
					fc.setSelectedFile(file);
					//fc.setApproveButtonText("Save");
					fc.setDialogType(JFileChooser.SAVE_DIALOG);
					
					//fc.showSaveDialog(MatlListTools2.this);
					int returnVal = fc.showSaveDialog(MatlListTools.this);
					
					if(returnVal == JFileChooser.APPROVE_OPTION)
					{
						file = fc.getSelectedFile();
						file.getPath();
						try {
							FileWriter tagWriter = new FileWriter(file.getPath());
							tagWriter.write(tagStr);
							tagWriter.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					
					}
				}
			}
		});


		//------------------------------------------------------------
		
		btnClose.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				//Change this if running within an ImageJ plugin
				//System.exit(0);
				JFrame  frame = MatlListTools.this;
				WindowManager.removeWindow(frame);
				frame.dispose();				
			}
		});
}
	
	//*********************************************************************************************
	
	private void tagDataTo2dObj(TagSet tagSet)
	{
		if(tagSet!=null)
		{
			int tagRows = tagSet.tagData.size();
			tagObject = new Object[tagRows][4];
			int row=0;
			for(TagData td : tagSet.tagData)
			{
				tagObject[row][0] = td.matlTag;
				tagObject[row][1] = td.matlName;
				tagObject[row][2] = td.matlFormula;
				tagObject[row][3] = td.matlGmPerCC;
				row++;
			}
		}
	}
	
	//*********************************************************************************************
	
	private void tagSetToTable(TagSet tagSet)
	{
		if(tagSet!=null)
		{
			tagDataTo2dObj(tagSet);
			try
			{
				table.setModel(new DefaultTableModel(tagObject,tblHdr)
				{
					@SuppressWarnings("rawtypes")
					Class[] columnTypes = new Class[] {Integer.class, String.class, String.class, Double.class};
					@SuppressWarnings({ "rawtypes", "unchecked" })
					public Class getColumnClass(int columnIndex)
					{
						return columnTypes[columnIndex];
					}
					//boolean[] columnEditables = new boolean[] {false, false, false, false};
					boolean[] columnEditables = new boolean[] {true, true, true, true};
					public boolean isCellEditable(int row, int column)
					{
						return columnEditables[column];
					}
				});
			} 
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			//maybe try https://tips4java.wordpress.com/2008/11/10/table-column-adjuster/
			table.getColumnModel().getColumn(0).setPreferredWidth(20);
			table.getColumnModel().getColumn(1).setPreferredWidth(120);
			table.getColumnModel().getColumn(2).setPreferredWidth(220);
		}
		else
		{
			JOptionPane.showMessageDialog(null, "The requested tags failed to load");
		}
	}
		
	//*********************************************************************************************

	private TagSet tableToTagSet()
	{
		
//		TagHdr  tagHdr = new TagHdr(hdrItems);
//		ArrayList<TagData> tagData = new ArrayList<TagData>();
//		for(int i=1;i<rowArr.length;i++)
//		{
//			String[] rowItems = rowArr[i].split(",");
//			tagData.add(new TagData(Integer.parseInt(rowItems[0]),rowItems[1],rowItems[2],Double.parseDouble(rowItems[3])));
//		}
//		tagSet = new TagSet(tagHdr,tagData);

		TagSet tagSet = null;
		int rowCnt = table.getRowCount();
		//int colCnt = table.getColumnCount();
		
		String[] colLabels = new String[4];		
		for(int i=0;i<4;i++)
		{
			colLabels[i] = table.getColumnName(i);			
		}
		
		TagHdr tagHdr = new TagHdr(colLabels);
		
		ArrayList<TagData> tagData = new ArrayList<TagData>();
		for(int row=0;row<rowCnt;row++)
		{
			tagData.add(new TagData((int) table.getValueAt(row, 0),
									(String) table.getValueAt(row, 1),
									(String) table.getValueAt(row,2),
									(double) table.getValueAt(row,3)));
		}
	
		tagSet = new TagSet(tagHdr,tagData);

		return tagSet;
	}
	
	//*********************************************************************************************
}

