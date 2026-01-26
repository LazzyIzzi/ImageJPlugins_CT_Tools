package tagTools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
//import javax.swing.event.RowSorterEvent;
//import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import ij.IJ;
import ij.WindowManager;
import jhd.MuMassCalculator.MuMassCalculator;
import tagTools.TagListTools.*;

public class TagListEditor{

	static Color myColor = new Color(240, 230, 190);// slightly darker than buff
	Font myFont = new Font("Tahoma", Font.BOLD, 12);
	String helpURL = "https://lazzyizzi.github.io/CT_ReconPages/MaterialsEditor.html";	
	MuMassCalculator mmc = new MuMassCalculator();
	TagListTools mlt = new TagListTools();
	
	public TagListEditor() {
	}
	
	/**Declare as globals the names of the Materials List Editor UI*/
	JTable myTable;
	DefaultTableModel myTableModel;
	private JFrame myJFrame;
	private JPanel myPanel;
	private JButton btnCancel,btnAddRow,btnInsertRow,btnDeleteRow,btnRevert,btnSave,btnHelp;
	private JLabel msgLabel;

	/**Declare a variable to hold the tags*/
	private TagSet tagSet;
	/**Declare a 2D Object required by the JTable variable to hold the tags*/
	Object [][] tagDataObj;

	/**Displays the DefaultMaterials.csv file data in a table for editing
	 * 
	 */
	public void editMaterialsList(String tagFilePath) {

		tagSet = mlt.readTagSetFile(tagFilePath);
		if (tagSet == null) {
			IJ.error("Error loading tag file");
			return;
		}

		tagDataObj = tagDataTo2dObj(tagSet);

		// Create the JFrame (Window) to hold the editor
		myJFrame = new JFrame("Materials Editor");
//		myJFrame.setIconImage(Toolkit.getDefaultToolkit()
//				.getImage(MaterialListEditor.class.getResource("/jhd/TagTools/LazzyIzzi-32.png")));
		myJFrame.setIconImage(Toolkit.getDefaultToolkit()
				.getImage(TagListEditor.class.getResource("/tagTools/LazzyIzzi-32.png")));
		myJFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		//myJFrame.setMaximumSize(new Dimension(541, 465));
		myJFrame.setResizable(false);
		myJFrame.setBounds(100, 100, 541, 465);
		myJFrame.setFont(myFont);
		myJFrame.setLocationRelativeTo(null);// Move to center of screen

		// Create a Panel to hold the components of the editor
		myPanel = new JPanel();
		myPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		myPanel.setLayout(null);// Use absolute positioning
		myPanel.setBackground(myColor);

		// Create a JTable containing the Materials tag information
		myTable = tagSetToTable(tagSet);
		myTable.setFont(myFont);
		myTable.getTableHeader().setFont(myFont);
		myTable.getTableHeader().setReorderingAllowed(false);

		// Create a scrollPane, put theJTable in to it and add it to the panel
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 10, 500, 300);
		scrollPane.setViewportView(myTable);
		myPanel.add(scrollPane);

		// Post a warning about blank cells "KISS"
		msgLabel = new JLabel("Do not leave any cells blank.");
		msgLabel.setFont(new Font("Tahoma", Font.BOLD, 12));
		msgLabel.setBounds(150, 305, 250, 30);
		myPanel.add(msgLabel);

		// Add the buttons to the bottom of the panel
		btnAddRow = new JButton("Add Row");
		btnAddRow.setToolTipText("Add a new row to the bottom of the table.");
		btnAddRow.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnAddRow.setBounds(10, 331, 120, 23);
		myPanel.add(btnAddRow);

		btnInsertRow = new JButton("Insert Row");
		btnInsertRow.setToolTipText("Insert a new row above the selected row");
		btnInsertRow.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnInsertRow.setBounds(140, 331, 120, 23);
		myPanel.add(btnInsertRow);

		btnDeleteRow = new JButton("Delete Row");
		btnDeleteRow.setToolTipText("Delete the currently selected row.");
		btnDeleteRow.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnDeleteRow.setBounds(270, 331, 120, 23);
		myPanel.add(btnDeleteRow);

		btnRevert = new JButton("Revert");
		btnRevert.setToolTipText("Restore the current file to its original list.");
		btnRevert.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnRevert.setBounds(400, 331, 110, 23);
		myPanel.add(btnRevert);

		btnHelp = new JButton("Help");
		btnHelp.setToolTipText("Opens the Help page in your default Browser");
		btnHelp.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnHelp.setBounds(10, 380, 120, 34);
		myPanel.add(btnHelp);

		btnSave = new JButton("Save & Close");
		btnSave.setToolTipText("Saves the current materials list and Closes this editor.");
		btnSave.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnSave.setBounds(270, 380, 120, 34);
		myPanel.add(btnSave);

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(new Font("Tahoma", Font.BOLD, 12));
		btnCancel.setBounds(400, 380, 110, 34);
		myPanel.add(btnCancel);

		// Load the panel into the window and show it to the user
		myJFrame.setContentPane(myPanel);
		myJFrame.setVisible(true);

		// Event Listeners
		myTable.getModel().addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				int row = e.getFirstRow();
				int col = e.getColumn();

				// Only notify when a cell is updated (not when structure changes)
				if (e.getType() == TableModelEvent.UPDATE && row >= 0 && col >= 0) {
					Object newValue = myTable.getModel().getValueAt(row, col);

					switch (col) {
					case 0:// tags
						// Only allow positive integers. May change to any numerical value
						int dupRow = checkTagsColumnForDuplicates();

						if (dupRow > 0) {
							String dupName = (String) myTable.getModel().getValueAt(dupRow, 1);
							JOptionPane.showMessageDialog(myJFrame,
									"Tag " + newValue + "  duplicates row " + dupRow + " " + dupName,
									"Table Data Changed", JOptionPane.INFORMATION_MESSAGE);
							btnSave.setEnabled(false);
						} else if ((Integer) newValue < 0) {
							JOptionPane.showMessageDialog(myJFrame, " " + newValue + " Is not a valid Tag",
									"Table Data Changed", JOptionPane.INFORMATION_MESSAGE);
							btnSave.setEnabled(false);
						} else {
							btnSave.setEnabled(true);
						}
						break;

					case 1:// Names
						break;
					case 2:// formulas
						if (newValue instanceof String) {
							String str = (String) newValue;
							if (str.isEmpty()) {
								JOptionPane.showMessageDialog(myJFrame, "Please enter a valid X:1:Y:1 formula",
										"Table Data Changed", JOptionPane.INFORMATION_MESSAGE);
								btnSave.setEnabled(false);
							} else {
								double formulaWeight = mmc.getFormulaWeight(str);
								if (formulaWeight == -1) {
									JOptionPane.showMessageDialog(myJFrame,
											" " + newValue + " Is not a valid X:1:Y:1 formula", "Table Data Changed",
											JOptionPane.INFORMATION_MESSAGE);
									btnSave.setEnabled(false);
								} else {
									btnSave.setEnabled(true);
								}
							}
						}
						break;
					case 3:// densities
						if ((Double) newValue < 0) {
							JOptionPane.showMessageDialog(myJFrame, " " + newValue + " Is not a valid density",
									"Table Data Changed", JOptionPane.INFORMATION_MESSAGE);
							btnSave.setEnabled(false);
						} else {
							btnSave.setEnabled(true);
						}
						break;
					}
				}
			}
		});

		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				WindowManager.removeWindow(myJFrame);
				myJFrame.dispose();
			}
		});

		btnAddRow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DefaultTableModel tm = (DefaultTableModel) myTable.getModel();
				try {
					tm.addRow(new Object[] { null, null, null, null });
					tm.fireTableDataChanged();
					int lastIndex = myTable.getRowCount() - 1;
					myTable.changeSelection(lastIndex, 0, false, false);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		btnDeleteRow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = myTable.getSelectedRows();
				DefaultTableModel tm = (DefaultTableModel) myTable.getModel();
				try {
					for (int i = selectedRows.length - 1; i >= 0; i--) {
						int j = myTable.convertRowIndexToModel(selectedRows[i]);
						tm.removeRow(j);
						tm.fireTableDataChanged();
					}
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(btnDeleteRow, "A row selection is required.");
				}
			}
		});

		btnInsertRow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int selectedRow = myTable.getSelectedRow();

				DefaultTableModel tm = (DefaultTableModel) myTable.getModel();
				try {
					tm.insertRow(selectedRow, new Object[] { null, null, null, null });
					tm.fireTableDataChanged();
					// tm.fireTableRowsUpdated(0, myTable.getRowCount());
					myTable.changeSelection(selectedRow, 0, false, false);
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(btnInsertRow,
							"A row selection is required.\nThe new row will appear above the selection.");
				}
			}
		});

		btnRevert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DefaultTableModel tm = (DefaultTableModel) myTable.getModel();
				try {
					String[] tblHdr = { "Tag", "Name", "X:1:Y:1 Formula", "gm/cc" };

					tm.setDataVector(tagDataObj, tblHdr);
					myTable.getColumnModel().getColumn(3).setCellRenderer(new DoubleFormatter("#.####"));
					myTable.getColumnModel().getColumn(0).setPreferredWidth(35);
					myTable.getColumnModel().getColumn(1).setPreferredWidth(120);
					myTable.getColumnModel().getColumn(2).setPreferredWidth(220);
					tm.fireTableDataChanged();
					btnSave.setEnabled(true);

				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openURL(helpURL);
			}
		});

		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!tableHasBlanks()) {

					TagSet tagSet = null;
					try {
						tagSet = tableToTagSet();
					} catch (Exception e1) {
						// e1.printStackTrace();
						IJ.error("Save Button" + e1.getMessage());
					}

					if (tagSet != null) {
						if (mlt.writeTagSetFile(tagSet,tagFilePath)) {
							myJFrame.dispose();
						}
					}
				}
			}
		});
	}
		
	/**Checks the "Tags" column for duplicate values 
	 * and reports the first matching row number
	 * @return
	 */
	private int checkTagsColumnForDuplicates() {
		int rows = myTable.getRowCount();
		Object oVal;
		Integer iVal=0;
		int dupRow=0;
		ArrayList<Integer> tags = new ArrayList<Integer>();
		
		try {
			for (int row = 0; row < rows; row++) {
				oVal = myTable.getValueAt(row, 0);
				if (oVal instanceof Integer) {
					iVal = (Integer) oVal;
				}
				tags.add(iVal);
			}
			tags.sort(null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			for(int row =1;row<rows;row++) {
				Integer dnVal = tags.get(row);
				Integer upVal = tags.get(row-1);
						
				if(upVal.equals(dnVal)) {
					dupRow = row;
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return dupRow;
	}
	
	private boolean tableHasBlanks() {
		
		
		boolean hasBlanks = false;
		int rowCnt = myTable.getRowCount();
		int colCnt = myTable.getColumnCount();
		Object oVal;
		String sVal;
		int row=0,col=0;
		for(row = 0;row<rowCnt;row++) {
			for(col=0;col<colCnt;col++) {
				oVal= myTable.getValueAt(row, col);
				if(oVal==null) {
					hasBlanks = true;
					break;
					
				}
				if(oVal instanceof String) {
					sVal = (String) oVal;
				}
				else {
					sVal = oVal.toString();
				}
				if(sVal.isEmpty() || sVal.trim().length()==0) {
					hasBlanks = true;
					break;
				}
			}
			if(hasBlanks) break;
		}
		if(hasBlanks) {
			JOptionPane.showMessageDialog(null, "The cell at row "+row+" column "+ col+" is blank.\n"
					+ "Please edit the table so that no cells are blank");			
		}
		return hasBlanks;
	}

	/**Converts the table data into a the TagSet format
	 * @return a TagSet containing the current table data
	 */
	private TagSet tableToTagSet() {
		TagSet tagSet = null;
		int rowCnt = myTable.getRowCount();

		String[] colLabels = new String[4];
		for (int i = 0; i < 4; i++) {
			colLabels[i] = myTable.getColumnName(i);
		}

		TagHdr tagHdr = mlt.new TagHdr(colLabels);
		//tagSet.tagHdr.colHdr = colLabels;

		ArrayList<TagData> tagData = new ArrayList<TagData>();
		for (int row = 0; row < rowCnt; row++) {
			
			Object oGmPerCC = myTable.getValueAt(row, 3); //returns Double on original data and String after editing the cell!!
			double dGmPerCC=0;
			if(oGmPerCC instanceof Double) {
				dGmPerCC = (double) oGmPerCC;				
			}
			else if (oGmPerCC instanceof String) {
				String sGmPerCC = (String) oGmPerCC;
				dGmPerCC = Double.parseDouble(sGmPerCC);				
			}
					
			tagData.add(mlt.new TagData((int) myTable.getValueAt(row, 0), (String) myTable.getValueAt(row, 1),
					(String) myTable.getValueAt(row, 2),  dGmPerCC));
		}

		tagSet = mlt.new TagSet(tagHdr, tagData);

		return tagSet;
	}

	/**Opens a URL in the default browser
	 * @param url the URL to open
	 */
	private static void openURL(String url) {
		// see
		// https://www.javathinking.com/blog/how-to-open-url-in-default-webbrowser-using-java/
		try {
			// Step 1: Check if Desktop is supported
			if (!Desktop.isDesktopSupported()) {
				System.err.println("Desktop is not supported on this platform.");
				return;
			}

			Desktop desktop = Desktop.getDesktop();

			// Step 2: Check if browsing is supported
			if (!desktop.isSupported(Desktop.Action.BROWSE)) {
				System.err.println("Browsing is not supported on this platform.");
				return;
			}

			// Step 3: Convert URL string to URI and open
			URI uri = new URI(url);
			desktop.browse(uri);
			System.out.println("URL opened successfully in default browser.");

		} catch (Exception e) {
			System.err.println("Failed to open URL: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**Converts the Data portion of a tagSet to a 2D Object required by the JTable
	 * @param tagSet a TagSet data type
	 * @return a 2D Object required by the JTable
	 */
	private Object[][] tagDataTo2dObj(TagSet tagSet) {
		Object[][]tagObject = null;
		if (tagSet != null) {
			int tagRows = tagSet.tagData.size();
			tagObject = new Object[tagRows][4];
			int row = 0;
			for (TagData td : tagSet.tagData) {
				tagObject[row][0] = td.matlTag;
				tagObject[row][1] = td.matlName;
				tagObject[row][2] = td.matlFormula;
				tagObject[row][3] = td.matlGmPerCC;
				row++;
			}
		}
		return tagObject;
	}
	
	class DoubleFormatter extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;
		private DecimalFormat decimalFormat;

	    public DoubleFormatter(String pattern) {
	        this.decimalFormat = new DecimalFormat(pattern);
	    }

	    @Override
	    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	        if (value instanceof Double) {
	            value = decimalFormat.format((Double) value);
	        }
	        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	    }
	}
	
	/**Creates a JTable and loads it with the TagSet data 
	 * @param tagSet the tagSet Data
	 * @return a JTable containing the Materials tag information
	 */
	private JTable tagSetToTable(TagSet tagSet) {
		JTable theTable = null;

		if (tagSet != null) {
			//String[] tblHdr = { "Tag", "Name", "X:1:Y:1 Formula", "gm/cc" };
			DefaultTableModel model = new DefaultTableModel(tagDataObj, tagSet.tagHdr.colHdr) {
				private static final long serialVersionUID = 1L;

				@SuppressWarnings("rawtypes")
				Class[] columnTypes = new Class[] { Integer.class, String.class, String.class, Double.class };

				boolean[] columnEditables = new boolean[] { true, true, true, true };

				@SuppressWarnings({ "rawtypes", "unchecked" })
				public Class getColumnClass(int columnIndex) {
					return columnTypes[columnIndex];
				}

				public boolean isCellEditable(int row, int column) {
					return columnEditables[column];
				}
			};

			theTable = new JTable(model);			
			// insertRow does not work properly after sorting the columns
			// I'll setAutoCreateRowSorter(true) when I fix my InsertRow listener.
			theTable.setAutoCreateRowSorter(false);			
			theTable.getColumnModel().getColumn(3).setCellRenderer(new DoubleFormatter("#.####"));
			theTable.getColumnModel().getColumn(0).setPreferredWidth(35);
			theTable.getColumnModel().getColumn(1).setPreferredWidth(120);
			theTable.getColumnModel().getColumn(2).setPreferredWidth(220);
		}
//		else {
//			JOptionPane.showMessageDialog(null, "The requested tags failed to load");
//		}
		return theTable;
	}
	    
}
