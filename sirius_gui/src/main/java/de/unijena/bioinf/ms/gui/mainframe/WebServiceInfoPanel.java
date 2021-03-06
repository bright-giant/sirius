/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe;

import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.rest.model.license.Subscription;
import de.unijena.bioinf.ms.rest.model.license.SubscriptionConsumables;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class WebServiceInfoPanel extends JToolBar implements PropertyChangeListener {
    private static final String INF = Character.toString('\u221E');
    private final JLabel license;
    private final JLabel consumedCompounds;
    //    private final JLabel connected = new JLabel("Connected: ?");
    private final JLabel pendingJobs;

    public WebServiceInfoPanel(ConnectionMonitor monitor) {
        super("Web service info");
        setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        setPreferredSize(new Dimension(getPreferredSize().width, 16));
        setFloatable(false);

        license = new JLabel("License: ?");
        license.setToolTipText("Web service license information.");
        consumedCompounds = new JLabel("Compounds: 'UNLIMITED'");
        consumedCompounds.setToolTipText(GuiUtils.formatToolTip("Consumed compounds in billing period. (If subscription is compound based)"));
        pendingJobs = new JLabel("Jobs: ?");
        pendingJobs.setToolTipText("Number of pending jobs on web server.");

        add(license);
        add(Box.createGlue());
        add(consumedCompounds);
        add(Box.createGlue());
        add(pendingJobs);
        monitor.addConnectionUpdateListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final ConnectionMonitor.ConnectionUpdateEvent cevt = (ConnectionMonitor.ConnectionUpdateEvent) evt;
        ConnectionMonitor.ConnectionCheck check = cevt.getConnectionCheck();

        if (check.licenseInfo.subscription().isPresent()) {
            @Nullable Subscription sub = check.licenseInfo.getSubscription();
            license.setText("<html>License: <b>" + sub.getSubscriberName() + "</b>" + (check.licenseInfo.getSubscription() == null ? "" : " (" + check.licenseInfo.getSubscription().getName() + ")</html>"));
            if (sub.getCountQueries()) {
                String max = sub.hasCompoundLimit() ? String.valueOf(sub.getCompoundLimit()) : INF;
                String current = check.licenseInfo.consumables().map(SubscriptionConsumables::getCountedCompounds).orElse(-1) < 0
                        ? "N/A" : check.licenseInfo.consumables().map(SubscriptionConsumables::getCountedCompounds)
                        .map(String::valueOf).get();
                consumedCompounds.setText("<html>Compounds: <b>" + current + "/" + max + "</b> (per " + (sub.hasCompoundLimit() ? "Year" : "Month") + ")</html>");
            } else {
                consumedCompounds.setText("<html>Compounds: <b>UNLIMITED</b></html>");
            }
        } else {
            license.setText("License: '?'");
            consumedCompounds.setText("Compounds: '?'");
        }

        if (check.workerInfo != null) {
            pendingJobs.setText("<html>Jobs: <b>" + check.workerInfo.getPendingJobs() + "</b></html>");
        } else {
            pendingJobs.setText("Jobs: ?");
        }

        revalidate();
        repaint();
    }
}
