/* On my honor, I have neither given nor received unauthorized aid on this assignment */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MIPSsims {

	public static int memorylocation = 124;
	public static List<String> disassemblyOutput = new ArrayList<>();

	public static void main(String[] args) {

		String filename = args[0];
		List<Long> input = ReadFile(filename);
		DisAssembler(input);
	}

	private static void DisAssembler(List<Long> input) {
		for (Long word : input) {
			memorylocation += 4;
			Category cat = GetCategory(word);
			ProcessInstructions(cat, word);
		}
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
		// TODO Auto-generated method stub

	}

	private static void ProcessCategory2(long word) {
		long rs, rt, rd;
		Category2OPS op;
		rs = ((word >> 24) & 0b11111);
		rt = ((word >> 19) & 0b11111);
		op = Category2OPS.fromByte((int) ((word >> 16) & 0b111));
		rd = ((word >> 11) & 0b11111);

		String disassembely = Long.toBinaryString(word) + "\t" + memorylocation + "\t" + op.toString() + " "
				+ RegisterName(rd) + ", " + RegisterName(rs) + ", " + RegisterName(rt);
		System.out.println(disassembely);
		disassemblyOutput.add(disassembely);
	}

	private static String RegisterName(long rd) {
		return "R" + rd;
	}

	private static void ProcessCategory1(long word) {
		// TODO Auto-generated method stub

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
				System.out.println(sCurrentLine);
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
		ADD(0b000), SUB(0b001), MUL(0b010), AND(0b011), OR(0b100), XOR(0b101), NOR(0b110);
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
