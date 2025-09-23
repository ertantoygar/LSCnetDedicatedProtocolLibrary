package tr.com.logidex.cnetdedicated.fxcontrols;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;

import java.util.function.Consumer;
import java.util.logging.Logger;
/**
 * Dokunmatik ekranlar için optimize edilmiş sayısal klavye sınıfı.
 * Singleton tasarım deseni ile uygulanmıştır, projenin her yerinde tek bir örnek kullanılır.
 */
public class TouchNumericKeypad {
    private static final Logger LOGGER = Logger.getLogger(TouchNumericKeypad.class.getName());
    // Singleton örneği
    private static TouchNumericKeypad instance;
    // Dialog penceresi
    private Dialog<String> keypadDialog;
    // Metin alanı
    private TextField inputField;
    // Tamamlandığında çağrılacak callback
    private Consumer<String> onValueConfirmed;
    // Değer aralığı kontrolü için
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;
    // Sadece tamsayı girişi kontrolü
    private boolean integerOnly = false;
    private boolean allSelected;

    /**
     * Private constructor - singleton desenini uygulamak için
     */
    private TouchNumericKeypad() {


    }


    /**
     * Singleton örneğini alır, gerekirse oluşturur
     *
     * @return TouchNumericKeypad singleton örneği
     */
    public static synchronized TouchNumericKeypad getInstance() {
        if (instance == null) {
            instance = new TouchNumericKeypad();
        }
        return instance;
    }


    /**
     * Klavye içeriğini oluşturur
     */
    private Dialog<String> createKeypadDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Sayı Giriş Klavyesi");
        // Dialog'un her zaman en üstte görünmesini sağla
        dialog.setOnShowing(event -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            stage.toFront();
        });
        // Ekran klavyesi içeriği
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #666666; -fx-border-width: 2px;");
        // Metin alanı
        inputField = new TextField();
        inputField.setStyle("-fx-font-size: 24px; -fx-padding: 10px;");
        inputField.getStyleClass().add("keypad-input");


      /// ///
// XGBCNetClient.touchScreen.get() kontrolü ile hybrid yaklaşım
        if (XGBCNetClient.touchScreen.get()) {
            // Try touch first
            inputField.setOnTouchPressed(event -> {
                if(allSelected) {
                    inputField.deselect();
                    allSelected = false;
                }
                event.consume(); // Prevent synthesis
            });

            // Fallback to synthesized mouse events
            inputField.setOnMouseClicked(event -> {
                if (event.isSynthesized()) {
                    if(allSelected) {
                        inputField.deselect();
                        allSelected = false;
                    }
                }
            });
        } else {
            // Mouse-only environment
            inputField.setOnMouseClicked(event -> {
                if(allSelected) {
                    inputField.deselect();
                    allSelected = false;
                }
            });
        }

/// //

        grid.add(inputField, 0, 0, 4, 1);
        // Min-Max Değerlerini gösteren etiket
        Label minMaxLabel = new Label(String.format("İzin verilen aralık: %.2f - %.2f", minValue, maxValue));
        minMaxLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555; -fx-font-weight: bold;");
        minMaxLabel.setAlignment(Pos.CENTER);
        minMaxLabel.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(minMaxLabel, Priority.ALWAYS);
        grid.add(minMaxLabel, 0, 1, 4, 1);
        // Numara butonları
        String[][] buttonLabels = {
                {"7", "8", "9", "←"},
                {"4", "5", "6", "-"},
                {"1", "2", "3", "C"},
                {"0", ".", "±", "✓"}
        };
        for (int row = 0; row < buttonLabels.length; row++) {
            for (int col = 0; col < buttonLabels[row].length; col++) {
                Button button = createButton(buttonLabels[row][col], dialog);
                grid.add(button, col, row + 2); // Button'ları bir satır aşağı kaydır (min-max etiketinden sonra)
            }
        }
        // İptal butonu
        Button cancelButton = new Button("İptal");
        cancelButton.getStyleClass().add("cancel-button");
        cancelButton.setStyle("-fx-base: #FF9800;");
        cancelButton.setMinSize(350, 60);
        cancelButton.setFont(Font.font("System", FontWeight.BOLD, 18));
//        cancelButton.setOnAction(e -> closeKeypad(dialog, null));
        if (XGBCNetClient.touchScreen.get()) {
            cancelButton.setOnTouchPressed(event -> {
                closeKeypad(dialog, null);
                event.consume();
            });

            cancelButton.setOnMouseClicked(event -> {
                if (event.isSynthesized()) {
                    closeKeypad(dialog, null);
                }
            });
        } else {
            cancelButton.setOnAction(e -> closeKeypad(dialog, null));
        }

        grid.add(cancelButton, 0, 6, 4, 1); // Satır numarasını bir arttır
        GridPane.setMargin(cancelButton, new Insets(10, 0, 0, 0));
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        // Kapatma düğmesini gizle
        Button closeButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        closeButton.setVisible(false);
        // Stil sınıfı ekle
        dialog.getDialogPane().getStyleClass().add("keypad-dialog");
        return dialog;
    }


    /**
     * Klavye tuşunu oluşturur
     *
     * @param label  Tuş etiketi
     * @param dialog İlgili dialog
     * @return Oluşturulan buton
     */
    private Button createButton(String label, Dialog<String> dialog) {
        Button button = new Button(label);
        button.setMinSize(80, 80);
        button.setFont(Font.font("System", FontWeight.BOLD, 20));
        // Stil sınıfı
        button.getStyleClass().add("function-button");
        // Tuş özel stilleri
        switch (label) {
            case "✓" -> {
                button.getStyleClass().add("confirm-button");
                button.setStyle("-fx-base: #4CAF50;"); // Yeşil onay butonu
            }
            case "C" -> {
                button.getStyleClass().add("clear-button");
                button.setStyle("-fx-base: #F44336;"); // Kırmızı temizleme butonu
            }
            case "←" -> {
                button.getStyleClass().add("backspace-button");
                button.setStyle("-fx-base: #2196F3;"); // Mavi silme butonu
            }
            default -> {
                button.getStyleClass().add("number-button");
                button.setStyle("-fx-base: #E0E0E0;"); // Gri sayısal tuşlar
            }
        }
        // Tuş işlevleri
        setupButtonHandler(button, label, dialog);
        return button;
    }

    private void setupButtonHandler(Button button, String label, Dialog<String> dialog) {
        if (XGBCNetClient.touchScreen.get()) {
            button.setOnTouchPressed(event -> {
                handleButtonPress(label, dialog);
                event.consume();
            });

            button.setOnMouseClicked(event -> {
                if (event.isSynthesized()) {
                    handleButtonPress(label, dialog);
                }
            });
        } else {
            button.setOnAction(e -> handleButtonPress(label, dialog));
        }
    }


    /**
     * Tuşa basıldığında yapılacak işlemleri yönetir
     *
     * @param buttonText Basılan tuşun metni
     * @param dialog     İlgili dialog
     */
    private void handleButtonPress(String buttonText, Dialog<String> dialog) {

       if(allSelected){
           inputField.clear();
           inputField.requestFocus();
           allSelected = false;
       }

        switch (buttonText) {
            case "✓" -> handleConfirmButton(dialog);
            case "C" -> inputField.clear(); // Temizleme tuşu
            case "←" -> handleBackspaceButton(); // Geri silme tuşu
            case "±" -> handleSignButton(); // Artı/eksi tuşu
            case "." -> handleDecimalButton(); // Ondalık nokta tuşu
            default -> inputField.appendText(buttonText); // Sayı tuşları
        }
    }


    /**
     * Onay tuşu işlemini yönetir
     */
    private void handleConfirmButton(Dialog<String> dialog) {
        try {
            String text = inputField.getText();
            if (text.isEmpty()) {
                closeKeypad(dialog, null);
                return;
            }
            // Değeri doğrula
            double value = Double.parseDouble(text);
            // Aralık kontrolü
            if (value < minValue || value > maxValue) {
                // Daha belirgin ve kullanıcı dostu hata mesajı
                String errorMessage = String.format("Değer izin verilen aralıkta değil!\nLütfen %.2f ile %.2f arasında bir değer girin.",
                        minValue, maxValue);
                showInputError(errorMessage);
                return;
            }
            // Tamsayı kontrolü
            if (integerOnly && value != Math.floor(value)) {
                showInputError("Lütfen tam sayı girin.");
                return;
            }
            // Tüm kontroller geçildi, klavyeyi kapat ve değeri geri döndür
            closeKeypad(dialog, text);
        } catch (NumberFormatException ex) {
            showInputError("Geçersiz sayı formatı.");
        }
    }


    /**
     * Geri silme tuşu işlemini yönetir
     */
    private void handleBackspaceButton() {
        String text = inputField.getText();
        if (text.length() > 0) {
            inputField.setText(text.substring(0, text.length() - 1));
        }
    }


    /**
     * İşaret değiştirme tuşu işlemini yönetir
     */
    private void handleSignButton() {
        String text = inputField.getText();
        if (text.startsWith("-")) {
            inputField.setText(text.substring(1));
        } else if (!text.isEmpty()) {
            inputField.setText("-" + text);
        }
    }


    /**
     * Ondalık nokta tuşu işlemini yönetir
     */
    private void handleDecimalButton() {
        // Tamsayı modunda ondalık noktaya izin verme
        if (integerOnly) {
            return;
        }
        String text = inputField.getText();
        // Zaten nokta içeriyorsa yeni nokta ekleme
        if (!text.contains(".")) {
            inputField.appendText(".");
        }
    }


    /**
     * Giriş hatası gösterir
     *
     * @param errorMessage Hata mesajı
     */
    private void showInputError(String errorMessage) {
        inputField.setStyle("-fx-background-color: #FFCDD2; -fx-font-size: 24px; -fx-padding: 10px;");
        // Tooltip ile hata mesajı göster
        Tooltip tooltip = new Tooltip(errorMessage);
        tooltip.setStyle("-fx-font-size: 16px;");
        inputField.setTooltip(tooltip);
        tooltip.setAutoHide(true);
        // 2 saniye sonra normal stile geri dön
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    inputField.setStyle("-fx-font-size: 24px; -fx-padding: 10px;");
                    inputField.setTooltip(null);
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }


    /**
     * Klavyeyi kapatır
     *
     * @param dialog Kapatılacak dialog
     * @param result Klavyeden dönen sonuç (onaylandığında giriş metni, iptal edildiğinde null)
     */
    private void closeKeypad(Dialog<String> dialog, String result) {
        if (result != null && onValueConfirmed != null) {
            onValueConfirmed.accept(result);
        }
        inputField.setOnMouseClicked(null);
        inputField.setOnTouchPressed(null);
        dialog.close();
    }


    /**
     * Sayısal klavyeyi gösterir
     *
     * @param initialValue  Başlangıç değeri
     * @param minValue      Minimum izin verilen değer
     * @param maxValue      Maksimum izin verilen değer
     * @param integerOnly   Sadece tamsayı girişine izin verilip verilmeyeceği
     * @param sourceControl Olayı tetikleyen kontrol
     * @param callback      Değer onaylandığında çağrılacak callback
     */
    public void show(String initialValue, double minValue, double maxValue, boolean integerOnly,
                    Control sourceControl, Consumer<String> callback ) {
        // Parametreleri ayarla
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.integerOnly = integerOnly;
        this.onValueConfirmed = callback;
        // Her kullanımda yeni Dialog nesnesi oluştur (kritik değişiklik!)
        Dialog<String> dialog = createKeypadDialog();
        // Başlangıç değerini ayarla
        inputField.setText(initialValue);
        inputField.setStyle("-fx-font-size: 24px; -fx-padding: 10px;");
        inputField.selectAll();
        allSelected = true;
        // Dialog penceresini doğru konuma yerleştir
        positionKeypad(dialog, sourceControl);
        // Klavyeyi göster
        dialog.showAndWait();
    }


    /**
     * Klavyeyi uygun konuma yerleştirir
     *
     * @param dialog        Dialog nesnesi
     * @param sourceControl Olayı tetikleyen kontrol
     */
    private void positionKeypad(Dialog<String> dialog, Control sourceControl) {
        // Dialog'un yerleştirilmesi, Dialog gösterilmeden önce yapılır
        dialog.setOnShown(showEvent -> {
            Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
            if (dialogStage == null) return;
            // Ekran boyutlarını al
            Rectangle2D screenBounds = TouchInputUtil.getScreenForNode(sourceControl).getVisualBounds();
            // Dialog boyutları (gösterildikten sonra gerçek boyut)
            double dialogWidth = dialogStage.getWidth();
            double dialogHeight = dialogStage.getHeight();
            double x, y;
            if ( sourceControl != null && sourceControl.getScene() != null) {
                // Olayın koordinatlarını al
                Scene scene = sourceControl.getScene();
                Stage sourceStage = (Stage) scene.getWindow();
                // Sahnenin pencere üzerindeki konumunu al
                double windowX = sourceStage.getX();
                double windowY = sourceStage.getY();
                // Kontrol'ün konumunu belirle
                x = screenBounds.getMinX() + 20;
                y = screenBounds.getMinY() + 20;
            } else if (sourceControl != null && sourceControl.getScene() != null) {
                // Olaysız, sadece kaynak kontrolün konumuna göre
                Scene scene = sourceControl.getScene();
                Stage sourceStage = (Stage) scene.getWindow();
                // Kontrol'ün sahne içindeki konumunu al
                double controlX = sourceControl.localToScene(0, 0).getX();
                double controlY = sourceControl.localToScene(0, 0).getY();
                // Sahnenin pencere üzerindeki konumunu al
                double windowX = sourceStage.getX();
                double windowY = sourceStage.getY();
                x = screenBounds.getMinX() + 20;
                y = screenBounds.getMinY() + 20;
            } else {
                // Hem olay hem kaynak kontrol yoksa, ekranın ortasına yerleştir
                x = screenBounds.getMinX() + (screenBounds.getWidth() - dialogWidth) / 2;
                y = screenBounds.getMinY() + (screenBounds.getHeight() - dialogHeight) / 2;
            }
            // Ekrandan taşmayı önle
            if (x < screenBounds.getMinX()) x = screenBounds.getMinX();
            if (y < screenBounds.getMinY()) y = screenBounds.getMinY();
            if (x + dialogWidth > screenBounds.getMaxX()) x = screenBounds.getMaxX() - dialogWidth;
            if (y + dialogHeight > screenBounds.getMaxY()) y = screenBounds.getMaxY() - dialogHeight;
            // Dialog'un konumunu ayarla
            dialogStage.setX(x);
            dialogStage.setY(y);
        });
    }
}