package org.gbif.collections.duplicates.issues;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Issue {
  @JsonIgnore private long number;
  private String title;
  private String body;
  private Set<String> assignees;
  private Set<String> labels;
}
