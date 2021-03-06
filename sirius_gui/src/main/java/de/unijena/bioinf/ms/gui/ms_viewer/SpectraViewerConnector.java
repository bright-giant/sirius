package de.unijena.bioinf.ms.gui.ms_viewer;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.VisualizationPanelSynchronizer;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.EpimetheusPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.FormulaOverviewPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.SpectraVisualizationPanel;
import de.unijena.bioinf.ms.gui.webView.WebViewPanel;

public class SpectraViewerConnector {

    VisualizationPanelSynchronizer sync;
    float selection = -1;

    public void registerSynchronizer(VisualizationPanelSynchronizer sync){
        this.sync = sync;
    }

    public float getCurrentSelection() {
        return selection;
    }

    public void selectionChanged(float peak_mz) {
        selection = peak_mz;
        sync.peakChanged(peak_mz);
    }
}
