package org.demo.tracing.model.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.demo.tracing.model.service.MongoService;

@Path("/entity")
public class MyResource {

    @Inject
    MongoService mongoService;

    @POST
    public void insert(@QueryParam("name") String name) {
        mongoService.insertWithOutbox(name);
    }
}
