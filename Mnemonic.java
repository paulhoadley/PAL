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

    /**
     * Be sure to change these constants when the mnemonics change.
     */
    public static final int CAL = 0;
    public static final int INC = 1;
    public static final int JIF = 2;
    public static final int JMP = 3;
    public static final int LCI = 4;
    public static final int LCR = 5;
    public static final int LCS = 6;
    public static final int LDA = 7;
    public static final int LDI = 8;
    public static final int LDU = 9;
    public static final int LDV = 10;
    public static final int MST = 11;
    public static final int OPR = 12;
    public static final int RDI = 13;
    public static final int RDR = 14;
    public static final int REH = 15;
    public static final int SIG = 16;
    public static final int STI = 17;
    public static final int STO = 18;

    public static int mnemonicToInt(String m) {

        return Arrays.binarySearch(mnemonicList, m);
    }

    public static String intToMnemonic(int i) {

        if(i < mnemonicList.length && i >= 0)
            return mnemonicList[i];

        return "XXX";
    }
}
