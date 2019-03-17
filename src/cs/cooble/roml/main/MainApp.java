package cs.cooble.roml.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;


public class MainApp extends Application implements Runnable {

    Stage window;
    Scene comScene;

    Button selectBtn;
    public Button flashBtn;
    public Button sendBtn;
    public ComboBox<String> comPortBox;
    public TextArea textArea;
    public TextArea textArea2;
    public TextField textField;
    public Controller controller;
    public CheckBox checkBox;
    private Runnable mujTimerListener;
    private boolean isRunning;

    private Font font = new Font("Consolas",15);

    @Override
    public void start(final Stage primaryStage) throws Exception {

        window = primaryStage;
        window.setTitle("EEPROM Flasher");
        setComScene();
        mujTimerListener=this;

        window.setScene(comScene);
        window.show();
        runLoop();
        window.setOnCloseRequest(event -> {
            try {
                controller.onProgramQuited();
                Platform.exit();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        controller = new Controller(this, comPortBox);


    }

    private void setComScene() {
        //Settings scene 1
        BorderPane layout1 = new BorderPane();
        comScene = new Scene(layout1, 1380, 720);
        comPortBox = new ComboBox<>();
        Label label = new Label("ComPort");
        selectBtn = new Button();
        selectBtn.setText("Connect");
        selectBtn.setOnAction(event -> controller.onButtonPressed(selectBtn));
        textArea = new TextArea();
        textArea.setFont(font);
        textArea.setPrefWidth(10000);
        textArea.setEditable(false);
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            textArea.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
            //use Double.MIN_VALUE to scroll to the top
        });
        textArea2 = new TextArea();
        textArea2.setEditable(false);
        textArea2.setFont(font);
        textArea2.setPrefWidth(10000);
        textArea2.textProperty().addListener((observable, oldValue, newValue) -> {
            textArea2.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
            //use Double.MIN_VALUE to scroll to the top
        });
        textField = new TextField();
        sendBtn = new Button("Send");
        sendBtn.setDisable(true);
        sendBtn.setOnAction(event -> controller.onButtonPressed(sendBtn));
        checkBox = new CheckBox("AutoScroll");
        checkBox.setOnAction(event -> {
            controller.setCheckBoxState(checkBox.isSelected());
            if(checkBox.isSelected()){
                textArea.appendText("");
                textArea2.appendText("");
            }
        });
        Button toPrintBtn = new Button("Load file");
        toPrintBtn.setOnAction(event -> controller.loadFile());
        Button okBtn = new Button("Ready");
        okBtn.setOnAction(event -> controller.testButton());
        Button checkBtn = new Button("CheckWrite");
        checkBtn.setOnAction(event -> controller.checkIfSuccessWrite());
        Button writeBtn = new Button("Write to Memory");
        writeBtn.setOnAction(event -> controller.writeToMemory());
        Button wholeBtn = new Button("Write whole to Memory");
        wholeBtn.setOnAction(event -> controller.writeWholeMemory());
        Button readToFileBtn = new Button("Read to file");
        readToFileBtn.setOnAction(event -> controller.readToFile());
        Button readBtnMe = new Button("Save wrongs");
        readBtnMe.setOnAction(event -> controller.saveWrongs());
        Button clearLogBtn = new Button("Clear log");
        clearLogBtn.setOnAction(event -> {
            controller.clearLog();
            textArea.setText("");
            textArea2.setText("");
        });
        BorderPane borderPaneTop = new BorderPane();
        HBox hBoxRIGHT = new HBox(20);
        HBox hBoxLEFT = new HBox(20);
        hBoxLEFT.getChildren().addAll(label, comPortBox, selectBtn, sendBtn, textField);
        hBoxRIGHT.getChildren().addAll(clearLogBtn, checkBox,okBtn,readBtnMe,checkBtn, wholeBtn,writeBtn,readToFileBtn,toPrintBtn);
        borderPaneTop.setRight(hBoxRIGHT);
        borderPaneTop.setLeft(hBoxLEFT);
        layout1.setTop(borderPaneTop);
        HBox textAreaBox = new HBox(20);
        textAreaBox.getChildren().addAll(textArea, textArea2);
        layout1.setCenter(textAreaBox);
    }

    public void sleep(int milis) {
        try {
            Thread.sleep(milis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void runLoop() {
        Thread thread = new Thread(() -> {
            isRunning = true;
            while (isRunning) {
                mujTimerListener.run();
                sleep(1);
            }
        });
        thread.start();

    }

    public void stopLoop() {
        isRunning = false;
    }

    @Override
    public void run() {

    }
}
