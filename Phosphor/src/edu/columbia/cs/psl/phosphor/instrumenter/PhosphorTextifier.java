package edu.columbia.cs.psl.phosphor.instrumenter;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;

import edu.columbia.cs.psl.phosphor.instrumenter.analyzer.TaggedValue;


public class PhosphorTextifier extends Textifier {
	static String[] MORE_OPCODES;
	static {
		MORE_OPCODES = new String[25];
		MORE_OPCODES[1] = "RAW_INSN";
		MORE_OPCODES[2] = "NOSTORE";
		MORE_OPCODES[3] = "IGNORE";
		MORE_OPCODES[4] = "NEXT_FORCE_TAINT";
		MORE_OPCODES[5] = "DUP_TAINT_TO_0";
		MORE_OPCODES[6] = "DUP_TAINT_TO_1";
		MORE_OPCODES[7] = "DUP_TAINT_TO_2";
		MORE_OPCODES[8] = "DUP_TAINT_TO_3";
		MORE_OPCODES[9] = "NO AUTOBOX";
		MORE_OPCODES[10] = "AUTOBOX";
		MORE_OPCODES[11] = "BOX_JMP";
		MORE_OPCODES[12] = "UNBOX_JMP";
		MORE_OPCODES[13] = "IS_TMP_STORE";
		MORE_OPCODES[14] = "BRANCH_START";
		MORE_OPCODES[15] = "BRANCH_END";
		MORE_OPCODES[16] = "FORCE_CTRL_STORE";
		MORE_OPCODES[17] = "FOLLOWED_BY_FRAME";
		MORE_OPCODES[21] = "LOOP_HEADER";
//		MORE_OPCODES[22] = "LOOP_HEADER";
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

	public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack) {
		buf.setLength(0);
		buf.append(ltab);
		buf.append("FRAME ");
		switch (type) {
		case Opcodes.F_NEW:
		case Opcodes.F_FULL:
			buf.append("FULL [");
			appendFrameTypes(nLocal, local);
			buf.append("] [");
			appendFrameTypes(nStack, stack);
			buf.append(']');
			break;
		case Opcodes.F_APPEND:
			buf.append("APPEND [");
			appendFrameTypes(nLocal, local);
			buf.append(']');
			break;
		case Opcodes.F_CHOP:
			buf.append("CHOP ").append(nLocal);
			break;
		case Opcodes.F_SAME:
			buf.append("SAME");
			break;
		case Opcodes.F_SAME1:
			buf.append("SAME1 ");
			appendFrameTypes(1, stack);
			break;
		}
		buf.append('\n');
		text.add(buf.toString());
	}

	private void appendFrameTypes(final int n, final Object[] o) {
		for (int i = 0; i < n; ++i) {
			if (i > 0) {
				buf.append(' ');
			}
			if (o[i] instanceof TaggedValue) {
				buf.append("TAGGED");
				if (((TaggedValue) o[i]).v instanceof String) {
					String desc = (String) ((TaggedValue) o[i]).v;

					if (desc.startsWith("[")) {
						appendDescriptor(FIELD_DESCRIPTOR, desc);
					} else {
						appendDescriptor(INTERNAL_NAME, desc);
					}
				}
				else
				{
					switch (((Integer) ((TaggedValue) o[i]).v ).intValue()) {
					case 0:
						appendDescriptor(FIELD_DESCRIPTOR, "T");
						break;
					case 1:
						appendDescriptor(FIELD_DESCRIPTOR, "I");
						break;
					case 2:
						appendDescriptor(FIELD_DESCRIPTOR, "F");
						break;
					case 3:
						appendDescriptor(FIELD_DESCRIPTOR, "D");
						break;
					case 4:
						appendDescriptor(FIELD_DESCRIPTOR, "J");
						break;
					case 5:
						appendDescriptor(FIELD_DESCRIPTOR, "N");
						break;
					case 6:
						appendDescriptor(FIELD_DESCRIPTOR, "U");
						break;
					}
				}

			} else if (o[i] instanceof String) {
				String desc = (String) o[i];
				if (desc.startsWith("[")) {
					appendDescriptor(FIELD_DESCRIPTOR, desc);
				} else {
					appendDescriptor(INTERNAL_NAME, desc);
				}
			} else if (o[i] instanceof Integer) {
				switch (((Integer) o[i]).intValue()) {
				case 0:
					appendDescriptor(FIELD_DESCRIPTOR, "T");
					break;
				case 1:
					appendDescriptor(FIELD_DESCRIPTOR, "I");
					break;
				case 2:
					appendDescriptor(FIELD_DESCRIPTOR, "F");
					break;
				case 3:
					appendDescriptor(FIELD_DESCRIPTOR, "D");
					break;
				case 4:
					appendDescriptor(FIELD_DESCRIPTOR, "J");
					break;
				case 5:
					appendDescriptor(FIELD_DESCRIPTOR, "N");
					break;
				case 6:
					appendDescriptor(FIELD_DESCRIPTOR, "U");
					break;
				}
			} else {
				appendLabel((Label) o[i]);
			}
		}
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
