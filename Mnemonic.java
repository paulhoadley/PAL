/* Mnemonic.java */

/* $Id$ */

/**
 * A class to define constant ints to represent mnemonics, and to
 * provide conversion methods between int and String representations.
 */
public class Mnemonic {
    
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
	if (m.equals("CAL")) {
	    return CAL;
	} else if (m.equals("INC")) {
	    return INC;
	} else if (m.equals("JIF")) {
	    return JIF;
	} else if (m.equals("JMP")) {
	    return JMP;
	} else if (m.equals("LCI")) {
	    return LCI;
	} else if (m.equals("LCR")) {
	    return LCR;
	} else if (m.equals("LCS")) {
	    return LCS;
	} else if (m.equals("LDA")) {
	    return LDA;
	} else if (m.equals("LDI")) {
	    return LDI;
	} else if (m.equals("LDU")) {
	    return LDU;
	} else if (m.equals("LDV")) {
	    return LDV;
	} else if (m.equals("MST")) {
	    return MST;
	} else if (m.equals("OPR")) {
	    return OPR;
	} else if (m.equals("RDI")) {
	    return RDI;
	} else if (m.equals("RDR")) {
	    return RDR;
	} else if (m.equals("REH")) {
	    return REH;
	} else if (m.equals("SIG")) {
	    return SIG;
	} else if (m.equals("STI")) {
	    return STI;
	} else if (m.equals("STO")) {
	    return STO;
	} else {
	    return -1;
	}
    }

    public static String intToMnemonic(int i) {
	switch (i) {
	case CAL:
	    return "CAL";
	case INC:
	    return "INC";
	case JIF:
	    return "JIF";
	case JMP:
	    return "JMP";
	case LCI:
	    return "LCI";
	case LCR:
	    return "LCR";
	case LCS:
	    return "LCS";
	case LDA:
	    return "LDA";
	case LDI:
	    return "LDI";
	case LDU:
	    return "LDU";
	case LDV:
	    return "LDV";
	case MST:
	    return "MST";
	case OPR:
	    return "OPR";
	case RDI:
	    return "RDI";
	case RDR:
	    return "RDR";
	case REH:
	    return "REH";
	case SIG:
	    return "SIG";
	case STI:
	    return "STI";
	case STO:
	    return "STO";
	default:
	    return "XXX";
	}
    }
}
