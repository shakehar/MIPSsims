/* On my honor, I have neither given nor received unauthorized aid on this assignment */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class MIPSsim {

	public static int memoryLocation = 124;
	public static int breakLocation = 0;
	public static List<String> disassemblyOutput = new ArrayList<>();
	public static List<String> simulationOutput = new ArrayList<>();
	public static Hashtable<String, Integer> registers = new Hashtable<String, Integer>();
	public static Hashtable<Integer, Integer> memory = new Hashtable<Integer, Integer>();
	public static Hashtable<Integer, String> instructions = new Hashtable<Integer, String>();
	static boolean isBreak = false;

	/*************************************************************************/
	private static int postMemQueue = -1;
	private static int postAluQueue = -1;
	private static int preMemQueue = -1;
	private static int[] preAluQueue = { -1, -1 };
	private static int preAluQueueSize = 0;
	private static int preAluQueueHead = 0;
	private static int preAluQueueTail = 1;
	private static ArrayList<Integer> preIssueQueue = new ArrayList<>(4);
	private static int preIssueQueueSize = 0;
	private static boolean preAluHasCapacity = true;

	private static HashMap<String, Boolean> isRegisterRead = new HashMap<>(32);
	private static HashMap<String, Boolean> isRegisterWrite = new HashMap<>(32);

	static int waitingInstruction = -1;
	static int executedInstruction = -1;

	static String branchDependency1 = null;
	static String branchDependency2 = null;

	/*************************************************************************/

	public static void main(String[] args) throws IOException {
		String filename = args[0];
		List<Long> input = ReadFile(filename);
		DisAssembler(input);
		InitializeRegisters();
		InitializeInstructions();
		InitializeMemory();
		Simulator();
		PrintFile(disassemblyOutput, "generated_disassembly.txt");
		PrintFile(simulationOutput, "generated_simulation.txt");
	}

	private static void PrintFile(List<String> output, String filename) throws IOException {
		File file = new File(filename);
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);

		for (String string : output) {
			bw.write(string + "\n");
		}

		bw.close();
	}

	private static void InitializeInstructions() {
		for (int i = 0; i < disassemblyOutput.size(); i++) {
			String[] ins = disassemblyOutput.get(i).split("\t");
			instructions.put(Integer.parseInt(ins[1]), ins[2]);
			if (ins[2].equals("BREAK"))
				break;
		}
	}

	private static void InitializeMemory() {
		int j = 0;
		for (int i = breakLocation; i <= memoryLocation; i += 4) {
			memory.put(i, Integer.parseInt(disassemblyOutput.get(((breakLocation - 128) / 4) + j).split("\t")[2]));
			j++;
		}

	}

	private static void InitializeRegisters() {
		for (int i = 0; i < 33; i++) {
			registers.put("R" + i, 0);
		}
	}

	private static void Simulator() {
		int cycle = 1;
		int counter = 128;
		while (true) {
			String instruction = instructions.get(counter);
			String simulation = "--------------------\nCycle:" + cycle;
			simulation += "\n\nIF Unit:";
			simulation += "\n\tWaiting Instruction:" + instructions.get(waitingInstruction);
			simulation += "\n\tExecuted Instruction:" + instructions.get(executedInstruction);
			simulation += "\nPre-Issue Queue:";
			simulation += "\n\tEntry 0:[" + instructions.get(preIssueQueue.get(0)) + "]";
			simulation += "\n\tEntry 1:[" + instructions.get(preIssueQueue.get(1)) + "]";
			simulation += "\n\tEntry 2:[" + instructions.get(preIssueQueue.get(2)) + "]";
			simulation += "\n\tEntry 3:[" + instructions.get(preIssueQueue.get(3)) + "]";
			simulation += "\nPre-ALU Queue:";
			simulation += "\n\tEntry 0:[" + instructions.get(preAluQueue[preAluQueueHead]) + "]";
			simulation += "\n\tEntry 1:[" + instructions.get(preAluQueue[preAluQueueTail]) + "]";
			simulation += "\nPre-MEM Queue:" + instructions.get(preMemQueue);
			simulation += "\nPost-MEM Queue:" + instructions.get(postMemQueue);
			simulation += "\nPost-ALU Queue:" + instructions.get(postAluQueue);
			simulation += "\n\nRegisters";

			counter = Pipeline(counter);
			for (int i = 0; i <= 24; i += 8) {
				String regNo = String.valueOf(i);
				if (regNo.length() == 1)
					regNo = "0" + regNo;
				simulation += "\nR" + regNo + ":";
				for (int j = i; j < i + 8; j++) {
					simulation += "\t" + registers.get("R" + j);
				}
			}
			simulation += "\n\nData";
			for (int i = breakLocation; i <= memoryLocation; i += 32) {
				simulation += "\n" + i + ":";
				for (int j = i; j < i + 32; j += 4) {
					if (memory.get(j) != null)
						simulation += "\t" + memory.get(j);
				}
			}
			if (!instruction.equals("BREAK"))
				simulation += "\n";
			System.out.println(simulation);
			simulationOutput.add(simulation);
			if (instruction.equals("BREAK"))
				break;
			cycle++;
		}
	}

	private static int Decode(String instruction, int counter) {
		String[] ins = instruction.split(" ");
		Ops ops = Ops.valueOf(ins[0]);
		String rd, rs, rt, iv, ii;
		switch (ops) {
		case ADD:
			rd = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			rt = ins[3];
			registers.put(rd, registers.get(rs) + registers.get(rt));
			counter += 4;
			break;
		case ADDI:
			rt = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			iv = ins[3].replace("#", "");
			registers.put(rt, Integer.parseInt(iv) + registers.get(rs));
			counter += 4;
			break;
		case AND:
			rd = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			rt = ins[3];
			registers.put(rd, registers.get(rt) & registers.get(rs));
			counter += 4;
			break;
		case ANDI:
			rt = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			iv = ins[3].replace("#", "");
			registers.put(rt, registers.get(rs) & Integer.parseInt(iv));
			counter += 4;
			break;
		case BEQ:
			rs = ins[1].replace(",", "");
			rt = ins[2].replace(",", "");
			ii = ins[3].replace("#", "");
			if (registers.get(rt) == registers.get(rs)) {
				counter = counter + 4 + Integer.parseInt(ii);
			} else
				counter += 4;
			break;
		case BGTZ:
			rs = ins[1].replace(",", "");
			ii = ins[2].replace("#", "");
			if (registers.get(rs) > 0) {
				counter = counter + 4 + Integer.parseInt(ii);
			} else
				counter += 4;
			break;
		case BREAK:
			counter += 4;
			break;
		case J:
			ii = ins[1].replace("#", "");
			counter = Integer.parseInt(ii);
			break;
		case LW:
			rt = ins[1].replace(",", "");
			iv = ins[2].split("\\(")[0];
			ii = ins[2].split("\\(")[1].replace(")", "");
			registers.put(rt, memory.get(Integer.parseInt(iv) + registers.get(ii)));
			counter += 4;
			break;
		case MUL:
			rd = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			rt = ins[3];
			registers.put(rd, registers.get(rs) * registers.get(rt));
			counter += 4;
			break;
		case NOR:
			rd = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			rt = ins[3];
			registers.put(rd, ~(registers.get(rs) | registers.get(rt)));
			counter += 4;
			break;
		case OR:
			rd = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			rt = ins[3];
			registers.put(rd, (registers.get(rs) | registers.get(rt)));
			counter += 4;
			break;
		case ORI:
			rt = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			iv = ins[3].replace("#", "");
			registers.put(rt, registers.get(rs) | Integer.parseInt(iv));
			counter += 4;
			break;
		case SUB:
			rd = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			rt = ins[3];
			registers.put(rd, (registers.get(rs) - registers.get(rt)));
			counter += 4;
			break;
		case SW:
			rt = ins[1].replace(",", "");
			iv = ins[2].split("\\(")[0];
			ii = ins[2].split("\\(")[1].replace(")", "");
			memory.put(Integer.parseInt(iv) + registers.get(ii), registers.get(rt));
			counter += 4;
			break;
		case XOR:
			rd = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			rt = ins[3];
			registers.put(rd, (registers.get(rs) ^ registers.get(rt)));
			counter += 4;
			break;
		case XORI:
			rt = ins[1].replace(",", "");
			rs = ins[2].replace(",", "");
			iv = ins[3].replace("#", "");
			registers.put(rt, registers.get(rs) ^ Integer.parseInt(iv));
			counter += 4;
			break;
		default:
			break;

		}
		return counter;
	}

	private static void DisAssembler(List<Long> input) {
		for (Long word : input) {
			memoryLocation += 4;
			if (!isBreak) {
				Category cat = GetCategory(word);
				ProcessInstructions(cat, word);
			} else
				TwosComplement(word);
		}
	}

	private static void TwosComplement(long word) {
		String stringWord = Long.toBinaryString(word);

		while (stringWord.length() < 32)
			stringWord = "0" + stringWord;
		String disassembly = stringWord + "\t" + memoryLocation;
		if (word >> 31 != 0) {
			word = (long) Long.valueOf(Long.toHexString(word), 16).intValue();
		}
		disassembly += "\t" + word;
		System.out.println(disassembly);
		disassemblyOutput.add(disassembly);

	}

	private static void ProcessInstructions(Category cat, long word) {
		switch (cat) {
		case category1:
			ProcessCategory1(word);
			break;
		case category2:
			ProcessCategory2(word);
			break;
		case category3:
			ProcessCategory3(word);
			break;
		}

	}

	private static void ProcessCategory3(long word) {
		long rs, rt, iv;
		Category3OPS op;
		rs = ((word >> 24) & 0b11111);
		rt = (word >> 19) & 0b11111;
		op = Category3OPS.fromByte((int) ((word >> 16) & 0b111));
		iv = (word & 0b1111111111111111);
		String disassembly = Long.toBinaryString(word) + "\t" + memoryLocation + "\t" + op.toString() + " "
				+ RegisterName(rt) + ", " + RegisterName(rs) + ", " + HashValue(iv);
		System.out.println(disassembly);
		disassemblyOutput.add(disassembly);
	}

	private static String HashValue(long iv) {
		return "#" + iv;
	}

	private static void ProcessCategory2(long word) {
		long rs, rt, rd;
		Category2OPS op;
		rs = ((word >> 24) & 0b11111);
		rt = ((word >> 19) & 0b11111);
		op = Category2OPS.fromByte((int) ((word >> 16) & 0b111));
		rd = ((word >> 11) & 0b11111);

		String disassembly = Long.toBinaryString(word) + "\t" + memoryLocation + "\t" + op.toString() + " "
				+ RegisterName(rd) + ", " + RegisterName(rs) + ", " + RegisterName(rt);
		System.out.println(disassembly);
		disassemblyOutput.add(disassembly);
	}

	private static String RegisterName(long rd) {
		return "R" + rd;
	}

	private static void ProcessCategory1(long word) {

		long rs, rt, ii, delaySlot;
		Category1OPS op;
		String stringWord = Long.toBinaryString(word);
		// ensure that length of word is 32
		while (stringWord.length() < 32)
			stringWord = "0" + stringWord;
		String disassembly = stringWord + "\t" + memoryLocation + "\t";
		op = Category1OPS.fromByte((int) ((word >> 26) & 0b111));
		switch (op) {
		case BEQ:
			rs = (word >> 21) & 0b11111;
			rt = (word >> 16) & 0b11111;
			delaySlot = (((memoryLocation + 4) >> 28) << 28);
			ii = (word & 0b1111111111111111) << 2;
			disassembly += op.toString() + " " + RegisterName(rs) + ", " + RegisterName(rt) + ", " + HashValue(ii);
			break;
		case BGTZ:
			rs = (word >> 21) & 0b11111;
			ii = (word & 0b1111111111111111) << 2;
			disassembly += op.toString() + " " + RegisterName(rs) + ", " + HashValue(ii);
			break;
		case BREAK:
			disassembly += op.toString();
			isBreak = true;
			breakLocation = memoryLocation + 4;
			break;
		case J:
			ii = (word << 2) & 0b11111111111111111111111111;
			delaySlot = (((memoryLocation + 4) >> 28) << 28);
			ii = ii | delaySlot;
			disassembly += op.toString() + " " + HashValue(ii);
			break;
		case LW:
			rs = (word >> 21) & 0b11111;
			rt = (word >> 16) & 0b11111;
			ii = word & 0b1111111111111111;
			disassembly += op.toString() + " " + RegisterName(rt) + ", " + ii + "(" + RegisterName(rs) + ")";
			break;
		case SW:
			rs = (word >> 21) & 0b11111;
			rt = (word >> 16) & 0b11111;
			ii = word & 0b1111111111111111;
			disassembly += op.toString() + " " + RegisterName(rt) + ", " + ii + "(" + RegisterName(rs) + ")";
			break;
		default:
			break;

		}
		System.out.println(disassembly);
		disassemblyOutput.add(disassembly);

	}

	private static Category GetCategory(Long word) {
		Category cat = null;
		int catt = (int) ((word >> 29) & 0b111);

		if (catt == 0b000) {
			cat = Category.category1;
		}
		if (catt == 0b110) {
			cat = Category.category2;
		}
		if (catt == 0b111) {
			cat = Category.category3;
		}
		return cat;
	}

	private static List<Long> ReadFile(String filename) {
		BufferedReader br = null;
		List<Long> arrays = new ArrayList<Long>();
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(filename));

			while ((sCurrentLine = br.readLine()) != null) {
				// System.out.println(sCurrentLine);
				Long b = Long.parseLong(sCurrentLine, 2);
				arrays.add(b);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return arrays;
	}

	enum Category {
		category1, category2, category3;
	}

	enum Category1OPS {

		J(0b000), BEQ(0b010), BGTZ(0b100), BREAK(0b101), SW(0b110), LW(0b111);
		private int numVal;

		Category1OPS(int numVal) {
			this.numVal = numVal;
		}

		public static Category1OPS fromByte(int val) {
			for (Category1OPS cat : values()) {
				if (cat.numVal == val)
					return cat;
			}
			return null;
		}

		public int getNumVal() {
			return numVal;
		}
	}

	enum Category2OPS {
		ADD(0b000), SUB(0b001), MUL(0b010), AND(0b011), OR(0b100), XOR(0b101), NOR(0b110);
		private int numVal;

		Category2OPS(int numVal) {
			this.numVal = numVal;
		}

		public static Category2OPS fromByte(int val) {
			for (Category2OPS cat : values()) {
				if (cat.numVal == val)
					return cat;
			}
			return null;
		}

		public int getNumVal() {
			return numVal;
		}
	}

	enum Category3OPS {
		ADDI(0b000), ANDI(0b001), ORI(0b010), XORI(0b011);
		private int numVal;

		Category3OPS(int numVal) {
			this.numVal = numVal;
		}

		public static Category3OPS fromByte(int val) {
			for (Category3OPS cat : values()) {
				if (cat.numVal == val)
					return cat;
			}
			return null;
		}

		public int getNumVal() {
			return numVal;
		}
	}

	enum Ops {
		J, BEQ, BGTZ, BREAK, SW, LW, ADD, SUB, MUL, AND, OR, XOR, NOR, ADDI, ANDI, ORI, XORI;
	}

	/*******************************************************************************************************************************/

	public static int Pipeline(int counter) {
		PostMemMove();
		PostALUMove();
		PreMemMove();
		PreAluMove();
		PreIssueMove();
		int newCounter = PreIssueInsert(counter);
		return newCounter;
	}

	private static int PreIssueInsert(int instOne) {
		int nextCounter = instOne;
		int instTwo = instOne + 4;

		if (waitingInstruction == -1 && executedInstruction != -1) {

			String[] ins = (instructions.get(executedInstruction).split(" "));
			Ops ops = Ops.valueOf(ins[0]);
			String rs, rt, ii;
			switch (ops) {
			case BEQ:
				rs = ins[1].replace(",", "");
				rt = ins[2].replace(",", "");
				ii = ins[3].replace("#", "");
				if (registers.get(rt) == registers.get(rs)) {
					nextCounter = nextCounter + 4 + Integer.parseInt(ii);
				} else
					nextCounter += 4;
				break;
			case BGTZ:
				rs = ins[1].replace(",", "");
				ii = ins[2].replace("#", "");
				if (registers.get(rs) > 0) {
					nextCounter = nextCounter + 4 + Integer.parseInt(ii);
				} else
					nextCounter += 4;
				break;
			default:
				break;
			}

			executedInstruction = -1;
		}
		if (waitingInstruction != -1) {
			if (branchDependency1 != null && !isRegisterWrite.get(branchDependency1)) {
				branchDependency1 = null;
			}
			if (branchDependency2 != null && !isRegisterWrite.get(branchDependency2)) {
				branchDependency2 = null;
			}
			if (branchDependency1 == null && branchDependency2 == null) {
				executedInstruction = waitingInstruction;
				waitingInstruction = -1;
			}
		} else if (preIssueQueueSize < 3) {
			Ops op1 = GetOpType(instOne);
			Ops op2 = null;
			if (instructions.contains(instTwo)) {
				op2 = GetOpType(instTwo);
			}
			if (op1.equals(Ops.BEQ) || op1.equals(Ops.BGTZ)) {
				String[] input = GetRegisters(instructions.get(instOne));
				if (isRegisterWrite.get(input[2])) {
					branchDependency1 = input[2];
				}
				if (isRegisterWrite.get(input[3])) {
					branchDependency2 = input[3];
				}
				waitingInstruction = instOne;
				nextCounter = instOne + 4;
			} else if (op1.equals(Ops.J)) {
				String ii = instructions.get(instOne).split(" ")[1].replace("#", "");
				nextCounter = Integer.parseInt(ii);
			} else if (op1.equals(Ops.BREAK)) {
				return nextCounter;
			}
			if (op2 != null) {
				if (op2.equals(Ops.BEQ) || op2.equals(Ops.BGTZ)) {
					String[] input = GetRegisters(instructions.get(instTwo));
					if (isRegisterWrite.get(input[2])) {
						branchDependency1 = input[2];
					}
					if (isRegisterWrite.get(input[3])) {
						branchDependency2 = input[3];
					}
					waitingInstruction = instTwo;
					nextCounter = instTwo + 4;
				} else if (op2.equals(Ops.J)) {

					String ii = instructions.get(instTwo).split(" ")[1].replace("#", "");
					nextCounter = Integer.parseInt(ii);
				} else if (op2.equals(Ops.BREAK)) {
					return nextCounter += 4;
				} else
					nextCounter = instTwo + 4;
			}
			preIssueQueue.add(instOne);
			preIssueQueue.add(instTwo);
		}
		return nextCounter;
	}

	/**
	 * Move from PreIssue to PreALU. Checks for Hazards here Check preAlucache
	 * Was free last time
	 */
	private static void PreIssueMove() {
		if (preAluHasCapacity) {
			int counter = 0;
			while (counter < preIssueQueueSize && preAluQueueSize < 2) {
				if (!HasHazard(preIssueQueue.get(counter)) && !HasLocalHazard(counter)) {
					Ops op = GetOpType(preIssueQueue.get(counter));
					if (!op.equals(Ops.J)) {
						ReserveResources(preIssueQueue.get(counter));
						preAluQueue[preAluQueueTail] = preIssueQueue.get(counter);
						preIssueQueue.remove(counter);
						preAluQueueSize += 1;

					} else {
						Decode(instructions.get(preIssueQueue.get(counter)), 0);
						break;
					}

				}
				counter++;
			}
		}
	}

	private static boolean HasLocalHazard(int counter) {
		HashMap<String, Boolean> isLocalRegisterRead = new HashMap<>();
		HashMap<String, Boolean> isLocalRegisterWrite = new HashMap<>();
		boolean hasStore = false;
		for (int i = 0; i < counter; i++) {
			String[] input = GetRegisters(instructions.get(preIssueQueue.get(i)));
			if (Ops.valueOf(input[0]).equals(Ops.SW)) {
				hasStore = true;
			}
			if (input[1] != null) {
				isLocalRegisterWrite.put(input[0], true);
			}
			for (int j = 2; j < 4; i++) {
				if (input[j] != null) {
					isLocalRegisterRead.put(input[j], true);
				}
			}
		}
		String[] input = GetRegisters(instructions.get(preIssueQueue.get(counter)));
		if ((Ops.valueOf(input[0]).equals(Ops.LW) || Ops.valueOf(input[0]).equals(Ops.SW)) && hasStore) {
			return true;
		}
		if (input[1] != null) {
			if (isLocalRegisterWrite.get(input[1])) {
				return true;
			}
		}
		if (input[2] != null) {
			if (isLocalRegisterWrite.get(input[2])) {
				return true;
			}
		}
		if (input[3] != null) {
			if (isLocalRegisterWrite.get(input[3])) {
				return true;
			}
		}
		// WAR Hazard
		if (input[1] != null) {
			if (isLocalRegisterRead.get(input[1])) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Move from PreIssue to PreALU
	 */
	private static void PreAluMove() {
		if (preAluQueueSize > 0) {
			if (preAluQueueSize == 2) {
				preAluHasCapacity = false;
			} else {
				preAluHasCapacity = true;
			}
			OpTypes op = GetOpTypeGroup(preAluQueue[preAluQueueHead]);
			if (op.equals(OpTypes.CALC) && postAluQueue == -1) {
				postAluQueue = preAluQueue[preAluQueueHead];
				preAluQueue[preAluQueueHead] = -1;
				preAluQueueHead = (preAluQueueHead + 1) % 2;
				preAluQueueSize -= 1;
			} else if (op.equals(OpTypes.LDSW) && preMemQueue == -1) {
				preMemQueue = preAluQueue[preAluQueueHead];
				preAluQueue[preAluQueueHead] = -1;
				preAluQueueHead = (preAluQueueHead + 1) % 2;
				preAluQueueSize -= 1;
			}
		}
	}

	private static void PreMemMove() {
		if (preMemQueue > 0) {
			if (postMemQueue == -1) {
				postMemQueue = preMemQueue;
				preMemQueue = -1;
			}
		}

	}

	private static void PostALUMove() {
		if (postAluQueue > 0) {
			WriteBack(postAluQueue);
			postAluQueue = -1;
		}

	}

	private static void PostMemMove() {
		if (postMemQueue > 0) {
			WriteBack(postMemQueue);
			postMemQueue = -1;
		}

	}

	private static void WriteBack(int instructionCode) {
		// TODO Call Decode here , might be required elsewhere
		FreeRegisters(instructionCode);
		Decode(instructions.get(instructionCode), 0);

	}

	private static void FreeRegisters(int instructionCode) {
		String input[] = GetRegisters(instructions.get(instructionCode));
		if (input[1] != null) {
			isRegisterWrite.put(input[1], false);
		}
		if (input[2] != null) {
			isRegisterRead.put(input[2], false);
		}
		if (input[3] != null) {
			isRegisterWrite.put(input[3], false);
		}

	}

	private static Ops GetOpType(int instructionCode) {
		String[] input = GetRegisters(instructions.get(instructionCode));
		return Ops.valueOf(input[0]);
	}

	private static OpTypes GetOpTypeGroup(int instructionCode) {
		Ops op = GetOpType(instructionCode);
		OpTypes opType = null;
		switch (op) {
		case BEQ:
		case BGTZ:
			opType = OpTypes.Branch;
			break;
		case J:
			opType = OpTypes.JUMP;
			break;
		case LW:
		case SW:
			opType = OpTypes.LDSW;
			break;
		default:
			opType = OpTypes.CALC;
			break;
		}
		return opType;
	}

	/**
	 * Add registers to read or write hashmap
	 * 
	 * @param instructionCode
	 */
	private static void ReserveResources(int instructionCode) {
		String[] input = GetRegisters(instructions.get(instructionCode));
		if (input[1] != null) {
			isRegisterWrite.put(input[0], true);
		}
		for (int i = 2; i < 4; i++) {
			if (input[i] != null) {
				isRegisterRead.put(input[i], true);
			}
		}

	}

	private static String[] GetRegisters(String instruction) {

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

	private static boolean HasHazard(int instructionCode) {
		// RAW -WAW Hazard
		String[] input = GetRegisters(instructions.get(instructionCode));
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

enum OpTypes {
	Branch, LDSW, CALC, JUMP;
}
