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
    public static final int INC = mnemonicToInt("CAL");
    public static final int JIF = mnemonicToInt("CAL");
    public static final int JMP = mnemonicToInt("CAL");
    public static final int LCI = mnemonicToInt("CAL");
    public static final int LCR = mnemonicToInt("CAL");
    public static final int LCS = mnemonicToInt("CAL");
    public static final int LDA = mnemonicToInt("CAL");
    public static final int LDI = mnemonicToInt("CAL");
    public static final int LDU = mnemonicToInt("CAL");
    public static final int LDV = mnemonicToInt("CAL");
    public static final int MST = mnemonicToInt("CAL");
    public static final int OPR = mnemonicToInt("CAL");
    public static final int RDI = mnemonicToInt("CAL");
    public static final int RDR = mnemonicToInt("CAL");
    public static final int REH = mnemonicToInt("CAL");
    public static final int SIG = mnemonicToInt("CAL");
    public static final int STI = mnemonicToInt("CAL");
    public static final int STO = mnemonicToInt("CAL");

    public static int mnemonicToInt(String m) {

        return Arrays.binarySearch(mnemonicList, m);
    }

    public static String intToMnemonic(int i) {

        if(i < mnemonicList.length && i >= 0)
            return mnemonicList[i];

        return "XXX";
    }
}
