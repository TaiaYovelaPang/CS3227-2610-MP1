package teamsync;

import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

/** JavaFX entry point for TeamSync's attendance-aware duty roster. */
public final class TeamSyncApp extends Application {
    private static final String APP_TITLE = "TeamSync";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final WorkspaceStore workspaceStore = new WorkspaceStore();
    private final AttendanceService attendanceService = new AttendanceService();
    private final RosterService rosterService = new RosterService();
    private final Workspace workspace = workspaceStore.load();
    private final ObservableList<Duty> duties = FXCollections.observableArrayList();
    private final ObservableList<ImportantDate> importantDates = FXCollections.observableArrayList();
    private final Set<String> displayedReminders = new HashSet<>();
    private final Timeline reminderChecker = new Timeline(new KeyFrame(Duration.seconds(30), event -> checkReminders()));

    private final Label pageTitle = new Label();
    private final Label pageSubtitle = new Label();
    private final Label connectionStatus = new Label();
    private final Label attendanceStatus = new Label();
    private final Label overviewStatus = new Label();
    private final TextField sheetUrlField = new TextField();
    private final DatePicker datePicker = new DatePicker();
    private final TextField dutyNameField = new TextField();
    private final Spinner<Integer> peopleNeededSpinner = new Spinner<>();
    private final CheckBox onTimeDutyCheck = selectedCheckBox("On time");
    private final CheckBox lateDutyCheck = new CheckBox("Coming late");
    private final CheckBox leavingEarlyDutyCheck = new CheckBox("Leaving early");
    private final TableView<Duty> dutyTable = new TableView<>();
    private final TableView<MonthlyRosterHistory.MemberRow> rosterHistoryTable = new TableView<>();
    private final TextArea rosterArea = new TextArea();
    private final TextArea attendanceMessageArea = new TextArea();
    private final DatePicker rosterHistoryMonthPicker = new DatePicker();
    private final TextField importantDateNameField = new TextField();
    private final DatePicker importantDatePicker = new DatePicker();
    private final TextField importantDateTimeField = new TextField();
    private final TextField importantDateEndTimeField = new TextField();
    private final CheckBox reminderToggle = new CheckBox("Remind me");
    private final ComboBox<ReminderOption> reminderOptionBox = new ComboBox<>();
    private final Spinner<Integer> customReminderSpinner = new Spinner<>();
    private final Label customReminderLabel = new Label("hours before");
    private final TableView<ImportantDate> importantDatesTable = new TableView<>();

    private final StackPane content = new StackPane();
    private final Button overviewNav = navButton("Overview");
    private final Button attendanceNav = navButton("Attendance");
    private final Button dutiesNav = navButton("Duties");
    private final Button rosterNav = navButton("Roster");
    private final Button importantDatesNav = navButton("Important dates");

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
        importantDates.setAll(workspace.importantDates());
        removePastImportantDates(LocalDateTime.now());
        refreshWorkspaceDetails();
        reminderChecker.setCycleCount(Timeline.INDEFINITE);
        reminderChecker.play();
        showOverview();
        Platform.runLater(this::checkReminders);

        Scene scene = new Scene(root, 1120, 720);
        scene.getStylesheets().add(TeamSyncApp.class.getResource("/teamsync/theme.css").toExternalForm());
        stage.setTitle(APP_TITLE + " - Manage your team effortlessly");
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        reminderChecker.stop();
        saveWorkspace();
    }

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
        importantDatesNav.setOnAction(event -> showImportantDates());
        Label navigationLabel = new Label("WORKSPACE");
        navigationLabel.getStyleClass().add("navigation-label");
        VBox navigation = new VBox(8, navigationLabel, overviewNav, attendanceNav, dutiesNav, rosterNav, importantDatesNav);
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
        Button datesAction = new Button("View important dates");
        datesAction.getStyleClass().add("secondary-button");
        datesAction.setOnAction(event -> showImportantDates());
        VBox upcomingCard = sectionCard("Upcoming important dates", upcomingDatesSummary(), datesAction);
        upcomingCard.getStyleClass().add("upcoming-summary-card");
        VBox overviewBody = new VBox(20, heading, explanation, cards, upcomingCard, overviewStatus);
        content.getChildren().setAll(pageContent(overviewBody));
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
        Label formatHint = new Label("Sheet format: the first column is Member; date columns use YYYY-MM-DD; use 1 = on time, 0 = absent, L = late, E = leaving early. Duty eligibility controls who may be assigned.");
        formatHint.setWrapText(true);
        formatHint.getStyleClass().add("help-text");
        attendanceStatus.getStyleClass().setAll("status-message");
        attendanceMessageArea.setEditable(false);
        attendanceMessageArea.setWrapText(true);
        attendanceMessageArea.getStyleClass().setAll("attendance-output");
        attendanceMessageArea.setText(attendanceMessage());
        attendanceMessageArea.setPrefRowCount(12);
        Button copyMessage = new Button("Copy attendance message");
        copyMessage.getStyleClass().add("secondary-button");
        copyMessage.setDisable(workspace.attendance().isEmpty());
        copyMessage.setOnAction(event -> copyAttendanceMessage());

        VBox statistics = attendanceStatistics();
        Button exportStatistics = new Button("Export statistics as image");
        exportStatistics.getStyleClass().add("secondary-button");
        exportStatistics.setDisable(workspace.attendance().isEmpty());
        exportStatistics.setOnAction(event -> exportStatistics(exportStatistics));
        VBox source = sectionCard("Attendance source",
                styledBox("form-row", 12, sheetUrlField, link), "Session date",
                styledBox("form-row", 12, datePicker, load), attendanceStatus, formatHint);
        VBox attendanceBody = new VBox(18, source,
                sectionCard("Copy-ready attendance update", attendanceMessageArea, copyMessage),
                sectionCard("Session statistics", statistics, exportStatistics));
        content.getChildren().setAll(pageContent(attendanceBody));
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
        HBox eligibilityRow = styledBox("form-row", 12, new Label("May be assigned to people:"), onTimeDutyCheck, lateDutyCheck, leavingEarlyDutyCheck);
        HBox actions = styledBox("action-row", 10, update, delete);
        VBox tableSection = new VBox(12, dutyTable, actions);
        VBox.setVgrow(dutyTable, Priority.ALWAYS);
        content.getChildren().setAll(pageContent(sectionCard("Add a duty", new VBox(12, nameRow, eligibilityRow), "Current duties", tableSection)));
    }

    private void showRoster() {
        selectNavigation(rosterNav);
        setPageHeading("Roster", "Generate an attendance-aware and balanced assignment");
        Button generate = new Button("Generate duty roster");
        generate.getStyleClass().add("primary-button");
        generate.setOnAction(event -> generateRoster());
        Label rule = new Label("Eligible members receive one duty before anyone is assigned a second duty.");
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
        rosterHistoryMonthPicker.setValue(workspace.sessionDate());
        rosterHistoryMonthPicker.setOnAction(event -> refreshRosterHistory());
        Button exportHistory = new Button("Export month as CSV");
        exportHistory.getStyleClass().add("secondary-button");
        exportHistory.setOnAction(event -> exportRosterHistory());
        Label historyHint = new Label("Select any day in the month to compare each member's duty allocations.");
        historyHint.getStyleClass().add("help-text");
        HBox historyToolbar = styledBox("roster-toolbar", 12, new Label("History month"), rosterHistoryMonthPicker, exportHistory);
        configureRosterHistoryTable();
        refreshRosterHistory();
        VBox historySection = new VBox(12, historyToolbar, historyHint, rosterHistoryTable);
        VBox.setVgrow(rosterHistoryTable, Priority.ALWAYS);
        VBox body = new VBox(18, sectionCard("Duty roster", rosterSection), sectionCard("Monthly allocation history", historySection));
        VBox.setVgrow(historySection, Priority.ALWAYS);
        content.getChildren().setAll(pageContent(body));
    }

    private void showImportantDates() {
        selectNavigation(importantDatesNav);
        setPageHeading("Important dates", "Keep team events and their optional reminders in one place");
        configureImportantDatesTable();
        importantDates.setAll(workspace.importantDates());
        clearImportantDateFields();

        importantDateNameField.setPromptText("e.g. Equipment briefing");
        importantDateTimeField.setPromptText("HH:mm (24-hour time)");
        importantDateEndTimeField.setPromptText("End: HH:mm");
        HBox.setHgrow(importantDateNameField, Priority.ALWAYS);
        Button add = new Button("Add event");
        add.getStyleClass().add("primary-button");
        add.setOnAction(event -> addImportantDate());
        Button update = new Button("Update selected");
        update.getStyleClass().add("secondary-button");
        update.setOnAction(event -> updateImportantDate());
        Button delete = new Button("Delete selected");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(event -> deleteImportantDate());

        HBox eventFields = styledBox("form-row", 12, importantDateNameField, importantDatePicker,
                importantDateTimeField, importantDateEndTimeField, add);
        HBox reminderFields = styledBox("form-row", 12, reminderToggle, reminderOptionBox, customReminderSpinner, customReminderLabel);
        Label timeHint = new Label("Use 24-hour time in HH:mm format, for example 09:30 or 14:00. Events must end after they start.");
        timeHint.getStyleClass().add("help-text");
        Label reminderHint = new Label("Reminders appear while TeamSync is open. Choose a standard lead time or set your own number of hours.");
        reminderHint.setWrapText(true);
        reminderHint.getStyleClass().add("help-text");
        HBox actions = styledBox("action-row", 10, update, delete);
        VBox tableSection = new VBox(12, importantDatesTable, actions);
        VBox.setVgrow(importantDatesTable, Priority.ALWAYS);
        VBox body = new VBox(18,
                sectionCard("Add an important date", eventFields, timeHint, "Reminder", reminderFields, reminderHint),
                sectionCard("Saved events", tableSection));
        VBox.setVgrow(tableSection, Priority.ALWAYS);
        content.getChildren().setAll(pageContent(body));
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
        TableColumn<Duty, String> eligibility = new TableColumn<>("Eligible attendance");
        eligibility.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(dutyEligibilityLabel(cell.getValue())));
        eligibility.setCellFactory(column -> wrappingCell());
        dutyTable.getColumns().setAll(List.of(name, people, eligibility));
        dutyTable.setItems(duties);
        dutyTable.setPlaceholder(new Label("No duties yet. Add the first job above."));
        dutyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        dutyTable.getSelectionModel().selectedItemProperty().addListener((observable, oldDuty, selectedDuty) -> {
            if (selectedDuty != null) {
                dutyNameField.setText(selectedDuty.name());
                peopleNeededSpinner.getValueFactory().setValue(selectedDuty.peopleNeeded());
                setDutyEligibility(selectedDuty.eligibleStatuses());
            }
        });
    }

    private void configureImportantDatesTable() {
        if (!importantDatesTable.getColumns().isEmpty()) return;
        reminderOptionBox.setItems(FXCollections.observableArrayList(
                ReminderOption.AT_EVENT_TIME, ReminderOption.ONE_HOUR_BEFORE,
                ReminderOption.ONE_DAY_BEFORE, ReminderOption.CUSTOM));
        reminderOptionBox.setValue(ReminderOption.AT_EVENT_TIME);
        customReminderSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 720, 1));
        customReminderSpinner.setEditable(true);
        importantDateTimeField.setPrefWidth(105);
        importantDateTimeField.setMaxWidth(105);
        importantDateEndTimeField.setPrefWidth(105);
        importantDateEndTimeField.setMaxWidth(105);
        customReminderSpinner.setPrefWidth(125);
        customReminderLabel.setVisible(false);
        customReminderLabel.setManaged(false);
        customReminderSpinner.setVisible(false);
        customReminderSpinner.setManaged(false);
        reminderOptionBox.setDisable(true);
        reminderToggle.selectedProperty().addListener((observable, oldValue, selected) -> updateReminderFields());
        reminderOptionBox.valueProperty().addListener((observable, oldValue, selected) -> updateReminderFields());

        TableColumn<ImportantDate, String> name = new TableColumn<>("Event");
        name.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().name()));
        name.setCellFactory(column -> wrappingCell());
        TableColumn<ImportantDate, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
                cell.getValue().date().format(DateTimeFormatter.ofPattern("EEE, d MMM uuuu"))));
        TableColumn<ImportantDate, String> time = new TableColumn<>("Start");
        time.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().time().format(TIME_FORMATTER)));
        TableColumn<ImportantDate, String> endTime = new TableColumn<>("End");
        endTime.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().endTime().format(TIME_FORMATTER)));
        TableColumn<ImportantDate, String> reminder = new TableColumn<>("Reminder");
        reminder.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().reminderLabel()));
        reminder.setCellFactory(column -> wrappingCell());
        importantDatesTable.getColumns().setAll(List.of(name, date, time, endTime, reminder));
        importantDatesTable.setItems(importantDates);
        importantDatesTable.setPlaceholder(new Label("No important dates yet. Add the first event above."));
        importantDatesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        importantDatesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldEvent, selectedEvent) -> {
            if (selectedEvent != null) populateImportantDateFields(selectedEvent);
        });
    }

    private void updateReminderFields() {
        boolean enabled = reminderToggle.isSelected();
        reminderOptionBox.setDisable(!enabled);
        boolean custom = enabled && reminderOptionBox.getValue() == ReminderOption.CUSTOM;
        customReminderSpinner.setVisible(custom);
        customReminderSpinner.setManaged(custom);
        customReminderLabel.setVisible(custom);
        customReminderLabel.setManaged(custom);
    }

    private void populateImportantDateFields(ImportantDate event) {
        importantDateNameField.setText(event.name());
        importantDatePicker.setValue(event.date());
        importantDateTimeField.setText(event.time().format(TIME_FORMATTER));
        importantDateEndTimeField.setText(event.endTime().format(TIME_FORMATTER));
        reminderToggle.setSelected(event.hasReminder());
        reminderOptionBox.setValue(event.hasReminder() ? event.reminderOption() : ReminderOption.AT_EVENT_TIME);
        if (event.reminderOption() == ReminderOption.CUSTOM) {
            customReminderSpinner.getValueFactory().setValue(Math.max(1, event.customReminderHours()));
        }
        updateReminderFields();
    }

    private void configureRosterHistoryTable() {
        rosterHistoryTable.setPlaceholder(new Label("No duty allocations have been saved for this month."));
        rosterHistoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
    }

    private void refreshRosterHistory() {
        LocalDate selectedDate = rosterHistoryMonthPicker.getValue();
        if (selectedDate == null) return;
        MonthlyRosterHistory report = MonthlyRosterHistory.from(YearMonth.from(selectedDate), workspace.history());
        TableColumn<MonthlyRosterHistory.MemberRow, String> member = new TableColumn<>("Member");
        member.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().memberName()));
        List<TableColumn<MonthlyRosterHistory.MemberRow, ?>> columns = new java.util.ArrayList<>();
        columns.add(member);
        for (String dutyName : report.dutyNames()) {
            TableColumn<MonthlyRosterHistory.MemberRow, String> duty = new TableColumn<>(dutyName);
            duty.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatAllocationDates(cell.getValue().datesFor(dutyName))));
            duty.setCellFactory(column -> wrappingCell());
            columns.add(duty);
        }
        TableColumn<MonthlyRosterHistory.MemberRow, Number> total = new TableColumn<>("Total");
        total.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().totalAssignments()));
        columns.add(total);
        rosterHistoryTable.getColumns().setAll(columns);
        rosterHistoryTable.setItems(FXCollections.observableArrayList(report.members()));
    }

    private String formatAllocationDates(List<LocalDate> dates) {
        if (dates.isEmpty()) return "—";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM");
        return dates.size() + " ×\n" + dates.stream().map(formatter::format).collect(java.util.stream.Collectors.joining(", "));
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
            attendanceStatus.setText(attendingCount() + " attending member(s) loaded for " + date + ".");
            loadButton.setDisable(false);
            showAttendance();
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
            DutyValidator.ensureUniqueName(workspace.duties(), dutyNameField.getText(), null);
            Duty duty = new Duty(dutyNameField.getText(), peopleNeededSpinner.getValue());
            duty.setEligibleStatuses(selectedDutyStatuses());
            workspace.duties().add(duty);
            duties.setAll(workspace.duties()); clearDutyFields(); saveWorkspace(); refreshWorkspaceDetails();
        } catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void updateDuty() {
        Duty duty = dutyTable.getSelectionModel().getSelectedItem();
        if (duty == null) { showError("Select a duty first."); return; }
        try {
            DutyValidator.ensureUniqueName(workspace.duties(), dutyNameField.getText(), duty.id());
            Duty updated = new Duty(duty.id(), dutyNameField.getText(), peopleNeededSpinner.getValue());
            updated.setEligibleStatuses(selectedDutyStatuses());
            duty.rename(dutyNameField.getText()); duty.setPeopleNeeded(peopleNeededSpinner.getValue());
            duty.setEligibleStatuses(updated.eligibleStatuses());
            duties.setAll(workspace.duties()); clearDutyFields(); saveWorkspace(); refreshWorkspaceDetails();
        } catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void deleteDuty() {
        Duty duty = dutyTable.getSelectionModel().getSelectedItem();
        if (duty == null) { showError("Select a duty first."); return; }
        workspace.duties().remove(duty);
        duties.setAll(workspace.duties()); clearDutyFields(); saveWorkspace(); refreshWorkspaceDetails();
    }

    private void addImportantDate() {
        try {
            ImportantDate event = eventFromFields();
            ImportantDateValidator.validate(event, workspace.importantDates(), null, LocalDateTime.now());
            workspace.importantDates().add(event);
            sortImportantDates();
            clearImportantDateFields();
            saveWorkspace();
        } catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void updateImportantDate() {
        ImportantDate selected = importantDatesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select an event first."); return; }
        try {
            ImportantDate updated = eventFromFields();
            ImportantDateValidator.validate(updated, workspace.importantDates(), selected, LocalDateTime.now());
            selected.update(updated.name(), updated.date(), updated.time(), updated.endTime(),
                    updated.reminderOption(), updated.customReminderMinutes());
            sortImportantDates();
            clearImportantDateFields();
            saveWorkspace();
        } catch (IllegalArgumentException error) { showError(error.getMessage()); }
    }

    private void deleteImportantDate() {
        ImportantDate selected = importantDatesTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select an event first."); return; }
        workspace.importantDates().remove(selected);
        importantDates.remove(selected);
        clearImportantDateFields();
        saveWorkspace();
    }

    private ImportantDate eventFromFields() {
        LocalTime time;
        LocalTime endTime;
        try {
            time = LocalTime.parse(importantDateTimeField.getText().trim(), TIME_FORMATTER);
            endTime = LocalTime.parse(importantDateEndTimeField.getText().trim(), TIME_FORMATTER);
        } catch (java.time.format.DateTimeParseException error) {
            throw new IllegalArgumentException("Enter start and end times in 24-hour HH:mm format, for example 09:30 and 10:30.");
        }
        ReminderOption option = reminderToggle.isSelected() ? reminderOptionBox.getValue() : null;
        return new ImportantDate(importantDateNameField.getText(), importantDatePicker.getValue(), time, endTime, option,
                Math.multiplyExact(customReminderSpinner.getValue(), 60));
    }

    private void sortImportantDates() {
        workspace.importantDates().sort(java.util.Comparator.comparing(ImportantDate::occursAt));
        importantDates.setAll(workspace.importantDates());
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
            if (rosterHistoryMonthPicker.getValue() != null) refreshRosterHistory();
        } catch (IllegalStateException error) { showError(error.getMessage()); }
    }

    private void exportRosterHistory() {
        LocalDate selectedDate = rosterHistoryMonthPicker.getValue();
        if (selectedDate == null) { showError("Choose a month to export."); return; }
        MonthlyRosterHistory report = MonthlyRosterHistory.from(YearMonth.from(selectedDate), workspace.history());
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export monthly duty allocations");
        chooser.setInitialFileName("duty-allocations-" + report.month() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File target = chooser.showSaveDialog(content.getScene().getWindow());
        if (target == null) return;
        try {
            Files.writeString(target.toPath(), rosterHistoryCsv(report), StandardCharsets.UTF_8);
        } catch (IOException error) {
            showError("Could not export duty allocations: " + error.getMessage());
        }
    }

    private String rosterHistoryCsv(MonthlyRosterHistory report) {
        StringBuilder csv = new StringBuilder("Month,Member,Duty,Date\n");
        for (MonthlyRosterHistory.MemberRow member : report.members()) {
            for (String duty : report.dutyNames()) {
                for (LocalDate date : member.datesFor(duty)) {
                    csv.append(csvField(report.month().toString())).append(',')
                            .append(csvField(member.memberName())).append(',')
                            .append(csvField(duty)).append(',').append(date).append('\n');
                }
            }
        }
        csv.append("\nMonth,Member,Total duty count\n");
        for (MonthlyRosterHistory.MemberRow member : report.members()) {
            csv.append(csvField(report.month().toString())).append(',')
                    .append(csvField(member.memberName())).append(',')
                    .append(member.totalAssignments()).append('\n');
        }
        return csv.toString();
    }

    private String csvField(String value) { return '"' + value.replace("\"", "\"\"") + '"'; }

    private void refreshWorkspaceDetails() {
        connectionStatus.setText(workspace.sheetUrl().isBlank() ? "Sheet not linked" : "Sheet linked");
        connectionStatus.getStyleClass().removeAll("status-neutral", "status-success");
        connectionStatus.getStyleClass().add(workspace.sheetUrl().isBlank() ? "status-neutral" : "status-success");
    }

    private VBox upcomingDatesSummary() {
        List<ImportantDate> upcoming = workspace.importantDates().stream()
                .filter(event -> event.endsAt().isAfter(LocalDateTime.now()))
                .sorted(java.util.Comparator.comparing(ImportantDate::occursAt))
                .limit(3)
                .toList();
        VBox summary = new VBox(8);
        if (upcoming.isEmpty()) {
            Label empty = new Label("No upcoming events. Add dates for meetings, deadlines, and briefings.");
            empty.getStyleClass().add("muted-text");
            summary.getChildren().add(empty);
            return summary;
        }
        for (ImportantDate event : upcoming) {
            Label name = new Label(event.name());
            name.getStyleClass().add("upcoming-event-name");
            Label details = new Label(event.date().format(DateTimeFormatter.ofPattern("EEE, d MMM")) + " · "
                    + event.time().format(TIME_FORMATTER) + "–" + event.endTime().format(TIME_FORMATTER));
            details.getStyleClass().add("upcoming-event-details");
            VBox eventText = new VBox(2, name, details);
            HBox.setHgrow(eventText, Priority.ALWAYS);
            HBox row = new HBox(10, eventText);
            row.getStyleClass().add("upcoming-event-row");
            summary.getChildren().add(row);
        }
        return summary;
    }

    private void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        removePastImportantDates(now);
        for (ImportantDate event : workspace.importantDates()) {
            if (!event.hasReminder()) continue;
            LocalDateTime reminderAt = event.occursAt().minusMinutes(event.reminderMinutesBefore());
            String reminderKey = event.name() + "|" + event.occursAt() + "|" + event.reminderMinutesBefore();
            if (!now.isBefore(reminderAt) && !now.isAfter(reminderAt.plusMinutes(2)) && displayedReminders.add(reminderKey)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(APP_TITLE + " reminder");
                alert.setHeaderText(event.name());
                alert.setContentText("Scheduled for " + event.date().format(DateTimeFormatter.ofPattern("EEE, d MMM uuuu"))
                        + " at " + event.time().format(TIME_FORMATTER) + ".");
                alert.show();
            }
        }
    }

    private void removePastImportantDates(LocalDateTime now) {
        if (!workspace.removePastImportantDates(now)) return;
        importantDates.setAll(workspace.importantDates());
        saveWorkspace();
    }

    private String attendanceSummary() { return workspace.attendance().isEmpty() ? "No attendance loaded yet." : attendingCount() + " attending member(s) for " + workspace.sessionDate() + "."; }
    private String dutySummary() { return workspace.duties().isEmpty() ? "No duties have been added." : workspace.duties().size() + " duty type(s) added."; }
    private String rosterSummary() { return workspace.attendance().isEmpty() || workspace.duties().isEmpty() ? "Attendance and duties are needed first." : "Ready to generate assignments."; }
    private int confirmedCount() { return (int) workspace.attendance().stream().filter(AttendanceRecord::isConfirmed).count(); }
    private int attendingCount() { return (int) workspace.attendance().stream().filter(AttendanceRecord::attended).count(); }
    private int count(AttendanceStatus status) {
        return (int) workspace.attendance().stream().filter(record -> record.status() == status).count();
    }

    private String attendanceMessage() {
        return AttendanceUpdateFormatter.format(workspace.sessionDate(), workspace.attendance());
    }

    private VBox attendanceStatistics() {
        VBox statistics = new VBox(12);
        statistics.getStyleClass().add("statistics-export");
        Label title = new Label("Attendance snapshot — " + workspace.sessionDate());
        title.getStyleClass().add("statistics-title");
        HBox cards = new HBox(12,
                statisticCard(attendingCount() + " / " + workspace.attendance().size(), "Attended", percentage(attendingCount(), workspace.attendance().size()) + " attendance rate"),
                statisticCard(count(AttendanceStatus.ON_TIME) + " / " + Math.max(attendingCount(), 1), "On time", percentage(count(AttendanceStatus.ON_TIME), attendingCount()) + " of attendees"),
                statisticCard(String.valueOf(count(AttendanceStatus.LATE)), "Late arrivals", percentage(count(AttendanceStatus.LATE), attendingCount()) + " of attendees"),
                statisticCard(String.valueOf(count(AttendanceStatus.LEFT_EARLY)), "Early departures", percentage(count(AttendanceStatus.LEFT_EARLY), attendingCount()) + " of attendees"));
        cards.getStyleClass().add("statistics-cards");
        statistics.getChildren().addAll(title, cards);
        return statistics;
    }

    private VBox statisticCard(String value, String label, String detail) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("statistic-value");
        Label labelLabel = new Label(label);
        labelLabel.getStyleClass().add("statistic-label");
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("statistic-detail");
        VBox card = new VBox(4, valueLabel, labelLabel, detailLabel);
        card.getStyleClass().add("statistic-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private String percentage(int numerator, int denominator) {
        if (denominator == 0) return "0%";
        return Math.round(numerator * 100.0 / denominator) + "%";
    }

    private void copyAttendanceMessage() {
        javafx.scene.input.ClipboardContent clipboardContent = new javafx.scene.input.ClipboardContent();
        clipboardContent.putString(attendanceMessage());
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(clipboardContent);
        attendanceStatus.setText("Attendance update copied to the clipboard.");
    }

    private void exportStatistics(Button exportButton) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export attendance statistics");
        chooser.setInitialFileName("attendance-statistics-" + workspace.sessionDate() + ".png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        File target = chooser.showSaveDialog(content.getScene().getWindow());
        if (target == null) return;
        exportButton.setDisable(true);
        attendanceStatus.setText("Preparing monthly attendance statistics for export.");
        Task<MonthlyAttendanceReport> task = new Task<>() {
            @Override protected MonthlyAttendanceReport call() throws Exception {
                return attendanceService.loadMonthlyAttendance(workspace.sheetUrl(), YearMonth.from(workspace.sessionDate()));
            }
        };
        task.setOnSucceeded(event -> {
            try {
                WritableImage image = snapshotExportStatistics(task.getValue());
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", target);
                attendanceStatus.setText("Statistics exported to " + target.getName() + ".");
            } catch (IOException error) {
                showError("Could not export the statistics image: " + error.getMessage());
            } finally {
                exportButton.setDisable(false);
            }
        });
        task.setOnFailed(event -> {
            exportButton.setDisable(false);
            attendanceStatus.setText("Statistics could not be exported.");
            showError(rootMessage(task.getException()));
        });
        Thread exporter = new Thread(task, "attendance-statistics-exporter");
        exporter.setDaemon(true);
        exporter.start();
    }

    private WritableImage snapshotExportStatistics(MonthlyAttendanceReport report) {
        VBox exportView = new VBox(18, attendanceStatistics(), monthlyAttendanceStatistics(report));
        exportView.getStyleClass().add("export-sheet");
        exportView.setPrefWidth(920);
        Scene exportScene = new Scene(exportView);
        exportScene.getStylesheets().add(TeamSyncApp.class.getResource("/teamsync/theme.css").toExternalForm());
        exportView.applyCss();
        exportView.resize(920, exportView.prefHeight(920));
        exportView.layout();
        return exportView.snapshot(new SnapshotParameters(), null);
    }

    private VBox monthlyAttendanceStatistics(MonthlyAttendanceReport report) {
        String monthName = report.month().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                + " " + report.month().getYear();
        Label title = new Label("Monthly attendance — " + monthName);
        title.getStyleClass().add("statistics-title");
        Label explanation = new Label("Late arrivals and early departures count as attended. Percentages use marked sessions only.");
        explanation.getStyleClass().add("statistic-detail");
        Label monthlyOverall = new Label("Monthly team attendance: " + report.overallAttendancePercentage() + "% ("
                + report.attendedMembersAcrossSessions() + " / " + report.markedMembersAcrossSessions() + " marked records)");
        monthlyOverall.getStyleClass().add("monthly-overall-statistic");
        VBox sessionRows = new VBox(5);
        report.sessions().forEach(session -> sessionRows.getChildren().add(monthlyRow(
                session.date().toString(), session.attendedMembers() + " / " + session.markedMembers()
                        + " attended", session.percentage() + "%")));
        VBox memberRows = new VBox(5);
        report.members().forEach(member -> memberRows.getChildren().add(monthlyRow(
                member.memberName(), member.attendedSessions() + " / " + member.markedSessions() + " sessions",
                member.percentage() + "%")));
        VBox monthly = new VBox(12, title, explanation, monthlyOverall, sectionLabel("Attendance by session"), sessionRows,
                sectionLabel("Individual attendance"), memberRows);
        monthly.getStyleClass().add("statistics-export");
        return monthly;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("statistic-label");
        return label;
    }

    private HBox monthlyRow(String primary, String secondary, String percentage) {
        Label primaryLabel = new Label(primary);
        primaryLabel.getStyleClass().add("monthly-row-primary");
        Label secondaryLabel = new Label(secondary);
        secondaryLabel.getStyleClass().add("monthly-row-secondary");
        Label percentageLabel = new Label(percentage);
        percentageLabel.getStyleClass().add("monthly-row-percentage");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, primaryLabel, secondaryLabel, spacer, percentageLabel);
        row.getStyleClass().add("monthly-statistic-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    private void clearDutyFields() {
        dutyNameField.clear();
        peopleNeededSpinner.getValueFactory().setValue(1);
        setDutyEligibility(Set.of(AttendanceStatus.ON_TIME));
        dutyTable.getSelectionModel().clearSelection();
    }

    private static CheckBox selectedCheckBox(String label) {
        CheckBox checkBox = new CheckBox(label);
        checkBox.setSelected(true);
        return checkBox;
    }

    private Set<AttendanceStatus> selectedDutyStatuses() {
        Set<AttendanceStatus> statuses = EnumSet.noneOf(AttendanceStatus.class);
        if (onTimeDutyCheck.isSelected()) statuses.add(AttendanceStatus.ON_TIME);
        if (lateDutyCheck.isSelected()) statuses.add(AttendanceStatus.LATE);
        if (leavingEarlyDutyCheck.isSelected()) statuses.add(AttendanceStatus.LEFT_EARLY);
        return statuses;
    }

    private void setDutyEligibility(Set<AttendanceStatus> statuses) {
        onTimeDutyCheck.setSelected(statuses.contains(AttendanceStatus.ON_TIME));
        lateDutyCheck.setSelected(statuses.contains(AttendanceStatus.LATE));
        leavingEarlyDutyCheck.setSelected(statuses.contains(AttendanceStatus.LEFT_EARLY));
    }

    private String dutyEligibilityLabel(Duty duty) {
        return java.util.Arrays.stream(AttendanceStatus.values()).filter(duty::isEligible)
                .map(AttendanceStatus::displayName).collect(java.util.stream.Collectors.joining(", "));
    }

    private void clearImportantDateFields() {
        importantDateNameField.clear();
        LocalDateTime defaultStart = LocalDateTime.now().withSecond(0).withNano(0);
        if (!defaultStart.toLocalTime().isBefore(LocalTime.of(23, 0))) defaultStart = defaultStart.plusDays(1).withHour(0).withMinute(0);
        importantDatePicker.setValue(defaultStart.toLocalDate());
        importantDateTimeField.setText(defaultStart.toLocalTime().format(TIME_FORMATTER));
        importantDateEndTimeField.setText(defaultStart.toLocalTime().plusHours(1).format(TIME_FORMATTER));
        reminderToggle.setSelected(false);
        reminderOptionBox.setValue(ReminderOption.AT_EVENT_TIME);
        customReminderSpinner.getValueFactory().setValue(1);
        importantDatesTable.getSelectionModel().clearSelection();
        updateReminderFields();
    }

    private void setPageHeading(String title, String subtitle) {
        pageTitle.setText(title); pageTitle.getStyleClass().setAll("page-title");
        pageSubtitle.setText(subtitle); pageSubtitle.getStyleClass().setAll("page-subtitle");
    }

    private void selectNavigation(Button selected) {
        for (Button button : List.of(overviewNav, attendanceNav, dutiesNav, rosterNav, importantDatesNav)) button.getStyleClass().remove("navigation-button-selected");
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

    private <T> TableCell<T, String> wrappingCell() {
        TableCell<T, String> cell = new TableCell<>();
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
