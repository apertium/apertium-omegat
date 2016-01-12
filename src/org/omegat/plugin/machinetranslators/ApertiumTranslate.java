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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.apertium.Translator;
import org.omegat.core.Core;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.util.Language;

/**
 *
 * @author Mikel Artetxe
 */
public class ApertiumTranslate extends BaseTranslate {

    public ApertiumTranslate() {
        JMenuItem item = new JMenuItem("Apertium settings...");
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SettingsDialog(null, true).setVisible(true);
                initModes(new File(prefs.get("packagesPath", null)));
                Translator.setDisplayMarks(prefs.getBoolean("displayMarks", true));
            }
        });
        Core.getMainWindow().getMainMenu().getOptionsMenu().add(item);
        init();
    }
    
    @Override
    protected String getPreferenceName() {
        return "allow_apertium_translate_plugin";
    }
    
    @Override
    public String getName() {
        return "Apertium (offline)";
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws Exception {
        String title = sLang.getLocale().getDisplayLanguage() + " → " + tLang.getLocale().getDisplayLanguage();
        String base = titleToBase.get(title);
        if (base != null) {
            synchronized (Translator.class) {
                Translator.setBase(base);
                Translator.setMode(titleToMode.get(title));
                return Translator.translate(text, "omegat");
            }
        }
        return "This language pair isn't installed.";
    }
    

    protected static final Preferences prefs = Preferences.userNodeForPackage(ApertiumTranslate.class);
    protected static final FilenameFilter filter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.matches("apertium-[a-z][a-z][a-z]?-[a-z][a-z][a-z]?.jar");
        }
    };

    private HashMap<String, String> titleToBase;
    private HashMap<String, String> titleToMode;

    private void init() {
        Translator.setParallelProcessingEnabled(false);
        
        File packagesDir = null;
        String packagesPath = prefs.get("packagesPath", null);
        if (packagesPath != null) packagesDir = new File(packagesPath);
        while (packagesDir == null || !packagesDir.isDirectory()) {
            String options[] = {"Create default directory", "Choose my own directory"};
            int answer = JOptionPane.showOptionDialog(null,
                    "Welcome to Apertium!\n"
                    + "It seems that this is the first time that you run this plug-in.\n"
                    + "First of all, we need to set the directory in which to install the\n"
                    + "language pair packages.\n"
                    + "You can either create the default directory (a folder called \n"
                    + "\"Apertium OmegaT packages\" in your home directory) or select a\n"
                    + "custom one.\n",
                    "Apertium plug-in for OmegaT",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                packagesDir = new File(new File(System.getProperty("user.home")), "Apertium OmegaT packages");
                packagesDir.mkdir();
                prefs.put("packagesPath", packagesDir.getPath());
                try {
                    new File(packagesDir, ".apertium-omegat").createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(ApertiumTranslate.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (answer == 1) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setApproveButtonText("OK");
                fc.setDialogTitle("Choose a directory");
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    packagesDir = fc.getSelectedFile();
                    while (new File(packagesDir, ".apertium-caffeine").exists()) {
                        JOptionPane.showMessageDialog(null,
                                "The selected directory is being used by Apertium Caffeine.\n"
                                + "Please, select a different one.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                            packagesDir = fc.getSelectedFile();
                        else System.exit(0);
                    }
                    prefs.put("packagesPath", packagesDir.getPath());
                    try {
                        new File(packagesDir, ".apertium-omegat").createNewFile();
                    } catch (IOException ex) {
                        Logger.getLogger(ApertiumTranslate.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else System.exit(0);
            } else System.exit(0);
        }

        initModes(packagesDir);
        if (titleToBase.isEmpty() &&
                JOptionPane.showConfirmDialog(null,
                "You don't have any language pair installed yet.\n"
                + "Would you like to install some now?",
                "Apertium plug-in for OmegaT", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            try {
                new InstallDialog((Frame)null, true) {
                    @Override
                    protected void initStrings() {
                        STR_TITLE = "Apertium plug-in for OmegaT";
                        STR_INSTRUCTIONS = "Check the language pairs to install.";
                    }
                }.setVisible(true);
                initModes(packagesDir);
            } catch (IOException ex) {
                Logger.getLogger(ApertiumTranslate.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, ex, "Error", JOptionPane.ERROR_MESSAGE);
            }

        Translator.setDisplayMarks(prefs.getBoolean("displayMarks", true));

        Translator.setCacheEnabled(true);

        if (prefs.getBoolean("checkUpdates", true))
            new Thread() {
                @Override
                public void run() {
                    try {
                        UpdateDialog ud = new UpdateDialog((Frame)null, true);
                        if (ud.updatesAvailable() && JOptionPane.showConfirmDialog(null,
                                "Updates are available for some language pairs!\n"
                                + "Would you like to install them now?",
                                "Updates found", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                            ud.setVisible(true);
                    } catch (IOException ex) {
                        Logger.getLogger(ApertiumTranslate.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
    }

    private void initModes(File packagesDir) {
        titleToBase = new HashMap<String, String>();
        titleToMode = new HashMap<String, String>();
        File packages[] = packagesDir.listFiles();
        for (File p : packages) {
            try {
                String base = p.getPath();
                Translator.setBase(base);
                for (String mode : Translator.getAvailableModes()) {
                    String title = Translator.getTitle(mode);
                    titleToBase.put(title, base);
                    titleToMode.put(title, mode);
                }
            } catch (Exception ex) {
                //Perhaps the directory contained a file that wasn't a valid package...
                Logger.getLogger(ApertiumTranslate.class.getName()).log(Level.WARNING, null, ex);
            }
        }
    }
    
}