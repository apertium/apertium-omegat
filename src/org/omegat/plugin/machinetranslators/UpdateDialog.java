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
