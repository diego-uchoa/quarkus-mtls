# quarkus-mtls
Bootstrapping

Let’s create both the server and client applications we will secure.

```
mvn io.quarkus:quarkus-maven-plugin:1.4.1.Final:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=quarkus-server-mtls \
    -DclassName="org.acme.server.mtls.GreetingResource" \
    -Dextensions="rest-client, resteasy-jsonb, kubernetes-client" \
    -Dpath="/hello-server"
```

```
mvn io.quarkus:quarkus-maven-plugin:1.4.1.Final:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=quarkus-client-mtls \
    -DclassName="org.acme.client.mtls.GreetingResource" \
    -Dextensions="rest-client, resteasy-jsonb, kubernetes-client" \
    -Dpath="/hello-client"
```

Certificate and Truststore generation

Of course, you need a server, client certificates and a truststore :)

```
keytool -genkeypair -storepass password -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore quarkus-server-mtls/src/main/resources/META-INF/resources/server.keystore


keytool -genkeypair -storepass password -keyalg RSA -keysize 2048 -dname "CN=client" -alias client -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore quarkus-client-mtls/src/main/resources/META-INF/resources/client.keystore
```
For this example, we are simulating a truststore using:

- client.keystoreas truststore for the server application.
- server.keystoreas truststore for the client application.
```
cp quarkus-server-mtls/src/main/resources/META-INF/resources/server.keystore quarkus-client-mtls/src/main/resources/META-INF/resources/client.truststore


cp quarkus-client-mtls/src/main/resources/META-INF/resources/client.keystore quarkus-server-mtls/src/main/resources/META-INF/resources/server.truststore
```

Hello Server Application

Let’s open and configure the server quarkus-server-mtls


Enable SSL on the Hello Server Application

Add the following properties to enable SSL in your application src/main/resources/application.properties

application.properties
```
quarkus.ssl.native=true


quarkus.http.ssl-port=8443
quarkus.http.ssl.certificate.key-store-file=META-INF/resources/server.keystore
quarkus.http.ssl.certificate.key-store-password=password


quarkus.http.port=0
quarkus.http.test-port=0
```

See the guide Using SSL with Nativeto explore in details how SSL works in Quarkus.

Activate client authentication

application.properties
```
quarkus.http.ssl.client-auth=required
quarkus.http.ssl.certificate.trust-store-file=META-INF/resources/server.truststore
quarkus.http.ssl.certificate.trust-store-password=password
```

Update GreetingResource

To better see that the response is coming from the server application, let’s update the org.acme.server.mtls.GreetingResourceclass.

org.acme.server.mtls.GreetingResource
```
@Path("/hello-server")
public class GreetingResource {


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello from server"; 
    }
}
```

Change the return statement in "hello from server"
And the test class.

org.acme.server.mtls.GreetingResourceTest
```
@QuarkusTest
public class GreetingResourceTest {


    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello-server")
          .then()
             .statusCode(200)
             .body(is("hello from server")); 
    }


}
```

Change the matcher in "hello from server"

Run it

```
mvn quarkus:dev
```
If everything is correct when you try to hit the /hello-serverendpoint, you should expect the following error.

```
curl -k https://localhost:8443/hello-server
curl: (35) error:1401E412:SSL routines:CONNECT_CR_FINISHED:sslv3 alert bad certificate
```
This means that your client (curl) did not provide a trusted certificate when it connected to the server.


Hello Client Application

At this point, the server application is ready to accomplish Mutual TLS. Let’s open and configure the client quarkus-client-mtls


Add Rest client for the server application

org.acme.client.mtls.GreetingService
```
@Path("/")
@ApplicationScoped
@RegisterRestClient
public interface GreetingService {


    @GET
    @Path("/hello-server")
    @Produces(MediaType.TEXT_PLAIN)
    String hello();
}
```
Inject the GreetingService rest client on org.acme.client.mtls.GreetingResource.

org.acme.client.mtls.GreetingResource
```
@Path("/hello-client")
public class GreetingResource {


    @Inject 
    @RestClient 
    GreetingService greetingService;


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return greetingService.hello(); 
    }
}
```


Update the unit test

Add quarkus-junit5-mockitodependency to your project.

pom.xml
```
    <dependency>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-junit5-mockito</artifactId>
    </dependency>
```

org.acme.client.mtls.GreetingResourceTest
```
@QuarkusTest
public class GreetingResourceTest {


    @InjectMock 
    @RestClient 
    GreetingService greetingService;


    @Test
    public void testHelloEndpoint() {
        Mockito.when(greetingService.hello()).thenReturn("hello from server"); 


        given()
          .when().get("/hello-client")
          .then()
             .statusCode(200)
             .body(is("hello from server"));
    }


}
```


Configure MicroProfile rest client for Mutual TLS

Add the following properties to enable SSL in your application src/main/resources/application.properties

application.properties
```
org.acme.client.mtls.GreetingService/mp-rest/url=https://localhost:8443
org.acme.client.mtls.GreetingService/mp-rest/trustStore=classpath:/META-INF/resources/client.truststore
org.acme.client.mtls.GreetingService/mp-rest/trustStorePassword=password
org.acme.client.mtls.GreetingService/mp-rest/keyStore=classpath:/META-INF/resources/client.keystore
org.acme.client.mtls.GreetingService/mp-rest/keyStorePassword=password


quarkus.ssl.native=true
```

Run it

```
mvn quarkus:dev
```
Now let’s hit the client /hello-client endpoint, and you should expect the following.

```
curl http://localhost:8080/hello-client
hello from server
```
