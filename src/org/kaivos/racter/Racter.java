package org.kaivos.racter;

import java.io.IOException;

public class Racter {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: racter <initial script file>");
			System.exit(1);
		}
		
		try {
			RacterInterpreter.runFile(args[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
