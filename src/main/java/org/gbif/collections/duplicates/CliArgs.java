package org.gbif.collections.duplicates;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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

  @Parameter(names = {"--sameName", "-sn"}, arity = 1)
  private Boolean sameName;

  @Parameter(names = {"--sameFuzzyName", "-sfn"}, arity = 1)
  private Boolean sameFuzzyName;

  @Parameter(names = {"--sameCode", "-sc"}, arity = 1)
  private Boolean sameCode;

  @Parameter(names = {"--sameCountry", "-sco"}, arity = 1)
  private Boolean sameCountry;

  @Parameter(names = {"--sameCity", "-sci"}, arity = 1)
  private Boolean sameCity;

  @Parameter(names = {"--sameInstitution", "-si"}, arity = 1)
  private Boolean sameInstitution;

  @Parameter(names = {"--inCountries", "-ic"})
  private List<String> inCountries;

  @Parameter(names = {"--notInCountries", "-nic"})
  private List<String> notInCountries;

  @Parameter(names = {"--excludeKeys", "-ek"})
  private List<UUID> excludeKeys;

  @Parameter(names = {"--inInstitutions", "-ii"})
  private List<UUID> inInstitutions;

  @Parameter(names = {"--notInInstitutions", "-nii"})
  private List<UUID> notInInstitutions;

  @Parameter(names = {"--entityType", "-et"})
  private Set<EntityType> entityType;

  public enum EntityType {
    ALL,
    INSTITUTIONS,
    COLLECTIONS;
  }
}
