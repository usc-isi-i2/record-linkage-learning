package usc.linkage.uniqueIdGenerator;

import org.testng.annotations.Test;

public class UniqueIDGeneratorTest {
	@Test(description="test configuration and JSONParser")
	public void uniqueIDGeneratorTest(){
		for(int i = 0; i < 1010; i++){
			System.out.println(UniqueIDGenerator.getUniqueID());
		}
	}
}
