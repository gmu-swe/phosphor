package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;


public class PhosphorTextifier extends Textifier {
	static String[] MORE_OPCODES;
	static {
		MORE_OPCODES = new String[25];
		MORE_OPCODES[1] = "RAW_INSN";
		MORE_OPCODES[2] = "NOSTORE";
		MORE_OPCODES[3] = "IGNORE";
		MORE_OPCODES[4] = "NO_UNBOX";
		MORE_OPCODES[5] = "NO_LOAD_TAINT";
		MORE_OPCODES[6] = "GENERATETAITN";
		MORE_OPCODES[7] = "NEXTLOADTAINTED";
		MORE_OPCODES[8] = "NEXTLOADNOTTAINT";
		MORE_OPCODES[9] = "NO AUTOBOX";
		MORE_OPCODES[10] = "AUTOBOX";
		MORE_OPCODES[11] = "BOX_JMP";
		MORE_OPCODES[12] = "UNBOX_JMP";
		MORE_OPCODES[13] = "IS_TMP_STORE";
		MORE_OPCODES[14] = "BRANCH_START";
		MORE_OPCODES[15] = "BRANCH_END";
		MORE_OPCODES[16] = "FORCE_CTRL_STORE";
		MORE_OPCODES[17] = "FOLLOWED_BY_FRAME";
		MORE_OPCODES[21] = "NEXT_INSN_TAINT_AWARE";
		/*
		 * 
		 * public static final int RAW_INSN = 201; public static final int
		 * NO_TAINT_STORE_INSN = 202; public static final int IGNORE_EVERYTHING
		 * = 203; public static final int NO_TAINT_UNBOX = 204; public static
		 * final int DONT_LOAD_TAINT = 205; public static final int
		 * GENERATETAINTANDSWAP = 206; public static final int
		 * NEXTLOAD_IS_TAINTED = 207; public static final int
		 * NEXTLOAD_IS_NOT_TAINTED = 208; public static final int NEVER_AUTOBOX
		 * = 209; public static final int ALWAYS_AUTOBOX = 210; ublic static
		 * final int IS_TMP_STORE = 213;
		 * 
		 * public static final int BRANCH_START = 214; public static final int
		 * BRANCH_END = 215; public static final int FORCE_CTRL_STORE = 216;
		 * 
		 * public static final int FOLLOWED_BY_FRAME = 217; public static final
		 * int CUSTOM_SIGNAL_1 = 218; public static final int CUSTOM_SIGNAL_2 =
		 * 219; public static final int CUSTOM_SIGNAL_3 = 220;
		 */
	}

	public PhosphorTextifier() {
		super(Opcodes.ASM5);
	}

	@Override
	protected Textifier createTextifier() {
		return new PhosphorTextifier();
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		if (opcode > 200) {
			buf.setLength(0);
			buf.append(tab2).append(MORE_OPCODES[opcode - 200]).append(' ').append(var).append('\n');
			text.add(buf.toString());
		} else
			super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitInsn(int opcode) {
		if (opcode > 200) {
			buf.setLength(0);
			buf.append(tab2).append(MORE_OPCODES[opcode - 200]).append('\n');
			text.add(buf.toString());
		} else
			super.visitInsn(opcode);
	}

	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (opcode > 200) {
			buf.setLength(0);
			buf.append(tab2).append(MORE_OPCODES[opcode - 200]).append(' ').append(opcode == Opcodes.NEWARRAY ? TYPES[operand] : Integer.toString(operand)).append('\n');
			text.add(buf.toString());
		} else
			super.visitIntInsn(opcode, operand);
	}
}
