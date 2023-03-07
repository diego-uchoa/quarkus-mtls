package org.acme.client.mtls;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.eclipse.microprofile.rest.client.inject.RestClient;

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