package org.kaivos.racter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;

public class RacterInterpreter {

	public static final Random RANDOM = new Random(8947594l);
	
	CodeSection[] sections = new CodeSection[255];
	String[] slots = new String[255];
	
	private class CodeSection {
		private Map<String, List<Integer>> lineMap = new HashMap<>();
		private List<String> lines = new ArrayList<>();
		private String name;
		private int size, type;
		
		public CodeSection(String name, int type, int size) {
			this.name = name;
			this.type = type;
			this.size = size;
		}
		
		public String getName() {
			return name;
		}
		
		public int getSize() {
			return size;
		}
		
		public void putLine(String name, String line) {
			if (lineMap.get(name) == null) lineMap.put(name, new ArrayList<Integer>());
			lineMap.get(name).add(lines.size());
			lines.add(line);
		}
		
		public int getLine(String name) {
			if (lineMap.get(name) != null) {
				List<Integer> choices = lineMap.get(name);
				return choices.get(RANDOM.nextInt(choices.size()));
			}
			for (String key : lineMap.keySet()) {
				if (key.startsWith(name)) {
					return getLine(key);
				}
			}
			System.err.println("Unknown line " + name);
			System.exit(1);
			return -1;
		}
		
		public String getLine(int id) {
			return lines.get(id);
		}
		
		public List<String> getLines(String name) {
			if (lineMap.get(name) != null) {
				List<Integer> choices = lineMap.get(name);
				return choices.stream().map(this::getLine).collect(Collectors.toList());
			}
			for (String key : lineMap.keySet()) {
				if (key.startsWith(name)) {
					return getLines(key);
				}
			}
			System.err.println("Unknown line " + name);
			System.exit(1);
			return null;
		}
	}
	
	public static void runFile(String file) throws IOException {
		RacterInterpreter racter = new RacterInterpreter();
		racter.loadScript(file);
		racter.start(1, "START");
		System.out.println();
	}
	
	private void start(int secid, String linename) {
		CodeSection currentSection = sections[secid];
		int ip = linename.length() > 0 ? currentSection.getLine(linename) : 0;
		while (true) {
			String line = currentSection.getLine(ip);
			if (runLine(line)) ip++;
			else break;
		}
		System.out.print(out);
		out = " ";
	}
	
	private boolean runLine(String string) {
		String[] instructions = string.split(" ");
		for (String instruction : instructions) {
			switch (runInstruction(instruction)) {
			case ESCAPE:
				return false;
			case NEXT_LINE:
				return true;
			case NEXT_COMMAND:
				break;
			}
		}
		
		return false;
	}
	
	private BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
	
	private String readLine() {
		try {
			System.out.print(">");
			return input.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean accumulator = false;
	private String out = "", in = "";
	private int selWord = 0;
	
	enum Escape { NEXT_LINE, NEXT_COMMAND, ESCAPE }
	private Escape runInstruction(String code) {
		if (false) {
			System.err.print(code + " : " + in + " : " + selWord + "," + accumulator);
			for (int i = 0; i < slots.length; i++) {
				String s = slots[i];
				if (s != null && !s.isEmpty())
					System.err.print("," + i++ + "=" + s);
			}
			System.err.println();
		}
		if (code.equals("#")) {
			return Escape.NEXT_LINE;
		}
		else if (code.startsWith("#*")) {
			String addr = code.substring(2);
			jump(addr);
			return Escape.ESCAPE;
		}
		else if (code.startsWith("*")) { // TODO muuttujaviittaukset ja taivutusmuodot sekä mystiset huutomerkit
			String addr = code.substring(1);
			jump(addr);
		}
		else if (code.matches("'[0-9]+\\*[0-9]+")) {
			// TODO
		}
		else if (code.equals("??")) {
			System.out.println(out);
			out = "";
			in = readLine();
		}
		else if (code.equals(":ZAP")) {
			slots = new String[255];
		}
		else if (code.equals(":F=0")) {
			selWord=0;
		}
		else if (code.equals(":F+1")) {
			selWord++;
		}
		else if (code.equals(":F-1")) {
			selWord--;
		}
		else if (code.equals("D")) {
			// ????
		}
		else if (code.startsWith(">")) {
			int slot = Integer.parseInt(code.substring(1, code.indexOf('=')));
			String[] values = code.substring(code.indexOf('=')+1).split(",");
			String ans = "";
			for (String value : values) {
				ans += " ";
				if (value.equals("F")) {
					String[] words = in.split(" ");
					ans += selWord >= 1 && words.length > selWord-1 ? words[selWord-1] : "";
				}
				else if (value.equals("R")) {
					String[] words = in.split(" ");
					for (int i = selWord; i < words.length; i++) {
						if (i != selWord) ans += " ";
						ans += words[i];
					}
				}
				else if (value.equals("L")) {
					String[] words = in.split(" ");
					for (int i = 0; i < selWord-1; i++) {
						if (i != 0) ans += " ";
						ans += words[i];
					}
				}
				else if (value.matches("[0-9]+")) {
					int slotid = Integer.parseInt(value);
					ans = slots[slotid] != null ? slots[slotid] : "";
				}
				else ans += value;
			}
			slots[slot] = ans.trim();
		}
		else if (code.startsWith("$")) {
			int slot = Integer.parseInt(code.substring(1));
			return runInstruction(slots[slot] != null ? slots[slot] : "");
		}
		else if (code.length() > 1 && code.startsWith("?")) {
			String condition = code.substring(1);
			boolean not = condition.startsWith("-:");
			if (not) condition = condition.substring(2);
			
			String[] words = in.split(" ");
			
			boolean ans;
			if (condition.equals("CAP")) {
				ans = false;
				for (int i = 0; i < words.length; i++) {
					if (Character.isUpperCase(words[i].charAt(0))) {
						if (!ans) selWord = i+1;
						ans = true;
					}
				}
			}
			else if (condition.equals("CAP+1")) {
				ans = in.length() > 0 ? Character.isUpperCase(in.charAt(0)) : false;
			}
			else if (condition.contains("=")) {
				String[] operands = condition.split("=");
				int slot = Integer.parseInt(operands[0]);
				if (slots[slot] == null) slots[slot] = "";
				if (operands.length == 1) {
					ans = slots[slot].isEmpty();
				}
				else if (operands[1].matches("[0-9]+")) {
					ans = slots[slot].equalsIgnoreCase(slots[Integer.parseInt(operands[1])]);
				}
				else {
					ans = slots[slot].equalsIgnoreCase(operands[1].replaceAll(",", " ").trim());
				}
			}
			else {
				String[] matches = condition.split(",");
				ans = false;
				for (String match : matches) {
					for (int i = 0; i < words.length; i++) {
						if (match.equalsIgnoreCase(words[i])) {
							selWord = i+1;
							ans = true;
						}
					}
				}
			}
			
			accumulator = not ^ ans;
		}
		else if (code.startsWith("/")) {
			if (accumulator) {
				return runInstruction(code.substring(1));
			}
		}
		else if (code.startsWith("\\")) {
			if (!accumulator) {
				return runInstruction(code.substring(1));
			}
		}
		else if (code.equals("")) {
			// ei mitään
		}
		else if (code.startsWith("<")) {
			out += code.substring(1);
		}
		else {
			if (out.length() > 0) out += " ";
			out += code;
		}
		return Escape.NEXT_COMMAND;
	}
	
	private void jump(String addr) {
		String secidstr = "";
		while (addr.length() > 0 && Character.isDigit(addr.charAt(0))) {
			secidstr += addr.substring(0, 1); addr = addr.substring(1);
		}
		int secid = Integer.parseInt(secidstr);
		start(secid, addr);
	}

	public void loadScript(String filename) throws IOException {
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String name = reader.readLine();
		int idcounter = Integer.parseInt(reader.readLine().trim());
		int sections = Integer.parseInt(reader.readLine().trim());
		int size = Integer.parseInt(reader.readLine().trim());
		
		System.err.println("Reading " + name + " (" + size + " lines)");
		
		List<CodeSection> seclist = new ArrayList<>();
		
		{
			for (int i = 0; i < sections; i++) {
				String secname = reader.readLine();
				int secid = idcounter++;
				int sectype = Integer.parseInt(reader.readLine().trim());
				int secsize = Integer.parseInt(reader.readLine().trim());
				
				System.err.println("Registering *" + secid + " (" + secsize + " lines)");
				
				CodeSection section = new CodeSection(secname, sectype, secsize);
				seclist.add(RacterInterpreter.this.sections[secid] = section);
			}
			for (CodeSection sec : seclist) {
				for (int i = 0; i < sec.getSize(); i++) {
					String line = reader.readLine();
					String linename = line.substring(0, line.indexOf(' '));
					String linecode = line.substring(line.indexOf(' ')+1, line.length());
					sec.putLine(linename, linecode);
					
					System.err.println("" + sec.name + " : " + line);
				}
			}
		}
		reader.close();
	}

}
