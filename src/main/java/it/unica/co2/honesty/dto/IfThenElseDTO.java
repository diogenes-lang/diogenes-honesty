package it.unica.co2.honesty.dto;



public class IfThenElseDTO extends ProcessDTO {

	public ProcessDTO thenStmt;
	public ProcessDTO elseStmt;
	
	@Override
	public ProcessDTO copy() {
		IfThenElseDTO tmp = new IfThenElseDTO();
		tmp.thenStmt = thenStmt.copy();
		tmp.elseStmt = elseStmt.copy();
		return tmp;
	}


}
