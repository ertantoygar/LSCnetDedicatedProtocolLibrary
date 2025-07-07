package tr.com.logidex.cnetdedicated.fxcontrols;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;
import java.util.logging.Logger;
/**
 * Dokunmatik ekranlar için optimize edilmiş QWERTY klavye sınıfı.
 * Singleton tasarım deseni ile uygulanmıştır, projenin her yerinde tek bir örnek kullanılır.
 */
public class TouchKeyboard {
    private static final Logger LOGGER = Logger.getLogger(TouchKeyboard.class.getName());
    // Singleton örneği
    private static TouchKeyboard instance;
    // Dialog penceresi
    private Dialog<String> keyboardDialog;
    // Metin alanı
    private TextField inputField;
    // Tamamlandığında çağrılacak callback
    private Consumer<String> onValueConfirmed;
    // Maksimum karakter sayısı kontrolü
    private int maxLength = Integer.MAX_VALUE;
    // Caps Lock durumu
    private boolean capsLockEnabled = false;
    // Shift durumu (geçici büyük harf)
    private boolean shiftPressed = false;
    // Shift butonu referansı
    private Button leftShiftButton;
    private Button rightShiftButton;
    private Button capsLockButton;


    /**
     * Private constructor - singleton desenini uygulamak için
     */
    private TouchKeyboard() {
        // DialogPane, ilk kullanımda oluşturulacak
    }


    /**
     * Singleton örneğini alır, gerekirse oluşturur
     *
     * @return TouchKeyboard singleton örneği
     */
    public static synchronized TouchKeyboard getInstance() {
        if (instance == null) {
            instance = new TouchKeyboard();
        }
        return instance;
    }


    /**
     * Klavye içeriğini oluşturur
     */
    private Dialog<String> createKeyboardDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Metin Giriş Klavyesi");
        // Dialog'un her zaman en üstte görünmesini sağla
        dialog.setOnShowing(event -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            stage.toFront();
        });
        // Ana container
        VBox mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(15));
        mainContainer.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #666666; -fx-border-width: 2px;");
        // Üst kısım - metin alanı ve bilgi
        VBox topSection = createTopSection();
        // Klavye kısmı
        VBox keyboardSection = createKeyboardSection(dialog);
        // Alt kısım - kontrol butonları
        HBox bottomSection = createBottomSection(dialog);
        mainContainer.getChildren().addAll(topSection, keyboardSection, bottomSection);
        dialog.getDialogPane().setContent(mainContainer);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        // Kapatma düğmesini gizle
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        // Stil sınıfı ekle
        dialog.getDialogPane().getStyleClass().add("keyboard-dialog");
        return dialog;
    }


    /**
     * Üst kısmı (metin alanı ve bilgi) oluşturur
     */
    private VBox createTopSection() {
        VBox topSection = new VBox(5);
        // Metin alanı
        inputField = new TextField();
        inputField.setStyle("-fx-font-size: 18px; -fx-padding: 8px;");
        inputField.getStyleClass().add("keyboard-input");
        // Karakter sayısı bilgisi
        Label charCountLabel = new Label();
        if (maxLength != Integer.MAX_VALUE) {
            charCountLabel.setText("Maksimum karakter sayısı: " + maxLength);
        } else {
            charCountLabel.setText("Karakter sınırı yok");
        }
        charCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #555555;");
        charCountLabel.setAlignment(Pos.CENTER);
        topSection.getChildren().addAll(inputField, charCountLabel);
        return topSection;
    }


    /**
     * Klavye kısmını oluşturur
     */
    private VBox createKeyboardSection(Dialog<String> dialog) {
        VBox keyboardSection = new VBox(5);
        // Sayı satırı
        HBox numberRow = createNumberRow(dialog);
        // QWERTY satırları
        HBox qwertyRow = createQwertyRow(dialog);
        HBox asdfRow = createAsdfRow(dialog);
        HBox zxcvRow = createZxcvRow(dialog);
        // Boşluk satırı
        HBox spaceRow = createSpaceRow(dialog);
        keyboardSection.getChildren().addAll(numberRow, qwertyRow, asdfRow, zxcvRow, spaceRow);
        return keyboardSection;
    }


    /**
     * Sayı satırını oluşturur
     */
    private HBox createNumberRow(Dialog<String> dialog) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER);
        String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        String[] symbols = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"};
        for (int i = 0; i < numbers.length; i++) {
            Button button = createKeyButton(numbers[i], symbols[i], dialog, false);
            row.getChildren().add(button);
        }
        // Backspace butonu
        Button backspaceButton = createFunctionButton("←", dialog);
        backspaceButton.setStyle("-fx-base: #2196F3; -fx-min-width: 60px;");
        row.getChildren().add(backspaceButton);
        return row;
    }


    /**
     * QWERTY satırını oluşturur
     */
    private HBox createQwertyRow(Dialog<String> dialog) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER);
        // Tab butonu
        Button tabButton = createFunctionButton("Tab", dialog);
        tabButton.setStyle("-fx-base: #9E9E9E; -fx-min-width: 50px;");
        row.getChildren().add(tabButton);
        String[] letters = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
        for (String letter : letters) {
            Button button = createKeyButton(letter, "", dialog, true);
            row.getChildren().add(button);
        }
        // Özel karakterler
        Button leftBracket = createKeyButton("[", "{", dialog, false);
        Button rightBracket = createKeyButton("]", "}", dialog, false);
        Button backslash = createKeyButton("\\", "|", dialog, false);
        row.getChildren().addAll(leftBracket, rightBracket, backslash);
        return row;
    }


    /**
     * ASDF satırını oluşturur
     */
    private HBox createAsdfRow(Dialog<String> dialog) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER);
        // Caps Lock butonu
        capsLockButton = createFunctionButton("Caps", dialog);
        capsLockButton.setStyle("-fx-base: #9E9E9E; -fx-min-width: 60px;");
        row.getChildren().add(capsLockButton);
        String[] letters = {"a", "s", "d", "f", "g", "h", "j", "k", "l"};
        for (String letter : letters) {
            Button button = createKeyButton(letter, "", dialog, true);
            row.getChildren().add(button);
        }
        // Özel karakterler
        Button semicolon = createKeyButton(";", ":", dialog, false);
        Button quote = createKeyButton("'", "\"", dialog, false);
        // Enter butonu
        Button enterButton = createFunctionButton("Enter", dialog);
        enterButton.setStyle("-fx-base: #4CAF50; -fx-min-width: 70px;");
        row.getChildren().addAll(semicolon, quote, enterButton);
        return row;
    }


    /**
     * ZXCV satırını oluşturur
     */
    private HBox createZxcvRow(Dialog<String> dialog) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER);
        // Sol Shift butonu
        leftShiftButton = createFunctionButton("Shift", dialog);
        leftShiftButton.setStyle("-fx-base: #9E9E9E; -fx-min-width: 70px;");
        row.getChildren().add(leftShiftButton);
        String[] letters = {"z", "x", "c", "v", "b", "n", "m"};
        for (String letter : letters) {
            Button button = createKeyButton(letter, "", dialog, true);
            row.getChildren().add(button);
        }
        // Özel karakterler
        Button comma = createKeyButton(",", "<", dialog, false);
        Button period = createKeyButton(".", ">", dialog, false);
        Button slash = createKeyButton("/", "?", dialog, false);
        // Sağ Shift butonu
        rightShiftButton = createFunctionButton("Shift", dialog);
        rightShiftButton.setStyle("-fx-base: #9E9E9E; -fx-min-width: 70px;");
        row.getChildren().addAll(comma, period, slash, rightShiftButton);
        return row;
    }


    /**
     * Boşluk satırını oluşturur
     */
    private HBox createSpaceRow(Dialog<String> dialog) {
        HBox row = new HBox(3);
        row.setAlignment(Pos.CENTER);
        // Ctrl butonu
        Button ctrlButton = createFunctionButton("Ctrl", dialog);
        ctrlButton.setStyle("-fx-base: #9E9E9E; -fx-min-width: 50px;");
        // Alt butonu
        Button altButton = createFunctionButton("Alt", dialog);
        altButton.setStyle("-fx-base: #9E9E9E; -fx-min-width: 50px;");
        // Boşluk butonu
        Button spaceButton = createFunctionButton(" ", dialog);
        spaceButton.setText("Space");
        spaceButton.setStyle("-fx-base: #E0E0E0; -fx-min-width: 200px;");
        row.getChildren().addAll(ctrlButton, altButton, spaceButton);
        return row;
    }


    /**
     * Alt kısmı (kontrol butonları) oluşturur
     */
    private HBox createBottomSection(Dialog<String> dialog) {
        HBox bottomSection = new HBox(10);
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.setPadding(new Insets(10, 0, 0, 0));
        // Temizle butonu
        Button clearButton = new Button("Temizle");
        clearButton.setStyle("-fx-base: #FF9800; -fx-min-width: 80px; -fx-min-height: 40px;");
        clearButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        clearButton.setOnAction(e -> inputField.clear());
        // Onay butonu
        Button confirmButton = new Button("Onayla");
        confirmButton.setStyle("-fx-base: #4CAF50; -fx-min-width: 80px; -fx-min-height: 40px;");
        confirmButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        confirmButton.setOnAction(e -> handleConfirmButton(dialog));
        // İptal butonu
        Button cancelButton = new Button("İptal");
        cancelButton.setStyle("-fx-base: #F44336; -fx-min-width: 80px; -fx-min-height: 40px;");
        cancelButton.setFont(Font.font("System", FontWeight.BOLD, 14));
        cancelButton.setOnAction(e -> closeKeyboard(dialog, null));
        bottomSection.getChildren().addAll(clearButton, confirmButton, cancelButton);
        return bottomSection;
    }


    /**
     * Normal tuş butonu oluşturur
     */
    private Button createKeyButton(String normalChar, String shiftChar, Dialog<String> dialog, boolean isLetter) {
        Button button = new Button(normalChar);
        button.setMinSize(35, 35);
        button.setFont(Font.font("System", FontWeight.NORMAL, 14));
        button.setStyle("-fx-base: #E0E0E0;");
        button.setOnAction(e -> handleKeyPress(normalChar, shiftChar, isLetter));
        return button;
    }


    /**
     * Fonksiyon tuşu butonu oluşturur
     */
    private Button createFunctionButton(String label, Dialog<String> dialog) {
        Button button = new Button(label);
        button.setMinSize(35, 35);
        button.setFont(Font.font("System", FontWeight.BOLD, 12));
        button.setOnAction(e -> handleFunctionKey(label, dialog));
        return button;
    }


    /**
     * Normal tuşa basıldığında yapılacak işlemleri yönetir
     */
    private void handleKeyPress(String normalChar, String shiftChar, boolean isLetter) {
        if (inputField.getText().length() >= maxLength) {
            showInputError("Maksimum karakter sınırına ulaşıldı!");
            return;
        }
        String charToAdd;
        if (isLetter) {
            // Harf tuşları için caps lock ve shift kontrolü
            boolean shouldCapitalize = capsLockEnabled ^ shiftPressed;
            charToAdd = shouldCapitalize ? normalChar.toUpperCase() : normalChar.toLowerCase();
        } else {
            // Diğer karakterler için sadece shift kontrolü
            charToAdd = (shiftPressed && !shiftChar.isEmpty()) ? shiftChar : normalChar;
        }
        inputField.appendText(charToAdd);
        // Shift tek kullanımlık, basıldıktan sonra sıfırlanır
        if (shiftPressed) {
            shiftPressed = false;
            updateShiftButtonStyles();
        }
    }


    /**
     * Fonksiyon tuşlarını yönetir
     */
    private void handleFunctionKey(String key, Dialog<String> dialog) {
        switch (key) {
            case "←" -> handleBackspace();
            case "Tab" -> inputField.appendText("\t");
            case "Caps" -> handleCapsLock();
            case "Shift" -> handleShift();
            case "Enter" -> inputField.appendText("\n");
            case " " -> inputField.appendText(" ");
            case "Ctrl", "Alt" -> {
                // Bu tuşlar şimdilik işlevsiz, gelecekte kısayol kombinasyonları için kullanılabilir
            }
        }
    }


    /**
     * Backspace işlemini yönetir
     */
    private void handleBackspace() {
        String text = inputField.getText();
        if (text.length() > 0) {
            inputField.setText(text.substring(0, text.length() - 1));
        }
    }


    /**
     * Caps Lock işlemini yönetir
     */
    private void handleCapsLock() {
        capsLockEnabled = !capsLockEnabled;
        updateCapsLockButtonStyle();
    }


    /**
     * Shift işlemini yönetir
     */
    private void handleShift() {
        shiftPressed = !shiftPressed;
        updateShiftButtonStyles();
    }


    /**
     * Caps Lock buton stilini günceller
     */
    private void updateCapsLockButtonStyle() {
        if (capsLockButton != null) {
            if (capsLockEnabled) {
                capsLockButton.setStyle("-fx-base: #FFC107; -fx-min-width: 60px;"); // Sarı - aktif
            } else {
                capsLockButton.setStyle("-fx-base: #9E9E9E; -fx-min-width: 60px;"); // Gri - pasif
            }
        }
    }


    /**
     * Shift butonları stilini günceller
     */
    private void updateShiftButtonStyles() {
        String activeStyle = "-fx-base: #FFC107; -fx-min-width: 70px;"; // Sarı - aktif
        String inactiveStyle = "-fx-base: #9E9E9E; -fx-min-width: 70px;"; // Gri - pasif
        if (leftShiftButton != null) {
            leftShiftButton.setStyle(shiftPressed ? activeStyle : inactiveStyle);
        }
        if (rightShiftButton != null) {
            rightShiftButton.setStyle(shiftPressed ? activeStyle : inactiveStyle);
        }
    }


    /**
     * Onay tuşu işlemini yönetir
     */
    private void handleConfirmButton(Dialog<String> dialog) {
        String text = inputField.getText();
        closeKeyboard(dialog, text);
    }


    /**
     * Giriş hatası gösterir
     */
    private void showInputError(String errorMessage) {
        inputField.setStyle("-fx-background-color: #FFCDD2; -fx-font-size: 18px; -fx-padding: 8px;");
        // Tooltip ile hata mesajı göster
        Tooltip tooltip = new Tooltip(errorMessage);
        tooltip.setStyle("-fx-font-size: 14px;");
        inputField.setTooltip(tooltip);
        tooltip.setAutoHide(true);
        // 2 saniye sonra normal stile geri dön
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    inputField.setStyle("-fx-font-size: 18px; -fx-padding: 8px;");
                    inputField.setTooltip(null);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }


    /**
     * Klavyeyi kapatır
     */
    private void closeKeyboard(Dialog<String> dialog, String result) {
        if (result != null && onValueConfirmed != null) {
            onValueConfirmed.accept(result);
        }
        // Durumları sıfırla
        capsLockEnabled = false;
        shiftPressed = false;
        dialog.close();
    }


    /**
     * Metin klavyesini gösterir
     *
     * @param initialValue  Başlangıç değeri
     * @param maxLength     Maksimum karakter sayısı (Integer.MAX_VALUE = sınırsız)
     * @param event         UI olayı (konumlandırma için)
     * @param sourceControl Olayı tetikleyen kontrol
     * @param callback      Değer onaylandığında çağrılacak callback
     */
    public void show(String initialValue, int maxLength, MouseEvent event,
                     Control sourceControl, Consumer<String> callback) {
        // Parametreleri ayarla
        this.maxLength = maxLength;
        this.onValueConfirmed = callback;
        // Her kullanımda yeni Dialog nesnesi oluştur
        Dialog<String> dialog = createKeyboardDialog();
        // Başlangıç değerini ayarla
        inputField.setText(initialValue != null ? initialValue : "");
        inputField.setStyle("-fx-font-size: 18px; -fx-padding: 8px;");
        inputField.selectAll();
        // Dialog penceresini doğru konuma yerleştir
        positionKeyboard(dialog, event, sourceControl);
        // Klavyeyi göster
        dialog.showAndWait();
    }


    /**
     * Basitleştirilmiş show metodu - maksimum karakter sınırı olmadan
     */
    public void show(String initialValue, MouseEvent event, Control sourceControl, Consumer<String> callback) {
        show(initialValue, Integer.MAX_VALUE, event, sourceControl, callback);
    }


    /**
     * Klavyeyi uygun konuma yerleştirir
     */
    private void positionKeyboard(Dialog<String> dialog, MouseEvent event, Control sourceControl) {
        dialog.setOnShown(showEvent -> {
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            if (dialogStage == null) return;
            // Ekran boyutlarını al
            Rectangle2D screenBounds = TouchInputUtil.getScreenForNode(sourceControl).getVisualBounds();
            // Dialog boyutları
            double dialogWidth = dialogStage.getWidth();
            double dialogHeight = dialogStage.getHeight();
            double x, y;
            if (event != null && sourceControl != null && sourceControl.getScene() != null) {
                // Olayın koordinatlarını al
                Scene scene = sourceControl.getScene();
                Stage sourceStage = (Stage) scene.getWindow();
                double windowX = sourceStage.getX();
                double windowY = sourceStage.getY();
                double sceneX = event.getSceneX();
                double sceneY = event.getSceneY();
                x = screenBounds.getMinX() + 20;
                y = screenBounds.getMinY() + 20;
            } else if (sourceControl != null && sourceControl.getScene() != null) {
                Scene scene = sourceControl.getScene();
                Stage sourceStage = (Stage) scene.getWindow();
                double controlX = sourceControl.localToScene(0, 0).getX();
                double controlY = sourceControl.localToScene(0, 0).getY();
                double windowX = sourceStage.getX();
                double windowY = sourceStage.getY();
                x = screenBounds.getMinX() + 20;
                y = screenBounds.getMinY() + 20;
            } else {
                // Ekranın ortasına yerleştir
                x = screenBounds.getMinX() + (screenBounds.getWidth() - dialogWidth) / 2;
                y = screenBounds.getMinY() + (screenBounds.getHeight() - dialogHeight) / 2;
            }
            // Ekrandan taşmayı önle
            if (x < screenBounds.getMinX()) x = screenBounds.getMinX();
            if (y < screenBounds.getMinY()) y = screenBounds.getMinY();
            if (x + dialogWidth > screenBounds.getMaxX()) x = screenBounds.getMaxX() - dialogWidth;
            if (y + dialogHeight > screenBounds.getMaxY()) y = screenBounds.getMaxY() - dialogHeight;
            dialogStage.setX(x);
            dialogStage.setY(y);
        });
    }
}