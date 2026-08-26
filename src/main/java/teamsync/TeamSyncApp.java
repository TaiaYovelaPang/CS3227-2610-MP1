package teamsync;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** JavaFX entry point for TeamSync's attendance-aware duty roster. */
public final class TeamSyncApp extends Application {
    private static final String APP_TITLE = "TeamSync";

    private final WorkspaceStore workspaceStore = new WorkspaceStore();
    private final AttendanceService attendanceService = new AttendanceService();
    private final RosterService rosterService = new RosterService();
    private final Workspace workspace = workspaceStore.load();
    private final ObservableList<Duty> duties = FXCollections.observableArrayList();

    private final Label pageTitle = new Label();
    private final Label pageSubtitle = new Label();
    private final Label connectionStatus = new Label();
    private final Label attendanceStatus = new Label();
    private final Label overviewStatus = new Label();
    private final TextField sheetUrlField = new TextField();
    private final DatePicker datePicker = new DatePicker();
    private final TextField dutyNameField = new TextField();
    private final Spinner<Integer> peopleNeededSpinner = new Spinner<>();
    private final TableView<Duty> dutyTable = new TableView<>();
    private final TextArea rosterArea = new TextArea();

    private final StackPane content = new StackPane();
    private final Button overviewNav = navButton("Overview");
    private final Button attendanceNav = navButton("Attendance");
    private final Button dutiesNav = navButton("Duties");
    private final Button rosterNav = navButton("Roster");

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(buildTopBar());
        root.setLeft(buildNavigation());
        javafx.scene.control.ScrollPane contentScroll = new javafx.scene.control.ScrollPane(content);
        contentScroll.setFitToWidth(true);
        contentScroll.getStyleClass().add("content-scroll");
        root.setCenter(contentScroll);

        duties.setAll(workspace.duties());
        refreshWorkspaceDetails();
        showOverview();

        Scene scene = new Scene(root, 1120, 720);
        scene.getStylesheets().add(TeamSyncApp.class.getResource("/teamsync/theme.css").toExternalForm());
        stage.setTitle(APP_TITLE + " - Manage your team effortlessly");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() { saveWorkspace(); }

    private HBox buildTopBar() {
        Label product = new Label(APP_TITLE);
        product.getStyleClass().add("product-name");
        Label subtitle = new Label("Team operations");
        subtitle.getStyleClass().add("topbar-subtitle");
        VBox brand = new VBox(0, product, subtitle);
        brand.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        connectionStatus.getStyleClass().addAll("status-pill", "status-neutral");
        return styledBox("top-bar", 18, brand, spacer, connectionStatus);
    }

    private VBox buildNavigation() {
        overviewNav.setOnAction(event -> showOverview());
        attendanceNav.setOnAction(event -> showAttendance());
        dutiesNav.setOnAction(event -> showDuties());
        rosterNav.setOnAction(event -> showRoster());
        Label navigationLabel = new Label("WORKSPACE");
        navigationLabel.getStyleClass().add("navigation-label");
        VBox navigation = new VBox(8, navigationLabel, overviewNav, attendanceNav, dutiesNav, rosterNav);
        navigation.getStyleClass().add("navigation");
        navigation.setPadding(new Insets(24, 14, 24, 14));
        return navigation;
    }

    private void showOverview() {
        selectNavigation(overviewNav);
        setPageHeading("Overview", "Your team operations at a glance");
        Label heading = new Label("Get your roster ready");
        heading.setWrapText(true);
        heading.getStyleClass().add("section-heading");
        Label explanation = new Label("Start with attendance, define the duties, then generate a fair roster.");
        explanation.setWrapText(true);
        explanation.getStyleClass().add("muted-text");

        Button attendanceAction = new Button("Manage attendance");
        attendanceAction.getStyleClass().add("primary-button");
        attendanceAction.setOnAction(event -> showAttendance());
        Button dutiesAction = new Button("Manage duties");
        dutiesAction.getStyleClass().add("secondary-button");
        dutiesAction.setOnAction(event -> showDuties());
        Button rosterAction = new Button("Open roster");
        rosterAction.getStyleClass().add("secondary-button");
        rosterAction.setOnAction(event -> showRoster());
        VBox attendanceCard = summaryCard("01", "Attendance", attendanceSummary(), attendanceAction);
        VBox dutiesCard = summaryCard("02", "Duties", dutySummary(), dutiesAction);
        VBox rosterCard = summaryCard("03", "Roster", rosterSummary(), rosterAction);
        FlowPane cards = styledFlow("summary-cards", 16, attendanceCard, dutiesCard, rosterCard);
        configureSummaryCardSizes(cards, attendanceCard, dutiesCard, rosterCard);
        content.getChildren().setAll(pageContent(styledBox("overview-content", 20, heading, explanation, cards, overviewStatus)));
    }

    private void showAttendance() {
        selectNavigation(attendanceNav);
        setPageHeading("Attendance", "Link the shared sheet and load the selected session");
        sheetUrlField.setText(workspace.sheetUrl());
        datePicker.setValue(workspace.sessionDate());
        sheetUrlField.setPromptText("Paste a Google Sheets URL");
        HBox.setHgrow(sheetUrlField, Priority.ALWAYS);
        Button link = new Button("Link sheet");
        link.getStyleClass().add("secondary-button");
        link.setOnAction(event -> linkSheet());
        Button load = new Button("Load attendance");
        load.getStyleClass().add("primary-button");
        load.setOnAction(event -> loadAttendance(load));
        Label formatHint = new Label("Sheet format: the first column is Member; date columns use YYYY-MM-DD; only attendance value 1 is selected for duties.");
        formatHint.setWrapText(true);
        formatHint.getStyleClass().add("help-text");
        attendanceStatus.getStyleClass().setAll("status-message");
        content.getChildren().setAll(pageContent(sectionCard("Attendance source",
                styledBox("form-row", 12, sheetUrlField, link), "Session date",
                styledBox("form-row", 12, datePicker, load), attendanceStatus, formatHint)));
    }

    private void showDuties() {
        selectNavigation(dutiesNav);
        setPageHeading("Duties", "Define the jobs that need coverage");
        configureDutyTable();
        dutyNameField.setPromptText("e.g. Equipment set-up");
        peopleNeededSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
        peopleNeededSpinner.setEditable(true);
        Button add = new Button("Add duty");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> addDuty());
        Button update = new Button("Update selected");
        update.getStyleClass().add("secondary-button");
        update.setOnAction(event -> updateDuty());
        Button delete = new Button("Delete selected");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteDuty());
        HBox nameRow = styledBox("form-row", 12, dutyNameField, peopleNeededSpinner, add);
        HBox.setHgrow(dutyNameField, Priority.ALWAYS);
        HBox actions = styledBox("action-row", 10, update, delete);
        VBox tableSection = new VBox(12, dutyTable, actions);
        VBox.setVgrow(dutyTable, Priority.ALWAYS);
        content.getChildren().setAll(pageContent(sectionCard("Add a duty", nameRow, "Current duties", tableSection)));
    }

    private void showRoster() {
        selectNavigation(rosterNav);
        setPageHeading("Roster", "Generate an attendance-aware and balanced assignment");
        Button generate = new Button("Generate duty roster");
        generate.getStyleClass().add("primary-button");
        generate.setOnAction(event -> generateRoster());
        Label rule = new Label("Confirmed members receive one duty before anyone is assigned a second duty.");
        rule.setWrapText(true);
        rule.getStyleClass().add("help-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = styledBox("roster-toolbar", 12, rule, spacer, generate);
        rosterArea.setEditable(false);
        rosterArea.setWrapText(false);
        rosterArea.getStyleClass().add("roster-output");
        if (rosterArea.getText().isBlank()) rosterArea.setText("Load attendance and add duties to generate a roster.");
        VBox rosterSection = new VBox(12, toolbar, rosterArea);
        VBox.setVgrow(rosterArea, Priority.ALWAYS);
        content.getChildren().setAll(pageContent(sectionCard("Duty roster", rosterSection)));
    }

    private VBox pageContent(Node body) {
        VBox page = new VBox(22, pageTitle, pageSubtitle, body);
        page.getStyleClass().add("page-content");
        page.setPadding(new Insets(32));
        VBox.setVgrow(body, Priority.ALWAYS);
        return page;
    }

    private VBox sectionCard(String title, Object... contentNodes) {
        VBox card = new VBox(14);
        card.getStyleClass().add("content-card");
        Label label = new Label(title);
        label.getStyleClass().add("card-heading");
        card.getChildren().add(label);
        for (Object node : contentNodes) {
            if (node instanceof String text) {
                Label fieldLabel = new Label(text);
                fieldLabel.getStyleClass().add("field-label");
                card.getChildren().add(fieldLabel);
            } else if (node instanceof Node child) card.getChildren().add(child);
        }
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox summaryCard(String step, String title, String detail, Button action) {
        Label number = new Label(step);
        number.getStyleClass().add("step-number");
        Label cardTitle = new Label(title);
        cardTitle.getStyleClass().add("card-heading");
        Label description = new Label(detail);
        description.setWrapText(true);
        description.getStyleClass().add("muted-text");
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        VBox card = new VBox(12, number, cardTitle, description, spacer, action);
        card.getStyleClass().add("summary-card");
        card.setMinWidth(220);
        return card;
    }

    private void configureDutyTable() {
        if (!dutyTable.getColumns().isEmpty()) return;
        TableColumn<Duty, String> name = new TableColumn<>("Duty");
        name.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().name()));
        name.setCellFactory(column -> wrappingCell());
        TableColumn<Duty, Number> people = new TableColumn<>("People needed");
        people.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().peopleNeeded()));
        dutyTable.getColumns().setAll(List.of(name, people));
        dutyTable.setItems(duties);
        dutyTable.setPlaceholder(new Label("No duties yet. Add the first job above."));
        dutyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        dutyTable.getSelectionModel().selectedItemProperty().addListener((observable, oldDuty, selectedDuty) -> {
            if (selectedDuty != null) {
                dutyNameField.setText(selectedDuty.name());
                peopleNeededSpinner.getValueFactory().setValue(selectedDuty.peopleNeeded());
            }
        });
    }

    private void linkSheet() {
        try {
            String url = sheetUrlField.getText().trim();
            attendanceService.csvUri(url);
            workspace.setSheetUrl(url);
            saveWorkspace();
            refreshWorkspaceDetails();
            attendanceStatus.setText("Sheet linked. Choose a date and load attendance.");
        } catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void loadAttendance(Button loadButton) {
        LocalDate date = datePicker.getValue();
        if (date == null) { showError("Choose a session date."); return; }
        if (workspace.sheetUrl().isBlank()) { showError("Link a Google Sheet before loading attendance."); return; }
        loadButton.setDisable(true);
        attendanceStatus.setText("Loading attendance from Google Sheets");
        Task<List<AttendanceRecord>> task = new Task<>() {
            @Override protected List<AttendanceRecord> call() throws Exception {
                return attendanceService.loadAttendance(workspace.sheetUrl(), date);
            }
        };
        task.setOnSucceeded(event -> {
            workspace.setSessionDate(date);
            workspace.setAttendance(task.getValue());
            saveWorkspace();
            refreshWorkspaceDetails();
            attendanceStatus.setText(confirmedCount() + " confirmed members loaded for " + date + ".");
            loadButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            loadButton.setDisable(false);
            attendanceStatus.setText("Attendance could not be loaded.");
            showError(rootMessage(task.getException()));
        });
        Thread loader = new Thread(task, "attendance-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void addDuty() {
        try {
            workspace.duties().add(new Duty(dutyNameField.getText(), peopleNeededSpinner.getValue()));
            duties.setAll(workspace.duties()); clearDutyFields(); saveWorkspace(); refreshWorkspaceDetails();
        } catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void updateDuty() {
        Duty duty = dutyTable.getSelectionModel().getSelectedItem();
        if (duty == null) { showError("Select a duty first."); return; }
        try {
            duty.rename(dutyNameField.getText()); duty.setPeopleNeeded(peopleNeededSpinner.getValue());
            duties.setAll(workspace.duties()); clearDutyFields(); saveWorkspace(); refreshWorkspaceDetails();
        } catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void deleteDuty() {
        Duty duty = dutyTable.getSelectionModel().getSelectedItem();
        if (duty == null) { showError("Select a duty first."); return; }
        workspace.duties().remove(duty);
        duties.setAll(workspace.duties()); clearDutyFields(); saveWorkspace(); refreshWorkspaceDetails();
    }

    private void generateRoster() {
        try {
            List<RosterAssignment> roster = rosterService.generate(workspace);
            saveWorkspace();
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

    private void refreshWorkspaceDetails() {
        connectionStatus.setText(workspace.sheetUrl().isBlank() ? "Sheet not linked" : "Sheet linked");
        connectionStatus.getStyleClass().removeAll("status-neutral", "status-success");
        connectionStatus.getStyleClass().add(workspace.sheetUrl().isBlank() ? "status-neutral" : "status-success");
    }

    private String attendanceSummary() { return workspace.attendance().isEmpty() ? "No attendance loaded yet." : confirmedCount() + " confirmed member(s) for " + workspace.sessionDate() + "."; }
    private String dutySummary() { return workspace.duties().isEmpty() ? "No duties have been added." : workspace.duties().size() + " duty type(s) added."; }
    private String rosterSummary() { return workspace.attendance().isEmpty() || workspace.duties().isEmpty() ? "Attendance and duties are needed first." : "Ready to generate assignments."; }
    private int confirmedCount() { return (int) workspace.attendance().stream().filter(AttendanceRecord::isConfirmed).count(); }
    private void clearDutyFields() { dutyNameField.clear(); peopleNeededSpinner.getValueFactory().setValue(1); dutyTable.getSelectionModel().clearSelection(); }

    private void setPageHeading(String title, String subtitle) {
        pageTitle.setText(title); pageTitle.getStyleClass().setAll("page-title");
        pageSubtitle.setText(subtitle); pageSubtitle.getStyleClass().setAll("page-subtitle");
    }

    private void selectNavigation(Button selected) {
        for (Button button : List.of(overviewNav, attendanceNav, dutiesNav, rosterNav)) button.getStyleClass().remove("navigation-button-selected");
        selected.getStyleClass().add("navigation-button-selected");
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE); button.setAlignment(Pos.CENTER_LEFT); button.getStyleClass().add("navigation-button");
        return button;
    }

    private HBox styledBox(String styleClass, double spacing, Node... nodes) {
        HBox box = new HBox(spacing, nodes);
        box.getStyleClass().add(styleClass); box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private FlowPane styledFlow(String styleClass, double spacing, Node... nodes) {
        FlowPane pane = new FlowPane(spacing, spacing, nodes);
        pane.getStyleClass().add(styleClass);
        return pane;
    }

    private void configureSummaryCardSizes(FlowPane cards, VBox... cardsToSize) {
        for (VBox card : cardsToSize) {
            card.prefWidthProperty().bind(Bindings.createDoubleBinding(() -> {
                double availableWidth = cards.getWidth();
                if (availableWidth >= 760) return (availableWidth - 32) / 3;
                if (availableWidth >= 500) return (availableWidth - 16) / 2;
                return availableWidth;
            }, cards.widthProperty()));
        }
    }

    private TableCell<Duty, String> wrappingCell() {
        TableCell<Duty, String> cell = new TableCell<>();
        cell.setWrapText(true);
        cell.setTextOverrun(OverrunStyle.CLIP);
        cell.itemProperty().addListener((observable, oldValue, newValue) -> cell.setText(newValue));
        return cell;
    }

    private void saveWorkspace() {
        try { workspaceStore.save(workspace); }
        catch (IOException error) { showError("Could not save the local workspace: " + error.getMessage()); }
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(APP_TITLE); alert.setHeaderText("Action could not be completed");
            alert.setContentText(message == null || message.isBlank() ? "An unexpected error occurred." : message);
            alert.showAndWait();
        });
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage();
    }

    public static void main(String[] args) { launch(args); }
}
