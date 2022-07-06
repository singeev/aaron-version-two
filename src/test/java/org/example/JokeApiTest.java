package org.example;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.example.response.Category;
import org.example.response.DataResponse;
import org.example.response.JokeMeta;
import org.junit.jupiter.api.Test;

@Slf4j
class JokeApiTest {

  private static final String API_HOST_HEADER = "x-rapidapi-host";
  private static final String API_HOST_VALUE = "jokes.p.rapidapi.com";
  private static final String API_KEY_HEADER = "x-rapidapi-key";
  private static final String API_KEY_VALUE = "56d7a4653emsh4c19b463b18e6b7p144eb7jsn030e478c59b2";

  private final URI apiUri = URI.create("https://jokes.p.rapidapi.com");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSuccessfullyGetResponseFromTestEndpoint() {

    var response = performGetRequest(apiUri.resolve("/jod/test"));
    assertTrue(response.isPresent());

    var data = response.get();
    assertAll(
        () -> assertNull(data.getError()),
        () -> assertNotEquals(404, data.getError().getCode()),
        () -> assertNotEquals("Not Found", data.getError().getMessage()));
  }

  @Test
  void shouldFetchJokeByCategoryAndWriteItToFile() throws URISyntaxException {

    var categoriesResponse = performGetRequest(apiUri.resolve("/jod/categories"));
    assertTrue(categoriesResponse.isPresent());
    assertNull(categoriesResponse.get().getError());

    List<Category> categories = categoriesResponse.get().getContents().getCategories();
    log.info(
        "Got {} categories from server: {}",
        categories.size(),
        categories.stream().map(Category::getName).collect(Collectors.joining(", ")));

    var categoryName = categories.get(2).getName();
    log.info("Fetching jokes by category name: {}", categoryName);

    var uri = new URIBuilder(apiUri.resolve("/jod")).addParameter("category", categoryName).build();

    var jokesResponse = performGetRequest(uri);
    assertTrue(jokesResponse.isPresent());
    assertNull(jokesResponse.get().getError());

    List<JokeMeta> jokes = jokesResponse.get().getContents().getJokes();
    assertDoesNotThrow(() -> writeJokesToFile(jokes));
  }

  private Optional<DataResponse> performGetRequest(URI uri) {
    try (var client = HttpClients.createDefault()) {
      HttpGet request = new HttpGet(uri);
      request.setHeader(API_HOST_HEADER, API_HOST_VALUE);
      request.setHeader(API_KEY_HEADER, API_KEY_VALUE);

      try (var response = client.execute(request)) {
        assertEquals(200, response.getStatusLine().getStatusCode());
        return deserializeResponse(response);
      }

    } catch (IOException e) {
      log.error("Failed to get response from URI: {}", uri.toString());
      throw new RuntimeException(e);
    }
  }

  private Optional<DataResponse> deserializeResponse(CloseableHttpResponse response)
      throws IOException {
    var entity = response.getEntity();
    if (entity != null) {
      String json = EntityUtils.toString(entity);
      var dataResponse = objectMapper.readValue(json, DataResponse.class);
      return of(dataResponse);
    }
    return empty();
  }

  private void writeJokesToFile(List<JokeMeta> jokes) {
    jokes.forEach(
        joke -> {
          String path = "src/test/resources/" + joke.getJoke().getId() + ".txt";
          StringJoiner content = new StringJoiner("\r\n");

          content.add("Description: " + joke.getDescription());
          content.add("Category: " + joke.getCategory());
          content.add("Title: " + joke.getJoke().getTitle());
          content.add("Text: " + joke.getJoke().getText());

          try {
            Files.writeString(Paths.get(path), content.toString());
            log.info("Text file with a new joke successfully created: {}", path);
          } catch (IOException e) {
            log.error("Can't write joke to a file: ", e);
            throw new RuntimeException(e);
          }

          var jokeFile = new File(path);
          assertTrue(jokeFile.exists());
          assertTrue(jokeFile.length() > 100);
        });
  }
}
