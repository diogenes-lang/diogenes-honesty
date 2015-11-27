package it.unica.co2.api.contract;

import java.io.Serializable;

public abstract class Sort<T> implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static UnitSort UNIT = new UnitSort();
	private static IntegerSort INT = new IntegerSort();
	private static StringSort STRING = new StringSort();
	
	protected T validValue;
	
	public T getValidValue() {
		return validValue;
	}
	
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
	
	public static class UnitSort extends Sort<Void> {
		private static final long serialVersionUID = 1L;

		private UnitSort() {}
		
		@Override
		public String toString() {
			return "UNIT";
		}
	}
	
	public static class IntegerSort extends Sort<Integer> {
		private static final long serialVersionUID = 1L;

		private IntegerSort() {
			this.validValue=0;
		}
		
		private IntegerSort(Integer validValue) {
			this.validValue=validValue;
		}
		
		@Override
		public String toString() {
			return "INT";
		}
	}
	
	public static class StringSort extends Sort<String> {
		
		private static final long serialVersionUID = 1L;
		
		private StringSort() {
			this.validValue="a string";
		}
		
		private StringSort(String validValue) {
			this.validValue=validValue;
		}
		
		@Override
		public String toString() {
			return "STRING";
		}
	}
	
}