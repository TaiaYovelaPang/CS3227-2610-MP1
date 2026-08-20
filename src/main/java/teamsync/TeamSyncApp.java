package teamsync;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

/** Swing entry point for TeamSync's attendance-aware duty roster MVP. */
public final class TeamSyncApp extends JFrame {
    private final WorkspaceStore workspaceStore = new WorkspaceStore();
    private final AttendanceService attendanceService = new AttendanceService();
    private final RosterService rosterService = new RosterService();
    private final Workspace workspace = workspaceStore.load();

    private final JTextField sheetUrlField = new JTextField(48);
    private final JTextField dateField = new JTextField(10);
    private final JTextField dutyNameField = new JTextField(20);
    private final JTextField peopleNeededField = new JTextField("1", 4);
    private final JLabel attendanceStatus = new JLabel("No attendance loaded");
    private final JLabel workspaceStatus = new JLabel("Not linked");
    private final JTextArea rosterArea = new JTextArea();
    private final DefaultTableModel dutyTableModel = new DefaultTableModel(new String[] {"Duty", "People needed"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable dutyTable = new JTable(dutyTableModel);

    public TeamSyncApp() {
        super("TeamSync - Team Operations");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(950, 650));
        setSize(1050, 720);
        setLocationByPlatform(true);
        buildInterface();
        refreshView();
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent event) { saveWorkspace(); } });
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        root.setBackground(new Color(247, 248, 245));
        root.add(header(), BorderLayout.NORTH);

        JSplitPane content = new JSplitPane(JSplitPane.VERTICAL_SPLIT, workspacePanel(), rosterPanel());
        content.setBorder(null);
        content.setResizeWeight(0.62);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel header() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel title = new JLabel("TeamSync");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 27f));
        JLabel subtitle = new JLabel("Attendance-aware training duty rosters");
        subtitle.setForeground(new Color(88, 98, 106));
        JPanel words = new JPanel(new BorderLayout()); words.setOpaque(false); words.add(title, BorderLayout.NORTH); words.add(subtitle, BorderLayout.SOUTH);
        panel.add(words, BorderLayout.WEST);
        workspaceStatus.setHorizontalAlignment(SwingConstants.RIGHT);
        workspaceStatus.setForeground(new Color(35, 99, 67));
        panel.add(workspaceStatus, BorderLayout.EAST);
        return panel;
    }

    private JPanel workspacePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(titledBorder("1. Link attendance and manage duties"));
        GridBagConstraints c = constraints();

        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("Google Sheet URL"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; panel.add(sheetUrlField, c);
        JButton linkButton = new JButton("Link sheet"); linkButton.addActionListener(event -> linkSheet());
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE; panel.add(linkButton, c);

        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Training date (YYYY-MM-DD)"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; panel.add(dateField, c);
        JButton loadButton = new JButton("Load attendance"); loadButton.addActionListener(event -> loadAttendance(loadButton));
        c.gridx = 2; c.fill = GridBagConstraints.NONE; panel.add(loadButton, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL; panel.add(attendanceStatus, c);
        c.gridwidth = 1;

        c.gridx = 0; c.gridy = 3; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
        JLabel rule = new JLabel("Sheet format: first column 'Member'; date headers and attendance values of 1, 0.5, or 0. Only 1 is eligible.");
        rule.setForeground(new Color(88, 98, 106)); panel.add(rule, c); c.gridwidth = 1;

        c.gridx = 0; c.gridy = 4; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(18, 4, 4, 4);
        panel.add(new JLabel("Duties"), c); c.gridwidth = 1; c.insets = new Insets(5, 4, 5, 4);

        c.gridx = 0; c.gridy = 5; panel.add(new JLabel("Duty name"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; panel.add(dutyNameField, c);
        c.gridx = 2; c.fill = GridBagConstraints.NONE; panel.add(new JLabel("People needed"), c);
        c.gridx = 3; panel.add(peopleNeededField, c);

        JPanel dutyActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton add = new JButton("Add"); add.addActionListener(event -> addDuty());
        JButton update = new JButton("Update selected"); update.addActionListener(event -> updateDuty());
        JButton edit = new JButton("Load selected"); edit.addActionListener(event -> loadSelectedDuty());
        JButton delete = new JButton("Delete selected"); delete.addActionListener(event -> deleteDuty());
        dutyActions.add(add); dutyActions.add(update); dutyActions.add(edit); dutyActions.add(delete);
        c.gridx = 0; c.gridy = 6; c.gridwidth = 4; c.fill = GridBagConstraints.HORIZONTAL; panel.add(dutyActions, c); c.gridwidth = 1;

        JScrollPane tableScroll = new JScrollPane(dutyTable);
        tableScroll.setPreferredSize(new Dimension(600, 155));
        c.gridx = 0; c.gridy = 7; c.gridwidth = 4; c.weighty = 1; c.fill = GridBagConstraints.BOTH; panel.add(tableScroll, c);
        return panel;
    }

    private JPanel rosterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(titledBorder("2. Generate roster"));
        JPanel top = new JPanel(new BorderLayout());
        JLabel explanation = new JLabel("Each confirmed member receives one duty first. Repeats are allowed only when duty slots exceed attendees.");
        explanation.setForeground(new Color(88, 98, 106)); top.add(explanation, BorderLayout.WEST);
        JButton generate = new JButton("Generate duty roster"); generate.addActionListener(event -> generateRoster()); top.add(generate, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);
        rosterArea.setEditable(false); rosterArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13)); rosterArea.setText("Load attendance and add duties to generate a roster.");
        panel.add(new JScrollPane(rosterArea), BorderLayout.CENTER);
        return panel;
    }

    private void linkSheet() {
        String url = sheetUrlField.getText().trim();
        try { attendanceService.csvUri(url); }
        catch (IllegalArgumentException error) { showError(error.getMessage()); return; }
        workspace.setSheetUrl(url); saveWorkspace(); refreshView();
        attendanceStatus.setText("Sheet linked. Choose a date and load attendance.");
    }

    private void loadAttendance(JButton loadButton) {
        LocalDate date;
        try { date = LocalDate.parse(dateField.getText().trim()); }
        catch (DateTimeParseException error) { showError("Enter a date in YYYY-MM-DD format."); return; }
        if (workspace.sheetUrl().isBlank()) { showError("Link a Google Sheet before loading attendance."); return; }
        loadButton.setEnabled(false); attendanceStatus.setText("Loading attendance from Google Sheets...");
        new SwingWorker<List<AttendanceRecord>, Void>() {
            @Override protected List<AttendanceRecord> doInBackground() throws Exception { return attendanceService.loadAttendance(workspace.sheetUrl(), date); }
            @Override protected void done() {
                loadButton.setEnabled(true);
                try {
                    workspace.setSessionDate(date); workspace.setAttendance(get()); saveWorkspace(); refreshView();
                    attendanceStatus.setText(confirmedCount() + " confirmed members loaded for " + date + ".");
                } catch (Exception error) { showError(rootMessage(error)); attendanceStatus.setText("Attendance could not be loaded."); }
            }
        }.execute();
    }

    private void addDuty() {
        try { workspace.duties().add(new Duty(dutyNameField.getText(), peopleNeeded())); clearDutyFields(); saveWorkspace(); refreshView(); }
        catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void loadSelectedDuty() {
        Duty duty = selectedDuty(); if (duty == null) return;
        dutyNameField.setText(duty.name()); peopleNeededField.setText(String.valueOf(duty.peopleNeeded()));
    }

    private void updateDuty() {
        Duty duty = selectedDuty(); if (duty == null) return;
        try { duty.rename(dutyNameField.getText()); duty.setPeopleNeeded(peopleNeeded()); clearDutyFields(); saveWorkspace(); refreshView(); }
        catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void deleteDuty() {
        int row = dutyTable.getSelectedRow(); if (row < 0) { showError("Select a duty first."); return; }
        workspace.duties().remove(row); clearDutyFields(); saveWorkspace(); refreshView();
    }

    private void generateRoster() {
        try {
            List<RosterAssignment> roster = rosterService.generate(workspace); saveWorkspace();
            StringBuilder text = new StringBuilder("Duty roster - ").append(workspace.sessionDate()).append("\n\n");
            for (RosterAssignment assignment : roster) {
                text.append(assignment.dutyName()).append("\n");
                assignment.members().forEach(member -> text.append("  - ").append(member).append("\n"));
                if (assignment.unfilledSlots() > 0) text.append("  ! ").append(assignment.unfilledSlots()).append(" unfilled slot(s)\n");
                text.append("\n");
            }
            rosterArea.setText(text.toString());
        } catch (IllegalStateException error) { showError(error.getMessage()); }
    }

    private void refreshView() {
        sheetUrlField.setText(workspace.sheetUrl()); dateField.setText(workspace.sessionDate().toString());
        workspaceStatus.setText(workspace.sheetUrl().isBlank() ? "Not linked" : "Sheet linked");
        dutyTableModel.setRowCount(0); workspace.duties().forEach(duty -> dutyTableModel.addRow(new Object[] {duty.name(), duty.peopleNeeded()}));
    }

    private int peopleNeeded() { try { return Integer.parseInt(peopleNeededField.getText().trim()); } catch (NumberFormatException error) { throw new IllegalArgumentException("People needed must be a whole number."); } }
    private int confirmedCount() { return (int) workspace.attendance().stream().filter(AttendanceRecord::isConfirmed).count(); }
    private Duty selectedDuty() { int row = dutyTable.getSelectedRow(); if (row < 0) { showError("Select a duty first."); return null; } return workspace.duties().get(row); }
    private void clearDutyFields() { dutyNameField.setText(""); peopleNeededField.setText("1"); }
    private void saveWorkspace() { try { workspaceStore.save(workspace); } catch (IOException error) { showError("Could not save local workspace: " + error.getMessage()); } }
    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "TeamSync", JOptionPane.ERROR_MESSAGE); }
    private String rootMessage(Exception error) { return error.getCause() == null ? error.getMessage() : error.getCause().getMessage(); }
    private TitledBorder titledBorder(String title) { return BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(210, 220, 218)), title); }
    private GridBagConstraints constraints() { GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(5, 4, 5, 4); c.anchor = GridBagConstraints.WEST; return c; }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new TeamSyncApp().setVisible(true)); }
}
