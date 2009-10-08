/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.cismet.lagis.ressort.baum;

import de.cismet.lagisEE.entity.extension.baum.BaumMerkmal;
import javax.swing.JCheckBox;

/**
 *
 * @author Sebastian Puhl
 */
//ToDo make abstract(generic) is redundant also used in MiPa
public class MerkmalCheckBox extends JCheckBox {

    private BaumMerkmal baumMerkmal;

    public MerkmalCheckBox(final BaumMerkmal baumMerkmal) {
        super(baumMerkmal.getBezeichnung());
        this.baumMerkmal = baumMerkmal;
    }

    public BaumMerkmal getBaumMerkmal() {
        return baumMerkmal;
    }
                
}
