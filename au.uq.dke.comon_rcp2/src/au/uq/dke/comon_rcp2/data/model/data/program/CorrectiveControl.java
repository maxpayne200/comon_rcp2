package au.uq.dke.comon_rcp2.data.model.data.program;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;


@Entity
@Inheritance(strategy=InheritanceType.JOINED)
public class CorrectiveControl extends ControlIdentificationAndDefinition	{
	public CorrectiveControl(String name){
		super(name);
	}

	public CorrectiveControl() {

	}
	
}
