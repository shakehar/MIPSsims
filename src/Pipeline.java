import java.util.HashMap;

public class Pipeline {

	private int postMemQueue = -1;
	private int postAluQueue = -1;
	private int preMemQueue = -1;
	private int[] preAluQueue = { -1, -1 };
	private int preAluQueueSize = 0;
	private int preAluQueueHead = 0;
	private int preAluQueueTail = 1;
	private int[] preIssueQueue = { -1, -1, -1, -1 };
	private int preIssueQueueHead = 0;
	private int preIssueQueueTail = 0;
	private int preIssueQueueSize = 0;
	private boolean preAluHasCapacity = true;

	private static HashMap<Integer, String> instruction = new HashMap<Integer, String>();

	private static HashMap<String, Boolean> isRegisterRead = new HashMap<>(32);
	private static HashMap<String, Boolean> isRegisterWrite = new HashMap<>(32);

	static int waitingInstruction;
	static int executedInstruction;

	public void MoveItems(int instOne, int instTwo) {
		PostMemMove();
		PostALUMove();
		PreMemMove();
		PreAluMove();
		PreIssueMove();
		PreIssueInsert(instOne, instTwo);

	}

	private void PreIssueInsert(int instOne, int instTwo) {
		if (preIssueQueueSize < 3) {
			Ops op1 = GetOpType(instOne);
			if (op1.equals(Ops.BEQ) || op1.equals(Ops.BGTZ)) {
				// TODO set waitingInstruction
			}
			if (op1.equals(Ops.BREAK)) {
				// TODO break this shit
			}
			preIssueQueue[preIssueQueueTail] = instOne;
			preIssueQueueTail = (preIssueQueueTail + 1) % 4;
			preIssueQueue[preIssueQueueTail] = instTwo;
			preIssueQueueTail = (preIssueQueueTail + 1) % 4;
		}

	}

	/**
	 * Move from PreIssue to PreALU. Check for Hazards here Check preAlucycleWas
	 * free last time
	 */
	private void PreIssueMove() {
		if (preAluHasCapacity) {
			while (preIssueQueueSize > 0 && preAluQueueSize < 2) {

				if (!HasHazard(preIssueQueue[preIssueQueueHead])) {

					preAluQueue[preAluQueueTail] = preIssueQueue[preIssueQueueHead];
					ReserveResources(preIssueQueue[preIssueQueueHead]);
					preAluQueueTail = (preAluQueueTail + 1) % 2;
					preIssueQueue[preAluQueueHead] = -1;
					preIssueQueueHead = (preIssueQueueHead + 1) % 4;
					preIssueQueueSize -= 1;
					preAluQueueSize += 1;
				}
			}
		}

	}

	private void PreAluMove() {
		if (preAluQueueSize > 0) {
			if (preAluQueueSize == 2) {
				preAluHasCapacity = false;
			} else {
				preAluHasCapacity = true;
			}
			Ops op = GetOpType(preAluQueue[preAluQueueHead]);
			if (op.equals(OpTypess.CALC) && postAluQueue == -1) {
				postAluQueue = preAluQueue[preAluQueueHead];
				preAluQueue[preAluQueueHead] = -1;
				preAluQueueHead = (preAluQueueHead + 1) % 2;
				preAluQueueSize -= 1;
			} else if (op.equals(OpTypess.LDSW) && preMemQueue == -1) {
				preMemQueue = preAluQueue[preAluQueueHead];
				preAluQueue[preAluQueueHead] = -1;
				preAluQueueHead = (preAluQueueHead + 1) % 2;
				preAluQueueSize -= 1;
			}
		}
	}

	private void PreMemMove() {
		if (preMemQueue > 0) {
			if (postMemQueue == -1) {
				postMemQueue = preMemQueue;
				preMemQueue = -1;
			}
		}

	}

	private void PostALUMove() {
		if (postAluQueue > 0) {
			WriteBack(postAluQueue);
			postAluQueue = -1;
		}

	}

	private void PostMemMove() {
		if (postMemQueue > 0) {
			WriteBack(postMemQueue);
			postMemQueue = -1;
		}

	}

	private void WriteBack(int instructionCode) {
		// TODO Auto-generated method stub

	}

	private Ops GetOpType(int instructionCode) {
		String[] input = GetRegisters(instruction.get(instructionCode));
		return Ops.valueOf(input[0]);
	}

	/**
	 * Add registers to read or write hashmap
	 * 
	 * @param instructionCode
	 */
	private void ReserveResources(int instructionCode) {

		String[] input = GetRegisters(instruction.get(instructionCode));
		if (input[1] != null) {
			isRegisterWrite.put(input[0], true);
		}
		for (int i = 2; i < 4; i++) {
			if (input[i] != null) {
				isRegisterRead.put(input[i], true);
			}
		}

	}

	private String[] GetRegisters(String instruction) {

		String[] output = new String[4];

		String[] ins = instruction.split(" ");
		Ops ops = Ops.valueOf(ins[0]);

		output[0] = ops.toString();

		switch (ops) {
		case ADD:
		case AND:
		case MUL:
		case NOR:
		case OR:
		case SUB:
		case XOR:
			output[1] = ins[1].replace(",", "");
			output[2] = ins[2].replace(",", "");
			output[3] = ins[3];
			break;
		case ADDI:
		case ANDI:
		case XORI:
		case ORI:
			output[1] = ins[1].replace(",", "");
			output[2] = ins[2].replace(",", "");
			break;
		case BEQ:
			output[2] = ins[1].replace(",", "");
			output[3] = ins[2].replace(",", "");
			break;
		case BGTZ:
		case SW:
			output[2] = ins[1].replace(",", "");
			break;
		case BREAK:
			break;
		case J:
			// ii = ins[1].replace("#", "");
			break;
		case LW:
			output[1] = ins[1].replace(",", "");
			break;
		default:
			break;

		}

		return output;
	}

	private boolean HasHazard(int instructionCode) {
		// RAW -WAW Hazard
		String[] input = GetRegisters(instruction.get(instructionCode));
		if (input[1] != null) {
			if (isRegisterWrite.get(input[1])) {
				return true;
			}
		}
		if (input[2] != null) {
			if (isRegisterWrite.get(input[2])) {
				return true;
			}
		}
		if (input[3] != null) {
			if (isRegisterWrite.get(input[3])) {
				return true;
			}
		}
		// WAR Hazard
		if (input[1] != null) {
			if (isRegisterRead.get(input[1])) {
				return true;
			}
		}
		return false;
	}
}

enum Ops {
	J, BEQ, BGTZ, BREAK, SW, LW, ADD, SUB, MUL, AND, OR, XOR, NOR, ADDI, ANDI, ORI, XORI;
}

enum OpTypess {
	Branch, LDSW, CALC;
}
