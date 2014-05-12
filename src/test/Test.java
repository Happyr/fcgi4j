package test;

import com.googlecode.fcgi4j.FCGIConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author panzd
 */
public class Test {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        FCGIConnection connection = FCGIConnection.open();
        connection.connect(new InetSocketAddress("192.168.47.11", 9000));

        connection.beginRequest("fcgi.php");
        connection.setRequestMethod("post");
        connection.setQueryString("text=hello");
        connection.addParams("DOCUMENT_ROOT", "/var/www");

        byte[] postData = "hello=world".getBytes();

        connection.setContentLength(postData.length);
        connection.write(ByteBuffer.wrap(postData));

        Map<String, String> responseHeaders = connection.getResponseHeaders();
        for (String key : responseHeaders.keySet()) {
            System.out.println("HTTP HEADER: " + key + "->" + responseHeaders.get(key));
        }

        ByteBuffer buffer = ByteBuffer.allocate(10240);
        connection.read(buffer);
        buffer.flip();

        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        System.out.println(new String(data));

        System.out.println(connection.getEndRequest().getProtocolStatus());
    }
}
