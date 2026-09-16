package net.logicsquad.pal;

/**
 * The second argument to an instruction, parsed once by the loader.
 *
 * <p>
 * Which kind an instruction takes is fixed by its {@link Mnemonic}, so the
 * loader knows what to parse and can reject a mismatch with a line number
 * before the program runs. That is what lets the interpreter assume: it no
 * longer opens each instruction with a check that the operand is the type that
 * mnemonic has always required.
 *
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
sealed interface Operand permits IntOperand, RealOperand, StringOperand {
}
