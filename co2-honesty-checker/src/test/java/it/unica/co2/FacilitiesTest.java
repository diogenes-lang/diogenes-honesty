package it.unica.co2;

import static org.junit.Assert.*;
import static it.unica.co2.util.Facilities.*;

import org.junit.Test;


public class FacilitiesTest {

	
	@Test
	public void switch_1() {
		
		final Wrapper wrapper = new Wrapper();
		wrapper.stringValue = "a";
		
		
		_switch(
				wrapper.stringValue, 
				
				_case("a", ()->{ wrapper.count++; }),
				_case("b", ()->{ fail(); }),
				_case("c", ()->{ fail(); }),
				_case("d", ()->{ fail(); })
		);
		
		assertTrue(wrapper.count>0);
	}
	
	@Test
	public void switch_2() {
		
		final Wrapper wrapper = new Wrapper();
		wrapper.stringValue = "no match";
		
		
		_switch(
				wrapper.stringValue, 
				
				_case("a", ()->{ fail(); }),
				_case("b", ()->{ fail(); }),
				_case("c", ()->{ fail(); }),
				_case("d", ()->{ fail(); })
		);
		
		assertTrue(wrapper.count==0);
	}
	
	@Test
	public void switch_3() {
		
		final Wrapper wrapper = new Wrapper();
		wrapper.stringValue = "multi match";
		
		
		_switch(
				wrapper.stringValue, 
				
				_case("a", ()->{ fail(); }),
				_case("multi match", ()->{ wrapper.count++; }),
				_case("multi match", ()->{ wrapper.count++; }),
				_case("d", ()->{ fail(); })
		);
		
		assertTrue(wrapper.count==2);
	}
}

class Wrapper {
	public int count = 0;
	public String stringValue;
}
