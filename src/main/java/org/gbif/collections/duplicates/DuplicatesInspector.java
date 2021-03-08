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

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    log.info("Processing institution duplicates");
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
    log.info("Creating master issue for institution duplicates");
    Issue masterIssue =
        issueFactory.createMasterIssue(createdIssues, request, IssueFactory.IssueType.INSTITUTION);
    githubClient.createIssue(masterIssue);
  }

  public void inspectCollectionDuplicates(DuplicatesRequest request) {
    DuplicatesResult result = collectionClient.findPossibleDuplicates(request);

    log.info("Processing collection duplicates");
    List<CreatedIssueResponse> createdIssues = new ArrayList<>();
    result
        .getDuplicates()
        .forEach(
            d -> {
              // create issue
              Issue issue = issueFactory.createIssue(d, IssueFactory.IssueType.COLLECTION, request);
              githubClient
                  .createIssueIfNotExists(issue, IssueFactory.COLLECTION_LABEL)
                  .ifPresent(createdIssues::add);
            });

    // create master issue
    log.info("Creating master issue for collection duplicates");
    Issue masterIssue =
        issueFactory.createMasterIssue(createdIssues, request, IssueFactory.IssueType.COLLECTION);
    githubClient.createIssue(masterIssue);
  }
}
