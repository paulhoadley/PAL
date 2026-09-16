package net.logicsquad.pal;

import java.util.List;

/**
 * A loaded object file: the instructions, and the name to report faults
 * against.
 *
 * <p>
 * The name travels with the program because a diagnostic needs it, and because
 * a static field holding it, which is how the machine used to know, made the
 * name of the file being run a property of the whole JVM.
 *
 * @param name
 *            what to call this program in a diagnostic, usually its filename
 * @param instructions
 *            the instructions, in the order the loader read them
 * @author Paul Hoadley &lt;paulh@logicsquad.net&gt;
 */
record Program(String name, List<Instruction> instructions) {
}
