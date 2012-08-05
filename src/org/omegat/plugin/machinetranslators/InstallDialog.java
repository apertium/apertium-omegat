/*
 * Copyright (C) 2012 Mikel Artetxe
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package org.omegat.plugin.machinetranslators;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import org.apertium.Translator;

/**
 *
 * @author Mikel Artetxe
 */
public class InstallDialog extends javax.swing.JDialog {
    
    protected static final String REPO_URL = "https://apertium.svn.sourceforge.net/svnroot/apertium/builds/language-pairs";
    
    protected ArrayList<String> packages;
    protected ArrayList<String> installedPackages;
    protected ArrayList<String> updatablePackages;
    protected ArrayList<String> updatedPackages;
    protected HashMap<String, String> packageToFilename;
    protected HashMap<String, URL> packageToURL;
    protected Object tableContent[][];
    private boolean packagesToInstall[], packagesToUninstall[];
    
    protected String STR_TITLE = "Install / Uninstall language pairs";
    protected String STR_INSTRUCTIONS = "Check the language pairs to install and uncheck the ones to uninstall.";
    protected String STR_INSTALL = "install";
    protected String STR_INSTALLING = "Installing";
    protected String STR_UNINSTALL = "uninstall";
    protected String STR_UNINSTALLING = "Uninstalling";
    
    /** Creates new form InstallDialog */
    public InstallDialog(Dialog parent, boolean modal) throws IOException {
        super(parent, modal);
        init();
        this.setLocationRelativeTo(parent);
    }
    
    public InstallDialog(Frame parent, boolean modal) throws IOException {
        super(parent, modal);
        init();
        this.setLocationRelativeTo(parent);
    }
    
    private void init() throws IOException {
        initStrings();
        initComponents();
        initPackages();
        packagesToInstall = new boolean[packages.size()];
        packagesToUninstall = new boolean[packages.size()];
        initTableContent();
        table.setModel(new DefaultTableModel(tableContent, new String[] {"", "", ""}) {
            @Override
            public Class getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 0;
            }
        });
        table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(1);
        table.getColumnModel().getColumn(1).setResizable(false);
        table.getColumnModel().getColumn(1).setPreferredWidth(1000);
        table.getColumnModel().getColumn(2).setResizable(false);
        table.getColumnModel().getColumn(2).setPreferredWidth(1000);
        table.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getColumn() != 0) return;
                int row = e.getFirstRow();
                if (tableContent[row][0].equals(table.getModel().getValueAt(row, 0))) {
                    table.getModel().setValueAt(tableContent[row][1], row, 1);
                    table.getModel().setValueAt(tableContent[row][2], row, 2);
                    packagesToInstall[row] = packagesToUninstall[row] = false;
                } else {
                    boolean install = (Boolean)table.getModel().getValueAt(row, 0);
                    table.getModel().setValueAt("<html><b>" + tableContent[row][1] + "</b></html>", row, 1);
                    table.getModel().setValueAt("<html><b>Marked to " + (install ? STR_INSTALL : STR_UNINSTALL) + "</b></html>", row, 2);
                    if (install) packagesToInstall[row] = true;
                    else packagesToUninstall[row] = true;
                }
            }
        });
        final JPopupMenu popup = new JPopupMenu();
        final JMenuItem menuItem = new JMenuItem();
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                table.getModel().setValueAt(!(Boolean)table.getModel().getValueAt(table.getSelectedRow(), 0), table.getSelectedRow(), 0);
            }
        });
        popup.add(menuItem);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showPopup(e);
            }
            public void showPopup(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (!table.isRowSelected(row)) table.changeSelection(row, column, false, false);
                boolean installed = (Boolean)tableContent[row][0];
                boolean checked = (Boolean)table.getModel().getValueAt(row, 0);
                menuItem.setText((installed == checked ? "Mark to " : "Unmark from ") + (installed ? STR_UNINSTALL : STR_INSTALL));
                popup.show(table, e.getX(), e.getY());
            }
        });
    }
    
    protected void initStrings() {}
    
    private void initPackages() throws IOException {
        packages = new ArrayList<String>();
        installedPackages = new ArrayList<String>();
        updatablePackages = new ArrayList<String>();
        updatedPackages = new ArrayList<String>();
        packageToFilename = new HashMap<String, String>();
        packageToURL = new HashMap<String, URL>();
        ArrayList<String> installedPackagesFilenames = new ArrayList<String>(Arrays.asList(new File(ApertiumTranslate.prefs.get("packagesPath", null)).list(ApertiumTranslate.filter)));
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(REPO_URL).openStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] columns = line.split("\t");
            if (columns.length > 3) {
                String p = Translator.getTitle(columns[3]);
                packages.add(p);
                URL url = new URL(columns[1]);
                packageToURL.put(p, url);
                packageToFilename.put(p, columns[0] + ".jar");
                if (installedPackagesFilenames.contains(columns[0] + ".jar")) {
                    installedPackagesFilenames.remove(columns[0] + ".jar");
                    installedPackages.add(p);
                    long localLastModified = ApertiumTranslate.prefs.getLong("last_modified_" + columns[0] + ".jar", -1);
                    long onlineLastModified = url.openConnection().getLastModified();
                    if (onlineLastModified > localLastModified) updatablePackages.add(p);
                    else updatedPackages.add(p);
                }
            }
        }
        
        for (String code : installedPackagesFilenames) {
            packages.add(code);
            installedPackages.add(code);
            packageToFilename.put(code, code);
        }
        
        Collections.sort(packages);
        Collections.sort(updatedPackages);
        Collections.sort(updatablePackages);
    }
    
    protected void initTableContent() {
        tableContent = new Object[packages.size()][3];
        for (int i = 0; i < packages.size(); i++) {
            tableContent[i][0] = installedPackages.contains(packages.get(i));
            tableContent[i][1] = packages.get(i);
            if (updatedPackages.contains(packages.get(i)))
                tableContent[i][2] = "<html><i>Installed from repository</i></html>";
            else if (updatablePackages.contains(packages.get(i)))
                tableContent[i][2] = "<html><i>Installed from repository</i></html>";
            else if (installedPackages.contains(packages.get(i)))
                tableContent[i][2] = "<html><i>Manually installed</i></html>";
            else
                tableContent[i][2] = "<html><i>Not installed</i></html>";
        }
    }
    
    protected void selectAll() {
        for (int i = 0; i < table.getModel().getRowCount(); i++)
            table.setValueAt(true, i, 0);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(STR_TITLE);

        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        table.setTableHeader(null);
        jScrollPane1.setViewportView(table);

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        jLabel1.setText(STR_INSTRUCTIONS);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 465, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 142, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        final JDialog dialog = new JDialog(this, STR_INSTALLING + "...", true);
        final JLabel message = new JLabel("Preparing...");
        final JProgressBar progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        ((JPanel)dialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dialog.setLayout(new BorderLayout(5, 5));
        dialog.add(BorderLayout.NORTH, message);
        dialog.add(BorderLayout.CENTER, progress);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(this);
        
        new Thread() {
            @Override
            public void run() {
                int value = 0, length = 1;
                for (int i = 0; i < packagesToInstall.length; i++)
                    if (packagesToInstall[i]) {
                        try {
                            length += packageToURL.get(tableContent[i][1]).openConnection().getContentLength();
                        } catch (IOException ex) {
                            Logger.getLogger(InstallDialog.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                progress.setMaximum(length);
                
                for (int i = 0; i < packagesToInstall.length; i++)
                    if (packagesToInstall[i]) {
                        try {
                            message.setText(STR_INSTALLING + " " + tableContent[i][1] + "...");
                            BufferedInputStream in = new BufferedInputStream(packageToURL.get(tableContent[i][1]).openStream());
                            FileOutputStream fos = new FileOutputStream(new File(new File(ApertiumTranslate.prefs.get("packagesPath", "")), packageToFilename.get(tableContent[i][1])));
                            byte data[] = new byte[1024];
                            int count;
                            while((count = in.read(data, 0, 1024)) != -1) {
                                fos.write(data, 0, count);
                                value += count;
                                progress.setValue(value);
                                progress.setString(value * 100 / length + "%");
                            }
                            fos.close();
                            in.close();
                            ApertiumTranslate.prefs.putLong("last_modified_" + packageToFilename.get(tableContent[i][1]), packageToURL.get(tableContent[i][1]).openConnection().getLastModified());
                        } catch (IOException ex) {
                            Logger.getLogger(InstallDialog.class.getName()).log(Level.SEVERE, null, ex);
                            JOptionPane.showMessageDialog(InstallDialog.this, ex, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                
                for (int i = 0; i < packagesToInstall.length; i++)
                    if (packagesToUninstall[i]) {
                        message.setText(STR_UNINSTALLING + " " + tableContent[i][1] + "...");
                        if (!new File(new File(ApertiumTranslate.prefs.get("packagesPath", "")), packageToFilename.get(tableContent[i][1])).delete()) {
                            JOptionPane.showMessageDialog(InstallDialog.this, "Unable to uninstall " + tableContent[i][1], "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        ApertiumTranslate.prefs.remove("last_modified_" + packageToFilename.get(tableContent[i][1]));
                    }
                dialog.dispose();
            }
        }.start();
        dialog.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okButton;
    private javax.swing.JTable table;
    // End of variables declaration//GEN-END:variables
}
