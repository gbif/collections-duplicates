package org.gbif.collections.duplicates;

import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.vocabulary.Country;
import org.gbif.collections.duplicates.issues.CreatedIssueResponse;
import org.gbif.collections.duplicates.issues.GithubClient;
import org.gbif.collections.duplicates.issues.Issue;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DuplicatesInspectorTest {

  @Disabled("manual test")
  @Test
  public void checkInstitutionDuplicatesTest() {
    Config config = new Config();
    config.setRegistryWsUrl("http://api.gbif-dev.org/v1/");
    config.setGithubWsUrl("https://api.github.com/repos/gbif/ih-sync-tests/");
    // set GH credentials
    config.setRegistryPortalUrl("https://registry.gbif-dev.org");

    DuplicatesInspector duplicatesInspector = new DuplicatesInspector(config);

    DuplicatesRequest request = new DuplicatesRequest();
    request.setSameName(true);
    request.setSameCity(true);
    request.setInCountries(Collections.singletonList(Country.FINLAND.getIso2LetterCode()));
    duplicatesInspector.inspectInstitutionDuplicates(request);
  }

  @Disabled("manual test")
  @Test
  public void changeTitleToAllIssues() {
    Config config = new Config();
    config.setRegistryWsUrl("http://api.gbif-dev.org/v1/");
    config.setGithubWsUrl("https://api.github.com/repos/gbif/ih-sync-tests/");
    // set GH credentials
    config.setRegistryPortalUrl("https://registry.gbif-dev.org");

    GithubClient githubClient = GithubClient.getInstance(config);
    List<CreatedIssueResponse> issues = githubClient.listIssues("closed");

    for (CreatedIssueResponse createdIssue : issues) {
      if (createdIssue.getTitle().startsWith("IGNORE")
          || createdIssue.getTitle().contains("iDigBio")
          || createdIssue.getTitle().contains("IH")) {
        continue;
      }
      Issue issue =
          Issue.builder()
              .number(createdIssue.getNumber())
              .title("IGNORE - " + createdIssue.getTitle())
              .labels(
                  createdIssue.getLabels().stream()
                      .map(CreatedIssueResponse.Label::getName)
                      .collect(Collectors.toSet()))
              .assignees(
                  createdIssue.getAssignees().stream()
                      .map(CreatedIssueResponse.Assignee::getLogin)
                      .collect(Collectors.toSet()))
              .build();
      githubClient.updateIssue(issue);
    }
  }
}
