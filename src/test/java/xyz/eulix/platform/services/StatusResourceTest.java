package xyz.eulix.platform.services;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class StatusResourceTest {

  @Inject
  ApplicationProperties properties;

  @Test
  void testStatusEndpoint() {
    given()
        .when()
        .get("/status")
        .then()
        .statusCode(200)
        .body("status", is("ok"))
        .body("version", is(properties.getVersion()));
  }
}