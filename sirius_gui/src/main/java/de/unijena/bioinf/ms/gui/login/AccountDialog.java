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

package de.unijena.bioinf.ms.gui.login;

import de.unijena.bioinf.auth.AuthService;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AccountDialog extends JDialog implements PropertyChangeListener {
    private final AuthService service;
    private final Action signIn = SiriusActions.SIGN_IN.getInstance();
    private final Action signOut = SiriusActions.SIGN_OUT.getInstance();
    private AccountPanel center;

    public AccountDialog(Frame owner, AuthService service) {
        super(owner, true);
        this.service = service;
        build();
    }

    public AccountDialog(Dialog owner, AuthService service) {
        super(owner, true);
        this.service = service;
        build();
    }

    private void build() {
        setTitle("Account");
        setLayout(new BorderLayout());

        //============= NORTH =================
//        add(new DialogHeader(Icons.USER_64), BorderLayout.NORTH);


        //============= CENTER =================
        center = new AccountPanel(service);
        add(center, BorderLayout.CENTER);

        signIn.addPropertyChangeListener(this);
        signOut.addPropertyChangeListener(this);

        configureActions();

        setMinimumSize(new Dimension(500, getMinimumSize().height));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
        setVisible(true);
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        String escAction = "cancel";
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), escAction);
        getRootPane().getActionMap().put(escAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        if (signIn != null)
            signIn.removePropertyChangeListener(this);
        if (signOut != null)
            signOut.removePropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (e.getSource() == signIn || e.getSource() == signOut)
            center.reloadChanges();
    }
}



