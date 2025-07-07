package tr.com.logidex.cnetdedicated.fxcontrols;


import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;
import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.Device;
import tr.com.logidex.cnetdedicated.device.DisplayFormat;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.exceptions.FrameCheckException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoAcknowledgeMessageFromThePLCException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoResponseException;

import java.io.IOException;


public class LSTextField extends TextField {


    private boolean logging;
    private long savedLastLogTime;

    public enum DataLen {
        Bit,
        Byte,
        Word,
        Dword,
        Lword;
    }


    private Tag tag;
    private static final int MIN_INPUT_LIMIT = 8;
    private ValidationSupport validationSupport = new ValidationSupport();

    private String lastValue = "";

    private ObjectProperty<Device> device = new SimpleObjectProperty<Device>();
    private ObjectProperty<DataLen> dataType = new SimpleObjectProperty<DataLen>();
    private StringProperty tagAddress = new SimpleStringProperty();
    private ObjectProperty<DisplayFormat> displayFormat = new SimpleObjectProperty<DisplayFormat>();
    private IntegerProperty multiplier = new SimpleIntegerProperty(1);
    private IntegerProperty inputCharLimit = new SimpleIntegerProperty(MIN_INPUT_LIMIT);
    private IntegerProperty minValue = new SimpleIntegerProperty(Integer.MIN_VALUE);
    private IntegerProperty maxValue = new SimpleIntegerProperty(Integer.MAX_VALUE);
    private StringProperty name = new SimpleStringProperty();

    /**
     * The eventTagWrote variable represents an instance of the TagWroteEvent class,
     * which is an event that is fired when a tag is written.
     */
    private TagWroteEvent eventTagWrote = new TagWroteEvent();

    private BooleanProperty behaveLikeAfloat = new SimpleBooleanProperty();

    Background defaultBackground = new Background(new BackgroundFill(Color.rgb(158, 184, 217), new CornerRadii(5), Insets.EMPTY));
    Background focusedBackground = new Background(new BackgroundFill(Color.rgb(224, 159, 143), new CornerRadii(5), Insets.EMPTY));

    boolean firstChangeLog;

    public LSTextField() {

        setFocusTraversable(false);

        update();


        textProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue.length() > inputCharLimit.get()) {
                setText(oldValue);
            }

        });

        setOnMouseClicked(e -> {

            tag.pauseUpdating();
            saveLastValue();
            selectAll();


            if (displayFormat.get() == DisplayFormat.STRING) {


                TouchKeyboard.getInstance().show(getText(),255,e,LSTextField.this,value ->{setText(value);});


            } else {

                TouchNumericKeypad.getInstance().show(getText(),minValue.doubleValue(),maxValue.doubleValue(),true,e,LSTextField.this,value ->{setText(value);});

            }





        });


        setOnAction(e -> {

            System.out.println(e.getSource());


            if (!valid() && !behaveLikeAfloat.get()) {
                setText(getLastValue());
                getTag().resumeUpdating();
                if (getScene() != null) {
                    getScene().getRoot().requestFocus();
                }
                e.consume();
                return;
            }


            setByItsDataType();
            try {
                if (getDisplayFormat().equals(DisplayFormat.STRING)) {
                    XGBCNetClient.getInstance().writeSingleString(getTag(), getTag().getValue(), inputCharLimitProperty().get());
                } else {
                    if (tag.getDataType() == DataType.Word) {
                        XGBCNetClient.getInstance().writeSingle(getTag());
                    } else if (tag.getDataType() == DataType.Dword) {
                        XGBCNetClient.getInstance().writeDouble(getTag());
                    }
                }
                if(getScene() !=null) { // fire metodu ile deger degistiginde getscene nul donuyor
                    getScene().getRoot().requestFocus();
                }

                getTag().resumeUpdating();

            } catch (IOException | NoAcknowledgeMessageFromThePLCException | NoResponseException |
                     FrameCheckException ex) {
                ex.printStackTrace();
            }


        });


        setBackground(defaultBackground);
        setStyle("-fx-border-color: #9f9c9c;-fx-border-radius: 5px");


        focusedProperty().addListener(new ChangeListener<Boolean>() {

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {


                if (newValue) {
                    setBackground(focusedBackground);
                    tag.pauseUpdating();



                } else {
                    fireEvent(new ActionEvent());
                    setBackground(defaultBackground);
                    tag.resumeUpdating();

                }

            }
        });


    }





    public String getName() {
        return name.get();
    }


    public StringProperty nameProperty() {
        return name;
    }


    public void setName(String name) {
        this.name.set(name);
    }


    public Device getDevice() {
        return device.get();
    }

    public ObjectProperty<Device> deviceProperty() {
        return device;
    }

    public void setDevice(Device device) {
        this.device.set(device);
    }

    public DataLen getDataType() {
        return dataType.get();
    }

    public ObjectProperty<DataLen> dataTypeProperty() {
        return dataType;
    }

    public void setDataType(DataLen dataType) {
        this.dataType.set(dataType);
    }

    public String getTagAddress() {
        return tagAddress.get();
    }

    public StringProperty tagAddressProperty() {
        return tagAddress;
    }

    public void setTagAddress(String tagAddress) {

        // property uzerinden verilen adresler word adresleridir. 32 bit okuma yapilacaksa
        // double adresine erismek icin verilen adresin yarisi alinmalidir.
        if (getDataTypeForDataLen(getDataType()) == DataType.Dword) {
            int  newAddress = Integer.parseInt(tagAddress) / 2;
            tagAddress = String.valueOf(newAddress);
        }


        this.tagAddress.set(tagAddress);
        int tagMultiplier = isBehaveLikeAfloat() ? 1 : multiplier.getValue();
        tag = new Tag(nameProperty().get(), getDevice(), getDataTypeForDataLen(getDataType()), getTagAddress(), getDisplayFormat(), tagMultiplier);

        tag.valueProperty().addListener((observable, oldValue, newValue) -> update());
        Tooltip tt = new Tooltip();
        tt.setText(tag.toString() + ":" + getDisplayFormat());
        setTooltip(tt);

        if (!isDisable()) {
            addValidator();
        }


    }

    public DisplayFormat getDisplayFormat() {
        return displayFormat.get();
    }

    public ObjectProperty<DisplayFormat> displayFormatProperty() {
        return displayFormat;
    }

    public void setDisplayFormat(DisplayFormat displayFormat) {
        this.displayFormat.set(displayFormat);
    }


    public boolean isBehaveLikeAfloat() {
        return behaveLikeAfloat.get();
    }

    public BooleanProperty behaveLikeAfloatProperty() {
        return behaveLikeAfloat;
    }

    public void setBehaveLikeAfloat(boolean behaveLikeAfloat) {
        this.behaveLikeAfloat.set(behaveLikeAfloat);
    }


    public TagWroteEvent getEventTagWrote() {
        return eventTagWrote;
    }

    private void addValidator() {

        validationSupport.setValidationDecorator(null);


        validationSupport.setErrorDecorationEnabled(false);

        if (tag.isNumericTag()) {
            validationSupport.registerValidator(this, (control, value) -> {
                try {
                    int intValue = Integer.parseInt(this.getText());
                    if (intValue < minValue.get() || intValue > maxValue.get()) {
                        return ValidationResult.fromError(control, "Lütfen " + minValue.get() + " ile " + maxValue.get() + " arasında bir sayı girin.");
                    }
                } catch (NumberFormatException e) {
                    // Sayıya çevrilemeyen bir giriş
                    return ValidationResult.fromError(control, "Lütfen bir sayı girin.");
                }
                return null;
            });
        }

    }

    public boolean valid() {
        getValidationSupport().setErrorDecorationEnabled(true);
        getValidationSupport().redecorate();
        return !validationSupport.isInvalid();
    }


    public int getMultiplier() {
        return multiplier.get();
    }

    public IntegerProperty multiplierProperty() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier.set(multiplier);
    }

    public int getInputCharLimit() {
        return inputCharLimit.get();
    }

    public IntegerProperty inputCharLimitProperty() {
        return inputCharLimit;
    }

    public void setInputCharLimit(int inputCharLimit) {
        if (inputCharLimit < MIN_INPUT_LIMIT)
            return;
        this.inputCharLimit.set(inputCharLimit);
    }

    public String getLastValue() {
        return lastValue;
    }


    public int getMinValue() {
        return minValue.get();
    }

    public IntegerProperty minValueProperty() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue.set(minValue);
    }

    public int getMaxValue() {
        return maxValue.get();
    }

    public IntegerProperty maxValueProperty() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue.set(maxValue);
    }

    public Tag getTag() {
        return tag;
    }

    public ValidationSupport getValidationSupport() {
        return validationSupport;
    }

    public void update() {

        if(tag == null){
            return;
        }

        Platform.runLater(() -> {

            if (behaveLikeAfloat.get()) {

                String s = tag.getValue();


                float f = Float.parseFloat(s) / (float) multiplier.get();
                this.setText(Float.toString(f));
            } else {

                this.setText(tag.getValue());
            }
        });



    }


    private DataType getDataTypeForDataLen(DataLen dataLen) {
        switch (dataLen) {
            case Bit:
                return DataType.Bit;
            case Byte:
                return DataType.Byte;
            case Word:
                return DataType.Word;
            case Dword:
                return DataType.Dword;
            case Lword:
                return DataType.Lword;
            default:
                throw new IllegalArgumentException("Invalid DataLen: " + dataLen);

        }
    }


    public void saveLastValue() {
        lastValue = getText();
    }


    public void setByItsDataType() {

        Tag tag = getTag();

        if (behaveLikeAfloat.get()) {
            float f = Float.parseFloat(getText());
            short s = (short) (f * (float) multiplier.get());
            tag.setValueAsHexString(getTag().toHexString(s));
            return;
        }

        switch (tag.getDataType()) {
            case Word:
                if (getDisplayFormat().equals(DisplayFormat.STRING)) {
                    tag.setValueAsHexString(getTag().toHexString(getText().getBytes()));
                } else {
                    tag.setValueAsHexString(getTag().toHexString(Short.valueOf(getText())));
                }
                break;
            case Dword:
                if (getDisplayFormat().equals(DisplayFormat.FLOAT)) {
                    tag.setValueAsHexString(getTag().toHexString(Float.valueOf(getText())));
                } else {
                    tag.setValueAsHexString(getTag().toHexString(Integer.valueOf(getText())));

                }
                break;



        }

        if ((System.currentTimeMillis() - savedLastLogTime) > 1000 ){

                fireEvent(eventTagWrote);
                savedLastLogTime = System.currentTimeMillis();


        }




    }




}
