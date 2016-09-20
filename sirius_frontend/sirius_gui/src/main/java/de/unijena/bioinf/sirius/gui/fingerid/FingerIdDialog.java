/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.fingerid;

import de.unijena.bioinf.sirius.core.ApplicationCore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FingerIdDialog extends JDialog {

    public static final int APROVED=1, CANCELED=0, COMPUTE_ALL=2;

    protected CSIFingerIdComputation storage;
    protected FingerIdData data;
    protected boolean showComputeButton;
    protected int returnState = CANCELED;
    protected String db;
    protected JRadioButton pubchem, biodb;

    protected final static String BIO="bio database", ALL = "PubChem";

    public FingerIdDialog(Frame owner, CSIFingerIdComputation storage, FingerIdData data, boolean showComputeButton) {
        super(owner, "Search with CSI:FingerId", true);
        this.data = data;
        this.storage = storage;
        this.showComputeButton = showComputeButton;
    }

    public int run() {
        refresh();
        return returnState;
    }

    public void refresh() {
        this.setLayout(new BorderLayout());
        Box mainPanel = Box.createVerticalBox();
        add(mainPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        final JPanel dirForm = new JPanel();
        dirForm.setLayout(new BoxLayout(dirForm, BoxLayout.Y_AXIS));
        final JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
        dirForm.add(inner);
        dirForm.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"database directory"));
        final JTextField field = new JTextField(storage.getDirectory().toString());
        field.setPreferredSize(new Dimension(150, 26));
        final JButton changeDir = new JButton("change directory");
        final JFileChooser fileChooser = new JFileChooser(storage.getDirectory());
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        inner.add(field);
        inner.add(changeDir);
        changeDir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int r = fileChooser.showOpenDialog(FingerIdDialog.this);
                if (r==JFileChooser.APPROVE_OPTION) {
                    final File file = fileChooser.getSelectedFile();
                    if (file != null && file.isDirectory()) {
                        field.setText(file.toString());
                    }
                }
            }
        });
        final String tooltip = "Specify the directory where CSI:FingerId should store the compound candidates.Use the environment variable CSI_FINGERID_STORAGE to set this directory permanently.";
        field.setToolTipText(tooltip);
        changeDir.setToolTipText(tooltip);

        final JPanel dbForm = new JPanel();
        dbForm.setLayout(new BoxLayout(dbForm, BoxLayout.Y_AXIS));
        final JPanel inner2 = new JPanel();

        inner2.setLayout(new FlowLayout());
        //dbForm.setAlignmentX(0);dbForm.setAlignmentY(0);
        final ButtonGroup database = new ButtonGroup();
        pubchem = new JRadioButton("PubChem", !storage.isEnforceBio());
        biodb = new JRadioButton("bio databases", storage.isEnforceBio());
        database.add(pubchem);
        database.add(biodb);

        inner2.add(pubchem);
        inner2.add(biodb);
        dbForm.add(inner2);
        dbForm.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"search in"));
        mainPanel.add(dirForm);
        mainPanel.add(dbForm);

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
        this.add(southPanel,BorderLayout.SOUTH);

        if (showComputeButton) {
            final JButton computeAll = new JButton("Search all");
            computeAll.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String path = field.getText();
                    ApplicationCore.changeDefaultProptertyPersistent("de.unijena.bioinf.sirius.fingerID.cache" ,path);
                    storage.setDirectory(new File(path));
                    storage.setEnforceBio(biodb.isSelected());
                    storage.configured = true;
                    returnState = COMPUTE_ALL;
                    dispose();
                }
            });
            southPanel.add(computeAll);
        }

        JButton approve = new JButton("approve");
        approve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String path = field.getText();
                ApplicationCore.changeDefaultProptertyPersistent("de.unijena.bioinf.sirius.fingerID.cache" ,path);
                storage.setDirectory(new File(path));
                storage.configured = true;
                storage.setEnforceBio(biodb.isSelected());
                returnState = APROVED;
                dispose();
            }
        });
        final JButton abort = new JButton("Abort");
        abort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        southPanel.add(approve);
        southPanel.add(abort);
        pack();
        setVisible(true);
    }

}
