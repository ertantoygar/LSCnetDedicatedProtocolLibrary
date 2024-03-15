package tr.com.logidex.cnetdedicated.fxcontrols;

import com.fazecast.jSerialComm.SerialPortIOException;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;
import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.Device;
import tr.com.logidex.cnetdedicated.device.DisplayFormat;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.exceptions.FrameCheckException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoAcknowledgeMessageFromThePLCException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoResponseException;
import tr.com.logidex.cnetdedicated.util.XGBCNetUtil;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LSButton extends Button {

    private boolean touchFunctions = false;
    Border border = new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT));
    Background greenBackGround = new Background(new BackgroundFill(Color.PALEGREEN, new CornerRadii(10), javafx.geometry.Insets.EMPTY));
    Background normalBackGround = new Background(new BackgroundFill(Color.rgb(192, 205, 220), new CornerRadii(10), javafx.geometry.Insets.EMPTY));


    private final Lock lock = new ReentrantLock();


    private Thread t;
    private SimpleBooleanProperty readBitStatus = new SimpleBooleanProperty();


    /**
     * Represents an event that is triggered when a tag is written to.
     * This event is associated with a {@link TagWroteEvent} object.
     */
   private TagWroteEvent eventTagWrote = new TagWroteEvent();



    /**
     * Yazdığı bitten mi geri bildirim alıyor?Farklı bir bitten mi?
     *
     * @return
     */
    public String decideForReadingWord() {
        return usesAnotherFeedBackAddress.get() ? backWordIndex : writeWordIndex;
    }

    public String decideForReadingBit() {
        return usesAnotherFeedBackAddress.get() ? fBackBitPositionInTheWord : writeBitPositionInTheWord;
    }

    public enum BtnActionType {
        ON, OFF, MOMENTARY, ALTERNATIVE, NULLBUTTON
    }


    private Tag tagWrite;

    private Tag tagRead;

    private Tag tagTwin;

    private ObjectProperty<BtnActionType> actionType = new SimpleObjectProperty<BtnActionType>();
    private ObjectProperty<Device> device = new SimpleObjectProperty<Device>();
    private StringProperty tagAddressInHex = new SimpleStringProperty();

    /**
     * ana bite yazilinca aynisi varsa bu bite de yazilir.
     */
    private StringProperty twinBitAddressInHex = new SimpleStringProperty();

    private StringProperty trueText = new SimpleStringProperty(new String(""));

    private StringProperty falseText = new SimpleStringProperty(new String(""));

    private BooleanProperty usesAnotherFeedBackAddress = new SimpleBooleanProperty();
    private StringProperty backAddressInHex = new SimpleStringProperty();


    private String writeWordIndex, backWordIndex;
    private String writeBitPositionInTheWord;
    private String fBackBitPositionInTheWord;

    public ObjectProperty<BtnActionType> actionTypeProperty() {
        return actionType;
    }

    public ObjectProperty<Device> deviceProperty() {
        return device;
    }

    public StringProperty tagAddressInHexProperty() {
        return tagAddressInHex;
    }

    public BtnActionType getActionType() {
        return actionType.get();
    }

    public void setActionType(BtnActionType actionType) {
        this.actionType.set(actionType);
    }

    public StringProperty backAddressInHexProperty() {
        return backAddressInHex;
    }

    public String getBackAddressInHex() {
        return backAddressInHex.get();
    }

    public void setBackAddressInHex(String backAddressInHex) {
        this.backAddressInHex.set(backAddressInHex);

    }

    public String getTwinBitAddressInHex() {
        return twinBitAddressInHex.get();
    }

    public StringProperty twinBitAddressInHexProperty() {
        return twinBitAddressInHex;
    }

    public void setTwinBitAddressInHex(String twinBitAddressInHex) {
        twinBitAddressInHex = twinBitAddressInHex.trim();
        this.twinBitAddressInHex.set(twinBitAddressInHex);

        if (!this.twinBitAddressInHex.get().equals("")) {


            tagTwin = new Tag(deviceProperty().get(), DataType.Bit, this.twinBitAddressInHex.get(), DisplayFormat.BINARY, null);


        }
    }

    public boolean isUsesAnotherFeedBackAddress() {
        return usesAnotherFeedBackAddress.get();
    }

    public BooleanProperty usesAnotherFeedBackAddressProperty() {
        return usesAnotherFeedBackAddress;
    }

    public void setUsesAnotherFeedBackAddress(boolean usesAnotherFeedBackAddress) {
        this.usesAnotherFeedBackAddress.set(usesAnotherFeedBackAddress);
        if (usesAnotherFeedBackAddress && backAddressInHex != null) {
            backWordIndex = backAddressInHex.get().substring(0, backAddressInHex.get().length() - 1);
            backWordIndex = backWordIndex.equals("") ? "0" : backWordIndex;
            fBackBitPositionInTheWord = String.valueOf(backAddressInHex.get().charAt(backAddressInHex.get().length() - 1));
            tagRead = new Tag(deviceProperty().get(), DataType.Word, backWordIndex, DisplayFormat.BINARY, null);

            addFbListener();

           // System.out.println("used another!" + this.getText());


            StringBuilder sb = new StringBuilder(getTooltip().getText());
            sb.append("\n");
            sb.append("Different read addr. :" + backWordIndex + "." + fBackBitPositionInTheWord);
            getTooltip().setText(sb.toString());


        }
    }

    public Device getDevice() {
        return device.get();
    }

    public void setDevice(Device device) {
        this.device.set(device);
    }

    public String getTagAddressInHex() {
        return tagAddressInHex.get();
    }


    public String getBackWordIndex() {
        return backWordIndex;
    }

    public void setBackWordIndex(String backWordIndex) {
        this.backWordIndex = backWordIndex;
    }

    public void setTagRead(Tag tagRead) {
        this.tagRead = tagRead;
        addFbListener();
    }

    public Tag getTagRead() {
        return tagRead;
    }

    public void setTagAddressInHex(String tagAddressInHex) {
        tagAddressInHex = tagAddressInHex.trim();
        this.tagAddressInHex.set(tagAddressInHex);
        writeBitPositionInTheWord = String.valueOf(tagAddressInHex.charAt(tagAddressInHex.length() - 1));
        writeWordIndex = tagAddressInHex.substring(0, tagAddressInHex.length() - 1);
        writeWordIndex = writeWordIndex.equals("") ? "0" : writeWordIndex;

        tagWrite = new Tag(deviceProperty().get(), DataType.Bit, tagAddressInHex, DisplayFormat.BINARY, null);
        tagRead = new Tag(deviceProperty().get(), DataType.Word, writeWordIndex, DisplayFormat.BINARY, null);


        addFbListener();


        StringBuilder sbToolTip = new StringBuilder();
        sbToolTip.append("Device: " + device.get());
        sbToolTip.append("\n");
        sbToolTip.append("Tag Address: " + tagAddressInHex);
        sbToolTip.append("\n");
        sbToolTip.append("at Word: " + writeWordIndex);
        sbToolTip.append("\n");
        sbToolTip.append("Bit pos. in word: " + writeBitPositionInTheWord);


        Tooltip tt = new Tooltip();
        tt.setText(sbToolTip.toString());
        setTooltip(tt);

    }

    public String getWriteWordIndex() {
        return writeWordIndex;
    }

    public String getWriteBitPositionInTheWord() {
        return writeBitPositionInTheWord;
    }


    public String getTrueText() {
        return trueText.get();
    }

    public StringProperty trueTextProperty() {
        return trueText;
    }

    public void setTrueText(String trueText) {
        this.trueText.set(trueText);
    }

    public String getFalseText() {
        return falseText.get();
    }

    public StringProperty falseTextProperty() {
        return falseText;
    }

    public void setFalseText(String falseText) {
        this.falseText.set(falseText);
    }


    public SimpleBooleanProperty readBitStatusProperty() {
        return readBitStatus;
    }


    public void bindToEnableDisable(ObservableValue<Boolean> booleanProperty) {
        disableProperty().bind(booleanProperty);
    }

    public TagWroteEvent getEventTagWrote() {
        return eventTagWrote;
    }

    private void released() {
        switch (actionType.get()) {

            case MOMENTARY:

                try {
                    t.join(); // pressed dan kaynakli yazma threadini bekle
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                write(this.tagWrite, false);

                break;


        }
    }

    private void pressed() {
        switch (actionType.get()) {

            case MOMENTARY:

                write(this.tagWrite, true);

                break;
            case ALTERNATIVE:
                boolean currentStatus = false;
                if (tagWrite.getValueAsHexString() != null) {
                    currentStatus = tagWrite.getValue().equals("1") ? true : false;
                }
                write(this.tagWrite, !currentStatus);

                break;


        }
    }

    public void resetBit() {
        write(this.tagWrite, false);
    }


    public void setTouchFunctions(boolean touchFunctions) {
        this.touchFunctions = touchFunctions;
    }

    public boolean isTouchFunctions() {
        return touchFunctions;
    }




    public LSButton() {



        if (touchFunctions) {

            setOnTouchPressed(e -> {
                pressed();
            });
            setOnTouchReleased(e -> {
                released();
            });

        } else {


            setOnMousePressed(e -> {

                pressed();

            });
            setOnMouseReleased(e -> {


                released();

            });


        }


    }

    private void write(Tag tag, boolean b) {
        t = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    XGBCNetClient.getInstance().writeBit(tag, b);
                    if (tagTwin != null) { // ne yaziliyorsa ikiz bit e de yaz
                        XGBCNetClient.getInstance().writeBit(tagTwin, b);
                    }

                    eventTagWrote.setData(String.valueOf(b));
                    fireEvent(eventTagWrote);


                } catch (SerialPortIOException | NoAcknowledgeMessageFromThePLCException | NoResponseException |
                         FrameCheckException e) {
                    e.printStackTrace();
                }


            }
        });

        t.start();


    }

    public void addFbListener() {


        tagRead.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {


                Platform.runLater(() -> {
                    String bitPos = usesAnotherFeedBackAddress.get() ? fBackBitPositionInTheWord : writeBitPositionInTheWord;

                    readBitStatus.set(XGBCNetUtil.checkBit(newValue, Integer.parseInt(bitPos, 16)));

                    //  xxx(getText() + "  = " + status);
                    if (readBitStatus.get()) {
                        setBackground(greenBackGround);
                        if (!trueTextProperty().get().trim().equals("")) {
                            setText(trueTextProperty().get());
                        }
                        tagWrite.setValueAsHexString("1"); // to match at the first read!
                    } else {
                        setBackground(normalBackGround);
                        if (!falseTextProperty().get().trim().equals("")) {
                            setText(falseTextProperty().get());
                        }
                        tagWrite.setValueAsHexString("0");// to match at the first read!
                    }
                });
            }


        });

    }


}
