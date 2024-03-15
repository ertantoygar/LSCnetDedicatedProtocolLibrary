package tr.com.logidex.cnetdedicated.fxcontrols;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import tr.com.logidex.cnetdedicated.device.Tag;

import java.util.*;

public class Utils {

    public static List<LSButton> findAllButtonsInThe(List<LSButton> list, Pane pane) {

        for (Node n : pane.getChildren()) {

            if (n instanceof LSButton) {
                list.add((LSButton) n);
            }

            if (n instanceof Pane) {
                findAllButtonsInThe(list, (Pane) n);
            }

        }

        return list;
    }

    public static ArrayList<Tag> packageButtonAdressToRead(List<LSButton> allButtons) {

        Map<String, String> mapButtonAddresses = new TreeMap<String, String>();
        HashMap<String, Tag> tagMap = new HashMap<>();


        //Buton adresleri tekrarlanmasın diye
        for (LSButton button : allButtons) {
            mapButtonAddresses.put(button.getDevice() + button.decideForReadingWord(), button.decideForReadingBit()); // TODO sadece word indexine göre ayırt edemezin. Device Typi da kullanmalıyız.
        }


        mapButtonAddresses.forEach((key, value) -> {
            allButtons.forEach(b -> {
                if (b.decideForReadingWord().equals(key.substring(1, key.length()))) {
                    String targetKey = b.getDevice() + b.decideForReadingWord();
                    //Butonun  adresi okunacak tag listesinde varsa, listedeki tagi butona ata.
                    if (tagMap.containsKey(targetKey) && (tagMap.get(targetKey).getDevice() == b.getTagRead().getDevice())) {
                        b.setTagRead(tagMap.get(targetKey));
                    }
                    //Okunacak tag listesine ekle.
                    tagMap.put(targetKey, b.getTagRead());
                }
            });
        });

        return new ArrayList<Tag>(tagMap.values());

    }


}
