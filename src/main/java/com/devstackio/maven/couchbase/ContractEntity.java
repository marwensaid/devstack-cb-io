package com.devstackio.maven.couchbase;

import com.devstackio.maven.entity.DefaultEntity;

/**
 *
 * @author devstackio
 */
public class ContractEntity extends DefaultEntity {
	
	private String contract;
	
	public ContractEntity() {
		this.bucket = "default";
	}

	public String getContract() {
		return contract;
	}

	public void setContract(String contract) {
		this.contract = contract;
	}
	
}
