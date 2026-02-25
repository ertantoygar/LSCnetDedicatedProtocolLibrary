package tr.com.logidex.cnetdedicated.util;
import javafx.application.Platform;
import tr.com.logidex.cnetdedicated.app.XGBCNetClient;
import tr.com.logidex.cnetdedicated.device.Tag;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoAcknowledgeMessageFromThePLCException;
import tr.com.logidex.cnetdedicated.protocol.exceptions.NoResponseException;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * TagRegistry, PLC tag'lerinin merkezi yönetimini sağlayan ve değer değişikliklerini
 * farklı thread'lerde işleyen bir sınıftır. Bu sınıf aşağıdaki özellikleri sağlar:
 * - Tag'leri kategorizasyon ve önceliklendirme ile yönetme
 * - Tag değer değişikliklerini izleme ve bildirme
 * - UI ve iş mantığı bileşenlerini ayrı thread'lerde çalıştırma
 * - PLC ile iletişim optimizasyonu için tag gruplandırma
 */
public class TagRegistry {
    // LOGGER
    private static final Logger LOGGER = Logger.getLogger(TagRegistry.class.getName());

    // THREAD YÖNETIMI

    // İş mantığı için özel bir executor (tek thread)
    private final ExecutorService businessLogicExecutor = Executors.newSingleThreadExecutor(
            r -> {
                Thread thread = new Thread(r, "BusinessLogicThread");
                thread.setPriority(Thread.MAX_PRIORITY - 1); // Yüksek öncelik (ama cyclic thread'den düşük)
                thread.setDaemon(true);
                return thread;
            }
    );

    // Kritik iş mantığı için özel bir executor (tek thread)
    private final ExecutorService criticalBusinessExecutor = Executors.newSingleThreadExecutor(
            r -> {
                Thread thread = new Thread(r, "CriticalBusinessThread");
                thread.setPriority(Thread.MAX_PRIORITY - 1);
                thread.setDaemon(true);
                return thread;
            }
    );

    // TAG SAKLAMA VE KATEGORİZASYON

    // Tüm tag'lerin saklandığı ana koleksiyon
    private final Map<String, Tag> allTags = new ConcurrentHashMap<>();

    // Kritik tag'lerin isimleri (sürekli yüksek öncelikli okuma için)
    private final Set<String> criticalTagNames = ConcurrentHashMap.newKeySet();

    // Sayfalara göre tag'leri gruplandırma
    private final Map<String, Set<String>> pageTagsMap = new ConcurrentHashMap<>();

    // PLC kayıt numaralarına göre tag'leri gruplandırma
    private final Map<Integer, List<Tag>> registrationGroups = new ConcurrentHashMap<>();

    // Aktif sayfa ID'si
    private String activePageId = null;

    // TAG ABONELIK MEKANIZMASI

    // Dinleyici tipleri için enum
    public enum ListenerType {
        UI,       // UI bileşenleri için (JavaFX thread'inde çalışır)
        BUSINESS, // Normal iş mantığı için (business executor'da çalışır)
        CRITICAL  // Kritik iş mantığı için (critical executor'da çalışır)
    }

    // Her tag için dinleyici tiplerini saklama
    private final Map<String, Map<TagChangeListener, ListenerType>> tagListeners = new ConcurrentHashMap<>();

    // İstatistikler ve durum izleme
    private volatile long totalUpdates = 0;
    private volatile long lastUpdateTime = 0;

    /**
     * Tag değişikliklerini dinlemek için kullanılan arayüz.
     */
    public interface TagChangeListener {
        /**
         * Tag değeri değiştiğinde çağrılır.
         * @param tag Değişen tag
         */
        void onTagChanged(Tag tag);
    }

    /**
     * Yeni bir tag'i sisteme kaydeder.
     *
     * @param tag Kaydedilecek tag
     * @param isCritical Tag'in kritik olup olmadığı (sürekli yüksek öncelikli okuma için)
     * @param pageIds Bu tag'in görüntülendiği sayfaların ID'leri
     * @return Kaydedilen tag
     */
    public Tag registerTag(Tag tag, boolean isCritical, String... pageIds) {
        allTags.put(tag.getName(), tag);

        if (isCritical) {
            criticalTagNames.add(tag.getName());
        }

        for (String pageId : pageIds) {
            pageTagsMap.computeIfAbsent(pageId, k -> ConcurrentHashMap.newKeySet())
                    .add(tag.getName());
        }

        // Tag değeri değiştiğinde dinleyicilere bildirim yapacak property listener'ı ekle
        tag.valueProperty().addListener((obs, oldVal, newVal) -> {
            notifyTagChangedIfSignificant(tag, oldVal != null ? oldVal.toString() : null,
                    newVal != null ? newVal.toString() : null);
        });

        LOGGER.log(Level.FINE, "Tag kaydedildi: {0}, kritik: {1}", new Object[]{tag.getName(), isCritical});
        return tag;
    }

    /**
     * İsme göre kayıtlı bir tag döndürür.
     *
     * @param tagName Tag adı
     * @return Bulunan tag veya null
     */
    public Tag getTag(String tagName) {
        return allTags.get(tagName);
    }

    /**
     * Tüm kayıtlı tag'leri döndürür.
     *
     * @return Tüm tag'lerin bir listesi
     */
    public List<Tag> getAllTags() {
        return new ArrayList<>(allTags.values());
    }

    /**
     * Tag değişikliği dinleyicisi ekler ve tipini belirtir.
     *
     * @param tagName Dinlenecek tag adı
     * @param listener Değişiklikleri alacak listener
     * @param type Listener'ın çalışacağı thread tipi
     */
    public void addTagChangeListener(String tagName, TagChangeListener listener, ListenerType type) {
        tagListeners.computeIfAbsent(tagName, k -> new ConcurrentHashMap<>())
                .put(listener, type);

        LOGGER.log(Level.FINE, "Tag değişikliği dinleyicisi eklendi: {0}, tip: {1}",
                new Object[]{tagName, type});
    }

    /**
     * Tag değişikliği dinleyicisini kaldırır.
     *
     * @param tagName Tag adı
     * @param listener Kaldırılacak listener
     */
    public void removeTagChangeListener(String tagName, TagChangeListener listener) {
        if (tagListeners.containsKey(tagName)) {
            tagListeners.get(tagName).remove(listener);
            LOGGER.log(Level.FINE, "Tag değişikliği dinleyicisi kaldırıldı: {0}", tagName);
        }
    }

    /**
     * Değerdeki değişim önemliyse bildirim yapan metot.
     * Gereksiz güncellemeleri filtreler.
     */
    private void notifyTagChangedIfSignificant(Tag tag, String oldValue, String newValue) {
        // Değer değişmemişse bildirim yapma
        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        // Sayısal değerler için, değişim belirli bir eşiği geçmişse bildirim yap
        if (tag.isNumericTag()) {
            try {
                // Eski veya yeni değer null ise, her zaman bildirim yap
                if (oldValue == null || newValue == null) {
                    notifyTagChanged(tag);
                    return;
                }

                double oldNum = Double.parseDouble(oldValue);
                double newNum = Double.parseDouble(newValue);

                // Değişim %1'den az ve mutlak değişim 1'den az ise bildirim yapma
                // (çok küçük değişimler için gürültü filtreleme)
                if (Math.abs(newNum - oldNum) < 1.0 &&
                        (oldNum == 0 || Math.abs((newNum - oldNum) / oldNum) < 0.01)) {
                    return;
                }
            } catch (NumberFormatException e) {
                // Sayısal değer değilse, her değişimde bildirim yap
            }
        }

        // Değişim önemliyse bildirim yap
        notifyTagChanged(tag);
    }

    /**
     * Bir tag'in değeri değiştiğinde ilgili dinleyicilere
     * kendi thread'lerinde bildirim yapar.
     */
    private void notifyTagChanged(Tag tag) {
        totalUpdates++;
        lastUpdateTime = System.currentTimeMillis();

        if (tagListeners.containsKey(tag.getName())) {
            Map<TagChangeListener, ListenerType> listeners = tagListeners.get(tag.getName());

            for (Map.Entry<TagChangeListener, ListenerType> entry : listeners.entrySet()) {
                TagChangeListener listener = entry.getKey();
                ListenerType type = entry.getValue();

                try {
                    switch (type) {
                        case UI:
                            // UI bileşenleri için JavaFX thread'ini kullan
                            Platform.runLater(() -> safeNotify(listener, tag));
                            break;

                        case BUSINESS:
                            // İş mantığı için ayrı bir thread havuzunu kullan
                            businessLogicExecutor.submit(() -> safeNotify(listener, tag));
                            break;

                        case CRITICAL:
                            // Kritik iş mantığı için öncelikli thread'i kullan
                            criticalBusinessExecutor.submit(() -> safeNotify(listener, tag));
                            break;
                    }
                } catch (RejectedExecutionException e) {
                    // Executor kapatıldıysa, hata logla ama devam et
                    LOGGER.log(Level.WARNING, "Bildirim gönderilemedi, executor kapatılmış: {0}",
                            tag.getName());
                }
            }
        }
    }

    /**
     * Dinleyiciye güvenli bildirim yapar (hataları yakalar)
     */
    private void safeNotify(TagChangeListener listener, Tag tag) {
        try {
            listener.onTagChanged(tag);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Tag değişikliği işlenirken hata: " + tag.getName(), e);
        }
    }

    /**
     * Tag'leri PLC'deki döngüsel okuma için kayıt grubuna ekler.
     *
     * @param regNumber Kayıt grubu numarası
     * @param tags Gruba eklenecek tag'ler
     * @throws Exception PLC iletişim hatası durumunda
     */
    public void addTagsToRegistrationGroup(int regNumber, List<Tag> tags) throws Exception, NoAcknowledgeMessageFromThePLCException, NoResponseException {
        // Önceden kaydedilmiş bir grup varsa, önce onu temizle
        if (registrationGroups.containsKey(regNumber)) {
            registrationGroups.remove(regNumber);
        }

        // Tag'leri XGBCNetClient ile kaydet
        XGBCNetClient client = XGBCNetClient.getInstance();
        client.registerDevicesToMonitor(tags, String.valueOf(regNumber));

        // Başarılı kayıt sonrası, tag'leri gruba ekle
        registrationGroups.put(regNumber, new ArrayList<>(tags));

        LOGGER.log(Level.INFO, "Tag grubu kaydedildi. Grup No: {0}, Tag sayısı: {1}",
                new Object[]{regNumber, tags.size()});
    }

    /**
     * Belirli bir kayıt grubundaki tüm tag'leri okur ve değerlerini günceller.
     *
     * @param regNumber Okunacak kayıt grubu numarası
     * @return Güncellenen tag'ler listesi
     * @throws Exception PLC iletişim hatası durumunda
     */
    public List<Tag> readRegistrationGroup(int regNumber) throws Exception, NoAcknowledgeMessageFromThePLCException, NoResponseException {
        if (!registrationGroups.containsKey(regNumber)) {
            throw new IllegalArgumentException("Kayıt grubu bulunamadı: " + regNumber);
        }

        XGBCNetClient client = XGBCNetClient.getInstance();
        List<Tag> updatedTags = client.executeRegisteredDeviceToMonitor(String.valueOf(regNumber));

        LOGGER.log(Level.FINE, "Tag grubu okundu. Grup No: {0}, Güncelenen tag sayısı: {1}",
                new Object[]{regNumber, updatedTags != null ? updatedTags.size() : 0});

        return updatedTags;
    }

    /**
     * Aktif sayfayı değiştirir ve sayfa tag'lerinin önceliklerini günceller.
     *
     * @param pageId Aktif sayfa ID'si
     */
    public void setActivePage(String pageId) {
        this.activePageId = pageId;
        LOGGER.log(Level.INFO, "Aktif sayfa değişti: {0}", pageId);
    }

    /**
     * Aktif sayfa ID'sini döndürür.
     *
     * @return Aktif sayfa ID'si
     */
    public String getActivePageId() {
        return activePageId;
    }

    /**
     * Kritik olarak işaretlenmiş tüm tag'leri döndürür.
     * Bu tag'ler her döngüde yüksek öncelikle okunmalıdır.
     *
     * @return Kritik tag'ler listesi
     */
    public List<Tag> getCriticalTags() {
        return criticalTagNames.stream()
                .map(allTags::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Aktif sayfada görüntülenen tag'leri döndürür.
     *
     * @return Aktif sayfa tag'leri
     */
    public List<Tag> getActivePageTags() {
        if (activePageId == null || !pageTagsMap.containsKey(activePageId)) {
            return Collections.emptyList();
        }

        return pageTagsMap.get(activePageId).stream()
                .map(allTags::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Belirli bir sayfada kullanılan tüm tag'leri döndürür.
     *
     * @param pageId Sayfa ID'si
     * @return Sayfa tag'leri
     */
    public List<Tag> getPageTags(String pageId) {
        if (!pageTagsMap.containsKey(pageId)) {
            return Collections.emptyList();
        }

        return pageTagsMap.get(pageId).stream()
                .map(allTags::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * PLC kayıt gruplarını ve tüm tag'lerin durumunu konsola yazdırır.
     * Hata ayıklama amaçlıdır.
     */
    public void printRegistryStatus() {
        StringBuilder status = new StringBuilder("=== TAG REGISTRY STATUS ===\n");
        status.append("Total registered tags: ").append(allTags.size()).append("\n");
        status.append("Critical tags: ").append(criticalTagNames.size()).append("\n");
        status.append("Registration groups: ").append(registrationGroups.size()).append("\n");
        status.append("Total tag updates: ").append(totalUpdates).append("\n");

        status.append("\nRegistration Groups:\n");
        for (Map.Entry<Integer, List<Tag>> entry : registrationGroups.entrySet()) {
            status.append("Group ").append(entry.getKey()).append(": ")
                    .append(entry.getValue().size()).append(" tags\n");
        }

        status.append("\nPage Tags:\n");
        for (Map.Entry<String, Set<String>> entry : pageTagsMap.entrySet()) {
            status.append("Page ").append(entry.getKey()).append(": ")
                    .append(entry.getValue().size()).append(" tags\n");
        }

        status.append("\nCurrent active page: ")
                .append(activePageId != null ? activePageId : "None").append("\n");

        LOGGER.info(status.toString());
    }

    /**
     * Kaynakları temizler.
     */
    public void shutdown() {
        LOGGER.info("Tag Registry kapatılıyor...");

        businessLogicExecutor.shutdown();
        criticalBusinessExecutor.shutdown();

        try {
            // Düzgün kapatmayı dene
            if (!businessLogicExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                businessLogicExecutor.shutdownNow();
            }
            if (!criticalBusinessExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                criticalBusinessExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            businessLogicExecutor.shutdownNow();
            criticalBusinessExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Tüm koleksiyonları temizle
        tagListeners.clear();
        criticalTagNames.clear();
        pageTagsMap.clear();
        registrationGroups.clear();
        allTags.clear();

        LOGGER.info("Tag Registry başarıyla kapatıldı.");
    }

    /**
     * Tag güncellemelerine ilişkin performans istatistiklerini döndürür.
     *
     * @return İstatistik bilgilerini içeren bir Map
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTags", allTags.size());
        stats.put("criticalTags", criticalTagNames.size());
        stats.put("totalUpdates", totalUpdates);
        stats.put("lastUpdateTime", lastUpdateTime);
        stats.put("registrationGroups", registrationGroups.size());
        stats.put("activeListeners", countActiveListeners());

        return stats;
    }

    /**
     * Aktif dinleyici sayısını hesaplar.
     *
     * @return Toplam aktif dinleyici sayısı
     */
    private int countActiveListeners() {
        int count = 0;
        for (Map<TagChangeListener, ListenerType> listeners : tagListeners.values()) {
            count += listeners.size();
        }
        return count;
    }
}