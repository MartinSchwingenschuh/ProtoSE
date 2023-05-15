package com.protose;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;

// import org.apache.batik.swing.JSVGCanvas;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;

import com.protose.shared.DP;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.awt.im.spi.InputMethodDescriptor;

public class GUI {
    
    private Client client;
    private Dimension screenSize;
    private Dimension windowSize;

    //the model for the document view
    private DefaultListModel<String> documentModel;

    //the model for the document part view
    private DefaultListModel<DP> partModel;

    //for pdf viewer
    private SwingController controller;

    // JPanel pdfPanel;

    //tabbed pane for the result panel
    JTabbedPane tabbedPane;

    // private JPanel partPanel;
    // private JPanel documentPanel;

    GUI(Client client){

        /*
         * test zone
         */

        // boolean b;
        // List<PDPage> pages = doc.getDocumentCatalog().getPages();
        // for (int p = 0; p < pages.size(); ++p)
        // {
        //     // RGB image with 300 dpi
        //     BufferedImage bim = pages.get(p).convertToImage(BufferedImage.TYPE_INT_RGB, 300);
            
        //     // save as PNG with default metadata
        //     b = ImageIO.write(bim, "png", new File("rgbpage" + (p+1) + ".png"));
        //     if (!b)
        //     {
        //         // error handling
        //     }
    
        //     // B/W image with 300 dpi
        //     bim = pages.get(p).convertToImage(BufferedImage.TYPE_BYTE_BINARY, 300);
            
        //     // save as TIF with dpi in the metadata
        //     // PDFBox will choose the best compression for you - here: CCITT G4
        //     // you need to add jai_imageio.jar to your classpath for this to work
        //     b = ImageIOUtil.writeImage(bim, "bwpage-" + (p+1) + ".tif", 300);
        //     if (!b)
        //     {
        //         // error handling
        //     }
        // }
    
        // doc.close();
        // // build a component controller
        // SwingController controller = new SwingController();

        // SwingViewBuilder factory = new SwingViewBuilder(controller);

        // JPanel viewerComponentPanel = factory.buildViewerPanel();

        // // add interactive mouse link annotation support via callback
        // controller.getDocumentViewController().setAnnotationCallback(
        //         new org.icepdf.ri.common.MyAnnotationCallback(
        //                 controller.getDocumentViewController()));

        // JFrame applicationFrame = new JFrame();
        // applicationFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // applicationFrame.getContentPane().add(viewerComponentPanel);

        // // Now that the GUI is all in place, we can try openning a PDF
        // controller.openDocument("./TestFileTwo.pdf");

        // // show the component
        // applicationFrame.pack();
        // applicationFrame.setVisible(true);


         /*test zone */


        this.client = client;

        documentModel = new DefaultListModel<>();
        partModel = new DefaultListModel<>();

        //look and feel options
        try {
            // UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        /***************************************************************************
         * TOP LAYER FRAME
        ***************************************************************************/

        //get the screen size and calculate the window size
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        windowSize = new Dimension((int) screenSize.getWidth()/2, (int) screenSize.getHeight()/2);
        // windowSize = screenSize;

        //create a frame
        JFrame frame = new JFrame("ProtoSE");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.setSize(windowSize);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        // frame.setUndecorated(true);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                client.shutDown();
            }
        });

        ImageIcon logo = new ImageIcon("./Client/resources/logo.png");
        frame.setIconImage(logo.getImage());

        /***************************************************************************
         * TOP LAYER PANELS
        ***************************************************************************/
        
        
        // JPanel resultPanel = new JPanel();
        // resultPanel.setPreferredSize(
        //     new Dimension(
        //         (int)windowSize.getWidth()/2,
        //         (int)windowSize.getHeight())
        // );

        // JPanel pdfRenderPanel = new JPanel();
        // pdfRenderPanel.setPreferredSize(
        //     new Dimension(
        //         (int)windowSize.getWidth()/2,
        //         (int)windowSize.getHeight())
        // );
        

        /***************************************************************************
         * MENU BAR
        ***************************************************************************/
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem fileUpload = new JMenuItem("upload file");
        fileMenu.add(fileUpload);
        JMenu searchMenu = new JMenu("Search");

        menuBar.add(fileMenu);
        menuBar.add(searchMenu);

        //add components to frame
        frame.getContentPane().add(BorderLayout.NORTH, menuBar);

        /***************************************************************************
         * DOCUMENT VIEW PANEL
        ***************************************************************************/
        JPanel documentPanel = new JPanel(new BorderLayout());
        // documentPanel.setSize(new Dimension((int) windowSize.getWidth()/2, (int) windowSize.getHeight()));

        JList<String> fileList = new JList<String>(documentModel);
        fileList.setCellRenderer(new FileRenderer(true));
        fileList.setLayoutOrientation(javax.swing.JList.VERTICAL);

        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setPreferredSize(new Dimension((int) windowSize.getWidth()/2, (int) windowSize.getHeight()));
        documentPanel.add(BorderLayout.WEST, scrollPane);

        //mouse listener 
        fileList.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

                // System.out.println("mouse event");

                //left mouse button pressed
                if(SwingUtilities.isLeftMouseButton(e)){
                    
                    // if(e.getClickCount() == 1){
                    //     //normal click
                    //     String selectedFile = fileList.getSelectedValue();

                    // }else
                    if(e.getClickCount() == 2){
                        //double click
                        
                        ByteArrayOutputStream fs = client.searchDocument(fileList.getSelectedValue());
                        // try(PDDocument document = PDDocument.load(fs.toByteArray());){
                            
                        // }catch(Exception ex){
                            
                        // }
                        File outputFile = new File("./Client/tmp/tmp.pdf");
                        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                            outputStream.write(fs.toByteArray());
                        }catch(Exception ex){
                            ex.printStackTrace();
                        }
                        // try {
                        //     Desktop.getDesktop().open(outputFile);
                        // } catch (IOException e1) {
                        //     e1.printStackTrace();
                        // }

                        // try {
                        //     String filename = "TestFileTwo.pdf";
                        
                        //     // open the document
                        //     PDDocument doc = PDDocument.load(new File(filename));
                        
                        //     // PDPage page = doc.getPage(0);
                        //     PDFRenderer renderer = new PDFRenderer(doc);
                        //     // BufferedImage bi = renderer.renderImageWithDPI(0, 300,org.apache.pdfbox.rendering.ImageType.RGB);
                        //     BufferedImage bi = renderer.renderImage(0, 1.5f, org.apache.pdfbox.rendering.ImageType.RGB);
                        //     // BufferedImage bi = renderer.renderImage(0);
                        //     File outputfile = new File("image.jpg");
                        //     ImageIO.write(bi, "jpg", outputfile);



                        //     // BufferedImage scaledImage =  resize(
                        //     //     bi, 
                        //     // (int) pdfPanel.getSize().getWidth(), 
                        //     // (int) pdfPanel.getSize().getWidth()); //= bi.getScaledInstance(500, 700,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way  
                            
                        //     // // File scaledOutput = new File("scaledImage.jpg");
                        //     // // ImageIO.write(scaledImage, "jpg", scaledOutput);
                            
                        //     // outputfile = new File("imagee.jpg");
                        //     // ImageIO.write(bi, "jpg", outputfile);
                            
                        //     // //set the panel
                        //     // Canvas c = new Canvas();
                        //     // JLabel picLabel = new JLabel(new ImageIcon(bi));
                        //     // // picLabel.setSize(500, 500);
                        //     // pdfPanel.add(picLabel);


                
                        // } catch (Exception ex) {
                        //     ex.printStackTrace();
                        // }


                        // Viewer viewer = new Viewer();
                        // viewer.setupViewer();
                        // viewer.executeCommand(ViewerCommands.OPENFILE, "./Client/tmp/tmp.pdf");
                        
                        // File docFile = new File("./Client/tmp/" + fileList.getSelectedValue());
                        byte[] doc = fs.toByteArray();
                        // File tmpFile = new File("./Client/tmp/tmp.pdf");
                        

                        controller.openDocument(doc, 0, doc.length, fileList.getSelectedValue(), null);
                        controller.setToolBarVisible(false);
                        controller.setUtilityPaneVisible(false);
                        controller.openDocument("./Client/tmp/tmp.pdf"); 
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
                            // System.out.println("elem: " + fileList.getSelectedValue() + " selected");
                            
                            try {
                                //get document and store as file in tmp directory
                                ByteArrayOutputStream fs = client.searchDocument(fileList.getSelectedValue());
                                File tmpFile = new File("./Client/tmp/docs/" + fileList.getSelectedValue());
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

        // resultPanel.add(documentPanel);

        /***************************************************************************
         * DOCUMENT PART VIEW PANEL
        ***************************************************************************/
        JPanel partPanel = new JPanel(new BorderLayout());
        // partPanel.setSize(new Dimension((int) windowSize.getWidth()/2, (int) windowSize.getHeight()));

        JList<DP> partList = new JList<DP>(partModel);
        partList.setCellRenderer(new PartRenderer());
        partList.setLayoutOrientation(javax.swing.JList.VERTICAL);

        JScrollPane partscrollPane = new JScrollPane(partList);
        partscrollPane.setPreferredSize(new Dimension((int) windowSize.getWidth()/2, (int) windowSize.getHeight()));
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
                        controller.openDocument("./Client/tmp/tmp.pdf"); 
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
                                File tmpFile = new File("./Client/tmp/docs/" + "part-" + selection.getDocName());
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


        //add the initial files stored on server
        // try {
        //     for (String docName : client.getDocumentNames()) {
        //         documentModel.addElement(docName);
        //     }
        // } catch (Exception e1) {
        //     e1.printStackTrace();
        // } 

        // resultPanel.add(partPanel);

        /***************************************************************************
         * RESULT TABBED WINDOW
        ***************************************************************************/
        tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize(new Dimension((int) windowSize.getWidth()/2, (int) windowSize.getHeight()));

        tabbedPane.addTab("Documents", documentPanel);
        tabbedPane.addTab("Search Results", partPanel);

        frame.getContentPane().add(tabbedPane, BorderLayout.WEST);

        /***************************************************************************
         * PDF VIEWER PANEL
        ***************************************************************************/
        // pdfPanel = new JPanel();
        // pdfPanel.setPreferredSize(
        //     new Dimension(
        //         (int)windowSize.getWidth()/2,
        //         (int)windowSize.getHeight())
        // );
        // frame.getContentPane().add(BorderLayout.EAST, pdfPanel);

        controller = new SwingController(); 

        SwingViewBuilder builder = new SwingViewBuilder(controller);
        // Build a SwingViewFactory configured with the controller
        SwingViewBuilder pdfViewerFactory = new SwingViewBuilder(controller);

        // Use the factory to build a JPanel that is pre-configured
        //with a complete, active Viewer UI.
        JPanel viewerPanel = pdfViewerFactory.buildViewerPanel();
        // JPanel viewerPanel = builder.buildViewerPanel();
        viewerPanel.setPreferredSize(
            new Dimension(
                (int)windowSize.getWidth()/2,
                (int)windowSize.getHeight())
        );

        // controller.setToolBarVisible(false);
        // controller.setUtilityPaneVisible(false);

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

        frame.getContentPane().add(BorderLayout.NORTH, inputPanel);


        /***************************************************************************
         * 
        ***************************************************************************/

        //add frame resizing
        frame.addComponentListener(new ComponentAdapter() {

            //TODO: implement resizing?
            // public void componentResized(ComponentEvent evt) {
            //     tabbedPane.setSize(
            //         (int)frame.getSize().getWidth()/2, 
            //         (int)frame.getSize().getWidth() 
            //     );

            //     viewerPanel.setSize(
            //         (int)frame.getSize().getWidth()/2, 
            //         (int)frame.getSize().getWidth() 
            //     );
                
            //     // frame.pack();

            //     Component c = (Component)evt.getSource();
            //     c.repaint();

            //     System.out.println("resizing from: " + frame.getSize() + " to " + c.getSize());
            //     // System.out.println("resizing frame");
            // }
        });


        //set frame to visible
        frame.pack();
        frame.setVisible(true);

    }    

    public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
    
        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
    
        return dimg;
    }  

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

            // File file = new File(filename);

            l.setText(fileName);
            

            //TODO: change icon to svg variant
            ImageIcon pdfIcon = new ImageIcon("./Client/resources/PDFIcon.png");
            Image image = pdfIcon.getImage(); // transform it 
            Image scaledImage = image.getScaledInstance(20, 24,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way  
            pdfIcon = new ImageIcon(scaledImage);  // transform it back
            l.setIcon(pdfIcon);

            if (pad) {
                l.setBorder(padBorder);
            }
    
            return l;
        }
    }

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