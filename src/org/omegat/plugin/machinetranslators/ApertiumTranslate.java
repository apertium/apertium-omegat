/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.omegat.plugin.machinetranslators;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
                new ManageDialog(null, true).setVisible(true);
                initModes(new File(prefs.get("packagesPath", null)));
                Translator.setDisplayMarks(prefs.getBoolean("OmegaT_displayMarks", true));
                Translator.setDisplayAmbiguity(prefs.getBoolean("OmegaT_displayAmbiguity", false));
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
            Translator.setBase(base);
            Translator.setMode(titleToMode.get(title));
            return Translator.translate(text);
        }
        return "This language pair isn't installed.";
    }
    

    protected static final Preferences prefs = Preferences.userNodeForPackage(Translator.class);

    private HashMap<String, String> titleToBase;
    private HashMap<String, String> titleToMode;

    private void init() {
        File packagesDir = null;
        String packagesPath = prefs.get("packagesPath", null);
        if (packagesPath != null) packagesDir = new File(packagesPath);
        while (packagesDir == null || !packagesDir.isDirectory()) {
            String options[] = {"Create default directory", "Choose my own directory"};
            int answer = JOptionPane.showOptionDialog(null,
                    "It seems that this is the first time that you run the program.\n"
                    + "First of all, we need to set the directory in which to install the\n"
                    + "language pair packages.\n"
                    + "You can either create the default directory (a folder called \n"
                    + "\"Apertium packages\" in your home directory) or select a custom one.\n",
                    "Welcome to Apertium!",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer == 0) {
                packagesDir = new File(new File(System.getProperty("user.home")), "Apertium packages");
                packagesDir.mkdir();
                prefs.put("packagesPath", packagesDir.getPath());
            } else if (answer == 1) {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setApproveButtonText("OK");
                fc.setDialogTitle("Choose a directory");
                if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                    packagesDir = fc.getSelectedFile();
                    prefs.put("packagesPath", packagesDir.getPath());
                }
            } else {
                System.exit(0);
            }
        }

        initModes(packagesDir);
        if (titleToBase.isEmpty() &&
                JOptionPane.showConfirmDialog(null,
                "You don't have any language pair installed yet.\n"
                + "Would you like to install some now?",
                "We need language pairs!", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
            try {
                new InstallDialog((Frame)null, true) {
                    @Override
                    protected void initStrings() {
                        STR_TITLE = "Install language pairs";
                        STR_INSTRUCTIONS = "Check the language pairs to install.";
                    }
                }.setVisible(true);
                initModes(packagesDir);
            } catch (IOException ex) {
                Logger.getLogger(ApertiumTranslate.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, ex, "Error", JOptionPane.ERROR_MESSAGE);
            }

        Translator.setDisplayMarks(prefs.getBoolean("OmegaT_displayMarks", true));
        Translator.setDisplayAmbiguity(prefs.getBoolean("OmegaT_displayAmbiguity", false));

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