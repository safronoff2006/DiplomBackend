package utils;

public class EmMarineConvert {

    public static String emHexToEmText(String hexStr) {

        String strRight = hexStr.substring(0, 4);
        String strLeft = hexStr.substring(4, 6);

        strRight = strRight.substring(2, 4) + strRight.substring(0, 2);

        StringBuilder textLeft = new StringBuilder(Long.toString(Long.parseLong(strLeft, 16)));
        while (textLeft.length() < 3)
            textLeft.insert(0, "0");

        StringBuilder textRight = new StringBuilder(Long.toString(Long.parseLong(strRight, 16)));
        while (textRight.length() < 5)
            textRight.insert(0, "0");

        return textLeft + "," + textRight;

    }

    public static String emTextToEmHex(String textStr) {

        int pos_delimiter = textStr.indexOf(",");
        if (pos_delimiter < 0)
            return "";

        String rightStr = textStr.substring(0, pos_delimiter);
        String leftStr = textStr.substring(pos_delimiter + 1);
        long rightLong = Long.parseLong(rightStr);
        long leftLong = Long.parseLong(leftStr);

        StringBuilder rightHEX = new StringBuilder(Long.toHexString(rightLong).toUpperCase());
        StringBuilder leftHex = new StringBuilder(Long.toHexString(leftLong).toUpperCase());
        while (leftHex.length() < 4)
            leftHex.insert(0, "0");

        while (rightHEX.length() < 2)
            rightHEX.insert(0, "0");

        leftHex = new StringBuilder(leftHex.substring(2, 4) + leftHex.substring(0, 2));

        return leftHex + rightHEX.toString() + "00";
    }



}
