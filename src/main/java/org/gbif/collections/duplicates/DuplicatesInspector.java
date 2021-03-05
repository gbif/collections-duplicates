package org.gbif.collections.duplicates;

import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.collections.duplicates.issues.CreatedIssueResponse;
import org.gbif.collections.duplicates.issues.GithubClient;
import org.gbif.collections.duplicates.issues.Issue;
import org.gbif.collections.duplicates.issues.IssueFactory;
import org.gbif.registry.ws.client.collections.CollectionClient;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.ws.client.ClientBuilder;

import java.util.ArrayList;
import java.util.List;

public class DuplicatesInspector {

  private final InstitutionClient institutionClient;
  private final CollectionClient collectionClient;
  private final GithubClient githubClient;
  private final IssueFactory issueFactory;

  public DuplicatesInspector(Config config) {
    ClientBuilder clientBuilder = new ClientBuilder();
    this.institutionClient =
        clientBuilder.withUrl(config.getRegistryWsUrl()).build(InstitutionClient.class);
    this.collectionClient =
        clientBuilder.withUrl(config.getRegistryWsUrl()).build(CollectionClient.class);
    this.githubClient = GithubClient.getInstance(config);
    this.issueFactory = new IssueFactory(config, institutionClient);
  }

  public void inspectInstitutionDuplicates(DuplicatesRequest request) {
    DuplicatesResult result = institutionClient.findPossibleDuplicates(request);

    List<CreatedIssueResponse> createdIssues = new ArrayList<>();
    result
        .getDuplicates()
        .forEach(
            d -> {
              // create issue
              Issue issue =
                  issueFactory.createIssue(d, IssueFactory.IssueType.INSTITUTION, request);
              githubClient
                  .createIssueIfNotExists(issue, IssueFactory.INSTITUTION_LABEL)
                  .ifPresent(createdIssues::add);
            });

    // create master issue
    Issue masterIssue = issueFactory.createMasterIssue(createdIssues, request);
    githubClient.createIssue(masterIssue);
  }

  public void inspectCollectionDuplicates(DuplicatesRequest request) {
    DuplicatesResult result = collectionClient.findPossibleDuplicates(request);

    List<CreatedIssueResponse> createdIssues = new ArrayList<>();
    result
        .getDuplicates()
        .forEach(
            d -> {
              // create issue
              Issue issue = issueFactory.createIssue(d, IssueFactory.IssueType.COLLECTION, request);
              githubClient
                  .createIssueIfNotExists(issue, IssueFactory.INSTITUTION_LABEL)
                  .ifPresent(createdIssues::add);
            });

    // create master issue
    Issue masterIssue = issueFactory.createMasterIssue(createdIssues, request);
    githubClient.createIssue(masterIssue);
  }
}
