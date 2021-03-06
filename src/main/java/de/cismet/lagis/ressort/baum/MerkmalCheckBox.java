/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.lagis.ressort.baum;

import javax.swing.JCheckBox;

import de.cismet.cids.custom.beans.lagis.BaumMerkmalCustomBean;

import de.cismet.lagisEE.entity.extension.baum.BaumMerkmal;

/**
 * DOCUMENT ME!
 *
 * @author   Sebastian Puhl
 * @version  $Revision$, $Date$
 */
//ToDo make abstract(generic) is redundant also used in MiPa
public class MerkmalCheckBox extends JCheckBox {

    //~ Instance fields --------------------------------------------------------

    private BaumMerkmalCustomBean baumMerkmal;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MerkmalCheckBox object.
     *
     * @param  baumMerkmal  DOCUMENT ME!
     */
    public MerkmalCheckBox(final BaumMerkmalCustomBean baumMerkmal) {
        super(baumMerkmal.getBezeichnung());
        this.baumMerkmal = baumMerkmal;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public BaumMerkmalCustomBean getBaumMerkmal() {
        return baumMerkmal;
    }
}
