/* On my honor, I have neither given nor received unauthorized aid on this assignment */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class MIPSsims {

	public static int memoryLocation = 124;
	public static int breakLocation = 0;
	public static List<String> disassemblyOutput = new ArrayList<>();
	public static List<String> simulationOutput = new ArrayList<>();
	public static Hashtable<Integer, Integer> registers = new Hashtable<Integer, Integer>();
	public static Hashtable<Integer, Integer> memory = new Hashtable<Integer, Integer>();
	static boolean isBreak = false;

	public static void main(String[] args) {
		String filename = args[0];
		List<Long> input = ReadFile(filename);
		DisAssembler(input);
		InitializeRegisters();
		InitializeMemory();
		Simulator(disassemblyOutput);
	}

	private static void InitializeMemory() {
		for (int i = breakLocation; i <= memoryLocation; i += 4) {
			memory.put(i, 0);
		}

	}

	private static void InitializeRegisters() {
		for (int i = 0; i < 33; i++) {
			registers.put(i, 0);
		}
	}

	private static void Simulator(List<String> disassemblyOutput) {
		int cycle = 1;
		boolean isInstruction = true;
		for (String instruction : disassemblyOutput) {
			if (isInstruction) {
				if (instruction.split("\t")[2].equals("BREAK")) {
					isInstruction = false;
				}
				String simulation = "--------------------\nCycle:" + cycle + "\t" + instruction.split("\t")[2]
						+ "\nRegisters";
				for (int i = 0; i <= 24; i += 8) {
					String regNo = String.valueOf(i);
					if (regNo.length() == 1)
						regNo = "0" + regNo;
					simulation += "\nR" + regNo + ":";
					for (int j = i; j < i + 8; j++) {
						simulation += "\t" + registers.get(j);
					}
				}
				simulation += "\nData";
				for (int i = breakLocation; i <= memoryLocation; i += 32) {
					simulation += "\n" + i + ":";
					for (int j = i; j < i + 32; j += 4) {
						simulation += "\t" + memory.get(i);
					}
				}
				System.out.println(simulation);
				simulationOutput.add(simulation);
				cycle++;
			}
		}

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
			ii = ii ^ delaySlot;
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
}
