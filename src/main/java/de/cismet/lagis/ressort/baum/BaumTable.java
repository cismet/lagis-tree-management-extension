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

import de.cismet.cids.custom.beans.lagis.BaumCustomBean;

import de.cismet.lagis.gui.tables.AbstractCidsBeanTable_Lagis;
import de.cismet.lagis.gui.tables.RemoveActionHelper;

/**
 * DOCUMENT ME!
 *
 * @author   gbaatz
 * @version  $Revision$, $Date$
 */
public class BaumTable extends AbstractCidsBeanTable_Lagis {

    //~ Instance fields --------------------------------------------------------

    private RemoveActionHelper removeActionHelper;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public RemoveActionHelper getRemoveActionHelper() {
        return removeActionHelper;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  removeActionHelper  DOCUMENT M
     */
    public void setRemoveActionHelper(final RemoveActionHelper removeActionHelper) {
        this.removeActionHelper = removeActionHelper;
    }

    @Override
    protected void addNewItem() {
        final BaumCustomBean tmpBaum = BaumCustomBean.createNew();
        ((BaumModel)getModel()).addCidsBean(tmpBaum);
    }

    @Override
    protected void removeItem(final int row) {
        ((BaumModel)getModel()).removeCidsBean(this.convertRowIndexToModel(getSelectedRow()));
        removeActionHelper.duringRemoveAction(this);
    }
}
