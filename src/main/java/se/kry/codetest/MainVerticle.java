package se.kry.codetest;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.entities.Service;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private List<Service> services = new LinkedList<Service>();

  private DBConnector connector;
  private Future<Void> startFuture;
  private Router router;
  private BackgroundPoller poller = new BackgroundPoller();

  @Override
  public void start(Future<Void> startFuture) {
    this.connector = new DBConnector(vertx);
    this.startFuture = startFuture;
    this.router = getRouter(vertx);

    //Set Background Worker for poller
    setBackgroundPoller(vertx);
    // Initialize Routes
    initializeRoutes();

    // 1. Create table if necessary
    // 2. Query DB for server list
    // 3. Create Server and Listen
    initializeServer();
  }

  private void initializeServer() {
    // 1. Create table if necessary
    createServiceTableIfNotExist().setHandler(createTable-> {
      if (createTable.succeeded()) {
        // 2. Query DB for server list
        fillServices().setHandler(selectQuery-> {
          if(selectQuery.succeeded()) {
            // 3. Create Server and Listen
            createServerAndListen();
          }
        });
      }
    });
  }

  private Router getRouter(Vertx vertx) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    return router;
  }

  private void setBackgroundPoller(Vertx vertx) {
    vertx.setPeriodic(1000 * 10, timerId -> poller.pollServices(vertx, services));
  }

  private void initializeRoutes(){
    router.route("/*").handler(StaticHandler.create());
    initializeGetService();
    initializePostService();
    initializeDeleteService();
    initializeUpdateService();

    initializeStatusCheckForServices();
  }


  private void initializeGetService() {
    router.get("/get-services").handler(req -> {
      List<JsonObject> jsonServices = services.stream().map(service ->
              new JsonObject()
                      .put("name", service.getName())
                      .put("url", service.getUrl())
                      .put("date", service.getEntryDate())
                      .put("status", service.getStatus()))
              .collect(Collectors.toList());
      req.response()
              .putHeader("content-type", "application/json")
              .end(new JsonArray(jsonServices).encode());
    });
  }

  private void initializePostService() {
    router.post("/insert-service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String serviceUrl = jsonBody.getString("url");
      if(CheckForUrlValidity(serviceUrl) && !ChecForDuplicate(serviceUrl)) {
        String serviceName = jsonBody.getString("name");
        Service service = new Service(serviceName, serviceUrl);
        insertService(service).setHandler(done -> {
          if (done.succeeded()) {
            services.add(service);
          }
          req.response()
                  .putHeader("content-type", "text/plain")
                  .end("OK");
        });
      }
      else  {
        SetResponseStatusAsBadRequest(req);
      }
    });
  }



  private void initializeDeleteService() {
    router.post("/delete-service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String serviceUrl = jsonBody.getString("url");
      deleteService(serviceUrl).setHandler(done -> {
        if(done.succeeded()) {
           services.removeIf(x->x.getUrl().equals(serviceUrl));
        }
        req.response()
                .putHeader("content-type", "text/plain")
                .end("OK");
      });
    });
  }

  private void initializeUpdateService() {
    router.post("/update-service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      String primaryUrl = jsonBody.getString("primaryUrl");
      String serviceUrl = jsonBody.getString("url");
      String serviceName = jsonBody.getString("name");
      if(CheckForUrlValidity(serviceUrl) && CheckForUrlValidity(primaryUrl)) {
        updateService(primaryUrl, serviceUrl, serviceName).setHandler(done -> {
          if (done.succeeded()) {
            Service service = services.stream()
                    .filter(x -> x.getUrl().equals(primaryUrl))
                    .findFirst().orElse(null);
            if(service!=null) {
              service.setName(serviceName);
              service.setUrl(serviceUrl);
              service.setStatus(Service.UNKNOWN);
            }
          }
          req.response()
                  .putHeader("content-type", "text/plain")
                  .end("OK");
        });
      }
      else  {
        SetResponseStatusAsBadRequest(req);
      }
    });
  }


  private void initializeStatusCheckForServices() {
    router.get("/check-services").handler(req -> {
      List<JsonObject> jsonServices = services.stream().map(service ->
              new JsonObject()
                      .put("url", service.getUrl())
                      .put("status", service.getStatus()))
              .collect(Collectors.toList());
      req.response()
              .putHeader("content-type", "application/json")
              .end(new JsonArray(jsonServices).encode());
    });
  }



  // create table if not exists
  private Future<Boolean> createServiceTableIfNotExist() {
    Future<Boolean> result = Future.future();
    String sql = "CREATE TABLE IF NOT EXISTS service (url VARCHAR(128) NOT NULL primary key, ";
    sql += "name varchar(128) null, ";
    sql += "entryDate DATETIME NOT NULL)";
    connector.query(sql).setHandler(done -> {
      if(done.succeeded()){
        System.out.println("make sure there is a table");
        result.complete(Boolean.TRUE);
      } else {
        done.cause().printStackTrace();
        result.complete(Boolean.FALSE);
      }
    });
    return result;
  }

  // get services from DB
  private Future<Boolean> fillServices() {
    Future<Boolean> result = Future.future();
    String sql = "SELECT * from service;";
    connector.query(sql).setHandler(done -> {
      if(done.succeeded()){
        List<Service> newServices = done.result().getRows().stream().
                map(Service::new).collect(Collectors.toList());
        for (Service service: newServices) {
          services.add(service);
        }
        System.out.println("select rows is completed");
        result.complete(Boolean.TRUE);
      } else {
        done.cause().printStackTrace();
        result.complete(Boolean.FALSE);
      }
    });
    return result;
  }

  // Create Server and listen for the actions
  private void createServerAndListen() {
    vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(8080, result -> {
              if (result.succeeded()) {
                System.out.println("KRY code test service started");
                startFuture.complete();
              } else {
                startFuture.fail(result.cause());
              }
            });
  }

  // Insert service to the Database
  private Future<Boolean> insertService(Service url) {
    Future<Boolean> result = Future.future();
    String sql = "INSERT INTO service (url, name, entryDate) VALUES (?, ?, ?);";
    connector.query(sql, url.ToJsonArray()).setHandler(done -> {
      if(done.succeeded()){
        System.out.println("insert row is completed");
        result.complete(Boolean.TRUE);
      } else {
        done.cause().printStackTrace();
        result.complete(Boolean.FALSE);
      }
    });
    return result;
  }

  // Delete service from Database
  private Future<Boolean> deleteService(String serviceUrl) {
    String sql = "delete from service where url = ?;";
    Future<Boolean> result = Future.future();
    connector.query(sql, new JsonArray().add(serviceUrl)).setHandler(done -> {
      if(done.succeeded()){
        System.out.println("delete row is completed");
        result.complete(Boolean.TRUE);
      } else {
        done.cause().printStackTrace();
        result.complete(Boolean.FALSE);
      }
    });
    return result;
  }

  // Since Url is Primary key I need to delete and insert
  // But in real life case I would use incrementalId or Guid for primary key
  private Future<Boolean> updateService(String primaryUrl, String serviceUrl, String serviceName) {
    Future<Boolean> result = Future.future();
    deleteService(primaryUrl).setHandler(delete -> {
       if(delete.succeeded()) {
         insertService(new Service(serviceName, serviceUrl)).setHandler(insert -> {
            if(insert.succeeded()) {
              result.complete(Boolean.TRUE);
            }
            else {
              result.complete(Boolean.FALSE);
            }
         });
       }
       else  {
         result.complete(Boolean.FALSE);
       }
    });
    return  result;
  }

  private boolean CheckForUrlValidity(String url) {
    try {
      URL u = new URL(url); // this would check for the protocol
      u.toURI(); // does the extra checking required for validation of URI
      return  true;
    }
    catch (Exception e) {
      return  false;
    }
  }

  private boolean ChecForDuplicate(String serviceUrl) {
     boolean result =  services.stream().
             anyMatch(x-> x.getUrl().equals(serviceUrl));
     return  result;
  }


  private void SetResponseStatusAsBadRequest(RoutingContext req) {
    req.response()
            .setStatusCode(400) //Bad Request
            .putHeader("content-type", "text/plain")
            .end("failed");
  }
}



