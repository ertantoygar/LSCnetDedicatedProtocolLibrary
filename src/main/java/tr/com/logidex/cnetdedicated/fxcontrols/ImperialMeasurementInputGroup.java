package tr.com.logidex.cnetdedicated.fxcontrols;

import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.InputEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;

import java.util.function.Consumer;


public class ImperialMeasurementInputGroup extends Group {

    private final TextField tfYard = new TextField("0");
    private final TextField tfInch = new TextField("0");
    private final TextField tfInchFraction = new TextField("0");
    private final Text textYard = new Text("YD");
    private final Text textInch = new Text("\"");
    private final Text textFraction = new Text("'");
    private final Label labelInfo = new Label("0");
    private final SimpleObjectProperty<ImperialMeasurement> imperialMeasurement =
            new SimpleObjectProperty<>(ImperialMeasurement.of(0, 0, 0));

    public ImperialMeasurementInputGroup() {
        initializeComponents();
        setupLayout();
        setAppearance();


    }

    public SimpleObjectProperty<ImperialMeasurement> imperialMeasurementProperty() {
        return imperialMeasurement;
    }

    private void initializeComponents() {

        tfYard.setPrefWidth(60);
        tfInch.setPrefWidth(60);
        tfInchFraction.setPrefWidth(60);


        setupFieldHandler(tfYard, c -> {
            inputDataIntoTheField(c, 0, ImperialMeasurement.YARD_MAX);
        });
        setupFieldHandler(tfInch, c -> {
            inputDataIntoTheField(c, 0, ImperialMeasurement.INCH_MAX);
        });
        setupFieldHandler(tfInchFraction, c -> {
            inputDataIntoTheField(c, 0, ImperialMeasurement.INCH_FRACTION_MAX);
        });


        addEvents(tfYard);
        addEvents(tfInch);
        addEvents(tfInchFraction);


    }


    private void setupFieldHandler(TextField field, Consumer<Control> handler) {
        if (XGBCNetClient.touchScreen.get()) {
            // Try touch first
            field.setOnTouchPressed((event) -> {
                System.out.printf("TouchPressed ");
                handler.accept(field);
                event.consume(); // Prevent synthesis
            });
            // Fallback to synthesized mouse events
            field.setOnMouseClicked((event) -> {
                if (event.isSynthesized()) {
                    System.out.printf("setOnMouseClicked sythesized ");
                    handler.accept(field);
                }
            });
        } else {
            field.setOnMouseClicked((event) -> {
                System.out.printf("setOnMouseClicked normal");
                    handler.accept(field);
            });
        }
    }


    private void inputDataIntoTheField(Control control, double min, double max)  {



        if (control instanceof TextField textField) {


            TouchNumericKeypad.getInstance().show(textField.getText(), min, max, true, control, value -> {
                textField.setText(value);

                try {
                    setNewMeasurement();
                } catch (Exception e) {

                }


            });
        }
    }


    private void addEvents(TextField tf) {
        tf.setOnAction(event -> {
            try {
                setNewMeasurement();
            } catch (Exception e) {
                tf.setText("0");
                try {
                    setNewMeasurement();
                } catch (Exception ignore) {

                }

            }
        });

        tf.focusedProperty().addListener((observable, oldValue, newValue) -> {

            if (!newValue) {
                try {
                    setNewMeasurement();
                } catch (Exception e) {
                    tf.setText("0");
                    try {
                        setNewMeasurement();
                    } catch (Exception ignore) {

                    }

                }
            }
        });


    }

    private void setupLayout() {


        VBox vbox = new VBox();
        vbox.setSpacing(10);
        vbox.setBackground(new Background(new BackgroundFill(Color.YELLOW, CornerRadii.EMPTY, Insets.EMPTY)));

        // Horizontal layout - yan yana dizilim
        HBox hbox = new HBox(2); // 5px spacing
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.setStyle("-fx-background-radius: 5px;-fx-border-radius: 5px;-fx-border-color:" +
                " black;-fx-background-color: yellow");
        //vbox.setPrefHeight();

        // Yard kısmı
        HBox yardBox = new HBox(2);
        yardBox.getChildren().addAll(tfYard, textYard);
        yardBox.setAlignment(Pos.CENTER_LEFT);
        yardBox.setPadding(new Insets(3));

        // Inch kısmı
        HBox inchBox = new HBox(2);
        inchBox.getChildren().addAll(tfInch, textInch);
        inchBox.setAlignment(Pos.CENTER_LEFT);
        inchBox.setPadding(new Insets(3));

        // Fraction kısmı
        HBox fractionBox = new HBox(2);
        fractionBox.getChildren().addAll(tfInchFraction, textFraction);
        fractionBox.setAlignment(Pos.CENTER_LEFT);
        fractionBox.setPadding(new Insets(3));


        hbox.getChildren().addAll(yardBox, inchBox, fractionBox);
        vbox.getChildren().add(hbox);
        //vbox.getChildren().add(labelInfo);

        getChildren().add(vbox);
    }

    private void setAppearance() {
        setAppearance(tfYard);
        setAppearance(tfInch);
        setAppearance(tfInchFraction);
    }

    private void setAppearance(TextField tf) {
        tf.getStyleClass().add("imperial-input");
        // CSS ile stillendirebilirsiniz
        tf.setStyle("-fx-text-alignment: center;-fx-font-size: 22px");
        textYard.setStyle("-fx-font-size: 16px");
        textInch.setStyle("-fx-font-size: 16px");
        textFraction.setStyle("-fx-font-size: 16px");
        labelInfo.setStyle("-fx-font-size: 16px;-fx-padding: 5px");
    }

    public void setNewMeasurement() throws Exception {

            var yard = Integer.parseInt(tfYard.getText());
            var inch = Integer.parseInt(tfInch.getText());
            var fraction = Integer.parseInt(tfInchFraction.getText());

            var measurement = ImperialMeasurement.of(yard, inch, fraction);
            imperialMeasurement.setValue(measurement);
             tfYard.setText(String.valueOf(measurement.yard()));
            tfInch.setText(String.valueOf(measurement.inch()));
            tfInchFraction.setText(String.valueOf(measurement.inchFraction()));
            var measInMM = measurement.getTotalInMM();
            labelInfo.setText(measInMM + " mm");

            System.out.println("updated");


    }


    public void setFromMM(double value) {
        var newMeas = ImperialMeasurement.fromMillimeters(value);
        imperialMeasurement.setValue(newMeas);
        tfYard.setText(String.valueOf(newMeas.yard()));
        tfInch.setText(String.valueOf(newMeas.inch()));
        tfInchFraction.setText(String.valueOf(newMeas.inchFraction()));


    }
}

