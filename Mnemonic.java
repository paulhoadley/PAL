/* Mnemonic.java */

/* $Id$ */

import java.util.Arrays;

/**
 * A class to define constant ints to represent mnemonics, and to
 * provide conversion methods between <code>int</code> and
 * <code>String</code> representations.
 *
 * @version $Revision$
 */
public class Mnemonic {
    /**
     * A lexicographically ordered array of the available mnemonics.
     */
    private static final String[] mnemonicList = {"CAL", "INC", "JIF", "JMP",
                                                  "LCI", "LCR", "LCS", "LDA",
                                                  "LDI", "LDU", "LDV", "MST",
                                                  "OPR", "RDI", "RDR", "REH",
                                                  "SIG", "STI", "STO"};

    // Important: The following constants must correspond to their
    // index in the "mnemonicList" array.

    /** Constant to represent the <code>CAL</code> mnemonic. */
    public static final int CAL = 0;
    /** Constant to represent the <code>INC</code> mnemonic. */
    public static final int INC = 1;
    /** Constant to represent the <code>JIF</code> mnemonic. */
    public static final int JIF = 2;
    /** Constant to represent the <code>JMP</code> mnemonic. */
    public static final int JMP = 3;
    /** Constant to represent the <code>LCI</code> mnemonic. */
    public static final int LCI = 4;
    /** Constant to represent the <code>LCR</code> mnemonic. */
    public static final int LCR = 5;
    /** Constant to represent the <code>LCS</code> mnemonic. */
    public static final int LCS = 6;
    /** Constant to represent the <code>LDA</code> mnemonic. */
    public static final int LDA = 7;
    /** Constant to represent the <code>LDI</code> mnemonic. */
    public static final int LDI = 8;
    /** Constant to represent the <code>LDU</code> mnemonic. */
    public static final int LDU = 9;
    /** Constant to represent the <code>LDV</code> mnemonic. */
    public static final int LDV = 10;
    /** Constant to represent the <code>MST</code> mnemonic. */
    public static final int MST = 11;
    /** Constant to represent the <code>OPR</code> mnemonic. */
    public static final int OPR = 12;
    /** Constant to represent the <code>RDI</code> mnemonic. */
    public static final int RDI = 13;
    /** Constant to represent the <code>RDR</code> mnemonic. */
    public static final int RDR = 14;
    /** Constant to represent the <code>REH</code> mnemonic. */
    public static final int REH = 15;
    /** Constant to represent the <code>SIG</code> mnemonic. */
    public static final int SIG = 16;
    /** Constant to represent the <code>STI</code> mnemonic. */
    public static final int STI = 17;
    /** Constant to represent the <code>STO</code> mnemonic. */
    public static final int STO = 18;

    /**
     * Returns the <code>int</code> representing the supplied
     * mnemonic.
     *
     * @param m A <code>String</code> containing the mnemonic.
     * @return An <code>int</code> representing the supplied mnemonic.
     */
    public static int mnemonicToInt(String m) {
        return Arrays.binarySearch(mnemonicList, m);
    }

    /**
     * Returns the <code>String</code> corresponding to the supplied
     * <code>int</code> code.
     *
     * @param i An <code>int</code> representing a mnemonic.
     * @return The corresponding mnemonic.
     */
    public static String intToMnemonic(int i) {
        if(i < mnemonicList.length && i >= 0)
            return mnemonicList[i];
        return "XXX";
    }
}
