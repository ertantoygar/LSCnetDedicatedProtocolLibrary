package tr.com.logidex.cnetdedicated.util;
import tr.com.logidex.cnetdedicated.device.DisplayFormat;
public class XGBCNetUtil {
    public static String addZeroIfNeed(int i) {
        String sSize = String.valueOf(i);
        return i < 10 ? "0" + sSize : sSize;
    }


    public static String addZeroIfNeed(String s) {
        return s.length() == 1 ? "0" + s : s;
    }


    public static boolean checkFrame(String message) {
        String etx = String.valueOf((char) 0x03);
        if (!message.contains(etx)) {
            return false;
        }
        String[] splitFromETX = message.split(etx);
        String bccCode = splitFromETX[1].trim();
        String data = splitFromETX[0] + (char) 0x03;
        String calcBCC = calcFrameCheck(data);
        return calcBCC.equals(bccCode);
    }


    public static String calcFrameCheck(String message) {
        int totalAsciiValue = 0;
        for (char c : message.toCharArray()) {
            totalAsciiValue += (int) c;
        }
        String hexValue = Integer.toHexString(totalAsciiValue);
        byte lowerByte = (byte) totalAsciiValue;
        return addZeroIfNeed(Integer.toHexString(lowerByte & 0xFF).toUpperCase());
    }


    public static String addZeroToStart(int targetBitCount, String binaryString) {
        return String.format("%" + String.valueOf(targetBitCount) + "s", binaryString).replace(' ', '0');
    }


    public static String convertTurkishToEnglish(String text) {
        String[] turkishChars = {"ı", "İ", "ş", "Ş", "ğ", "Ğ", "ü", "Ü", "ö", "Ö", "ç", "Ç"};
        String[] englishChars = {"i", "I", "s", "S", "g", "G", "u", "U", "o", "O", "c", "C"};
        for (int i = 0; i < turkishChars.length; i++) {
            text = text.replace(turkishChars[i], englishChars[i]);
        }
        return text;
    }


    public static String createValidationPattern(DisplayFormat displayFormat) {
        String validationPattern = null;
        switch (displayFormat) {
            case FLOAT:
                validationPattern = "^[-+]?\\d*\\.?\\d*$"; // 123 | +3.14159 | -3.14159
                break;
            case SIGNED_INT:
                validationPattern = "^(\\+|-)?\\d+$"; // -34 | 34 | +5
                break;
            case UNSIGNED_INT:
                validationPattern = "^\\d*$"; // 123 | 000 | 43
                break;
            case STRING:
                validationPattern = "/./";
                break;
            default:
                break;
        }
        return validationPattern;
    }


    public static boolean checkBit16(String binaryString, int bitIndex) {
        // 16 bit kontrolü
        if (binaryString.length() != 16) {
            throw new IllegalArgumentException("Binary string must be 16 bits long.");
        }
        // Belirtilen indeks geçerli mi kontrolü
        if (bitIndex < 0 || bitIndex >= 16) {
            throw new IllegalArgumentException("Bit index must be between 0 and 15 (inclusive).");
        }
        String reversedString = new StringBuilder(binaryString).reverse().toString();
        // İstenilen bitin değerini kontrol etme
        char bitChar = reversedString.charAt(bitIndex);
        return bitChar == '1';
    }


    public static boolean checkBit32(String binaryString, int bitIndex) {
        if (binaryString.length() != 32) {
            throw new IllegalArgumentException("Binary string must be 32 bits long.");
        } else if (bitIndex >= 0 && bitIndex < 32) {
            String reversedString = (new StringBuilder(binaryString)).reverse().toString();
            char bitChar = reversedString.charAt(bitIndex);
            return bitChar == '1';
        } else {
            throw new IllegalArgumentException("Bit index must be between 0 and 31 (inclusive).");
        }
    }


    public static String multiplyAddress(String s, int i) {
        String prefix = s.substring(0, 5);
        String originalAddress = s.substring(5);
        int newAddr = Integer.parseInt(originalAddress) * i;
        //The msg len might increase when multiplied with any number,so we must recalculate its length.
        // how many digits does the old number have?
        int digitCountOld = String.valueOf(Integer.parseInt(originalAddress)).length();
        // how many digits does the new number have?
        int digitCount = String.valueOf(newAddr).length();
        int addToPrefix = digitCount - digitCountOld;
        int len = Integer.parseInt(prefix.substring(0, 2));
        // increase the prefix much as addToPrefix
        String newLen = addZeroIfNeed(len + addToPrefix);
        prefix = newLen + prefix.substring(2, 5);
///////////////////////////////////////////////////////////
        return prefix + String.valueOf(newAddr);
    }


    public static String swap(String str) {
        if (str.length() != 8) {
            throw new IllegalArgumentException("The length of the string must be 8.");
        }
        String first4 = str.substring(0, 4);
        String last4 = str.substring(4, 8);
        return last4 + first4;
    }
}
