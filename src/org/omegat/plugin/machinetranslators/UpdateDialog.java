/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.omegat.plugin.machinetranslators;

import java.awt.Dialog;
import java.awt.Frame;
import java.io.IOException;

/**
 *
 * @author Mikel Artetxe
 */
public class UpdateDialog extends InstallDialog {
    
    public UpdateDialog(Dialog parent, boolean modal) throws IOException {
        super(parent, modal);
        selectAll();
    }
    
    public UpdateDialog(Frame parent, boolean modal) throws IOException {
        super(parent, modal);
        selectAll();
    }
    
    @Override
    protected void initStrings() {
        STR_TITLE = "Update language pairs";
        STR_INSTRUCTIONS = "Check the language pairs to update.";
        STR_INSTALL = "update";
        STR_INSTALLING = "Updating";
    }
    
    @Override
    protected void initTableContent() {
        tableContent = new Object[updatablePackages.size()][3];
        for (int i = 0; i < updatablePackages.size(); i++) {
            tableContent[i][0] = false;
            tableContent[i][1] = updatablePackages.get(i);
            tableContent[i][2] = "<html><i>Update available</i></html>";
        }
    }
    
    public boolean updatesAvailable() {
        return !updatablePackages.isEmpty();
    }
    
}
