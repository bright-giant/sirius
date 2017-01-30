package de.unijena.bioinf.sirius.gui.settings;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.core.PasswordCrypter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProxySettingsPanel extends TwoCloumnPanel implements ActionListener, SettingsPanel {
    private Properties props;
    private JCheckBox useProxy, useCredentials;
    private TwoCloumnPanel cred;
    private JTextField proxyHost, proxyUser;
    private JSpinner proxyPort;
    private JComboBox<String> proxyScheme;
    private JPasswordField pw;


    public ProxySettingsPanel(Properties properties) {
        super();
        this.props = properties;
        buildPanel();
        refreshValues();
    }

    private void buildPanel() {
        useProxy = new JCheckBox();
        useProxy.addActionListener(this);
        useProxy.setText("Use Proxy Server");
        useProxy.setSelected(Boolean.valueOf(props.getProperty("de.unijena.bioinf.sirius.proxy")));
        add(useProxy);

        proxyHost = new JTextField();
        proxyHost.setText(props.getProperty("de.unijena.bioinf.sirius.proxy.hostname"));
        add(new JLabel("Hostname:"), proxyHost);

        proxyPort = new JSpinner(new SpinnerNumberModel(8080, 1, 99999, 1));
        proxyPort.setEditor(new JSpinner.NumberEditor(proxyPort, "#"));
        proxyPort.setValue(Integer.valueOf(props.getProperty("de.unijena.bioinf.sirius.proxy.port")));
        add(new JLabel("Proxy Port:"), proxyPort);
        proxyScheme = new JComboBox<>(new String[]{"http", "https"});
        proxyScheme.setSelectedItem(props.getProperty("de.unijena.bioinf.sirius.proxy.scheme"));
        add(new JLabel("Proxy Scheme:"), proxyScheme);

        //############# Credentials Stuff ########################


        cred = new TwoCloumnPanel();
        cred.setBorder(new TitledBorder(new EmptyBorder(5, 5, 5, 5), "Proxy Credentials"));
        both.insets = new Insets(15, 0, 0, 0);
        add(cred);
        both.insets = new Insets(0, 0, 5, 0);


        //reset for new Panel
        useCredentials = new JCheckBox();
        useCredentials.addActionListener(this);
        useCredentials.setText("Use Credentials:");
        useCredentials.setSelected(Boolean.valueOf(props.getProperty("de.unijena.bioinf.sirius.proxy.credentials")));
        cred.add(useCredentials);

        proxyUser = new JTextField();
        proxyUser.setText(props.getProperty("de.unijena.bioinf.sirius.proxy.user"));
        cred.add(new JLabel("Username:"), proxyUser);

        pw = new JPasswordField();
        String text = PasswordCrypter.decryptProp("de.unijena.bioinf.sirius.proxy.pw", props);
        pw.setText(text);
        cred.add(new JLabel("Password:"), pw);

        addVerticalGlue();

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == useProxy) {
            refreshValues();
        } else if (e.getSource() == useCredentials) {
            refreshValues();
        }
    }

    @Override
    public void refreshValues() {
        proxyHost.setEnabled(useProxy.isSelected());
        proxyPort.setEnabled(useProxy.isSelected());
        proxyScheme.setEnabled(useProxy.isSelected());

        useCredentials.setEnabled(useProxy.isSelected());
        proxyUser.setEnabled(useCredentials.isSelected() && useProxy.isSelected());
        pw.setEnabled(useCredentials.isSelected() && useProxy.isSelected());
    }

    @Override
    public void saveProperties() {
        props.setProperty("de.unijena.bioinf.sirius.proxy", String.valueOf(useProxy.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.credentials", String.valueOf(useCredentials.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.hostname", proxyHost.getText());
        props.setProperty("de.unijena.bioinf.sirius.proxy.port", String.valueOf(proxyPort.getValue()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.scheme", (String) proxyScheme.getSelectedItem());
        props.setProperty("de.unijena.bioinf.sirius.proxy.user", proxyUser.getText());

        PasswordCrypter.setEncryptetProp("de.unijena.bioinf.sirius.proxy.pw", String.valueOf(pw.getPassword()), props);
    }

    @Override
    public String name() {
        return "Proxy";
    }


    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                String s = ApplicationCore.VERSION_STRING;
                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new ProxySettingsPanel(new Properties(System.getProperties())));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
