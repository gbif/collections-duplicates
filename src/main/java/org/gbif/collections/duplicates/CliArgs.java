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

  @Parameter(names = {"--sameName", "-sn"})
  private Boolean sameName;

  @Parameter(names = {"--sameFuzzyName", "-sfn"})
  private Boolean sameFuzzyName;

  @Parameter(names = {"--sameCode", "-sc"})
  private Boolean sameCode;

  @Parameter(names = {"--sameCountry", "-sco"})
  private Boolean sameCountry;

  @Parameter(names = {"--sameCity", "-sci"})
  private Boolean sameCity;

  @Parameter(names = {"--sameInstitution", "-si"})
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
    INSTITUTIONS,
    COLLECTIONS;
  }
}
