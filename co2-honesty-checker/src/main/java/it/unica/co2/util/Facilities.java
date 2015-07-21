package it.unica.co2.util;



public class Facilities {

	
	/**
	 * Execute each case that contains an object equals to the given parameter.
	 * @param obj
	 * @param caseStmts
	 */
	@SafeVarargs
	public static final <T> void _switch(T obj, Case<T>... caseStmts) {
		
		for (Case<T> caseStmt : caseStmts) {
			
			caseStmt.execute(obj);
		}
	}
	
	
	public static <T> Case<T> _case(T obj, Runnable runnable) {
		return new Case<T>(obj, runnable);
	}
	
	
	
	public static class Case<T> {

		private final Object obj;
		private final Runnable caseStatement;
		
		/*
		 * JPF-flag
		 */
		@SuppressWarnings("unused") private String actionName;
		
		public Case(Object obj, Runnable caseStatement) {
			this.obj = obj;
			this.caseStatement = caseStatement;
		}

		public void execute(T t) {
			
			if (obj.equals(t)) {
				actionName = String.valueOf(obj);
				runCase();
			}
		}
		
		private void runCase() {
			caseStatement.run();
		}
		
	}
}
