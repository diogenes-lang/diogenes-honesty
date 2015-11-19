package it.unica.co2.api.contract;

import java.io.Serializable;

public abstract class Sort implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static UnitSort UNIT = new UnitSort();
	public static IntegerSort INT = new IntegerSort();
	public static StringSort STRING = new StringSort();
	
	public static UnitSort unit() {
		return UNIT;
	}
	
	public static IntegerSort integer() {
		return INT;
	}
	
	public static StringSort string() {
		return STRING;
	}
	
	public static StringSort string(String pattern) {
		return new StringSort(pattern);
	}
	
	public static class UnitSort extends Sort {
		private static final long serialVersionUID = 1L;

		private UnitSort() {}
	}
	
	public static class IntegerSort extends Sort {
		private static final long serialVersionUID = 1L;

		private IntegerSort() {}
	}
	
	public static class StringSort extends Sort {
		
		private static final long serialVersionUID = 1L;
		public static final String base64Pattern =  "[a-z0-9A-Z]{22}==";
		public static final String integerPattern = "[0-9]{1,9}";
		public static final String longPattern = "[0-9]{10,18}";
		
		private final String pattern;
		
		private StringSort() {
			this(".*");
		}
		
		private StringSort(String pattern) {
			this.pattern=pattern;
		}

		public String getPattern() {
			return pattern;
		}
		
	}
}