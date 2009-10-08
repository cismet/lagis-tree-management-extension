/*
 * BaumRessortWidget.java
 *
 * Created on 23. April 2008, 11:26
 */
package de.cismet.lagis.ressort.baum;

import de.cismet.cismap.commons.features.Feature;
import de.cismet.cismap.commons.features.FeatureCollectionEvent;
import de.cismet.cismap.commons.features.FeatureCollectionListener;
import de.cismet.cismap.commons.gui.MappingComponent;
import de.cismet.lagis.broker.EJBroker;
import de.cismet.lagis.broker.LagisBroker;
import de.cismet.lagis.editor.DateEditor;
import de.cismet.lagis.gui.checkbox.JCheckBoxList;
import de.cismet.lagis.interfaces.FeatureSelectionChangedListener;
import de.cismet.lagis.interfaces.FlurstueckChangeListener;
import de.cismet.lagis.interfaces.FlurstueckSaver;
import de.cismet.lagis.interfaces.GeometrySlotProvider;
import de.cismet.lagis.models.DefaultUniqueListModel;
import de.cismet.lagis.renderer.DateRenderer;
import de.cismet.lagis.renderer.FlurstueckSchluesselRenderer;
import de.cismet.lagis.thread.BackgroundUpdateThread;
import de.cismet.lagis.utillity.GeometrySlotInformation;
import de.cismet.lagis.validation.Validatable;
import de.cismet.lagis.validation.Validator;
import de.cismet.lagis.widget.AbstractWidget;
import de.cismet.lagisEE.entity.core.Flurstueck;
import de.cismet.lagisEE.entity.core.FlurstueckSchluessel;
import de.cismet.lagisEE.entity.core.hardwired.FlurstueckArt;
import de.cismet.lagisEE.entity.extension.baum.Baum;
import de.cismet.lagisEE.entity.extension.baum.BaumKategorie;
import de.cismet.lagisEE.entity.extension.baum.BaumKategorieAuspraegung;
import de.cismet.lagisEE.entity.extension.baum.BaumMerkmal;
import de.cismet.lagisEE.entity.extension.baum.BaumNutzung;
import de.cismet.tools.CurrentStackTrace;
import edu.umd.cs.piccolo.PCanvas;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.jdesktop.swingx.decorator.SortOrder;

/**
 *
 * @author  Sebastian Puhl
 */
public class BaumRessortWidget extends AbstractWidget implements FlurstueckChangeListener, FlurstueckSaver, MouseListener, ListSelectionListener, ItemListener, GeometrySlotProvider, FeatureSelectionChangedListener, FeatureCollectionListener, TableModelListener {

    private final Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    private boolean isFlurstueckEditable = true;
    private boolean isInEditMode = false;
    private BaumModel baumModel = new BaumModel();
    private BackgroundUpdateThread<Flurstueck> updateThread;
    private static final String PROVIDER_NAME = "Baum";
    private Flurstueck currentFlurstueck;
    private Validator valTxtBemerkung;
    private ImageIcon icoExistingContract = new javax.swing.ImageIcon(getClass().getResource("/de/cismet/lagis/ressource/icons/toolbar/contract.png"));
    private JComboBox cbxAuspraegung = new JComboBox();
    private boolean ignoreFeatureSelectionEvent = false;
    private final ActionListener cboAuspraegungsActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent event) {

        }
    };

    /** Creates new form BaumRessortWidget */
    public BaumRessortWidget(String widgetName, String iconPath) {
        initComponents();
        setWidgetName(widgetName);
        setWidgetIcon(iconPath);
        configureComponents();
        configBackgroundThread();
        setOpaqueRecursive(panBackground.getComponents());
    }

    private void setOpaqueRecursive(Component[] components) {
        for (Component currentComp : components) {
            if (currentComp instanceof Container) {
                setOpaqueRecursive(((Container) currentComp).getComponents());
            }
            if (currentComp instanceof JComponent) {
                ((JComponent) currentComp).setOpaque(false);
            }
        }
    }

    private void configureComponents() {
        tblBaum.setModel(baumModel);
        tblBaum.setDefaultEditor(Date.class, new DateEditor());
        tblBaum.setDefaultRenderer(Date.class, new DateRenderer());
        tblBaum.addMouseListener(this);
        
        HighlightPredicate noGeometryPredicate = new HighlightPredicate() {

            
           public boolean isHighlighted(Component renderer, ComponentAdapter componentAdapter) {
                int displayedIndex = componentAdapter.row;
                int modelIndex = ((JXTable) tblBaum).getFilters().convertRowIndexToModel(displayedIndex);
                Baum mp = baumModel.getBaumAtRow(modelIndex);
                return mp != null && mp.getGeometry() == null;
            }
        };
        Highlighter noGeometryHighlighter = new ColorHighlighter(noGeometryPredicate, LagisBroker.grey, null);        
        HighlightPredicate contractExpiredPredicate = new HighlightPredicate() {

            
           public boolean isHighlighted(Component renderer, ComponentAdapter componentAdapter) {
                int displayedIndex = componentAdapter.row;
                int modelIndex = ((JXTable) tblBaum).getFilters().convertRowIndexToModel(displayedIndex);
                Baum mp = baumModel.getBaumAtRow(modelIndex);
                return mp != null && mp.getFaelldatum() != null && mp.getErfassungsdatum() != null && mp.getFaelldatum().getTime() < System.currentTimeMillis();
            }
        };
        Highlighter contractExpiredHighlighter = new ColorHighlighter(contractExpiredPredicate, LagisBroker.SUCCESSFUL_COLOR, null);

        ((JXTable) tblBaum).setHighlighters(LagisBroker.ALTERNATE_ROW_HIGHLIGHTER,contractExpiredHighlighter,noGeometryHighlighter);

        Comparator dateComparator = new Comparator() {

            public int compare(Object o1, Object o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                } else if (o1 == null) {
                    return 1;
                } else if (o2 == null) {
                    return -1;
                } else {
                    return -1 * ((Date) o1).compareTo((Date) o2);
                }
            }
        };
        ((JXTable) tblBaum).getColumnExt(baumModel.FAELLDATUM_COLUMN).setComparator(dateComparator);
        ((JXTable) tblBaum).setSortOrder(baumModel.FAELLDATUM_COLUMN, SortOrder.ASCENDING);
        tblBaum.getSelectionModel().addListSelectionListener(this);
        ((JXTable) tblBaum).setColumnControlVisible(true);
        ((JXTable) tblBaum).setHorizontalScrollEnabled(true);        
        TableColumn tc = tblBaum.getColumnModel().getColumn(baumModel.BAUMBESTAND_COLUMN);
        // Kategorien EditorCombobox
        final JComboBox combo = new JComboBox();
        combo.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
        combo.setEditable(true);
        Set<BaumKategorie> alleKategorien = EJBroker.getInstance().getAllBaumKategorien();
        for (BaumKategorie currentKategorie : alleKategorien) {
            combo.addItem(currentKategorie);
        }
        combo.addItem("");
        org.jdesktop.swingx.autocomplete.AutoCompleteDecorator.decorate(combo);
        combo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {          
            }
        });
        tc.setCellEditor(new org.jdesktop.swingx.autocomplete.ComboBoxCellEditor(combo));

        tc = tblBaum.getColumnModel().getColumn(baumModel.AUSPRAEGUNG_COLUMN);
        cbxAuspraegung.setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0));
        cbxAuspraegung.setEditable(true);
        org.jdesktop.swingx.autocomplete.AutoCompleteDecorator.decorate(cbxAuspraegung);
        cbxAuspraegung.addActionListener(cboAuspraegungsActionListener);
        tc.setCellEditor(new org.jdesktop.swingx.autocomplete.ComboBoxCellEditor(cbxAuspraegung));


        ((JXTable) tblBaum).packAll();

        taBemerkung.setDocument(baumModel.getBemerkungDocumentModel());

        enableSlaveComponents(false);
        //lstMerkmale.setm        
        Set<BaumMerkmal> baumMerkmal = EJBroker.getInstance().getAllBaumMerkmale();
        Vector<MerkmalCheckBox> merkmalCheckBoxes = new Vector<MerkmalCheckBox>();
        if (baumMerkmal != null && baumMerkmal.size() > 0) {
            for (BaumMerkmal currentMerkmal : baumMerkmal) {
                if (currentMerkmal != null && currentMerkmal.getBezeichnung() != null) {
                    MerkmalCheckBox newMerkmalCheckBox = new MerkmalCheckBox(currentMerkmal);
                    setOpaqueRecursive(newMerkmalCheckBox.getComponents());
                    newMerkmalCheckBox.setOpaque(false);
                    newMerkmalCheckBox.addItemListener(this);
                    merkmalCheckBoxes.add(newMerkmalCheckBox);
                }
            }
        }
        lstMerkmale.setListData(merkmalCheckBoxes);

        lstCrossRefs.setCellRenderer(new FlurstueckSchluesselRenderer());
        lstCrossRefs.setModel(new DefaultUniqueListModel());
        lstCrossRefs.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstCrossRefs.addMouseListener(this);
        lstCrossRefs.setCellRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                String s = null;
                if (value instanceof FlurstueckSchluessel) {
                    s = ((FlurstueckSchluessel) value).getKeyString();
                } else {
                    s = value.toString();
                }

                setText(s);
                setOpaque(false);

                setEnabled(list.isEnabled());
                setFont(list.getFont());
                return this;
            }
        });

        PCanvas pc = LagisBroker.getInstance().getMappingComponent().getSelectedObjectPresenter();
        pc.setBackground(this.getBackground());
        LagisBroker.getInstance().getMappingComponent().getFeatureCollection().addFeatureCollectionListener(this);
        baumModel.addTableModelListener(this);
    }

    private void updateCbxAuspraegung(Baum mp) {
        log.debug("Update der Ausprägungen");
        cbxAuspraegung.removeActionListener(cboAuspraegungsActionListener);
        cbxAuspraegung.removeAllItems();        
        if (mp != null && mp.getBaumNutzung() != null && mp.getBaumNutzung().getBaumKategorie() != null && mp.getBaumNutzung().getBaumKategorie().getKategorieAuspraegungen() != null) {
            log.debug("Ausprägungen sind vorhanden");
            Set<BaumKategorieAuspraegung> auspraegungen = mp.getBaumNutzung().getBaumKategorie().getKategorieAuspraegungen();
            for (BaumKategorieAuspraegung currentAuspraegung : auspraegungen) {
                log.debug("currentAusprägung: "+currentAuspraegung);
                cbxAuspraegung.addItem(currentAuspraegung);
            }
            cbxAuspraegung.addItem("");
        } else {
            log.debug("Keine Ausprägungen vorhanden");
        }
        cbxAuspraegung.validate();
        cbxAuspraegung.repaint();
        cbxAuspraegung.updateUI();
        cbxAuspraegung.addActionListener(cboAuspraegungsActionListener);
    }

    private void configBackgroundThread() {
        updateThread = new BackgroundUpdateThread<Flurstueck>() {

            protected void update() {
                try {
                    if (isUpdateAvailable()) {
                        cleanup();
                        return;
                    }
                    clearComponent();
                    if (isUpdateAvailable()) {
                        cleanup();
                        return;
                    }
                    FlurstueckArt flurstueckArt = getCurrentObject().getFlurstueckSchluessel().getFlurstueckArt();
                    if (flurstueckArt != null && flurstueckArt.getBezeichnung().equals(FlurstueckArt.FLURSTUECK_ART_BEZEICHNUNG_STAEDTISCH)) {
                        log.debug("Flurstück ist städtisch und kann editiert werden");
                        isFlurstueckEditable = true;
                    } else {
                        log.debug("Flurstück ist nicht städtisch und kann nicht editiert werden");
                        isFlurstueckEditable = false;
                    }
                    baumModel.refreshTableModel(getCurrentObject().getBaeume());
                    if (isUpdateAvailable()) {
                        cleanup();
                        return;
                    }
                    Set<FlurstueckSchluessel> crossRefs = getCurrentObject().getBaeumeQuerverweise();
                    if (crossRefs != null && crossRefs.size() > 0) {
                        lstCrossRefs.setModel(new DefaultUniqueListModel(crossRefs));
                    }
                    if (isUpdateAvailable()) {
                        cleanup();
                        return;
                    }
                    EventQueue.invokeLater(new Runnable() {

                        public void run() {
                            Vector<Feature> features = baumModel.getAllBaumFeatures();
                            if (features != null) {
                                for (Feature currentFeature : features) {
                                    if (currentFeature != null) {
                                        if (isWidgetReadOnly()) {
                                            ((Baum) currentFeature).setModifiable(false);
                                        }
                                        LagisBroker.getInstance().getMappingComponent().getFeatureCollection().addFeature(currentFeature);
                                    }
                                }
                            }
                            ((JXTable) tblBaum).packAll();
                        }
                    });
                    if (isUpdateAvailable()) {
                        cleanup();
                        return;
                    }

                    LagisBroker.getInstance().flurstueckChangeFinished(BaumRessortWidget.this);
                } catch (Exception ex) {
                    log.error("Fehler im refresh thread: ", ex);
                    LagisBroker.getInstance().flurstueckChangeFinished(BaumRessortWidget.this);
                }
            }

            protected void cleanup() {
            }
        };
        updateThread.setPriority(Thread.NORM_PRIORITY);
        updateThread.start();
    }

    @Override
    public void clearComponent() {
        log.debug("clearComponent", new CurrentStackTrace());
        baumModel.clearSlaveComponents();
        deselectAllListEntries();
        baumModel.refreshTableModel(new HashSet<Baum>());
        lstCrossRefs.setModel(new DefaultUniqueListModel());
        if (EventQueue.isDispatchThread()) {
            lstCrossRefs.updateUI();
            lstCrossRefs.repaint();
        } else {
            EventQueue.invokeLater(new Runnable() {

                public void run() {
                    lstCrossRefs.updateUI();
                    lstCrossRefs.repaint();
                }
            });
        }
    }

    public void deselectAllListEntries() {
        log.debug("deselect all entries", new CurrentStackTrace());
        for (int i = 0; i < lstMerkmale.getModel().getSize(); i++) {
            MerkmalCheckBox currentCheckBox = (MerkmalCheckBox) lstMerkmale.getModel().getElementAt(i);
            currentCheckBox.removeItemListener(this);
            currentCheckBox.setSelected(false);
            currentCheckBox.addItemListener(this);
        }
        lstMerkmale.repaint();
    }

    @Override
    public void refresh(Object arg0) {
    }

    @Override
    public void setComponentEditable(boolean isEditable) {
        if (isFlurstueckEditable) {
            log.debug("BaumRessortWidget --> setComponentEditable");
            isInEditMode = isEditable;
            baumModel.setIsInEditMode(isEditable);
            TableCellEditor currentEditor = tblBaum.getCellEditor();
            if (currentEditor != null) {
                currentEditor.cancelCellEditing();
            }

            if (isEditable && tblBaum.getSelectedRow() != -1) {
                log.debug("Editable und TabellenEintrag ist gewählt");
                btnRemoveBaum.setEnabled(true);
                enableSlaveComponents(isEditable);
            } else if (!isEditable) {
                deselectAllListEntries();
                enableSlaveComponents(isEditable);
                btnRemoveBaum.setEnabled(isEditable);
            }

            btnAddExitingBaum.setEnabled(isEditable);
            btnAddBaum.setEnabled(isEditable);
            baumModel.setIsInEditMode(isEditable);
            log.debug("BaumRessortWidget --> setComponentEditable finished");
        } else {
            log.debug("Flurstück ist nicht städtisch Bäume können nicht editiert werden");
        }
    }

    public void flurstueckChanged(Flurstueck newFlurstueck) {
        try {
            log.info("FlurstueckChanged");
            currentFlurstueck = newFlurstueck;
            updateThread.notifyThread(currentFlurstueck);
        } catch (Exception ex) {
            log.error("Fehler beim Flurstückswechsel: ", ex);
            LagisBroker.getInstance().flurstueckChangeFinished(BaumRessortWidget.this);
        }
    }

    public void updateFlurstueckForSaving(Flurstueck flurstueck) {
        Set<Baum> baeume = flurstueck.getBaeume();
        log.debug("Anzahl baeume aktuell gespeichert im Flurstück");
        log.debug("Anzahl Baueme im tablemodel: "+baumModel.getAllBaeume().size());
        if (baeume != null) {
            log.debug("warens schon baueme vorhanden");
            baeume.clear();
            baeume.addAll(baumModel.getAllBaeume());
            log.debug("baueme im Flurstueck Set: "+baeume.size());
        } else {
            log.debug("Waren noch keine Baueme vorhanden");
            HashSet newSet = new HashSet();
            newSet.addAll(baumModel.getAllBaeume());
            flurstueck.setBaeume(newSet);
        }
    }

    public void mouseClicked(MouseEvent e) {
        Object source = e.getSource();
        if (source instanceof JXTable) {
            log.debug("Mit maus auf BaumTaeble geklickt");
            int selecetdRow = tblBaum.getSelectedRow();
            if (selecetdRow != -1) {
                if (isInEditMode) {
                    enableSlaveComponents(true);
                    btnRemoveBaum.setEnabled(true);
                } else {
                    enableSlaveComponents(false);
                    log.debug("Liste ausgeschaltet");
                    if (selecetdRow == -1) {
                        deselectAllListEntries();
                    }
                }
            } else {
                //currentSelectedRebe = null;
                btnRemoveBaum.setEnabled(false);
            }
        } else if (source instanceof JList) {
            if (e.getClickCount() > 1) {
                FlurstueckSchluessel key = (FlurstueckSchluessel) lstCrossRefs.getSelectedValue();
                if (key != null) {
                    LagisBroker.getInstance().loadFlurstueck(key);
                }
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        mouseClicked(e);
    }

    public void mouseReleased(MouseEvent e) {
    }

    private void enableSlaveComponents(boolean isEnabled) {
        taBemerkung.setEditable(isEnabled);
        lstMerkmale.setEnabled(isEnabled);
    }

    public void valueChanged(ListSelectionEvent e) {
        log.debug("SelectionChanged Baum");
        MappingComponent mappingComp = LagisBroker.getInstance().getMappingComponent();
        final int viewIndex = tblBaum.getSelectedRow();
        if (viewIndex != -1) {
            if (isInEditMode) {
                btnRemoveBaum.setEnabled(true);
            } else {
                btnRemoveBaum.setEnabled(false);
            }

            final int index = ((JXTable) tblBaum).getFilters().convertRowIndexToModel(viewIndex);
            if (index != -1 && tblBaum.getSelectedRowCount() <= 1) {
                Baum selectedBaum = baumModel.getBaumAtRow(index);
                baumModel.setCurrentSelectedBaum(selectedBaum);
                if (selectedBaum != null) {
                    updateCbxAuspraegung(selectedBaum);
                    if (selectedBaum.getGeometry() == null) {
                        log.debug("SetBackgroundEnabled abgeschaltet: ", new CurrentStackTrace());
                    //((SimpleBackgroundedJPanel) this.panBackground).setBackgroundEnabled(false);
                    } else {
                        //if (!((SimpleBackgroundedJPanel) this.panBackground).isBackgroundEnabled()) {
//                            ((SimpleBackgroundedJPanel) this.panBackground).setBackgroundEnabled(true);
                        //                      }
                    }
                }
                Set<BaumMerkmal> merkmale = selectedBaum.getBaumMerkmal();
                if (merkmale != null) {
                    log.debug("Merkmale vorhanden");
                    for (int i = 0; i < lstMerkmale.getModel().getSize(); i++) {
                        MerkmalCheckBox currentCheckBox = (MerkmalCheckBox) lstMerkmale.getModel().getElementAt(i);
                        if (currentCheckBox != null && currentCheckBox.getBaumMerkmal() != null && merkmale.contains(currentCheckBox.getBaumMerkmal())) {
                            log.debug("Merkmal ist in Baum vorhanden");
                            currentCheckBox.removeItemListener(this);
                            currentCheckBox.setSelected(true);
                            currentCheckBox.addItemListener(this);
                            lstMerkmale.repaint();
                        } else {
                            log.debug("Merkmal ist nicht in Baum vorhanden");
                            currentCheckBox.removeItemListener(this);
                            currentCheckBox.setSelected(false);
                            currentCheckBox.addItemListener(this);
                            lstMerkmale.repaint();
                        }
                    }

                } else {
                    log.debug("Keine Merkmale vorhanden");
                    deselectAllListEntries();
                }
                if (isInEditMode) {
                    enableSlaveComponents(isInEditMode);
                } else {
                    enableSlaveComponents(isInEditMode);
                }
                if (selectedBaum.getGeometry() != null && !mappingComp.getFeatureCollection().isSelected(selectedBaum)) {
                    log.debug("SelectedBaum hat eine Geometry und ist nicht selektiert --> wird selektiert");
                    ignoreFeatureSelectionEvent = true;
                    mappingComp.getFeatureCollection().select(selectedBaum);
                    ignoreFeatureSelectionEvent = false;
                } else if (selectedBaum.getGeometry() == null) {
                    log.debug("Keine Baum Geometrie vorhanden die selektiert werden kann, prüfe ob eine Baumm Geometrie selektiert ist");
                    Collection selectedFeatures = mappingComp.getFeatureCollection().getSelectedFeatures();
                    if (selectedFeatures != null) {
                        for (Object currentObject : selectedFeatures) {
                            if (currentObject != null && currentObject instanceof Baum) {
                                log.debug("Eine Baum Geometrie ist selektiert --> deselekt");
                                ignoreFeatureSelectionEvent = true;
                                mappingComp.getFeatureCollection().unselect((Baum) currentObject);
                                ignoreFeatureSelectionEvent = false;
                            }
                        }
                    } else {
                        log.debug("selected FeatureCollection ist leer");
                    }
                } else {
                    log.debug("Die Geometrie des selektierten Baeume kann nicht seleketiert werden ");
                    log.debug("alreadySelected: " + (mappingComp.getFeatureCollection().isSelected(selectedBaum)) + " hasGeometry: " + (selectedBaum.getGeometry() != null));
                    log.debug("get Selected Feature: " + mappingComp.getFeatureCollection().getSelectedFeatures());
                }
            }
        } else {
            btnRemoveBaum.setEnabled(false);
            deselectAllListEntries();
            baumModel.clearSlaveComponents();
            enableSlaveComponents(false);
            return;
        }
        ((JXTable) tblBaum).packAll();
    }

    public int getStatus() {
        if (tblBaum.getCellEditor() != null) {
            validationMessage = "Bitte vollenden Sie alle Änderungen bei den Baumeinträgen.";
            return Validatable.ERROR;
        }

        Vector<Baum> baeume = baumModel.getAllBaeume();
        if (baeume != null || baeume.size() > 0) {
            for (Baum currentBaum : baeume) {


                if (currentBaum != null && (currentBaum.getBaumNutzung() == null || currentBaum.getBaumNutzung().getBaumKategorie() == null)) {
                    validationMessage = "Alle Baueme müssen einen Baumbestand (Kategorie) enthalten";
                    return Validatable.ERROR;
                }
                if (currentBaum != null && (currentBaum.getAuftragnehmer() == null || currentBaum.getAuftragnehmer().equals(""))) {
                    validationMessage = "Alle Baueme müssen einen Auftragnehmer besitzen.";
                    return Validatable.ERROR;
                }
                if (currentBaum != null && currentBaum.getLage() == null) {
                    validationMessage = "Alle Baueme müssen eine Lage besitzen.";
                    return Validatable.ERROR;
                }
                if (currentBaum != null && currentBaum.getBaumnummer() == null) {
                    validationMessage = "Alle Baueme müssen ein Baumnummer besitzen.";
                    return Validatable.ERROR;
                }
                if (currentBaum != null && currentBaum.getErfassungsdatum() != null && currentBaum.getFaelldatum() != null && currentBaum.getErfassungsdatum().compareTo(currentBaum.getFaelldatum()) > 0) {
                    validationMessage = "Das Datum der Erfassung muss vor dem Datum des Fällens liegen.";
                    return Validatable.ERROR;
                }
            }
        }
        return Validatable.VALID;
    }

    public void itemStateChanged(ItemEvent e) {
        log.debug("Item State of BaumMerkmal Changed " + e);
        //TODO use Constants from Java
        MerkmalCheckBox checkBox = (MerkmalCheckBox) e.getSource();
        if (tblBaum.getSelectedRow() != -1) {
            Baum baum = baumModel.getBaumAtRow(((JXTable) tblBaum).getFilters().convertRowIndexToModel(tblBaum.getSelectedRow()));
            if (baum != null) {

                Set<BaumMerkmal> merkmale = baum.getBaumMerkmal();
                if (merkmale == null) {
                    log.info("neues Hibernateset für Merkmale angelegt");
                    merkmale = new HashSet<BaumMerkmal>();
                    baum.setBaumMerkmal(merkmale);
                }

                if (e.getStateChange() == 1) {
                    log.debug("Checkbox wurde selektiert --> füge es zum Set hinzu");
                    merkmale.add(checkBox.getBaumMerkmal());
                } else {
                    log.debug("Checkbox wurde deselektiert --> lösche es aus Set");
                    merkmale.remove(checkBox.getBaumMerkmal());
                }
            } else {
                log.warn("Kann merkmalsänderung nicht speichern da kein Eintrag unter diesem Index im Modell vorhanden ist");
            }
        } else {
            log.warn("Kann merkmalsänderung nicht speichern da kein Eintrag selektiert ist");
        }
    }

    public String getProviderName() {
        return PROVIDER_NAME;
    }

    public Vector<GeometrySlotInformation> getSlotInformation() {
        //VerwaltungsTableModel tmp = (VerwaltungsTableModel) tNutzung.getModel();
        Vector<GeometrySlotInformation> result = new Vector<GeometrySlotInformation>();
        if (isWidgetReadOnly()) {
            return result;
        } else {
            int rowCount = baumModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                Baum currentBaum = baumModel.getBaumAtRow(i);
                //Geom geom;
                if (currentBaum.getGeometry() == null) {
                    String idValue1 = currentBaum.getLage();
                    BaumNutzung idValue2 = currentBaum.getBaumNutzung();

                    StringBuffer identifier = new StringBuffer();

                    if (idValue1 != null) {
                        identifier.append(idValue1);
                    } else {
                        identifier.append("keine Lage");
                    }

                    if (idValue2 != null && idValue2.getBaumKategorie() != null) {
                        identifier.append(GeometrySlotInformation.SLOT_IDENTIFIER_SEPARATOR + idValue2.getBaumKategorie());
                    } else {
                        identifier.append(GeometrySlotInformation.SLOT_IDENTIFIER_SEPARATOR + "keine Nutzung");
                    }

                    if (idValue2 != null &&  idValue2.getAusgewaehlteAuspraegung() != null) {
                            identifier.append(GeometrySlotInformation.SLOT_IDENTIFIER_SEPARATOR + idValue2.getAusgewaehlteAuspraegung());
                    } else {
                        identifier.append(GeometrySlotInformation.SLOT_IDENTIFIER_SEPARATOR + "keine Ausprägung");
                    }
                    result.add(new GeometrySlotInformation(getProviderName(), identifier.toString(), currentBaum, this));
                }
            }
            return result;
        }
    }

    //TODO multiple Selection
    //HINT If there are problems try to remove/add Listselectionlistener at start/end of Method
    public void featureSelectionChanged(Collection<Feature> features) {
        log.debug("FeatureSelectionChanged", new CurrentStackTrace());
        //knaup
        if (!ignoreFeatureSelectionEvent) {
            if (features.size() == 0) {
                return;
            }
            int[] selectedRows = tblBaum.getSelectedRows();
            if (selectedRows != null && selectedRows.length > 0) {
                for (int i = 0; i < selectedRows.length; i++) {
                    int modelIndex = ((JXTable) tblBaum).getFilters().convertRowIndexToModel(selectedRows[i]);
                    if (modelIndex != -1) {
                        Baum currentBaum = baumModel.getBaumAtRow(modelIndex);
                        if (currentBaum != null && currentBaum.getGeometry() == null) {
                            tblBaum.getSelectionModel().removeSelectionInterval(selectedRows[i], selectedRows[i]);
                        }
                    }
                }
            }

            for (Feature feature : features) {
                if (feature instanceof Baum) {
                    //TODO Refactor Name
                    int index = baumModel.getIndexOfBaum((Baum) feature);
                    int displayedIndex = ((JXTable) tblBaum).getFilters().convertRowIndexToView(index);
                    if (index != -1 && LagisBroker.getInstance().getMappingComponent().getFeatureCollection().isSelected(feature)) {
                        //tReBe.changeSelection(((JXTable)tReBe).getFilters().convertRowIndexToView(index),0,false,false);
                        if (feature.getGeometry() != null) {
                            //((SimpleBackgroundedJPanel) this.panBackground).setBackgroundEnabled(true);
                        } else {
                            log.debug("SetBackgroundEnabled abgeschaltet: ", new CurrentStackTrace());
                        //((SimpleBackgroundedJPanel) this.panBackground).setBackgroundEnabled(false);
                        }
                        tblBaum.getSelectionModel().addSelectionInterval(displayedIndex, displayedIndex);
                        Rectangle tmp = tblBaum.getCellRect(displayedIndex, 0, true);
                        if (tmp != null) {
                            tblBaum.scrollRectToVisible(tmp);
                        }
                    } else {
                        tblBaum.getSelectionModel().removeSelectionInterval(displayedIndex, displayedIndex);
                        log.debug("SetBackgroundEnabled abgeschaltet: ", new CurrentStackTrace());
                    //war schon ausdokumentiert
                    //((SimpleBackgroundedJPanel) this.panBackground).setBackgroundEnabled(false);
                    }
                } else {
                    tblBaum.clearSelection();
                    log.debug("SetBackgroundEnabled abgeschaltet: ", new CurrentStackTrace());
                //((SimpleBackgroundedJPanel) this.panBackground).setBackgroundEnabled(false);
                }
            }
        } else {
            log.debug("Aktuelles change event wird ignoriert");
        }
    }

    public void stateChanged(ChangeEvent e) {
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        panBaumBordered = new javax.swing.JPanel();
        cpBaum = new javax.swing.JScrollPane();
        tblBaum = new JXTable();
        btnAddBaum = new javax.swing.JButton();
        btnRemoveBaum = new javax.swing.JButton();
        btnAddExitingBaum = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        panBackground = new javax.swing.JPanel();
        panQuerverweise = new javax.swing.JPanel();
        panQuerverweiseTitled = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstCrossRefs = new javax.swing.JList();
        jLabel2 = new javax.swing.JLabel();
        panMerkmale = new javax.swing.JPanel();
        panMerkmaleTitled = new javax.swing.JPanel();
        spMerkmale = new javax.swing.JScrollPane();
        lstMerkmale = new JCheckBoxList();
        jLabel1 = new javax.swing.JLabel();
        panBemerkung = new javax.swing.JPanel();
        panBemerkungTitled = new javax.swing.JPanel();
        spBemerkung = new javax.swing.JScrollPane();
        taBemerkung = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTabbedPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );

        panBaumBordered.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        cpBaum.setBorder(null);

        tblBaum.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Nummer", "Lage", "Fläche m²", "Nutzung", "Nutzer", "Vertragsbeginn", "Vertragsende"
            }
        ));
        cpBaum.setViewportView(tblBaum);

        btnAddBaum.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/cismet/lagis/ressource/icons/buttons/add.png"))); // NOI18N
        btnAddBaum.setBorder(null);
        btnAddBaum.setBorderPainted(false);
        btnAddBaum.setFocusPainted(false);
        btnAddBaum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddBaumActionPerformed(evt);
            }
        });

        btnRemoveBaum.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/cismet/lagis/ressource/icons/buttons/remove.png"))); // NOI18N
        btnRemoveBaum.setBorder(null);
        btnRemoveBaum.setBorderPainted(false);
        btnRemoveBaum.setFocusPainted(false);
        btnRemoveBaum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveBaumActionPerformed(evt);
            }
        });

        btnAddExitingBaum.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/cismet/lagis/ressource/icons/toolbar/contract.png"))); // NOI18N
        btnAddExitingBaum.setBorder(null);
        btnAddExitingBaum.setBorderPainted(false);
        btnAddExitingBaum.setFocusPainted(false);
        btnAddExitingBaum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddExitingBaumActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panBaumBorderedLayout = new javax.swing.GroupLayout(panBaumBordered);
        panBaumBordered.setLayout(panBaumBorderedLayout);
        panBaumBorderedLayout.setHorizontalGroup(
            panBaumBorderedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBaumBorderedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panBaumBorderedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panBaumBorderedLayout.createSequentialGroup()
                        .addComponent(btnAddExitingBaum, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnAddBaum, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemoveBaum, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cpBaum, javax.swing.GroupLayout.DEFAULT_SIZE, 708, Short.MAX_VALUE))
                .addContainerGap())
        );

        panBaumBorderedLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {btnAddBaum, btnAddExitingBaum, btnRemoveBaum});

        panBaumBorderedLayout.setVerticalGroup(
            panBaumBorderedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBaumBorderedLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panBaumBorderedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnRemoveBaum, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddBaum, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAddExitingBaum, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cpBaum, javax.swing.GroupLayout.DEFAULT_SIZE, 299, Short.MAX_VALUE)
                .addContainerGap())
        );

        panBaumBorderedLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {btnAddBaum, btnAddExitingBaum, btnRemoveBaum});

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        panQuerverweise.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panQuerverweise.setOpaque(false);

        panQuerverweiseTitled.setOpaque(false);

        jScrollPane1.setBorder(null);
        jScrollPane1.setOpaque(false);

        lstCrossRefs.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        lstCrossRefs.setOpaque(false);
        jScrollPane1.setViewportView(lstCrossRefs);

        javax.swing.GroupLayout panQuerverweiseTitledLayout = new javax.swing.GroupLayout(panQuerverweiseTitled);
        panQuerverweiseTitled.setLayout(panQuerverweiseTitledLayout);
        panQuerverweiseTitledLayout.setHorizontalGroup(
            panQuerverweiseTitledLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
        );
        panQuerverweiseTitledLayout.setVerticalGroup(
            panQuerverweiseTitledLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
        );

        jLabel2.setText("Querverweise:");

        javax.swing.GroupLayout panQuerverweiseLayout = new javax.swing.GroupLayout(panQuerverweise);
        panQuerverweise.setLayout(panQuerverweiseLayout);
        panQuerverweiseLayout.setHorizontalGroup(
            panQuerverweiseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panQuerverweiseLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panQuerverweiseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(panQuerverweiseTitled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panQuerverweiseLayout.setVerticalGroup(
            panQuerverweiseLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panQuerverweiseLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panQuerverweiseTitled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        panMerkmale.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panMerkmale.setOpaque(false);

        panMerkmaleTitled.setOpaque(false);

        spMerkmale.setBorder(null);
        spMerkmale.setOpaque(false);

        lstMerkmale.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        lstMerkmale.setOpaque(false);
        spMerkmale.setViewportView(lstMerkmale);

        javax.swing.GroupLayout panMerkmaleTitledLayout = new javax.swing.GroupLayout(panMerkmaleTitled);
        panMerkmaleTitled.setLayout(panMerkmaleTitledLayout);
        panMerkmaleTitledLayout.setHorizontalGroup(
            panMerkmaleTitledLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spMerkmale, javax.swing.GroupLayout.DEFAULT_SIZE, 171, Short.MAX_VALUE)
        );
        panMerkmaleTitledLayout.setVerticalGroup(
            panMerkmaleTitledLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spMerkmale, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
        );

        jLabel1.setText("Merkmale:");

        javax.swing.GroupLayout panMerkmaleLayout = new javax.swing.GroupLayout(panMerkmale);
        panMerkmale.setLayout(panMerkmaleLayout);
        panMerkmaleLayout.setHorizontalGroup(
            panMerkmaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panMerkmaleLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panMerkmaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panMerkmaleTitled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1))
                .addContainerGap())
        );
        panMerkmaleLayout.setVerticalGroup(
            panMerkmaleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panMerkmaleLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panMerkmaleTitled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        panBemerkung.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panBemerkung.setOpaque(false);

        panBemerkungTitled.setOpaque(false);

        spBemerkung.setBorder(null);
        spBemerkung.setOpaque(false);

        taBemerkung.setColumns(20);
        taBemerkung.setLineWrap(true);
        taBemerkung.setRows(5);
        taBemerkung.setWrapStyleWord(true);
        taBemerkung.setOpaque(false);
        spBemerkung.setViewportView(taBemerkung);

        javax.swing.GroupLayout panBemerkungTitledLayout = new javax.swing.GroupLayout(panBemerkungTitled);
        panBemerkungTitled.setLayout(panBemerkungTitledLayout);
        panBemerkungTitledLayout.setHorizontalGroup(
            panBemerkungTitledLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spBemerkung, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
        );
        panBemerkungTitledLayout.setVerticalGroup(
            panBemerkungTitledLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(spBemerkung, javax.swing.GroupLayout.DEFAULT_SIZE, 164, Short.MAX_VALUE)
        );

        jLabel3.setText("Bemerkung");

        javax.swing.GroupLayout panBemerkungLayout = new javax.swing.GroupLayout(panBemerkung);
        panBemerkung.setLayout(panBemerkungLayout);
        panBemerkungLayout.setHorizontalGroup(
            panBemerkungLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBemerkungLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panBemerkungLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panBemerkungTitled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel3))
                .addContainerGap())
        );
        panBemerkungLayout.setVerticalGroup(
            panBemerkungLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBemerkungLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panBemerkungTitled, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout panBackgroundLayout = new javax.swing.GroupLayout(panBackground);
        panBackground.setLayout(panBackgroundLayout);
        panBackgroundLayout.setHorizontalGroup(
            panBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panBackgroundLayout.createSequentialGroup()
                .addComponent(panMerkmale, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panQuerverweise, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panBemerkung, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panBackgroundLayout.setVerticalGroup(
            panBackgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panBemerkung, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panMerkmale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(panQuerverweise, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panBackground, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panBackground, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panBaumBordered, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panBaumBordered, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    private void btnAddBaumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddBaumActionPerformed
        final Baum tmpBaum = new Baum();
        baumModel.addBaum(tmpBaum);
        baumModel.fireTableDataChanged();
}//GEN-LAST:event_btnAddBaumActionPerformed

    private void btnRemoveBaumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveBaumActionPerformed
        int currentRow = tblBaum.getSelectedRow();
        if (currentRow != -1) {
            //VerwaltungsTableModel currentModel = (VerwaltungsTableModel)tNutzung.getModel();
            baumModel.removeBaum(((JXTable) tblBaum).getFilters().convertRowIndexToModel(currentRow));
            baumModel.fireTableDataChanged();
            updateCrossRefs();
            enableSlaveComponents(false);
            deselectAllListEntries();
            log.debug("liste ausgeschaltet");
        }
}//GEN-LAST:event_btnRemoveBaumActionPerformed

    private void btnAddExitingBaumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddExitingBaumActionPerformed
        JDialog dialog = new JDialog(LagisBroker.getInstance().getParentComponent(), "", true);
        dialog.add(new AddExistingBaumPanel(currentFlurstueck, baumModel, lstCrossRefs.getModel()));
        dialog.pack();
        dialog.setIconImage(icoExistingContract.getImage());
        dialog.setTitle("Vorhandener Vertrag hinzufügen...");
        dialog.setLocationRelativeTo(LagisBroker.getInstance().getParentComponent());
        dialog.setVisible(true);
}//GEN-LAST:event_btnAddExitingBaumActionPerformed

    private void updateCrossRefs() {
        log.debug("Update der Querverweise");
        Set<FlurstueckSchluessel> crossRefs = EJBroker.getInstance().getCrossreferencesForBaeume(new HashSet(baumModel.getAllBaeume()));
        DefaultUniqueListModel newModel = new DefaultUniqueListModel();
        if (crossRefs != null) {
            log.debug("Es sind Querverweise auf Baeume vorhanden");
            currentFlurstueck.setVertraegeQuerverweise(crossRefs);
            Iterator<FlurstueckSchluessel> it = crossRefs.iterator();
            while (it.hasNext()) {
                log.debug("Ein Querverweis hinzugefügt");
                newModel.addElement(it.next());
            }
            newModel.removeElement(currentFlurstueck.getFlurstueckSchluessel());
        }
        lstCrossRefs.setModel(newModel);
    }

    private void updateWidgetUi() {
        tblBaum.repaint();
        lstCrossRefs.repaint();
        lstMerkmale.repaint();
    }

    public void allFeaturesRemoved(FeatureCollectionEvent fce) {
        updateWidgetUi();
    }

    public void featureCollectionChanged() {
        updateWidgetUi();
    }

    public void featureReconsiderationRequested(FeatureCollectionEvent fce) {
        updateWidgetUi();
    }

    public void featureSelectionChanged(FeatureCollectionEvent fce) {
        updateWidgetUi();
    }

    public void featuresAdded(FeatureCollectionEvent fce) {
        updateWidgetUi();
    }

    public void featuresChanged(FeatureCollectionEvent fce) {
        updateWidgetUi();
    }

    public void featuresRemoved(FeatureCollectionEvent fce) {
        updateWidgetUi();
    }

    public void tableChanged(TableModelEvent e) {
        ((JXTable) tblBaum).packAll();
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddBaum;
    private javax.swing.JButton btnAddExitingBaum;
    private javax.swing.JButton btnRemoveBaum;
    private javax.swing.JScrollPane cpBaum;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JList lstCrossRefs;
    private javax.swing.JList lstMerkmale;
    private javax.swing.JPanel panBackground;
    private javax.swing.JPanel panBaumBordered;
    private javax.swing.JPanel panBemerkung;
    private javax.swing.JPanel panBemerkungTitled;
    private javax.swing.JPanel panMerkmale;
    private javax.swing.JPanel panMerkmaleTitled;
    private javax.swing.JPanel panQuerverweise;
    private javax.swing.JPanel panQuerverweiseTitled;
    private javax.swing.JScrollPane spBemerkung;
    private javax.swing.JScrollPane spMerkmale;
    private javax.swing.JTextArea taBemerkung;
    private javax.swing.JTable tblBaum;
    // End of variables declaration//GEN-END:variables
}
