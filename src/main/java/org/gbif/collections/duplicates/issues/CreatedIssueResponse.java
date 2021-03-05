package org.gbif.collections.duplicates.issues;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreatedIssueResponse {
  private long number;
  private String title;
  private List<Label> labels;
  private List<Assignee> assignees;

  @JsonProperty("html_url")
  private String htmlUrl;

  @Data
  public static class Label {
    private String name;
  }

  @Data
  public static class Assignee {
    private String login;
  }
}
