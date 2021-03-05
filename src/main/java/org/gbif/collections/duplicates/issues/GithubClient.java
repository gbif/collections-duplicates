package org.gbif.collections.duplicates.issues;

import org.gbif.collections.duplicates.Config;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.*;

/** Lightweight client for the Github API. */
@Slf4j
public class GithubClient {

  private static final ConcurrentMap<Config, GithubClient> clientsMap = new ConcurrentHashMap<>();
  private final API api;
  private final Set<String> assignees;

  private GithubClient(String githubWsUrl, String user, String password, Set<String> assignees) {
    Objects.requireNonNull(githubWsUrl);
    Objects.requireNonNull(user);
    Objects.requireNonNull(password);

    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    OkHttpClient okHttpClient =
        new OkHttpClient.Builder()
            .cache(null)
            .addInterceptor(new BasicAuthInterceptor(user, password))
            .build();

    Retrofit retrofit =
        new Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(githubWsUrl)
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build();
    api = retrofit.create(API.class);
    this.assignees = assignees;
  }

  public static GithubClient getInstance(Config config) {
    Objects.requireNonNull(config);
    GithubClient client = clientsMap.get(config);
    if (client != null) {
      return client;
    } else {
      GithubClient newClient =
          new GithubClient(
              config.getGithubWsUrl(),
              config.getGithubUser(),
              config.getGithubPassword(),
              config.getGhIssuesAssignees());
      clientsMap.put(config, newClient);
      return newClient;
    }
  }

  public CreatedIssueResponse createIssue(Issue issue) {
    if (assignees != null && !assignees.isEmpty()) {
      // we use the assignees from the config if they were set
      issue.setAssignees(assignees);
    }

    return syncCall(api.createIssue(issue));
  }

  public Optional<Issue> findIssueWithSameTitleAndLabels(String title, List<String> labels) {
    int page = 1;
    int perPage = 100;
    String state = "all";

    // first call
    List<CreatedIssueResponse> issues = syncCall(api.listIssues(labels, state, page, perPage));

    // paginate over issues till we find a match
    while (!issues.isEmpty()) {
      Optional<CreatedIssueResponse> match =
          issues.stream().filter(i -> title.equalsIgnoreCase(i.getTitle())).findFirst();
      if (match.isPresent()) {
        return match.map(
            ir ->
                Issue.builder()
                    .number(ir.getNumber())
                    .title(ir.getTitle())
                    .labels(
                        ir.getLabels().stream()
                            .map(CreatedIssueResponse.Label::getName)
                            .collect(Collectors.toSet()))
                    .assignees(
                        ir.getAssignees().stream()
                            .map(CreatedIssueResponse.Assignee::getLogin)
                            .collect(Collectors.toSet()))
                    .build());
      }

      issues = syncCall(api.listIssues(labels, state, page++, perPage));
    }

    return Optional.empty();
  }

  public Optional<CreatedIssueResponse> createIssueIfNotExists(Issue issue, String label) {
    Optional<Issue> existingIssueOpt =
        findIssueWithSameTitleAndLabels(issue.getTitle(), Collections.singletonList(label));
    if (!existingIssueOpt.isPresent()) {
      return Optional.of(createIssue(issue));
    }
    return Optional.empty();
  }

  private interface API {
    @POST("issues")
    Call<CreatedIssueResponse> createIssue(@Body Issue issue);

    @GET("issues")
    Call<List<CreatedIssueResponse>> listIssues(
        @Query("labels") List<String> labels,
        @Query("state") String state,
        @Query("page") int page,
        @Query("per_page") int perPage);
  }

  private static <T> T syncCall(Call<T> call) {
    try {
      Response<T> response = call.execute();
      if (response.isSuccessful()) {
        return response.body();
      }
      log.error("Service responded with an error {}", response);
      throw new HttpException(response); // Propagates the failed response
    } catch (IOException ex) {
      throw new IllegalStateException("Error executing call", ex);
    }
  }
}
