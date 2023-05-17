package com.protose;

import java.awt.BorderLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;

import com.protose.shared.DP;

import java.awt.*;
import java.awt.event.*;

public class GUI {
    
    private Dimension screenSize;
    private Dimension windowSize;
    private Dimension halfSize;

    //the model for the document view
    private DefaultListModel<String> documentModel;
    private JList<String> fileList;

    //the model for the document part view
    private DefaultListModel<DP> partModel;
    private JList<DP> partList;

    //for pdf viewer
    private SwingController controller;

    // JPanel pdfPanel;

    //tabbed pane for the result panel
    JTabbedPane tabbedPane;

    // private JPanel partPanel;
    // private JPanel documentPanel;

    GUI(Client client){

        documentModel = new DefaultListModel<>();
        partModel = new DefaultListModel<>();

        //look and feel options
        String os = System.getProperty("os.name");
        if(os.contains("Windows")){
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        

        /***************************************************************************
         * TOP LAYER FRAME
        ***************************************************************************/

        //get the screen size and calculate the window size
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        halfSize = new Dimension((int) screenSize.getWidth()/2, (int) screenSize.getHeight()/2);
        windowSize = halfSize;
        // windowSize = screenSize;

        //create a frame
        JFrame frame = new JFrame("ProtoSE");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                client.shutDown();
            }
        });

        //set size to half the screen size
        frame.setSize(new Dimension((int)windowSize.getWidth()/2, (int)windowSize.getHeight()/2));

        //set logo
        ImageIcon logo = new ImageIcon("./resources/Logo.png");
        frame.setIconImage(logo.getImage());

        //limit frame to window size
        frame.setMaximumSize(screenSize);

        /***************************************************************************
         * MENU BAR
        ***************************************************************************/
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem deleteAllMenu= new JMenuItem("Delete All");
        deleteAllMenu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.purge();
                documentModel.removeElement(fileList.getSelectedValue());
            }
        });
        fileMenu.add(deleteAllMenu);

        menuBar.add(fileMenu);

        //add components to frame
        frame.getContentPane().add(BorderLayout.NORTH, menuBar);

        /***************************************************************************
         * Wrapper pannel for result and input panel
        ***************************************************************************/
        JPanel westWrapper = new JPanel();
        westWrapper.setPreferredSize(new Dimension(
            (int) windowSize.getWidth()/2,
            (int) windowSize.getHeight())
        );

        /***************************************************************************
         * DOCUMENT VIEW PANEL
        ***************************************************************************/
        JPanel documentPanel = new JPanel(new BorderLayout());

        fileList = new JList<String>(documentModel);
        fileList.setCellRenderer(new FileRenderer(true));
        fileList.setLayoutOrientation(javax.swing.JList.VERTICAL);

        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(westWrapper.getPreferredSize());
        documentPanel.add(BorderLayout.WEST, scrollPane);

        //mouse listener 
        fileList.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

                //left mouse button pressed
                if(SwingUtilities.isLeftMouseButton(e)){
                    
                    if(e.getClickCount() == 2){
                        //double click
                        
                        ByteArrayOutputStream fs = client.searchDocument(fileList.getSelectedValue());
                        
                        byte[] doc = fs.toByteArray();                        

                        controller.openDocument(doc, 0, doc.length, fileList.getSelectedValue(), null);
                        controller.setToolBarVisible(false);
                        controller.setUtilityPaneVisible(false);
                        frame.pack();
                    }
                   
                }

                //right mouse button
                if(SwingUtilities.isRightMouseButton(e)){
                    
                    //context menu for more options
                    JPopupMenu popup = new JPopupMenu("context");

                    //open with extern pdf viewer
                    JMenuItem item = new JMenuItem("open with default pdf viewer");
                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            
                            try {
                                //get document and store as file in tmp directory
                                ByteArrayOutputStream fs = client.searchDocument(fileList.getSelectedValue());
                                File tmpFile = new File("./tmp/docs/" + fileList.getSelectedValue());
                                try (FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
                                    outputStream.write(fs.toByteArray());
                                }catch(Exception ex){
                                    ex.printStackTrace();
                                }
                                Desktop.getDesktop().open(tmpFile);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    popup.add(item);

                    //delete document
                    JMenuItem deleteItem = new JMenuItem("delete document");
                    deleteItem.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            client.deleteDocument(fileList.getSelectedValue());
                            documentModel.removeElement(fileList.getSelectedValue());
                        }
                        
                    });
                    popup.add(deleteItem);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                    
                }               
            }

            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}            
        });


        //add the initial files stored on server
        try {
            for (String docName : client.getDocumentNames()) {
                documentModel.addElement(docName);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } 

        /***************************************************************************
         * DOCUMENT PART VIEW PANEL
        ***************************************************************************/
        JPanel partPanel = new JPanel(new BorderLayout());

        partList = new JList<DP>(partModel);
        partList.setCellRenderer(new PartRenderer());
        partList.setLayoutOrientation(javax.swing.JList.VERTICAL);

        JScrollPane partscrollPane = new JScrollPane(partList);
        partscrollPane.setPreferredSize(westWrapper.getPreferredSize());
        partPanel.add(BorderLayout.WEST, partscrollPane);

        //mouse listener 
        partList.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e)){
                    if(e.getClickCount() == 2){
                        //double click
                
                        DP selection = partList.getSelectedValue();
                        controller.openDocument(selection.getPDFinMem(), 0, selection.getPDFinMem().length, selection.getDocName(), null);
                        controller.setToolBarVisible(false);
                        controller.setUtilityPaneVisible(false);
                        frame.pack();
                    }
                }
                
                //popup for right mouse button
                if(SwingUtilities.isRightMouseButton(e)){
                    //context menu for more options
                    JPopupMenu popup = new JPopupMenu("context");

                    //show the selected page
                    JMenuItem item = new JMenuItem("show page in extern viewer");
                    item.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            DP selection = partList.getSelectedValue();
                            try {
                                //get document and store as file in tmp directory
                                File tmpFile = new File("./tmp/docs/" + "part-" + selection.getDocName());
                                try (FileOutputStream outputStream = new FileOutputStream(tmpFile)) {
                                    outputStream.write(selection.getPDFinMem());
                                }catch(Exception ex){
                                    ex.printStackTrace();
                                }
                                Desktop.getDesktop().open(tmpFile);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    popup.add(item);

                    //select the document in the doc view
                    JMenuItem selectDocument = new JMenuItem("select Document");
                    selectDocument.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            tabbedPane.setSelectedIndex(0);
                            fileList.setSelectedValue(partList.getSelectedValue().getDocName(), true);
                        }
                    });
                    popup.add(selectDocument);

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }

               
            }

            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}            
        });

        /***************************************************************************
         * RESULT TABBED WINDOW
        ***************************************************************************/
        tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension(
            (int)windowSize.getWidth()/2, 
            (int) (windowSize.getHeight() * 0.9f))
        );

        tabbedPane.addTab("Documents", documentPanel);
        tabbedPane.addTab("Search Results", partPanel);

        westWrapper.add(tabbedPane, BorderLayout.CENTER);

        /***************************************************************************
         * PDF VIEWER PANEL
        ***************************************************************************/
        controller = new SwingController(); 

        // Build a SwingViewFactory configured with the controller
        SwingViewBuilder pdfViewerFactory = new SwingViewBuilder(controller);

        // Use the factory to build a JPanel that is pre-configured
        //with a complete, active Viewer UI.
        JPanel viewerPanel = pdfViewerFactory.buildViewerPanel();
        viewerPanel.setPreferredSize(
            new Dimension(
                (int)windowSize.getWidth()/2,
                (int)windowSize.getHeight())
        );

        //disable the ice toolbar
        controller.setToolBarVisible(false);

        frame.getContentPane().add(BorderLayout.EAST, viewerPanel);

        /***************************************************************************
         * INPUT PANEL
        ***************************************************************************/
        JPanel inputPanel = new JPanel();
        
        //text field for search
        JTextField searchText = new JTextField();
        searchText.setPreferredSize(new Dimension(
            200,
            20
        ));
        inputPanel.add(searchText);

        //search button
        JButton searchButton = new JButton("SEARCH");
        searchButton.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("searching for word: " + searchText.getText());
                
                //set the search result pane active
                tabbedPane.setSelectedIndex(1);

                List<DP> retVal = client.searchWord(searchText.getText());
                partModel.clear();
                for (DP dp : retVal) {
                    partModel.addElement(dp);
                }
            }
        });
        inputPanel.add(searchButton);


        //upload button
        JButton uploadButton = new JButton("UPLOAD");
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("./"));
                int fileSuc = fileChooser.showOpenDialog(uploadButton);

                //handle the selected file
                if (fileSuc == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    System.out.println("[INFO] uploading file: " + file); 
                    client.addDocument(file);
                    documentModel.addElement(file.getName());
                } else {
                    System.out.println("[ERROR] error in filechooser");
                }
            }
        });
        inputPanel.add(uploadButton);  

        //delete button
        JButton deleteButton = new JButton("DELETE");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.deleteDocument(fileList.getSelectedValue());
                documentModel.removeElement(fileList.getSelectedValue());
            }
        });
        inputPanel.add(deleteButton);

        westWrapper.add(BorderLayout.SOUTH, inputPanel);

        /***************************************************************************
         *  FINISH FRAME
        ***************************************************************************/

        frame.getContentPane().add(westWrapper, BorderLayout.WEST);

        //add frame resizing
        frame.addComponentListener(new ComponentAdapter() {
            //TODO: implement resizing?
        });

        //set frame to visible
        frame.pack();
        frame.setVisible(true);
    }    

    //renderer for the file list
    class FileRenderer extends DefaultListCellRenderer {

        private boolean pad;
        private Border padBorder = new EmptyBorder(3,3,3,3);
    
        FileRenderer(boolean pad) {
            this.pad = pad;
        }
    
        @Override
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
    
            Component c = super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
            JLabel l = (JLabel)c;
            String fileName = (String)value;

            ImageIcon pdfIcon = new ImageIcon("./resources/PDFIcon.png");
            Image image = pdfIcon.getImage(); // transform it 
            Image scaledImage = image.getScaledInstance(50, 50,  java.awt.Image.SCALE_REPLICATE); // 20, 24
            pdfIcon = new ImageIcon(scaledImage);  // transform it back
            l.setIcon(pdfIcon);

            l.setText(fileName);

            if (pad) {
                l.setBorder(padBorder);
            }
    
            return l;
        }
    }

    //renderer for the part list
    class PartRenderer extends DefaultListCellRenderer{

        @Override
        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus){

            Component c = super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
            JLabel l = (JLabel)c;
            DP docPart = (DP)value;
            l.setText("Document: " + docPart.getDocName() + " Page: " + docPart.getPos());
            return l;
        }

    }

}