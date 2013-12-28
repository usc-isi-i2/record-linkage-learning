package com.restservices;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("orders")
public class RLService {
	@POST
	@Path("/{order}")
	@Consumes("text/plain")
	@Produces("text/plain")
	public javax.ws.rs.core.Response link(String jsonStr) throws Exception {
		System.out.println(jsonStr);
		ComputeSVMWeights p = new ComputeSVMWeights();
		return javax.ws.rs.core.Response.ok().entity(p.parseJSON_1(jsonStr)).build();
	}
}