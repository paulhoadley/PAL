/* Mnemonic.java */

/* $Id$ */

import java.util.Arrays;

/**
 * A class to define constant ints to represent mnemonics, and to
 * provide conversion methods between int and String representations.
 */
public class Mnemonic {

    /**
     * The heart of the class - an array of mnemonics.
     * NOTE: must be lexicographically ordered.
     */
    private static String[] mnemonicList = {"CAL", "INC", "JIF", "JMP",
                                            "LCI", "LCR", "LCS", "LDA",
                                            "LDI", "LDU", "LDV", "MST",
                                            "OPR", "RDI", "RDR", "REH",
                                            "SIG", "STI", "STO"};

    public static final int CAL = mnemonicToInt("CAL");
    public static final int INC = mnemonicToInt("INC");
    public static final int JIF = mnemonicToInt("JIF");
    public static final int JMP = mnemonicToInt("JMP");
    public static final int LCI = mnemonicToInt("LCI");
    public static final int LCR = mnemonicToInt("LCR");
    public static final int LCS = mnemonicToInt("LCS");
    public static final int LDA = mnemonicToInt("LDA");
    public static final int LDI = mnemonicToInt("LDI");
    public static final int LDU = mnemonicToInt("LDU");
    public static final int LDV = mnemonicToInt("LDV");
    public static final int MST = mnemonicToInt("MST");
    public static final int OPR = mnemonicToInt("OPR");
    public static final int RDI = mnemonicToInt("RDI");
    public static final int RDR = mnemonicToInt("RDR");
    public static final int REH = mnemonicToInt("REH");
    public static final int SIG = mnemonicToInt("SIG");
    public static final int STI = mnemonicToInt("STI");
    public static final int STO = mnemonicToInt("STO");

    public static int mnemonicToInt(String m) {

        return Arrays.binarySearch(mnemonicList, m);
    }

    public static String intToMnemonic(int i) {

        if(i < mnemonicList.length && i >= 0)
            return mnemonicList[i];

        return "XXX";
    }
}
