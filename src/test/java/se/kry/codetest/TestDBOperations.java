package se.kry.codetest;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import se.kry.codetest.entities.Service;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.format.DateTimeFormatter;

@ExtendWith(VertxExtension.class)
public class TestDBOperations {

    private DBConnector connector;

    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
        connector = new DBConnector(vertx);
    }

    @Test
    @DisplayName("test db connector class")
    @Timeout(value = 60, timeUnit = TimeUnit.SECONDS)
    void create_insert_delete_db_operations(Vertx vertx, VertxTestContext testContext) {
        // WorkerExecutor sharedWorker = vertx.createSharedWorkerExecutor("my-shared-pool", 20);
        connector.query("CREATE TABLE IF NOT EXISTS testService (url VARCHAR(128) NOT NULL primary key)").setHandler(done -> {
            assertEquals(done.succeeded(),true);
            if(done.succeeded()){
                System.out.println("completed db migrations");
                insert_row(new Service("test-url",""));
            } else {
                done.cause().printStackTrace();
            }
            testContext.completeNow();
        });
    }


    private void insert_row(Service url) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String sql = "INSERT INTO testService (url) VALUES (?);";
        connector.query(sql, new JsonArray().add(url.getUrl() + " " + dtf.format(now))).setHandler(done -> {
            if(done.succeeded()){
                System.out.println("insert row is completed");
                query_row();
            } else {
                done.cause().printStackTrace();
            }
        });
    }

    private void query_row() {
        connector.query("SELECT * from testService;").setHandler(done -> {
            assertEquals(done.succeeded(),true);
            if(done.succeeded()){
                //getValue("url").
                List<String> urls = done.result().getRows().stream().
                        map(x-> x.getString("url")).collect(Collectors.toList());
                delete_row(0, urls);
                System.out.println("query row is completed");
            } else {
                done.cause().printStackTrace();
            }
        });
    }

    private void delete_row(int i, List<String> urls) {
        try {
            if (urls.size() == i) {
                return;
            }
            String sql = "delete from testService where url = ?;";
            String url = urls.get(i);
            connector.query(sql, new JsonArray().add(url)).setHandler(done -> {
                try {
                    assertEquals(done.succeeded(), true);
                    if (done.succeeded()) {
                        delete_row(i + 1, urls);
                        System.out.println("query row is completed");
                    } else {
                        done.cause().printStackTrace();
                    }
                }
                catch (Exception e) {
                    System.out.println(e.fillInStackTrace());
                }
            });
        }
        catch (Exception e) {
            System.out.println(e.fillInStackTrace());
        }
    }
}
