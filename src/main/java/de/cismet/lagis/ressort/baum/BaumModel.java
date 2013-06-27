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

import org.apache.log4j.Logger;

import java.util.*;

import javax.swing.DefaultListModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;

import de.cismet.cids.custom.beans.lagis.BaumCustomBean;
import de.cismet.cids.custom.beans.lagis.BaumKategorieAuspraegungCustomBean;
import de.cismet.cids.custom.beans.lagis.BaumKategorieCustomBean;
import de.cismet.cids.custom.beans.lagis.BaumNutzungCustomBean;

import de.cismet.cismap.commons.features.Feature;

import de.cismet.lagis.broker.LagisBroker;

import de.cismet.lagis.models.CidsBeanTableModel_Lagis;
import de.cismet.lagis.models.documents.SimpleDocumentModel;

import de.cismet.lagisEE.entity.extension.baum.Baum;
import de.cismet.lagisEE.entity.extension.baum.BaumKategorie;
import de.cismet.lagisEE.entity.extension.baum.BaumKategorieAuspraegung;
import de.cismet.lagisEE.entity.extension.baum.BaumNutzung;

/**
 * DOCUMENT ME!
 *
 * @author   Sebastian Puhl
 * @version  $Revision$, $Date$
 */
public class BaumModel extends CidsBeanTableModel_Lagis {

    //~ Static fields/initializers ---------------------------------------------

    private static final String[] COLUMN_HEADER = {
            "Lage",
            "Baumnummer",
            "Fläche m²",
            "Nutzung",
            "Baumbestand",
            "Ausprägung",
            "Auftragnehmer",
            "Erfassungsdatum",
            "Fälldatum",
        };

    private static final Class[] COLUMN_CLASSES = {
            String.class,
            String.class,
            Integer.class,
            String.class,
            BaumNutzung.class,
            Object.class,
            String.class,
            Date.class,
            Date.class
        };

    public static final int LAGE_COLUMN = 0;
    public static final int BAUMNUMMER_COLUMN = 1;
    public static final int FLAECHE_COLUMN = 2;
    public static final int ALTE_NUTZUNG_COLUMN = 3;
    public static final int BAUMBESTAND_COLUMN = 4;
    public static final int AUSPRAEGUNG_COLUMN = 5;
    public static final int AUFTRAGNEMER_COLUMN = 6;
    public static final int ERFASSUNGSDATUM_COLUMN = 7;
    public static final int FAELLDATUM_COLUMN = 8;

    //~ Instance fields --------------------------------------------------------

    private final Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    private SimpleDocumentModel bemerkungDocumentModel;
    private Baum currentSelectedBaum = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumModel object.
     */
    public BaumModel() {
        super(COLUMN_HEADER, COLUMN_CLASSES, BaumCustomBean.class);
        initDocumentModels();
    }

    /**
     * Creates a new BaumModel object.
     *
     * @param  baeume  DOCUMENT ME!
     */
    public BaumModel(final Collection<BaumCustomBean> baeume) {
        super(COLUMN_HEADER, COLUMN_CLASSES, baeume);
        initDocumentModels();
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        try {
            if (rowIndex >= getRowCount()) {
                log.warn("Cannot access row " + rowIndex + ". There are just " + getRowCount() + " rows.");
                return null;
            }

            final Baum value = getCidsBeanAtRow(rowIndex);
            switch (columnIndex) {
                case LAGE_COLUMN: {
                    return value.getLage();
                }
                case BAUMNUMMER_COLUMN: {
                    return value.getBaumnummer();
                }
                case FLAECHE_COLUMN: {
                    if (value.getFlaeche() != null) {
                        return value.getFlaeche().intValue();
                    } else {
                        return null;
                    }
                }
                case ALTE_NUTZUNG_COLUMN: {
                    return value.getAlteNutzung();
                }
                case BAUMBESTAND_COLUMN: {
                    if (value.getBaumNutzung() != null) {
                        return value.getBaumNutzung().getBaumKategorie();
                    } else {
                        return null;
                    }
                }
                case AUSPRAEGUNG_COLUMN: {
                    if ((value.getBaumNutzung() != null) && (value.getBaumNutzung().getBaumKategorie() != null)) {
                        return value.getBaumNutzung().getAusgewaehlteAuspraegung();
                    } else {
                        return null;
                    }
                }
                case AUFTRAGNEMER_COLUMN: {
                    return value.getAuftragnehmer();
                }
                case ERFASSUNGSDATUM_COLUMN: {
                    return value.getErfassungsdatum();
                }
                case FAELLDATUM_COLUMN: {
                    return value.getFaelldatum();
                }
                default: {
                    return "Spalte ist nicht definiert";
                }
            }
        } catch (Exception ex) {
            log.error("Fehler beim abrufen von Daten aus dem Modell: Zeile: " + rowIndex + " Spalte" + columnIndex, ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  rowIndex  DOCUMENT ME!
     */
    @Override
    public void removeCidsBean(final int rowIndex) {
        final Baum baum = getCidsBeanAtRow(rowIndex);
        if ((baum != null) && (baum.getGeometry() != null)) {
            LagisBroker.getInstance().getMappingComponent().getFeatureCollection().removeFeature(baum);
        }
        super.removeCidsBean(rowIndex);
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        if ((COLUMN_HEADER.length > columnIndex) && (getRowCount() > rowIndex) && isInEditMode()) {
            if (columnIndex == AUSPRAEGUNG_COLUMN) {
                final Baum currentBaum = getCidsBeanAtRow(rowIndex);
                if (((currentBaum != null) && (currentBaum.getBaumNutzung() != null)
                                && (currentBaum.getBaumNutzung().getBaumKategorie() != null))
                            || ((currentBaum.getBaumNutzung().getBaumKategorie().getKategorieAuspraegungen() != null)
                                && (currentBaum.getBaumNutzung().getBaumKategorie().getKategorieAuspraegungen()
                                    .size() > 0))) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<Feature> getAllBaumFeatures() {
        final ArrayList<Feature> tmp = new ArrayList<Feature>();
        if (getCidsBeans() != null) {
            final Iterator<BaumCustomBean> it = (Iterator<BaumCustomBean>)getCidsBeans().iterator();
            while (it.hasNext()) {
                final Baum curBaum = it.next();
                if (curBaum.getGeometry() != null) {
                    tmp.add(curBaum);
                }
            }
            return tmp;
        } else {
            return null;
        }
    }

    @Override
    public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
        try {
            final Baum value = getCidsBeanAtRow(rowIndex);
            switch (columnIndex) {
                case LAGE_COLUMN: {
                    value.setLage((String)aValue);
                    break;
                }
                case BAUMNUMMER_COLUMN: {
                    value.setBaumnummer((String)aValue);
                    break;
                }
                case FLAECHE_COLUMN: {
                    if (aValue != null) {
                        value.setFlaeche(((Integer)aValue).doubleValue());
                    } else {
                        value.setFlaeche(null);
                    }
                    break;
                }
                case ALTE_NUTZUNG_COLUMN: {
                    value.setAlteNutzung((String)aValue);
                    break;
                }
                case BAUMBESTAND_COLUMN: {
                    if ((aValue != null) && (aValue instanceof String)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Leersting wurde eingebenen --> entferene Ausgewählte Kategorien+Ausprägung");
                        }
                        value.setBaumNutzung(null);
                        return;
                    }

                    if (value.getBaumNutzung() == null) {
                        value.setBaumNutzung(BaumNutzungCustomBean.createNew());
                        value.getBaumNutzung().setBaumKategorie((BaumKategorieCustomBean)aValue);
                        if ((aValue != null) && (((BaumKategorie)aValue).getKategorieAuspraegungen() != null)
                                    && (((BaumKategorie)aValue).getKategorieAuspraegungen().size() == 1)) {
                            for (final BaumKategorieAuspraegungCustomBean currentAuspraegung
                                        : ((BaumKategorie)aValue).getKategorieAuspraegungen()) {
                                value.getBaumNutzung().setAusgewaehlteAuspraegung(currentAuspraegung);
                            }
                        }
                    } else {
                        BaumKategorie oldKategory = null;
                        if (((oldKategory = value.getBaumNutzung().getBaumKategorie()) != null) && (aValue != null)) {
                            if (!oldKategory.equals(aValue)) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Kategorie hat sich geändert --> Ausprägung ist nicht mehr gültig");
                                }
                                value.getBaumNutzung().setAusgewaehlteAuspraegung(null);
                            }
                        }
                        value.getBaumNutzung().setBaumKategorie((BaumKategorieCustomBean)aValue);
                        if ((aValue != null) && (((BaumKategorie)aValue).getKategorieAuspraegungen() != null)
                                    && (((BaumKategorie)aValue).getKategorieAuspraegungen().size() == 1)) {
                            for (final BaumKategorieAuspraegungCustomBean currentAuspraegung
                                        : ((BaumKategorie)aValue).getKategorieAuspraegungen()) {
                                value.getBaumNutzung().setAusgewaehlteAuspraegung(currentAuspraegung);
                            }
                        }
                    }
                    break;
                }
                case AUSPRAEGUNG_COLUMN: {
                    if (log.isDebugEnabled()) {
                        log.debug("set AuspraegungColumn:" + aValue.getClass());
                    }
                    if (value.getBaumNutzung() == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Nutzung == null --> new BaumNutzung");
                        }
                        value.setBaumNutzung(BaumNutzungCustomBean.createNew());
                    } else if ((aValue != null) && (aValue instanceof BaumKategorieAuspraegung)) {
                        if (log.isDebugEnabled()) {
                            log.debug("instance of BaumKategorieAuspraegung");
                        }
                        value.getBaumNutzung().setAusgewaehlteAuspraegung((BaumKategorieAuspraegungCustomBean)aValue);
                    } else if ((aValue != null) && (aValue instanceof Integer)) {
                        if (log.isDebugEnabled()) {
                            log.debug("instance of integer");
                        }
                        value.getBaumNutzung().setAusgewaehlteAuspraegung(null);
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("else");
                        }
                        value.getBaumNutzung().setAusgewaehlteAuspraegung(null);
                    }
                    break;
                }
                case AUFTRAGNEMER_COLUMN: {
                    if ((aValue != null) && ((String)aValue).equals("")) {
                        value.setAuftragnehmer(null);
                    } else {
                        value.setAuftragnehmer((String)aValue);
                    }
                    break;
                }
                case ERFASSUNGSDATUM_COLUMN: {
                    if ((aValue instanceof Date) || (aValue == null)) {
                        value.setErfassungsdatum((Date)aValue);
                    } // else if(aValue == null){
//                        value.setVertragsbeginn(null);
//                    }
                    break;
                }
                case FAELLDATUM_COLUMN: {
                    if ((aValue instanceof Date) || (aValue == null)) {
                        value.setFaelldatum((Date)aValue);
                    }
                    break;
                }
                default: {
                    log.warn("Keine Spalte für angegebenen Index vorhanden: " + columnIndex);
                    return;
                }
            }
            fireTableDataChanged();
        } catch (Exception ex) {
            log.error("Fehler beim setzen von Daten in dem Modell: Zeile: " + rowIndex + " Spalte" + columnIndex, ex);
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void initDocumentModels() {
        bemerkungDocumentModel = new SimpleDocumentModel() {

                @Override
                public void assignValue(final String newValue) {
                    if (log.isDebugEnabled()) {
                        log.debug("Bemerkung assigned");
                        log.debug("new Value: " + newValue);
                    }
                    valueToCheck = newValue;
                    fireValidationStateChanged(this);
                    if ((currentSelectedBaum != null) && (getStatus() == VALID)) {
                        currentSelectedBaum.setBemerkung(newValue);
                    }
                }
            };
    }

    /**
     * DOCUMENT ME!
     */
    public void clearSlaveComponents() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Clear Slave Components");
            }
            bemerkungDocumentModel.clear(0, bemerkungDocumentModel.getLength());
        } catch (Exception ex) {
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public SimpleDocumentModel getBemerkungDocumentModel() {
        return bemerkungDocumentModel;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  newBaum  DOCUMENT ME!
     */
    public void setCurrentSelectedBaum(final Baum newBaum) {
        currentSelectedBaum = newBaum;
        if (currentSelectedBaum != null) {
            try {
                bemerkungDocumentModel.clear(0, bemerkungDocumentModel.getLength());
                bemerkungDocumentModel.insertString(0, currentSelectedBaum.getBemerkung(), null);
            } catch (BadLocationException ex) {
                // TODO Böse
                log.error("Fehler beim setzen des BemerkungsModells: ", ex);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("nichts selektiert lösche Felder");
            }
            clearSlaveComponents();
        }
    }
}
