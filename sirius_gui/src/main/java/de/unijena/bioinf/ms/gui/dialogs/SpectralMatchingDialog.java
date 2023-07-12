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

package de.unijena.bioinf.ms.gui.dialogs;


import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.mainframe.MainFrame;
import de.unijena.bioinf.ms.gui.ms_viewer.WebViewSpectraViewer;
import de.unijena.bioinf.ms.gui.ms_viewer.data.SpectraJSONWriter;
import de.unijena.bioinf.spectraldb.SpectralLibrary;
import de.unijena.bioinf.spectraldb.entities.Ms2ReferenceSpectrum;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SpectralMatchingDialog extends JDialog {

    // FIXME content not visible?
    private final WebViewSpectraViewer browser;

    private final SpectraJSONWriter spectraWriter;

    private final FingerprintCandidateBean fpBean;

    private final List<SpectralLibrary.SearchResult> searchResults;

    public SpectralMatchingDialog(FingerprintCandidateBean fpBean, List<SpectralLibrary.SearchResult> searchResults) {
        super(MainFrame.MF, "Spectral comparison", false);
        this.fpBean = fpBean;
        this.searchResults = searchResults;
        this.spectraWriter = new SpectraJSONWriter();
        this.browser = new WebViewSpectraViewer();

        this.setLayout(new BorderLayout());

        // TODO add selection table

        this.add(this.browser, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(getParent());

        selectionChanged(0);
    }

    private void selectionChanged(int index) {
        if (index < searchResults.size() && searchResults.size() > 0) {
            Jobs.runEDTLater(() -> {
                SpectralLibrary.SearchResult result = searchResults.get(index);
                Ms2ReferenceSpectrum reference = fpBean.getMs2SpectralData(result.getReference());

                final SimpleSpectrum query = new SimpleSpectrum(result.getQuery());
                final SimpleSpectrum referenceSpectrum = new SimpleSpectrum(reference.getSpectrum());

                String json = spectraWriter.ms2MirrorJSON(query, referenceSpectrum, reference.getName());
                this.browser.loadData(json, null, null);
            });
        }
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            setLocationRelativeTo(MainFrame.MF);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setPreferredSize(new Dimension(
                    Math.min(screenSize.width, (int) Math.floor(0.8 * MainFrame.MF.getWidth())),
                    Math.min(screenSize.height, (int) Math.floor(0.8 * MainFrame.MF.getHeight())))
            );
            pack();
        }
        super.setVisible(b);
    }

}