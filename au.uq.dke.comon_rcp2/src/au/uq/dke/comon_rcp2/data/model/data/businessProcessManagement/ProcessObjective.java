package au.uq.dke.comon_rcp2.data.model.data.businessProcessManagement;

import javax.persistence.Entity;

@Entity
public class ProcessObjective extends BusinessProcess {


	public ProcessObjective(){
		
	}
	public ProcessObjective(String name) {
		super(name);
	}


}
