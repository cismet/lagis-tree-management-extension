/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * AddExistingVorgangPanel.java
 *
 * Created on 27. August 2007, 15:08
 */
package de.cismet.lagis.ressort.baum;

import org.apache.log4j.Logger;

import org.jdesktop.swingx.JXTable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.cismet.cids.custom.beans.lagis.BaumCustomBean;
import de.cismet.cids.custom.beans.lagis.FlurstueckSchluesselCustomBean;

import de.cismet.lagis.broker.CidsBroker;
import de.cismet.lagis.broker.LagisBroker;

import de.cismet.lagis.gui.panels.*;

import de.cismet.lagis.models.DefaultUniqueListModel;

import de.cismet.lagis.validation.ValidationStateChangedListener;

import de.cismet.lagisEE.entity.core.Flurstueck;
import de.cismet.lagisEE.entity.core.FlurstueckSchluessel;
import de.cismet.lagisEE.entity.extension.baum.Baum;

/**
 * DOCUMENT ME!
 *
 * @author   Sebastian Puhl
 * @version  $Revision$, $Date$
 */
public class AddExistingBaumPanel extends javax.swing.JPanel implements ValidationStateChangedListener,
    ListSelectionListener {

    //~ Instance fields --------------------------------------------------------

    private final Logger log = org.apache.log4j.Logger.getLogger(this.getClass());

    private BaumModel baumModel = new BaumModel();
    private BaumModel currentBaumTabelModel;
    private Flurstueck currentFlurstueck;
    private DefaultUniqueListModel currentListModel;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton Cancel;
    private javax.swing.JButton btnOK;
    private de.cismet.lagis.gui.panels.FlurstueckChooser flurstueckChooser1;
    private javax.swing.JLabel jLabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane;
    private javax.swing.JSeparator jSeparator;
    private javax.swing.JTable tblBaum;
    // End of variables declaration//GEN-END:variables

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates new form AddExistingVorgangPanel.
     *
     * @param  currentFlurstueck      DOCUMENT ME!
     * @param  currentBaumTabelModel  DOCUMENT ME!
     * @param  currentListModel       DOCUMENT ME!
     */
    public AddExistingBaumPanel(final Flurstueck currentFlurstueck,
            final BaumModel currentBaumTabelModel,
            final ListModel currentListModel) {
        this.currentFlurstueck = currentFlurstueck;
        // TODO UGLY if the model ever changed
        this.currentListModel = (DefaultUniqueListModel)currentListModel;
        this.currentBaumTabelModel = currentBaumTabelModel;
        initComponents();

        btnOK.setEnabled(false);
        tblBaum.setModel(baumModel);
        tblBaum.getSelectionModel().addListSelectionListener(this);
        final Flurstueck selectedFlurstueck = LagisBroker.getInstance().getCurrentFlurstueck();
        if ((selectedFlurstueck != null) && (selectedFlurstueck.getFlurstueckSchluessel() != null)) {
            if (log.isDebugEnabled()) {
                log.debug("Vorauswahl kann getroffen werden");
            }
            flurstueckChooser1.doAutomaticRequest(
                FlurstueckChooser.AutomaticFlurstueckRetriever.COPY_CONTENT_MODE,
                selectedFlurstueck.getFlurstueckSchluessel());
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Vorauswahl kann nicht getroffen werden");
            }
        }
        ((JXTable)tblBaum).packAll();
        flurstueckChooser1.addValidationStateChangedListener(this);
        flurstueckChooser1.addRemoveFilter(currentFlurstueck.getFlurstueckSchluessel());
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void validationStateChanged(final Object validatedObject) {
        if (log.isDebugEnabled()) {
            log.debug("Validation Status: " + flurstueckChooser1.getStatus());
        }
        if (flurstueckChooser1.getStatus() == flurstueckChooser1.VALID) {
            final FlurstueckSchluesselCustomBean currentKey = flurstueckChooser1.getCurrentFlurstueckSchluessel();
            final Collection<BaumCustomBean> baeume = CidsBroker.getInstance().getBaumForKey(currentKey);
            if (baeume != null) {
                // Check if the Contract ist already  added
                // if(currentFlurstueck != null && currentFlurstueck.getVertraege() != null){
                final Iterator<BaumCustomBean> it = currentBaumTabelModel.getAllBaeume().iterator();
                while (it.hasNext()) {
                    final Baum curBaum = it.next();
                    if (baeume.contains(curBaum)) {
                        baeume.remove(curBaum);
                    }
                }
                baumModel.refreshTableModel(baeume);
            } else {
                // TODO Meldung an Benutzer das keine Verträge vorhanden sind oder reicht leere Tabelle
                log.info("Es sind keine Baeume für das gewählte Flurstueck vorhanden");
            }
        } else {
            baumModel.refreshTableModel(null);
        }
    }

    @Override
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getSource().equals(tblBaum.getSelectionModel())) {
            if (log.isDebugEnabled()) {
                log.debug("Benutzer selektion changed");
                log.debug("Anzahl selektierter Spalten: " + tblBaum.getSelectedRowCount());
            }
            if (tblBaum.getSelectedRowCount() > 0) {
                btnOK.setEnabled(true);
            } else {
                btnOK.setEnabled(false);
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        jLabel2 = new javax.swing.JLabel();
        jLabel = new javax.swing.JLabel();
        jSeparator = new javax.swing.JSeparator();
        jScrollPane = new javax.swing.JScrollPane();
        tblBaum = new JXTable();
        btnOK = new javax.swing.JButton();
        Cancel = new javax.swing.JButton();
        flurstueckChooser1 = new de.cismet.lagis.gui.panels.FlurstueckChooser();

        jLabel2.setText("jLabel2");

        jLabel.setText("Flurstück");

        tblBaum.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][] {
                    { null, null, null, null },
                    { null, null, null, null },
                    { null, null, null, null },
                    { null, null, null, null }
                },
                new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        jScrollPane.setViewportView(tblBaum);

        btnOK.setText("OK");
        btnOK.addActionListener(new java.awt.event.ActionListener() {

                @Override
                public void actionPerformed(final java.awt.event.ActionEvent evt) {
                    btnOKActionPerformed(evt);
                }
            });

        Cancel.setText("Abbrechen");
        Cancel.addActionListener(new java.awt.event.ActionListener() {

                @Override
                public void actionPerformed(final java.awt.event.ActionEvent evt) {
                    CancelActionPerformed(evt);
                }
            });

        final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(
                        jSeparator,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        482,
                        Short.MAX_VALUE).addComponent(jLabel).addGroup(
                        javax.swing.GroupLayout.Alignment.TRAILING,
                        layout.createSequentialGroup().addGroup(
                            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(
                                layout.createSequentialGroup().addComponent(btnOK).addPreferredGap(
                                    javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(Cancel))
                                        .addComponent(
                                            jScrollPane,
                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                            477,
                                            Short.MAX_VALUE)).addGap(5, 5, 5)).addComponent(
                        flurstueckChooser1,
                        javax.swing.GroupLayout.PREFERRED_SIZE,
                        474,
                        javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(
                layout.createSequentialGroup().addContainerGap().addComponent(jLabel).addGap(1, 1, 1).addComponent(
                    flurstueckChooser1,
                    javax.swing.GroupLayout.PREFERRED_SIZE,
                    javax.swing.GroupLayout.DEFAULT_SIZE,
                    javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(
                    javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
                    jSeparator,
                    javax.swing.GroupLayout.PREFERRED_SIZE,
                    10,
                    javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(
                    javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
                    jScrollPane,
                    javax.swing.GroupLayout.DEFAULT_SIZE,
                    264,
                    Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(Cancel)
                                .addComponent(btnOK)).addContainerGap()));
    } // </editor-fold>//GEN-END:initComponents

    /**
     * DOCUMENT ME!
     */
    private void closeDialog() {
        ((JDialog)getParent().getParent().getParent().getParent()).dispose();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  evt  DOCUMENT ME!
     */
    private void btnOKActionPerformed(final java.awt.event.ActionEvent evt) { //GEN-FIRST:event_btnOKActionPerformed
        final int[] selectedRows = tblBaum.getSelectedRows();
        for (int i = 0; i < selectedRows.length; i++) {
            final BaumCustomBean curBaum = baumModel.getBaumAtRow(((JXTable)tblBaum).convertRowIndexToModel(
                        selectedRows[i]));
            currentBaumTabelModel.addBaum(curBaum);
            currentBaumTabelModel.fireTableDataChanged();
            final Collection<FlurstueckSchluesselCustomBean> crossRefs = CidsBroker.getInstance()
                        .getCrossReferencesForBaum(curBaum);
            if (crossRefs != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Es sind Querverweise auf die Baeume vorhanden");
                }
                final Iterator<FlurstueckSchluesselCustomBean> it = crossRefs.iterator();
                while (it.hasNext()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ein Querverweis hinzugefügt");
                    }
                    currentListModel.addElement(it.next());
                }
                currentListModel.removeElement(currentFlurstueck.getFlurstueckSchluessel());
            }
        }
        closeDialog();
    }                                                                         //GEN-LAST:event_btnOKActionPerformed

    /**
     * DOCUMENT ME!
     *
     * @param  evt  DOCUMENT ME!
     */
    private void CancelActionPerformed(final java.awt.event.ActionEvent evt) { //GEN-FIRST:event_CancelActionPerformed
        closeDialog();
    }                                                                          //GEN-LAST:event_CancelActionPerformed
}
