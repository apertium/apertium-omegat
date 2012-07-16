/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.omegat.gui.exttrans;

import org.omegat.util.Language;

/**
 *
 * @author Mikel Artetxe
 */
public interface IMachineTranslation {
    String getName();
    String getTranslation(Language sLang, Language tLang, String text) throws Exception;
}
