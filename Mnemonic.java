/* Mnemonic.java */

/* $Id$ */

/**
 * PAL Machine Simulator: An implementation in Java
 *
 * Copyright (c) 2002, 2003 Philip J. Roberts and Paul A. Hoadley
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the authors nor the names of any other
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Arrays;

/**
 * A class to define constant ints to represent mnemonics, and to
 * provide conversion methods between <code>int</code> and
 * <code>String</code> representations.
 *
 * @version $Revision$
 * @author Philip Roberts &lt;philip.roberts@student.adelaide.edu.au&gt;
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
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
