/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.VisualizationPanelSynchronizer;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaListDetailView;

import javax.swing.*;
import java.awt.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaOverviewPanel extends JPanel implements PanelDescription {
    @Override
    public String getDescription() {
        return "<html>"
                +"<b>SIRIUS - Molecular Formulas Identification</b>"
                +"<br>"
                + "Overview about your Experiment and Results of the Formula Identification with SIRIUS."
                + "</html>";
    }

    public FormulaOverviewPanel(FormulaList suriusResultElements) {
        super(new BorderLayout());


        final FormulaListDetailView north = new FormulaListDetailView(suriusResultElements);


        TreeVisualizationPanel overviewTVP = new TreeVisualizationPanel();
        suriusResultElements.addActiveResultChangedListener(overviewTVP);
        SpectraVisualizationPanel overviewSVP = new SpectraVisualizationPanel();
        suriusResultElements.addActiveResultChangedListener((experiment, sre, resultElements, selections) ->
                overviewSVP.resultsChanged(experiment, sre.getFormulaId(), null));

        // Class to synchronize selected peak/node
        VisualizationPanelSynchronizer synchronizer = new VisualizationPanelSynchronizer(
            overviewTVP, overviewSVP);

        JSplitPane east = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, overviewSVP, overviewTVP);
        east.setDividerLocation(.5d);
        east.setResizeWeight(.5d);
        JSplitPane major = new JSplitPane(JSplitPane.VERTICAL_SPLIT, north, east);
        major.setDividerLocation(250);
        add(major, BorderLayout.CENTER);
    }
}
