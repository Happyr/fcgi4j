# FCGI4j

Connect to Fast CGI from your java application. With this library you may start PHP scripts with PHP-FPM. 

This is a fork from the [subversion repo][google] at revision r17. The project was not maintained any more and I could not get in
touch with the author. I decided to maintain this library myself. r17 was the last revision where the library worked
as expected.

## Usage example

```java

//create FastCGI connection
FCGIConnection connection = FCGIConnection.open();
connection.connect(new InetSocketAddress("localhost", 5672));

connection.beginRequest("/var/www/foobar.php");
connection.setRequestMethod("POST");

byte[] postData = "hello=world".getBytes();

//set contentLength, it's important
connection.setContentLength(postData.length);
connection.write(ByteBuffer.wrap(postData));

//print response headers
Map<String, String> responseHeaders = connection.getResponseHeaders();
for (String key : responseHeaders.keySet()) {
    System.out.println("HTTP HEADER: " + key + "->" + responseHeaders.get(key));
}

//read response data
ByteBuffer buffer = ByteBuffer.allocate(10240);
connection.read(buffer);
buffer.flip();

byte[] data = new byte[buffer.remaining()];
buffer.get(data);

System.out.println(new String(data));

//close the connection
connection.close();

```

## Specification
This library implement the [FastCGI Specification](http://www.fastcgi.com/devkit/doc/fcgi-spec.html).

[google]:https://code.google.com/p/fcgi4j/
