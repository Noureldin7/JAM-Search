package eng.server;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import eng.indexer.classes.Indexer;
import eng.indexer.classes.MongoStorage;
import eng.indexer.classes.Storage;
import eng.indexer.classes.WebIndexer;
import eng.ranker.Ranker;
import eng.util.Preprocessor;
import io.github.cdimascio.dotenv.Dotenv;
public class HTTPServer {
    HttpServer server;
    Ranker ranker;
    HTTPServer(int port) throws Exception
    {
        Dotenv dotenv = Dotenv.load();
        String connectionString = dotenv.get("CONN_STRING");
        Storage storage = new MongoStorage(connectionString);
        Indexer indexer = new WebIndexer(storage);
        this.ranker = new Ranker(indexer);
        this.server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        this.server.createContext("/", new handler());
        this.server.setExecutor(Executors.newFixedThreadPool(2));
        this.server.start();
        System.out.println("Server Listening");
    }
    class handler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            if("POST".equals(exchange.getRequestMethod()))
            {
                String body = IOUtils.toString(exchange.getRequestBody());
                try {
                    JSONObject bodyJson = new JSONObject(body);
                    String text = bodyJson.getString("text");
                    String[] query = Preprocessor.preprocess(text).toArray(new String[0]);
                    // System.out.println(query[1]);
                    // System.out.println(ranker.rank(query).toString());
                    // IOUtils.
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    // exchange.getResponseHeaders().add("content-Type", "plain/text");
                    exchange.sendResponseHeaders(200, 0);
                    JSONObject resJson = new JSONObject().put("urls", ranker.rank(query));
                    IOUtils.write(resJson.toString(), exchange.getResponseBody());
                    exchange.getResponseBody().close();
                    exchange.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else{
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            }
        }
        
    }
    public static void main(String args[]) throws Exception
    {
        new HTTPServer(5000);
    }
}
