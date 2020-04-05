package se.kry.codetest.entities;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;



public class Service {

    public static final String UNKNOWN = "unknown";
    public static final String FAILED ="failed";
    public static final String OK = "ok";


    String url;
    String name;
    LocalDateTime dateTime;
    String status = UNKNOWN;


    public String getStatus() {
        return status;
    }
    public synchronized void setStatus(String status) {
        this.status = status;
    }


    public String getUrl() {
        return url;
    }
    public void setUrl(String serviceUrl) {
        this.url = serviceUrl;
    }
    public String getName() {
        return name;
    }
    public void setName(String serviceName) {
        this.name = serviceName;
    }
    public  String getEntryDate() {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss"));
    }

    public  Service(String name, String url) {
        this.url = url;
        this.name = name;
        this.dateTime = LocalDateTime.now();
    }


    public Service(JsonObject entry) {
        url = entry.getString("url");
        name = entry.getString("name");
        dateTime = getDateTime(entry);
    }

    private LocalDateTime getDateTime(JsonObject entry) {
        String str= entry.getString("entryDate");
       // String str = "2016-03-04 11:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        return  dateTime;
    }


    public JsonArray ToJsonArray() {
        //YYYY-MM-DD HH:MI:SS
        String dateTime = this.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        JsonArray res =  new JsonArray().add(this.url).add(this.name).add(dateTime);
        return res;
    }



}
