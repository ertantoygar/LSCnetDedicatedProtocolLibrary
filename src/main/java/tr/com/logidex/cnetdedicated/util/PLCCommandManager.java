package tr.com.logidex.cnetdedicated.util;

import tr.com.logidex.cnetdedicated.app.XGBCNetClient;
import tr.com.logidex.cnetdedicated.device.DataType;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoAcknowledgeMessageFromThePLCException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * PLC ile iletişim komutlarını yöneten ve sıralayan sınıf.
 * Bu sınıf, farklı kaynaklardan gelen okuma/yazma isteklerini tek bir thread üzerinden
 * sırayla işleyerek, iletişim çakışmalarını önler.
 */
public class PLCCommandManager   {
    private static final Logger logger = Logger.getLogger(PLCCommandManager.class.getName());

    // Singleton örneği
    private static PLCCommandManager instance;

    // PLC istemcisi
    private final XGBCNetClient plcClient;

    // Tag kayıt sistemi referansı
    private final TagRegistry tagRegistry;

    // Komut kuyruğu
    private final BlockingQueue<PLCCommand> commandQueue;

    private static final int MAX_QUEUE_SIZE = 500;

    // Logging level (can be configured)
    private Level logLevel = Level.INFO;


    // Komut işleyici thread
    private Thread commandProcessorThread;

    // Çalışma durumu
    private volatile boolean running = false;





    // Öncelikler için sabitler
    public enum Priority {
        HIGH(0),     // Yüksek öncelik - kritik yazma komutları
        NORMAL(5),   // Normal öncelik - normal yazma ve manuel okuma komutları
        LOW(10);     // Düşük öncelik - periyodik okuma komutları

        private final int value;

        Priority(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Özel constructor - singleton desenini uygulamak için
     */
    private PLCCommandManager(TagRegistry tagRegistry) {
        this.tagRegistry = tagRegistry;
        this.plcClient = XGBCNetClient.getInstance();
        plcClient.setLogLevel(Level.SEVERE);
        logger.setLevel(logLevel);

        // Önceliğe göre sıralanan bir komut kuyruğu
        this.commandQueue = new PriorityBlockingQueue<>(
                100, // başlangıç kapasitesi
                (c1, c2) -> Integer.compare(c1.getPriority().getValue(), c2.getPriority().getValue())
        );
    }

    /**
     * Set the logging level for this command manager
     * @param level The logging level (e.g., Level.INFO, Level.WARNING, Level.FINE for debug)
     */
    public void setLogLevel(Level level) {
        this.logLevel = level;
        logger.setLevel(level);
    }

    /**
     * Enable console logging output with default level (INFO)
     */
    public void enableConsoleLogging() {
        enableConsoleLogging(Level.INFO);
    }

    /**
     * Enable console logging output with specified level
     * @param level The logging level for console output
     */
    public void enableConsoleLogging(Level level) {
        logger.setUseParentHandlers(false); // Disable default handlers
        java.util.logging.ConsoleHandler consoleHandler = new java.util.logging.ConsoleHandler();
        consoleHandler.setLevel(level);
        consoleHandler.setFormatter(new java.util.logging.SimpleFormatter());
        logger.addHandler(consoleHandler);
        logger.setLevel(level);
    }

    /**
     * Add a custom handler for logging output
     * This allows forwarding logs to custom destinations (e.g., AsyncLogger, files, etc.)
     * @param handler Custom logging handler
     */
    public void addLogHandler(Handler handler) {
        logger.addHandler(handler);
    }

    /**
     * Disable all logging by setting level to OFF
     */
    public void disableLogging() {
        logger.setLevel(Level.OFF);
    }

    /**
     * Singleton örneğini alır veya oluşturur
     */
    public static synchronized PLCCommandManager getInstance(TagRegistry tagRegistry) {
        if (instance == null) {
            instance = new PLCCommandManager(tagRegistry);
        }
        return instance;
    }

    /**
     * Komut işleyiciyi başlatır
     */
    public void start() {
        if (running) return;

        running = true;

        // Komut işleyici thread'i oluştur ve başlat
        commandProcessorThread = new Thread(this::processCommands, "PLCCommandProcessor");
        commandProcessorThread.setDaemon(true); // Ana uygulama kapandığında bu thread de otomatik kapanır
        commandProcessorThread.setPriority(Thread.MAX_PRIORITY - 1); // Yüksek öncelik
        commandProcessorThread.start();

        //LOGGER.info("PLC Komut Yöneticisi başlatıldı");
        logger.info("PLC Komut Yöneticisi başlatıldı");
    }

    /**
     * Komut işleyiciyi durdurur
     */
    public void stop() {
        running = false;

        if (commandProcessorThread != null) {
            commandProcessorThread.interrupt();
            try {
                commandProcessorThread.join(1000); // En fazla 1 saniye bekle
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        //LOGGER.info("PLC Komut Yöneticisi durduruldu");
        logger.info("PLC Komut Yöneticisi durduruldu");
    }

    /**
     * Kuyrukta bekleyen komut sayısını döndürür
     */
    public int getQueueSize() {
        return commandQueue.size();
    }

    private void processCommands(){
        while (running) {
            PLCCommand command = null;
            try {
                // Kuyruktan bir komut al
                command = commandQueue.take();

                // Komutun zaman aşımına uğrayıp uğramadığını kontrol et
                if (command.isExpired()) {
                    //LOGGER.log(Level.WARNING, "Komut zaman aşımına uğradı ve atlandı: {0}", command);
                    logger.warning("Komut zaman aşımına uğradı ve atlandı: " + command);
                    command.setSuccess(false);
                    command.notifyCompletion();
                    continue;
                }

                // Bağlantıyı kontrol et
                if (!plcClient.isConnected()) {
                    //LOGGER.warning("PLC bağlantısı yok, komut atlanıyor: " + command);
                    logger.warning("PLC bağlantısı yok, komut atlanıyor: " + command);
                    command.setSuccess(false);
                    command.notifyCompletion();

                    // Bağlantı yoksa kısa bir süre bekle
                    Thread.sleep(1000);
                    continue;
                }

                try {
                    // Komutu çalıştır
                    //LOGGER.log(Level.FINE, "Komut çalıştırılıyor: {0}", command);
                    logger.fine("Komut çalıştırılıyor: " + command);
                    command.execute(plcClient, tagRegistry);

                    // execute metodu success durumunu ayarlayacak, ek işlem gerekmez

                } catch (Exception | NoAcknowledgeMessageFromThePLCException | NoResponseException e) {
                    // Tüm hatalar için
                    //LOGGER.log(Level.SEVERE, "Komut çalıştırma hatası: " + command, e);
                    logger.log(Level.SEVERE, "Komut çalıştırma hatası: " + command, e);
                    command.setSuccess(false);

                    // Yeniden deneme mantığı
                    if (command.shouldRetry()) {
                        //LOGGER.info("Komut yeniden deneniyor: " + command);
                        logger.info("Komut yeniden deneniyor: " + command);
                        command.incrementRetryCount();

                        commandQueue.offer(command); // Komutu kuyruğa geri ekle

                    } else {
                        // Son deneme başarısız, tamamlanma bildirimi yap
                        command.notifyCompletion();
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //LOGGER.info("Komut işleyici thread kesintiye uğradı");
                logger.info("Komut işleyici thread kesintiye uğradı");
                break;
            } catch (Exception e) {
                // Thread'in durmaması için en üst seviyede tüm hataları yakala
                //LOGGER.log(Level.SEVERE, "Beklenmeyen komut işleme hatası", e);
                logger.log(Level.SEVERE, "Beklenmeyen komut işleme hatası", e);

                // Eğer komut varsa ve işleme sırasında hata oluştuysa bildirim yap
                if (command != null) {
                    command.setSuccess(false);
                    try {
                        command.notifyCompletion();
                    } catch (Exception notifyEx) {
                        //LOGGER.log(Level.SEVERE, "Bildirim sırasında hata", notifyEx);
                        logger.log(Level.SEVERE, "Bildirim sırasında hata", notifyEx);
                    }
                }
            }
        }
    }




    /**
     * Komut kuyruğunun durumunu loglayan bir metot
     */
    public void logQueueStatus() {
        //LOGGER.info("Komut kuyruğu durumu: Boyut=" + commandQueue.size() +
        //        ", Maksimum boyut=" + MAX_QUEUE_SIZE);
        logger.info("Komut kuyruğu durumu: Boyut=" + commandQueue.size() +
                ", Maksimum boyut=" + MAX_QUEUE_SIZE);
    }

    /**
     * Belirli bir kayıt grubunu okumak için komut kuyruğa ekler
     *
     * @param regNumber Kayıt grubu numarası
     * @param priority Komut önceliği
     * @return Komutun gelecekteki sonucunu temsil eden CompletableFuture
     */
    public CompletableFuture<List<Tag>> readRegistrationGroup(int regNumber, Priority priority) {

        if (commandQueue.size() >= MAX_QUEUE_SIZE) {
            // Düşük öncelikli komutları atla
            if (priority == Priority.LOW) {
                //LOGGER.warning("Komut kuyruğu dolu, düşük öncelikli okuma komutu atlandı: " + regNumber);
                logger.warning("Komut kuyruğu dolu, düşük öncelikli okuma komutu atlandı: " + regNumber);
                CompletableFuture<List<Tag>> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Komut kuyruğu dolu"));
                return future;
            }

            // Yüksek öncelikli komutlar için, düşük öncelikli bir komutu at
            removeLowestPriorityCommand();
        }


        ReadGroupCommand command = new ReadGroupCommand(regNumber, priority);
        commandQueue.offer(command);


        if (commandQueue.size() % 100 == 0) {
            //LOGGER.info("Komut kuyruğu boyutu: " + commandQueue.size());
            logger.info("Komut kuyruğu boyutu: " + commandQueue.size());
        }

        return command.getFuture();
    }


    private void removeLowestPriorityCommand() {
        // PriorityBlockingQueue doğrudan en düşük öncelikli elemanı çıkarmaya izin vermez
        // Bu yüzden önce kuyruğu kopyalayıp içinden arayacağız
        List<PLCCommand> commands = new ArrayList<>();
        commandQueue.drainTo(commands);

        // Eğer kuyruk boşsa (başka bir thread tarafından boşaltılmış olabilir) hiçbir şey yapma
        if (commands.isEmpty()) {
            return;
        }

        // Komutları önceliklerine göre sırala (düşük öncelikli olanlar önde)
        commands.sort((c1, c2) -> Integer.compare(c1.getPriority().getValue(), c2.getPriority().getValue()));

        // En düşük öncelikli komutu çıkar
        PLCCommand removedCommand = commands.remove(0);
        //LOGGER.warning("Kuyruk dolu, düşük öncelikli komut çıkarıldı: " + removedCommand);
        logger.warning("Kuyruk dolu, düşük öncelikli komut çıkarıldı: " + removedCommand);

        // Çıkarılan komutun future'ını exception ile tamamla
        if (removedCommand instanceof ReadGroupCommand) {
            ((ReadGroupCommand) removedCommand).getFuture().completeExceptionally(
                    new RuntimeException("Komut kuyruğu dolu olduğu için iptal edildi"));
        } else if (removedCommand instanceof WriteSingleCommand) {
            ((WriteSingleCommand) removedCommand).getFuture().completeExceptionally(
                    new RuntimeException("Komut kuyruğu dolu olduğu için iptal edildi"));
        } else if (removedCommand instanceof WriteBitCommand) {
            ((WriteBitCommand) removedCommand).getFuture().completeExceptionally(
                    new RuntimeException("Komut kuyruğu dolu olduğu için iptal edildi"));
        }

        // Kalan komutları kuyruğa geri koy
        commandQueue.addAll(commands);
    }


    /**
     * Tek bir tag'i yazmak için komut kuyruğa ekler
     *
     * @param tag Yazılacak tag
     * @param value Yazılacak değer
     * @param priority Komut önceliği
     * @return Komutun gelecekteki sonucunu temsil eden CompletableFuture
     */
    public CompletableFuture<Tag> writeSingleTag(Tag tag, String value, Priority priority) {
        WriteSingleCommand command = new WriteSingleCommand(tag, value, priority);
        commandQueue.offer(command);
        return command.getFuture();
    }

    /**
     * Tek bir tag'i okumak için komut kuyruğa ekler
     *
     * @param tag Okunacak tag
     * @param priority Komut önceliği
     * @return Komutun gelecekteki sonucunu temsil eden CompletableFuture
     */
    public  CompletableFuture<Tag> readSingleTag(Tag tag, Priority priority) {
        if (commandQueue.size() >= MAX_QUEUE_SIZE) {
            // Düşük öncelikli komutları atla
            if (priority == Priority.LOW) {
                //LOGGER.warning("Komut kuyruğu dolu, düşük öncelikli okuma komutu atlandı: " + tag.getName());
                logger.warning("Komut kuyruğu dolu, düşük öncelikli okuma komutu atlandı: " + tag.getName());
                CompletableFuture<Tag> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("Komut kuyruğu dolu"));
                return future;
            }

            // Yüksek öncelikli komutlar için, düşük öncelikli bir komutu at
            removeLowestPriorityCommand();
        }

        ReadSingleCommand command = new ReadSingleCommand(tag, priority);
        commandQueue.offer(command);

        if (commandQueue.size() % 100 == 0) {
            //LOGGER.info("Komut kuyruğu boyutu: " + commandQueue.size());
            logger.info("Komut kuyruğu boyutu: " + commandQueue.size());
        }

        if (commandQueue.size() > MAX_QUEUE_SIZE * 0.9) { // %90 doluluk
            //LOGGER.warning("Kuyruk kritik seviyede, düşük öncelikli komutlar temizleniyor");
            logger.warning("Kuyruk kritik seviyede, düşük öncelikli komutlar temizleniyor");
            cleanLowPriorityCommands();
        }

        return command.getFuture();
    }


    /**
     * Kuyruktan düşük öncelikli komutları temizler
     *
     * @return Temizlenen komut sayısı
     */
    private int cleanLowPriorityCommands() {
        //LOGGER.warning("Düşük öncelikli komutlar temizleniyor...");
        logger.warning("Düşük öncelikli komutlar temizleniyor...");

        // Kuyruktaki tüm komutları geçici bir listeye kopyala
        List<PLCCommand> allCommands = new ArrayList<>();
        commandQueue.drainTo(allCommands);

        if (allCommands.isEmpty()) {
            //LOGGER.info("Temizlenecek komut bulunamadı, kuyruk boş");
            logger.info("Temizlenecek komut bulunamadı, kuyruk boş");
            return 0;
        }

        // Komutları önceliğe göre ayır
        List<PLCCommand> highPriorityCommands = new ArrayList<>();
        List<PLCCommand> normalPriorityCommands = new ArrayList<>();
        List<PLCCommand> lowPriorityCommands = new ArrayList<>();

        for (PLCCommand command : allCommands) {
            if (command.getPriority() == Priority.HIGH) {
                highPriorityCommands.add(command);
            } else if (command.getPriority() == Priority.NORMAL) {
                normalPriorityCommands.add(command);
            } else if (command.getPriority() == Priority.LOW) {
                lowPriorityCommands.add(command);
            }
        }

        int cleanedCount = lowPriorityCommands.size();

        // Düşük öncelikli komutları exception ile tamamla
        for (PLCCommand command : lowPriorityCommands) {
            try {
                completeCommandWithException(command, "Kuyruk temizleme işlemi sırasında iptal edildi");
            } catch (Exception e) {
                //LOGGER.log(Level.WARNING, "Komut iptal edilirken hata: " + command, e);
                logger.warning("Komut iptal edilirken hata: " + command + " - " + e.getMessage());
            }
        }

        // Yüksek ve normal öncelikli komutları geri kuyruğa ekle
        commandQueue.addAll(highPriorityCommands);
        commandQueue.addAll(normalPriorityCommands);

        //LOGGER.info("Kuyruk temizleme tamamlandı: " + cleanedCount + " düşük öncelikli komut temizlendi. " +
        //        "Kalan komut sayısı: " + commandQueue.size() +
        //        " (Yüksek: " + highPriorityCommands.size() +
        //        ", Normal: " + normalPriorityCommands.size() + ")");
        logger.info("Kuyruk temizleme tamamlandı: " + cleanedCount + " düşük öncelikli komut temizlendi. " +
                "Kalan komut sayısı: " + commandQueue.size() +
                " (Yüksek: " + highPriorityCommands.size() +
                ", Normal: " + normalPriorityCommands.size() + ")");

        return cleanedCount;
    }


    /**
     * Komutun CompletableFuture'ını exception ile tamamlar
     */
    private void completeCommandWithException(PLCCommand command, String reason) {
        if (command instanceof ReadGroupCommand) {
            ((ReadGroupCommand) command).getFuture().completeExceptionally(
                    new RuntimeException(reason));
        } else if (command instanceof WriteSingleCommand) {
            ((WriteSingleCommand) command).getFuture().completeExceptionally(
                    new RuntimeException(reason));
        } else if (command instanceof WriteBitCommand) {
            ((WriteBitCommand) command).getFuture().completeExceptionally(
                    new RuntimeException(reason));
        } else if (command instanceof ReadSingleCommand) {
            ((ReadSingleCommand) command).getFuture().completeExceptionally(
                    new RuntimeException(reason));
        }
    }

    /**
     * Bit değeri yazmak için komut kuyruğa ekler
     *
     * @param tag Yazılacak bit tag'i
     * @param value Bit değeri (true/false)
     * @param priority Komut önceliği
     * @return Komutun gelecekteki sonucunu temsil eden CompletableFuture
     */
    public CompletableFuture<Tag> writeBitTag(Tag tag, boolean value, Priority priority) {
        WriteBitCommand command = new WriteBitCommand(tag, value, priority);
        commandQueue.offer(command);
        return command.getFuture();
    }

    // ========================== PLC KOMUT SINIFLARI ==========================

    /**
     * PLC komutları için temel soyut sınıf
     */
    private abstract static class PLCCommand {
        private final Priority priority;
        private final long creationTime;
        private final long timeoutMs;
        private int retryCount = 0;
        private boolean success = false;

        public PLCCommand(Priority priority) {
            this.priority = priority;
            this.creationTime = System.currentTimeMillis();
            this.timeoutMs = 5000; // 5 saniye varsayılan zaman aşımı
            this.success = false;

        }

        /**
         * Komut önceliğini döndürür
         */
        public Priority getPriority() {
            return priority;
        }

        /**
         * Komutun zaman aşımına uğrayıp uğramadığını kontrol eder
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - creationTime > timeoutMs;
        }

        /**
         * Yeniden deneme sayısını arttırır
         */
        public void incrementRetryCount() {
            retryCount++;
        }

        /**
         * Komutun yeniden denenip denenmeyeceğini belirler
         */
        public boolean shouldRetry() {
            return retryCount < 3; // En fazla 3 kez yeniden dene
        }

        /**
         * Komutun başarı durumunu ayarlar
         */
        public void setSuccess(boolean success) {
            this.success = success;
        }

        /**
         * Komutun başarı durumunu döndürür
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Komutun tamamlandığını bildirir
         */
        public abstract void notifyCompletion();

        /**
         * Komutu çalıştırır
         */
        public abstract void execute(XGBCNetClient plcClient, TagRegistry tagRegistry) throws Exception, NoAcknowledgeMessageFromThePLCException, NoResponseException;
    }

    /**
     * Tag grubunu okuma komutu
     */
    private static class ReadGroupCommand extends PLCCommand {
        private final int regNumber;
        private final CompletableFuture<List<Tag>> future;
        private List<Tag> result;

        public ReadGroupCommand(int regNumber, Priority priority) {
            super(priority);
            this.regNumber = regNumber;
            this.future = new CompletableFuture<>();
        }

        @Override
        public void execute(XGBCNetClient plcClient, TagRegistry tagRegistry) throws Exception, NoAcknowledgeMessageFromThePLCException, NoResponseException {
            try {
                result = tagRegistry.readRegistrationGroup(regNumber);
                // Okuma başarılı olduğunda success değişkenini true olarak ayarlayın
                setSuccess(true);
            } catch (Exception e) {
                // Hata durumunda success değişkenini false olarak ayarlayın
                setSuccess(false);
                throw e;  // Hatayı yukarıya taşımayı unutmayın
            } finally {
                // Her durumda notifyCompletion çağrılır
                notifyCompletion();
            }
        }

        @Override
        public void notifyCompletion() {
            if (isSuccess() && result != null) {
                future.complete(result);
            } else {
                String errorMessage;
                if (!isSuccess()) {
                    errorMessage = "Kayıt grubu okuma işlemi başarısız oldu (success=false): " + regNumber;
                } else {
                    errorMessage = "Kayıt grubu okuma başarısız (result=null): " + regNumber;
                }
                future.completeExceptionally(new RuntimeException(errorMessage));
            }
        }

        public CompletableFuture<List<Tag>> getFuture() {
            return future;
        }

        @Override
        public String toString() {
            return "ReadGroupCommand{regNumber=" + regNumber + "}";
        }
    }

    /**
     * Tek tag yazma komutu
     */
    private static class WriteSingleCommand extends PLCCommand {
        private final Tag tag;
        private final String value;
        private final CompletableFuture<Tag> future;

        public WriteSingleCommand(Tag tag, String value, Priority priority) {
            super(priority);
            this.tag = tag;
            this.value = value;
            this.future = new CompletableFuture<>();
        }

        @Override
        public void execute(XGBCNetClient plcClient, TagRegistry tagRegistry) throws Exception, NoAcknowledgeMessageFromThePLCException, NoResponseException {
            try {
                tag.setValueAsHexString(value);
                if(tag.getDataType() == DataType.Dword)
                    plcClient.writeDouble(tag);
                else
                plcClient.writeSingle(tag);
                // Yazma başarılı olduğunda success değişkenini true olarak ayarlayın
                setSuccess(true);
                //LOGGER.fine("Tag başarıyla yazıldı: " + tag.getName() + " = " + value);
                logger.fine("Tag başarıyla yazıldı: " + tag.getName() + " = " + value);
            } catch (Exception e) {
                // Hata durumunda success değişkenini false olarak ayarlayın
                setSuccess(false);
                //LOGGER.warning("Tag yazma hatası: " + tag.getName() + " - " + e.getMessage());
                logger.warning("Tag yazma hatası: " + tag.getName() + " - " + e.getMessage());
                throw e;  // Hatayı yukarıya taşıyın
            } finally {
                // Her durumda notifyCompletion çağırın
                notifyCompletion();
            }
        }

        @Override
        public void notifyCompletion() {
            if (isSuccess()) {
                future.complete(tag);
            } else {
                String errorMessage = "Tag yazma başarısız: " + tag.getName() + " (value: " + value + ")";
                future.completeExceptionally(new RuntimeException(errorMessage));
            }
        }

        public CompletableFuture<Tag> getFuture() {
            return future;
        }

        @Override
        public String toString() {
            return "WriteSingleCommand{tag=" + tag.getName() + ", value=" + value + "}";
        }
    }

    /**
     * Bit değeri yazma komutu
     */
    private static class WriteBitCommand extends PLCCommand {
        private final Tag tag;
        private final boolean bitValue;
        private final CompletableFuture<Tag> future;

        public WriteBitCommand(Tag tag, boolean bitValue, Priority priority) {
            super(priority);
            this.tag = tag;
            this.bitValue = bitValue;
            this.future = new CompletableFuture<>();
        }

        @Override
        public void execute(XGBCNetClient plcClient, TagRegistry tagRegistry) throws Exception, NoAcknowledgeMessageFromThePLCException, NoResponseException {
            try {
                plcClient.writeBit(tag, bitValue);
                // Yazma başarılı olduğunda success değişkenini true olarak ayarlayın
                setSuccess(true);
                //LOGGER.fine("Bit başarıyla yazıldı: " + tag.getName() + " = " + bitValue);
                logger.fine("Bit başarıyla yazıldı: " + tag.getName() + " = " + bitValue);
            } catch (Exception e) {
                // Hata durumunda success değişkenini false olarak ayarlayın
                setSuccess(false);
                //LOGGER.warning("Bit yazma hatası: " + tag.getName() + " - " + e.getMessage());
                logger.warning("Bit yazma hatası: " + tag.getName() + " - " + e.getMessage());
                throw e;  // Hatayı yukarıya taşıyın
            } finally {
                // Her durumda notifyCompletion çağırın
                notifyCompletion();
            }
        }

        @Override
        public void notifyCompletion() {
            if (isSuccess()) {
                future.complete(tag);
            } else {
                future.completeExceptionally(new RuntimeException("Bit yazma başarısız: " + tag.getName()));
            }
        }

        public CompletableFuture<Tag> getFuture() {
            return future;
        }

        @Override
        public String toString() {
            return "WriteBitCommand{tag=" + tag.getName() + ", bitValue=" + bitValue + "}";
        }
    }

    /**
     * Tek bir tag'i okuma komutu
     */
    private static class ReadSingleCommand extends PLCCommand {
        private final Tag tag;
        private final CompletableFuture<Tag> future;

        public ReadSingleCommand(Tag tag, Priority priority) {
            super(priority);
            this.tag = tag;
            this.future = new CompletableFuture<>();
        }

        @Override
        public void execute(XGBCNetClient plcClient, TagRegistry tagRegistry) throws Exception, NoAcknowledgeMessageFromThePLCException, NoResponseException {
            try {
                // Tag'i oku
                Tag result = plcClient.readSingle(tag);
                // Okuma başarılı olduğunda success değişkenini true olarak ayarla
                setSuccess(true);
                // Tag değerini güncelle
                tag.setValueAsHexString(result.getValueAsHexString());
            } catch (Exception e) {
                // Hata durumunda success değişkenini false olarak ayarla
                setSuccess(false);
                throw e;  // Hatayı yukarıya taşı
            } finally {
                // Her durumda notifyCompletion çağır
                notifyCompletion();
            }
        }

        @Override
        public void notifyCompletion() {
            if (isSuccess()) {
                future.complete(tag);
            } else {
                String errorMessage = "Tag okuma başarısız: " + tag.getName();
                future.completeExceptionally(new RuntimeException(errorMessage));
            }
        }

        public CompletableFuture<Tag> getFuture() {
            return future;
        }

        @Override
        public String toString() {
            return "ReadSingleCommand{tag=" + tag.getName() + "}";
        }
    }


}

