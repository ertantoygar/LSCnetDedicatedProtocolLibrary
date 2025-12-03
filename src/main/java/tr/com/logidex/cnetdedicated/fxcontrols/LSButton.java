package tr.com.logidex.cnetdedicated.fxcontrols;
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
    private final Lock lock = new ReentrantLock();
    Border border = new Border(new BorderStroke(Color.GRAY, BorderStrokeStyle.SOLID, new CornerRadii(10), BorderWidths.DEFAULT));
    Background greenBackGround = new Background(new BackgroundFill(Color.PALEGREEN, new CornerRadii(10), javafx.geometry.Insets.EMPTY));
    Background normalBackGround = new Background(new BackgroundFill(Color.rgb(192, 205, 220), new CornerRadii(10), javafx.geometry.Insets.EMPTY));
    private Thread t;
    private SimpleBooleanProperty readBitStatus = new SimpleBooleanProperty();
    /**
     * Represents an event that is triggered when a tag is written to.
     * This event is associated with a {@link TagWroteEvent} object.
     */
    private TagWroteEvent eventTagWrote = new TagWroteEvent();
    private Tag tagWrite;
    private Tag tagRead;
    private Tag tagTwin;
    private ObjectProperty<BtnActionType> actionType = new SimpleObjectProperty<BtnActionType>();
    private ObjectProperty<Device> device = new SimpleObjectProperty<Device>();
    private StringProperty tagAddressInHex = new SimpleStringProperty();
    private StringProperty name = new SimpleStringProperty();
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
    private boolean touchHandled = false;

    public LSButton() {
        // Touch event handlers - will fire on touch devices
        setOnTouchPressed(e -> {
            touchHandled = true;
            pressed();
            e.consume(); // Consume to prevent mouse event synthesis
        });
        setOnTouchReleased(e -> {
            released();
            touchHandled = false;
            e.consume(); // Consume to prevent mouse event synthesis
        });

        // Mouse event handlers - will fire on mouse devices or from synthesized touch events
        setOnMousePressed(e -> {
            // Handle if: real mouse click OR synthesized from touch (when touch handlers don't fire)
            if (!touchHandled) {
                pressed();
            }
        });
        setOnMouseReleased(e -> {
            // Handle if: real mouse click OR synthesized from touch (when touch handlers don't fire)
            if (!touchHandled) {
                released();
            }
        });
    }


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


    public ObjectProperty<BtnActionType> actionTypeProperty() {
        return actionType;
    }


    public ObjectProperty<Device> deviceProperty() {
        return device;
    }


    public StringProperty tagAddressInHexProperty() {
        return tagAddressInHex;
    }


    public StringProperty nameProperty() {
        return name;
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


    public void setTwinBitAddressInHex(String twinBitAddressInHex) {
        twinBitAddressInHex = twinBitAddressInHex.trim();
        this.twinBitAddressInHex.set(twinBitAddressInHex);
        if (!this.twinBitAddressInHex.get().equals("")) {
            tagTwin = new Tag(nameProperty().get(), deviceProperty().get(), DataType.Bit, this.twinBitAddressInHex.get(), DisplayFormat.BINARY, null);
        }
    }


    public StringProperty twinBitAddressInHexProperty() {
        return twinBitAddressInHex;
    }


    public boolean isUsesAnotherFeedBackAddress() {
        return usesAnotherFeedBackAddress.get();
    }


    public void setUsesAnotherFeedBackAddress(boolean usesAnotherFeedBackAddress) {
        this.usesAnotherFeedBackAddress.set(usesAnotherFeedBackAddress);
        if (usesAnotherFeedBackAddress && backAddressInHex != null) {
            backWordIndex = backAddressInHex.get().substring(0, backAddressInHex.get().length() - 1);
            backWordIndex = backWordIndex.equals("") ? "0" : backWordIndex;
            fBackBitPositionInTheWord = String.valueOf(backAddressInHex.get().charAt(backAddressInHex.get().length() - 1));
            tagRead = new Tag(nameProperty().get(), deviceProperty().get(), DataType.Word, backWordIndex, DisplayFormat.BINARY, null);
            addFbListener();
            // System.out.println("used another!" + this.getText());
            StringBuilder sb = new StringBuilder(getTooltip().getText());
            sb.append("\n");
            sb.append("Different read addr. :" + backWordIndex + "." + fBackBitPositionInTheWord);
            getTooltip().setText(sb.toString());
        }
    }


    public BooleanProperty usesAnotherFeedBackAddressProperty() {
        return usesAnotherFeedBackAddress;
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


    public void setTagAddressInHex(String tagAddressInHex) {
        tagAddressInHex = tagAddressInHex.trim();
        this.tagAddressInHex.set(tagAddressInHex);
        writeBitPositionInTheWord = String.valueOf(tagAddressInHex.charAt(tagAddressInHex.length() - 1));
        writeWordIndex = tagAddressInHex.substring(0, tagAddressInHex.length() - 1);
        writeWordIndex = writeWordIndex.equals("") ? "0" : writeWordIndex;
        tagWrite = new Tag(nameProperty().get(), deviceProperty().get(), DataType.Bit, tagAddressInHex, DisplayFormat.BINARY, null);
        tagRead = new Tag(nameProperty().get(), deviceProperty().get(), DataType.Word, writeWordIndex, DisplayFormat.BINARY, null);
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


    public String getName() {
        return name.get();
    }


    public void setName(String name) {
        this.name.set(name);
    }


    public String getBackWordIndex() {
        return backWordIndex;
    }


    public void setBackWordIndex(String backWordIndex) {
        this.backWordIndex = backWordIndex;
    }


    public Tag getTagRead() {
        return tagRead;
    }


    public void setTagRead(Tag tagRead) {
        this.tagRead = tagRead;
        addFbListener();
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


    public void setTrueText(String trueText) {
        this.trueText.set(trueText);
    }


    public StringProperty trueTextProperty() {
        return trueText;
    }


    public String getFalseText() {
        return falseText.get();
    }


    public void setFalseText(String falseText) {
        this.falseText.set(falseText);
    }


    public StringProperty falseTextProperty() {
        return falseText;
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
                } catch (IOException | NoAcknowledgeMessageFromThePLCException | NoResponseException |
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
                    readBitStatus.set(XGBCNetUtil.checkBit16(newValue, Integer.parseInt(bitPos, 16)));
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


    public enum BtnActionType {
        ON, OFF, MOMENTARY, ALTERNATIVE, NULLBUTTON
    }
}
