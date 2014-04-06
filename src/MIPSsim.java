/* On my honor, I have neither given nor received unauthorized aid on this assignment */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
			simulation += "\n\tEntry 0:[" + instructions.get(PreIssueQueue[0]);
			simulation += "\n\tEntry 1:[" + instructions.get(PreIssueQueue[1]);
			simulation += "\n\tEntry 2:[" + instructions.get(PreIssueQueue[2]);
			simulation += "\n\tEntry 3:[" + instructions.get(PreIssueQueue[3]);
			simulation += "\nPre-ALU Queue:";
			simulation += "\n\tEntry 0:[";
			simulation += "\n\tEntry 1:[";
			simulation += "\nPre-MEM Queue:";
			simulation += "\nPost-MEM Queue:";
			simulation += "\nPost-ALU Queue:";

			simulation += "\n\nRegisters";
			counter = Decode(instruction, counter);
			counter = Decode(instruction, counter);
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

	// ////

	static boolean[] isRegisterBusy = new boolean[32];

	static int waitingInstruction;
	static int executedInstruction;

	private static int[] PreIssueQueue = new int[4];
	private static int[] PreALUQueue = new int[2];
	private static int[] PostALUQueue = new int[1];
	private static int[] PreMemQueue = new int[1];
	private static int[] PostMemQueue = new int[1];

	private static int IFNextFree = 0;
	private static int IssueNextFree = 0;
	private static int ALUNextFree = 0;
	private static int MEMNextFree = 0;
	private static int WBNextFree = 0;

}

class Chronos {
	private int currentCycle;
	private int nextCycle;

	public int getCurrentCycle() {
		return currentCycle;
	}

	public void setCurrentCycle(int currentCycle) {
		this.currentCycle = currentCycle;
	}

	public int getNextCycle() {
		return nextCycle;
	}

	public void setNextCycle(int nextCycle) {
		this.nextCycle = nextCycle;
	}

}
