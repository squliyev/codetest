package se.kry.codetest;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import se.kry.codetest.entities.Service;
import io.vertx.core.*;

import java.util.List;

public class BackgroundPoller {

  // The result of poll can be saved to the separate log table
  // table: ServicePollResult -> columns:  Url, Status, PollDate
  public void pollServices(Vertx vertx, List<Service> services) {
    // for each service try to get status and update it
    services.forEach(service-> {
      try {
        WebClient client = WebClient.create(vertx);
        HttpRequest<Buffer> request = client.getAbs(service.getUrl()).timeout(5000);
        request.send(ar -> {
          boolean status = ar.succeeded();
          service.setStatus(status ? Service.OK : Service.FAILED);
          System.out.println("poll executed for " + service.getUrl());
        });
      }
      catch(Exception e) {
        System.out.println("failed for url: " + service.getUrl());
        service.setStatus(Service.FAILED);
      }
    });
  }
}
