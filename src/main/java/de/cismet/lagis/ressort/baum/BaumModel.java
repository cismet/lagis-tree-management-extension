/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.lagis.ressort.baum;

import de.cismet.cismap.commons.features.Feature;
import de.cismet.lagis.broker.LagisBroker;
import de.cismet.lagis.models.documents.SimpleDocumentModel;
import de.cismet.lagisEE.entity.extension.baum.Baum;
import de.cismet.lagisEE.entity.extension.baum.BaumKategorie;
import de.cismet.lagisEE.entity.extension.baum.BaumKategorieAuspraegung;
import de.cismet.lagisEE.entity.extension.baum.BaumNutzung;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;

/**
 *
 * @author Sebastian Puhl
 */
public class BaumModel extends AbstractTableModel  {

    private final Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    Vector<Baum> baeume;
    private final static String[] COLUMN_HEADER = {"Lage", "Baumnummer", "Fläche m²", "Nutzung", "Baumbestand", "Ausprägung", "Auftragnehmer", "Erfassungsdatum", "Fälldatum",};
    private boolean isInEditMode = false;
    private SimpleDocumentModel bemerkungDocumentModel;
    private Baum currentSelectedBaum = null;
    private DefaultListModel baumMerkmalsModel;
    public static final int LAGE_COLUMN = 0;
    public static final int BAUMNUMMER_COLUMN = 1;
    public static final int FLAECHE_COLUMN = 2;
    public static final int ALTE_NUTZUNG_COLUMN = 3;
    public static final int BAUMBESTAND_COLUMN = 4;
    public static final int AUSPRAEGUNG_COLUMN = 5;
    public static final int AUFTRAGNEMER_COLUMN = 6;
    public static final int ERFASSUNGSDATUM_COLUMN = 7;
    public static final int FAELLDATUM_COLUMN = 8;

    public BaumModel() {
        baeume = new Vector<Baum>();
        initDocumentModels();
        baumMerkmalsModel = new DefaultListModel();
    }

    public BaumModel(Set<Baum> baeume) {
        try {
            this.baeume = new Vector<Baum>(baeume);
        } catch (Exception ex) {
            log.error("Fehler beim anlegen des Models", ex);
            this.baeume = new Vector<Baum>();
        }        
        initDocumentModels();
        baumMerkmalsModel = new DefaultListModel();
    }

    public int getColumnCount() {
        return COLUMN_HEADER.length;
    }

    public int getRowCount() {
        return baeume.size();
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_HEADER[column];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            Baum value = baeume.get(rowIndex);
            switch (columnIndex) {
                case LAGE_COLUMN:
                    return value.getLage();
                case BAUMNUMMER_COLUMN:
                    return value.getBaumnummer();
                case FLAECHE_COLUMN:
                    if (value.getFlaeche() != null) {
                        return value.getFlaeche().intValue();                        
                    } else {
                        return null;
                    }
                case ALTE_NUTZUNG_COLUMN:
                    return value.getAlteNutzung();
                case BAUMBESTAND_COLUMN:
                    if (value.getBaumNutzung() != null) {
                        return value.getBaumNutzung().getBaumKategorie();
                    } else {
                        return null;
                    }
                case AUSPRAEGUNG_COLUMN:
                    if (value.getBaumNutzung() != null && value.getBaumNutzung().getBaumKategorie() != null) {
                        return value.getBaumNutzung().getAusgewaehlteAuspraegung();
                    } else {
                        return null;
                    }
                case AUFTRAGNEMER_COLUMN:
                    return value.getAuftragnehmer();
                case ERFASSUNGSDATUM_COLUMN:
                    return value.getErfassungsdatum();
                case FAELLDATUM_COLUMN:
                    return value.getFaelldatum();
                default:
                    return "Spalte ist nicht definiert";
            }
        } catch (Exception ex) {
            log.error("Fehler beim abrufen von Daten aus dem Modell: Zeile: " + rowIndex + " Spalte" + columnIndex, ex);
            return null;
        }
    }

    public void addBaum(Baum baum) {
        baeume.add(baum);
    }

    public Baum getBaumAtRow(int rowIndex) {
        return baeume.get(rowIndex);
    }

    public void removeBaum(int rowIndex) {
        Baum baum = baeume.get(rowIndex);
        if (baum != null && baum.getGeometry() != null) {
            LagisBroker.getInstance().getMappingComponent().getFeatureCollection().removeFeature(baum);
        }
        baeume.remove(rowIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if ((COLUMN_HEADER.length > columnIndex) && (baeume.size() > rowIndex) && isInEditMode) {
            if (columnIndex == AUSPRAEGUNG_COLUMN) {
                Baum currentBaum = getBaumAtRow(rowIndex);
                if (currentBaum != null && currentBaum.getBaumNutzung() != null && currentBaum.getBaumNutzung().getBaumKategorie() != null  || (currentBaum.getBaumNutzung().getBaumKategorie().getKategorieAuspraegungen() != null && currentBaum.getBaumNutzung().getBaumKategorie().getKategorieAuspraegungen().size() > 0)) {
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

    public void setIsInEditMode(boolean isEditable) {
        isInEditMode = isEditable;
    }

    public Vector<Feature> getAllBaumFeatures() {
        Vector<Feature> tmp = new Vector<Feature>();
        if (baeume != null) {
            Iterator<Baum> it = baeume.iterator();
            while (it.hasNext()) {
                Baum curBaum = it.next();
                if (curBaum.getGeometry() != null) {
                    tmp.add(curBaum);
                }
            }
            return tmp;
        } else {
            return null;
        }
    }

    public void refreshTableModel(Set<Baum> baeume) {
        try {
            log.debug("Refresh des BaeumeTableModell");
            this.baeume = new Vector<Baum>(baeume);
        } catch (Exception ex) {
            log.error("Fehler beim refreshen des Models", ex);
            this.baeume = new Vector<Baum>();
        }
        fireTableDataChanged();
    }

    public Vector<Baum> getAllBaeume() {
        return baeume;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            Baum value = baeume.get(rowIndex);
            switch (columnIndex) {
                case LAGE_COLUMN:
                    value.setLage((String) aValue);
                    break;
                case BAUMNUMMER_COLUMN:
                    value.setBaumnummer((String) aValue);
                    break;
                case FLAECHE_COLUMN:
                    if (aValue != null) {
                        value.setFlaeche(((Integer) aValue).doubleValue());
                    } else {
                        value.setFlaeche(null);
                    }
                    break;
                case ALTE_NUTZUNG_COLUMN:
                    value.setAlteNutzung((String) aValue);
                    break;
                case BAUMBESTAND_COLUMN:
                    if(aValue != null && aValue instanceof String){
                        log.debug("Leersting wurde eingebenen --> entferene Ausgewählte Kategorien+Ausprägung");
                        value.setBaumNutzung(null);
                        return;
                    }
                    
                    if (value.getBaumNutzung() == null) {
                        value.setBaumNutzung(new BaumNutzung());
                        value.getBaumNutzung().setBaumKategorie((BaumKategorie) aValue);
                        if (aValue != null && ((BaumKategorie) aValue).getKategorieAuspraegungen() != null && ((BaumKategorie) aValue).getKategorieAuspraegungen().size() == 1) {
                            for (BaumKategorieAuspraegung currentAuspraegung : ((BaumKategorie) aValue).getKategorieAuspraegungen()) {
                                value.getBaumNutzung().setAusgewaehlteAuspraegung(currentAuspraegung);
                            }
                        }
                    } else {
                        BaumKategorie oldKategory = null;
                        if ((oldKategory = value.getBaumNutzung().getBaumKategorie()) != null && aValue != null) {
                            if (!oldKategory.equals(aValue)) {
                                log.debug("Kategorie hat sich geändert --> Ausprägung ist nicht mehr gültig");
                                value.getBaumNutzung().setAusgewaehlteAuspraegung(null);                                
                            }
                        }
                        value.getBaumNutzung().setBaumKategorie((BaumKategorie) aValue);
                        if (aValue != null && ((BaumKategorie) aValue).getKategorieAuspraegungen() != null && ((BaumKategorie) aValue).getKategorieAuspraegungen().size() == 1) {
                            for (BaumKategorieAuspraegung currentAuspraegung : ((BaumKategorie) aValue).getKategorieAuspraegungen()) {
                                value.getBaumNutzung().setAusgewaehlteAuspraegung(currentAuspraegung);
                            }
                        }
                    }
                    break;
                case AUSPRAEGUNG_COLUMN:
                    log.debug("set AuspraegungColumn:"+aValue.getClass());
                    if (value.getBaumNutzung() == null) {
                        log.debug("Nutzung == null --> new BaumNutzung");
                        value.setBaumNutzung(new BaumNutzung());
                    } else if (aValue != null && aValue instanceof BaumKategorieAuspraegung) {
                        log.debug("instance of BaumKategorieAuspraegung");
                        value.getBaumNutzung().setAusgewaehlteAuspraegung((BaumKategorieAuspraegung) aValue);
                    } else if (aValue != null && aValue instanceof Integer) {
                        log.debug("instance of integer");
                        value.getBaumNutzung().setAusgewaehlteAuspraegung(null);                        
                    } else {
                        log.debug("else");
                        value.getBaumNutzung().setAusgewaehlteAuspraegung(null);                        
                    }
                    break;
                case AUFTRAGNEMER_COLUMN:
                    if(aValue != null && ((String)aValue).equals("")){
                        value.setAuftragnehmer(null);
                    } else {
                        value.setAuftragnehmer((String) aValue);
                    }                    
                    break;
                case ERFASSUNGSDATUM_COLUMN:
                    if (aValue instanceof Date || aValue == null) {
                        value.setErfassungsdatum((Date) aValue);
                    } // else if(aValue == null){
//                        value.setVertragsbeginn(null);
//                    }                    
                    break;
                case FAELLDATUM_COLUMN:
                    if (aValue instanceof Date || aValue == null) {
                        value.setFaelldatum((Date) aValue);
                    }
                    break;
                default:
                    log.warn("Keine Spalte für angegebenen Index vorhanden: "+columnIndex);
                    return;
            }
            fireTableDataChanged();
        } catch (Exception ex) {
            log.error("Fehler beim setzen von Daten in dem Modell: Zeile: " + rowIndex + " Spalte" + columnIndex, ex);

        }
    }

    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case LAGE_COLUMN:
                return String.class;
            case BAUMNUMMER_COLUMN:
                return String.class;
            case FLAECHE_COLUMN:
                return Integer.class;
            case ALTE_NUTZUNG_COLUMN:
                return String.class;
            case BAUMBESTAND_COLUMN:
                return BaumNutzung.class;
            case AUSPRAEGUNG_COLUMN:
                return Object.class;
            case AUFTRAGNEMER_COLUMN:
                return String.class;
            case ERFASSUNGSDATUM_COLUMN:
                return Date.class;
            case FAELLDATUM_COLUMN:
                return Date.class;
            default:
                log.warn("Die gewünschte Spalte exitiert nicht, es kann keine Klasse zurück geliefert werden");
                return null;
        }
    }

    private void initDocumentModels() {
        bemerkungDocumentModel = new SimpleDocumentModel() {

            public void assignValue(String newValue) {
                log.debug("Bemerkung assigned");
                log.debug("new Value: " + newValue);
                valueToCheck = newValue;
                fireValidationStateChanged(this);
                if (currentSelectedBaum != null && getStatus() == VALID) {
                    currentSelectedBaum.setBemerkung(newValue);
                }
            }
        };
    }

    public int getIndexOfBaum(Baum baum) {
        return baeume.indexOf(baum);
    }

    public void clearSlaveComponents() {
        try {
            log.debug("Clear Slave Components");
            bemerkungDocumentModel.clear(0, bemerkungDocumentModel.getLength());
        } catch (Exception ex) {
        }
    }

    public SimpleDocumentModel getBemerkungDocumentModel() {
        return bemerkungDocumentModel;
    }

    public void setCurrentSelectedBaum(Baum newBaum) {
        currentSelectedBaum = newBaum;
        if (currentSelectedBaum != null) {
            try {
                bemerkungDocumentModel.clear(0, bemerkungDocumentModel.getLength());
                bemerkungDocumentModel.insertString(0, currentSelectedBaum.getBemerkung(), null);
            } catch (BadLocationException ex) {
                //TODO Böse
                log.error("Fehler beim setzen des BemerkungsModells: ", ex);
            }
        } else {
            log.debug("nichts selektiert lösche Felder");
            clearSlaveComponents();
        }
    }
}
