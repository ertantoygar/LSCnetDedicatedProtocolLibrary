package tr.com.logidex.cnetdedicated.fxcontrols;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.lang.reflect.Method;

public class ImperialMeasLSTextField extends LSTextField {


    private final  ImperialMeasurementInputGroup inputGroup = new ImperialMeasurementInputGroup() ;
    private boolean popupShowing;
    private String defaultStyle;
    private StringProperty printable = new SimpleStringProperty("");
    public ImperialMeasLSTextField() {


        defaultStyle = getStyle();
        setOnMouseClicked(null); // remove from the super class.






        setOnMouseClicked(e->{
            if(popupShowing){
                e.consume();
                return;
            }
            Stage stage =  new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.setResizable(false);
            stage.setTitle("U.S/Imperial Meas.Unit");
            GridPane gridPane = new GridPane();
            gridPane.setHgap(10);

            try {
                inputGroup.setFromMM(Double.parseDouble(getText()));
            }
            catch (NumberFormatException ex) {

            }

            gridPane.add(inputGroup,0,0);
            Scene scene = new Scene(gridPane);
            stage.setScene(scene);
            stage.setOnCloseRequest(windowEvent->{
                popupShowing = false;
                try {
                    inputGroup.setNewMeasurement();
                } catch (Exception ignore) {

                }

            });

            popupShowing = true;
            stage.showAndWait();



        });



        inputGroup.imperialMeasurementProperty().addListener((meas,oldValue,newValue) -> {


            int value =(int)Math.round(newValue.getTotalInMM());


            if(value > getMaxValue()){
                setText("LIM.ERR");
                setStyle( getStyle() + ";-fx-text-fill: red;");
                return;
            }

            setStyle( defaultStyle);

            super.setText(String.valueOf(value));
            printable.set(newValue.yard() + "Y"+ newValue.inch()+"\"" + newValue.inchFraction()+"\'");

                getTag().pauseUpdating();

            Platform.runLater(()->{
                performAction();
            });



        });

    }


    public StringProperty printableProperty() {
        return printable;
    }

    public void addGroup() {

       Parent parent =  this.getScene().getRoot();
       inputGroup.setLayoutX(this.getLayoutX());
       inputGroup.setLayoutY(this.getLayoutY() + 40);

        try {
            Method[] methods = parent.getClass().getMethods();
            for (Method method : methods) {
                if (method.getName().equals("getChildren")) {

                    ((Pane)parent).getChildren().add(inputGroup);

                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(parent);

    }


}
