package genericRecommenderSystem;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.google.common.collect.BiMap;

import net.librec.data.model.TextDataModel;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;

public class RecommenderDashboard extends JFrame {

    private final Properties configProperties = new Properties();
    
    // UI Components
    private final JTextArea logArea;
    private final JLabel statusLabel;
    private final JButton runBtn;
    private final DefaultTableModel resultsModel;
    private final JLabel ndcgLabel, precisionLabel, recallLabel, f1Label;

    // Configuration Text Fields
    private final JTextField rsMetamodelField, domainMetamodelField;
    private final JTextField rsModelField, domainModelField;

    public RecommenderDashboard() {
        super("Generic Model-Driven Recommender System");
        
        logArea = new JTextArea();
        statusLabel = new JLabel("Status: Please configure file paths and run.");
        runBtn = new JButton("Run Recommender");
        resultsModel = new DefaultTableModel(new String[]{"User ID", "Item ID", "Score"}, 0);
        ndcgLabel = createMetricLabel("NDCG@10");
        precisionLabel = createMetricLabel("Precision@10");
        recallLabel = createMetricLabel("Recall@10");
        f1Label = createMetricLabel("F1@10");

        rsMetamodelField = new JTextField(40);
        domainMetamodelField = new JTextField(40);
        rsModelField = new JTextField(40);
        domainModelField = new JTextField(40);

        initializeUI();
    }
    
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Configuration", createConfigPanel());
        tabbedPane.addTab("Execution & Results", createExecutionPanel());
        
        add(tabbedPane);
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        panel.add(new JLabel("<html><b>Generic Metamodel (.ecore)</b></html>"), gbc);
        addFileSelectionRow("Recommender (rs):", rsMetamodelField, panel, ++gbc.gridy);
        
        gbc.gridy++;
        panel.add(new JLabel("<html><b>Domain-Specific Metamodel (.ecore)</b></html>"), gbc);
        addFileSelectionRow("Domain (e.g., movies, books):", domainMetamodelField, panel, ++gbc.gridy);
        
        gbc.gridy++;
        panel.add(new JSeparator(), gbc);
        
        gbc.gridy++;
        panel.add(new JLabel("<html><b>Model Instances (.model, .xmi)</b></html>"), gbc);
        addFileSelectionRow("Recommender Instance:", rsModelField, panel, ++gbc.gridy);
        addFileSelectionRow("Domain Instance (Items & Users):", domainModelField, panel, ++gbc.gridy);
        
        return panel;
    }

    private void addFileSelectionRow(String label, JTextField textField, JPanel panel, int yPos) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = yPos;
        gbc.gridx = 0; gbc.weightx = 0.2;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        panel.add(textField, gbc);

        gbc.gridx = 2; gbc.weightx = 0.1;
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                textField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        panel.add(browseBtn, gbc);
    }

    private JPanel createExecutionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runBtn.addActionListener(e -> runRecommender());
        controlsPanel.add(runBtn);
        panel.add(controlsPanel, BorderLayout.NORTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, createResultsSection(), createLogSection());
        splitPane.setResizeWeight(0.6);
        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createResultsSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel metricsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        metricsPanel.setBorder(BorderFactory.createTitledBorder("Evaluation Metrics"));
        metricsPanel.add(ndcgLabel);
        metricsPanel.add(precisionLabel);
        metricsPanel.add(recallLabel);
        metricsPanel.add(f1Label);
        panel.add(metricsPanel, BorderLayout.NORTH);

        JTable resultsTable = new JTable(resultsModel);
        JScrollPane tableScrollPane = new JScrollPane(resultsTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Recommendations"));
        panel.add(tableScrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private JScrollPane createLogSection() {
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log"));
        return scrollPane;
    }

    private void runRecommender() {
        log("--- Starting Recommender Process ---");
        statusLabel.setText("Status: Running...");
        runBtn.setEnabled(false);
        resultsModel.setRowCount(0);

        if (!buildPropertiesFromUI()) {
            // Error message is shown by buildPropertiesFromUI
            statusLabel.setText("Status: Error.");
            runBtn.setEnabled(true);
            return;
        }

        new Thread(() -> {
            PrintStream printStream = new PrintStream(new CustomOutputStream(logArea));
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            System.setOut(printStream);
            System.setErr(printStream);
            File tempConfigFile = null;

            try {
                tempConfigFile = File.createTempFile("recommender_config", ".properties");
                try (FileOutputStream out = new FileOutputStream(tempConfigFile)) {
                    configProperties.store(out, "Dynamic configuration from Dashboard");
                }
                
                System.setProperty("config.file", tempConfigFile.getAbsolutePath());
                Main.main(new String[0]);
                
                RecommendedList recs = Main.getLastRecommendedList();
                TextDataModel dataModel = Main.getLastDataModel();
                
                SwingUtilities.invokeLater(() -> {
                    updateMetrics(Main.getLastNdcg(), Main.getLastPrecision(), Main.getLastRecall(), Main.getLastF1());
                    if (recs != null && dataModel != null) {
                        updateResultsTable(recs, dataModel);
                    }
                    statusLabel.setText("Status: Process Finished Successfully.");
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Process Failed. See log for details."));
                e.printStackTrace();
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
                if (tempConfigFile != null) {
                    tempConfigFile.delete();
                }
                SwingUtilities.invokeLater(() -> runBtn.setEnabled(true));
            }
        }).start();
    }
    
    private boolean buildPropertiesFromUI() {
        configProperties.clear();

        File rsMetamodelFile = new File(rsMetamodelField.getText());
        File domainMetamodelFile = new File(domainMetamodelField.getText());

        String rsMetamodelUri = extractNsUriFromFile(rsMetamodelFile);
        String domainMetamodelUri = extractNsUriFromFile(domainMetamodelFile);

        if (rsMetamodelUri == null || domainMetamodelUri == null) {
            String errorMsg = "Could not extract nsURI from one or more metamodel files. Please check file paths and content.";
            log(errorMsg);
            JOptionPane.showMessageDialog(this, errorMsg, "Metamodel Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        configProperties.setProperty("metamodels.to.register", "recommender,domain");
        configProperties.setProperty("metamodel.recommender.path", rsMetamodelField.getText());
        configProperties.setProperty("metamodel.domain.path", domainMetamodelField.getText());
        
        configProperties.setProperty("models.to.load", "recommender_instance,domain_instance");
        
        configProperties.setProperty("model.recommender_instance.name", "Recommender");
        configProperties.setProperty("model.recommender_instance.path", rsModelField.getText());
        configProperties.setProperty("model.recommender_instance.metamodel_uri", rsMetamodelUri);
        
        configProperties.setProperty("model.domain_instance.name", "Domain");
        configProperties.setProperty("model.domain_instance.path", domainModelField.getText());
        configProperties.setProperty("model.domain_instance.metamodel_uri", domainMetamodelUri);
        
        configProperties.setProperty("eol.script", "/Users/ricksonsimionipereira/eclipse-workspace/Conferences/RecommenderSystem/genericRecommenderSystemModel-FocusGroup/src/main/Models/EOL_scripts/dataExtraction.eol");
        configProperties.setProperty("output.tmp", System.getProperty("java.io.tmpdir"));
        configProperties.setProperty("rec.knn", "50");
        return true;
    }

    private String extractNsUriFromFile(File ecoreFile) {
        if (ecoreFile == null || !ecoreFile.exists()) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Document doc = factory.newDocumentBuilder().parse(ecoreFile);
            doc.getDocumentElement().normalize();
            return doc.getDocumentElement().getAttribute("nsURI");
        } catch (Exception e) {
            log("Failed to parse nsURI from " + ecoreFile.getName() + ": " + e.getMessage());
            return null;
        }
    }
    
    private void updateResultsTable(RecommendedList recs, TextDataModel dataModel) {
        BiMap<String, Integer> userMapping = dataModel.getUserMappingData();
        BiMap<String, Integer> itemMapping = dataModel.getItemMappingData();
        for (int userIdx = 0; userIdx < recs.size(); userIdx++) {
            List<KeyValue<Integer, Double>> items = recs.getKeyValueListByContext(userIdx);
            if (items == null) continue;
            String userId = userMapping.inverse().get(userIdx);
            for (KeyValue<Integer, Double> kv : items) {
                String itemId = itemMapping.inverse().get(kv.getKey());
                resultsModel.addRow(new Object[]{userId, itemId, String.format("%.4f", kv.getValue())});
            }
        }
    }
    
    private void updateMetrics(double ndcg, double precision, double recall, double f1) {
        ndcgLabel.setText(String.format("%.4f", ndcg));
        precisionLabel.setText(String.format("%.4f", precision));
        recallLabel.setText(String.format("%.4f", recall));
        f1Label.setText(String.format("%.4f", f1));
    }

    private JLabel createMetricLabel(String title) {
        JLabel label = new JLabel("0.0000", JLabel.CENTER);
        label.setBorder(BorderFactory.createTitledBorder(title));
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        return label;
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RecommenderDashboard().setVisible(true));
    }
}

class CustomOutputStream extends OutputStream {
    private final JTextArea textArea;
    public CustomOutputStream(JTextArea textArea) {
        this.textArea = textArea;
    }
    @Override
    public void write(int b) throws IOException {
        SwingUtilities.invokeLater(() -> {
            textArea.append(String.valueOf((char)b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}