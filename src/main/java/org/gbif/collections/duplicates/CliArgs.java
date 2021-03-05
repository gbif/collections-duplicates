package org.gbif.collections.duplicates;

import java.util.Set;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CliArgs {

  @Parameter(
    names = {"--config", "-c"},
    required = true)
  private String confPath;

  @Parameter(names = {"--githubAssignees", "-ga"})
  private Set<String> githubAssignees;


}
