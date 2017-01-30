package de.unijena.bioinf.sirius.gui.mainframe.results.results_table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 24.01.17.
 */

import ca.odell.glazedlists.event.ListEvent;
import de.unijena.bioinf.sirius.gui.mainframe.ExperimentListChangeListener;
import de.unijena.bioinf.sirius.gui.mainframe.ExperimentListPanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.ActionTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultTablePanel extends JPanel implements ExperimentListChangeListener {

    private static final int[] BAR_COLS = {2, 3, 4};
    public final ActionTable<SiriusResultElement> table;
    private final JTextField searchField = new JTextField();

    private Set<ExperimentContainer> selected = new HashSet<>();


    ///////////////// Constructors //////////////////////////
    public SiriusResultTablePanel(ExperimentListPanel toObserve) {
        super(new BorderLayout());
        searchField.setPreferredSize(new Dimension(100, searchField.getPreferredSize().height));
        this.table = new ActionTable<>(new ArrayList<SiriusResultElement>(),
                new SiriusResultTableFormat(),
                new SiriusResultMatcherEditor(searchField),
                SiriusResultElement.class);

        table.setDefaultRenderer(Object.class, new SiriusResultTableCellRenderer());

        for (int i = 0; i < BAR_COLS.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(BAR_COLS[i]);
            col.setCellRenderer(new BarTableCellRenderer());
        }


        this.add(
                new JScrollPane(table, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER
        );

        addNorthPanel();
        addSouthPanel();
        addLeftPanel();
        addRightPanel();

        toObserve.addChangeListener(this);
    }


    ///////////////// Internal //////////////////////////
    protected void addNorthPanel() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JPanel sp = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        sp.add(new JLabel("Filter"));
        sp.add(searchField);
        top.add(sp);

        this.add(top, BorderLayout.NORTH);
    }

    protected void addRightPanel() {
    }

    protected void addLeftPanel() {
    }

    protected void addSouthPanel() {
    }


    private void addData(final Collection<ExperimentContainer> data) {
        if (data == null || data.isEmpty()) {
            table.elements.clear();
            selected.clear();
        } else {
            Set<ExperimentContainer> toRemove = new HashSet<>(selected);
            toRemove.removeAll(data);

            Set<ExperimentContainer> toAdd = new HashSet<>(data);
            toAdd.removeAll(selected);
            for (ExperimentContainer container : toRemove) {
                table.elements.removeAll(container.getResults());
            }
            for (ExperimentContainer container : toAdd) {
                table.elements.addAll(container.getResults());
            }
            selected = new HashSet<>(data);
        }
    }

    @Override
    public void listChanged(ListEvent<ExperimentContainer> event, JList<ExperimentContainer> source) {
        if (!source.isSelectionEmpty()) {
            while (event.next()){
                if (event.getType() == ListEvent.UPDATE && source.isSelectedIndex(event.getIndex())) {
                    table.elements.clear();
                    selected.clear();
                    addData(source.getSelectedValuesList()); //todo should i readd only the changed ones?
                    return;
                }
            }
        }
    }

    @Override
    public void listSelectionChanged(JList<ExperimentContainer> source) {
        addData(source.getSelectedValuesList());
    }
}
