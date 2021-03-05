package org.gbif.collections.duplicates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Setter
@Slf4j
public class Config {

  protected static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
  protected static final ObjectReader YAML_READER = YAML_MAPPER.readerFor(Config.class);

  private String registryWsUrl;
  private String githubWsUrl;
  private String githubUser;
  private String githubPassword;
  private String registryPortalUrl;
  private Set<String> ghIssuesAssignees = new HashSet<>();

  public static Config fromFileName(String configFileName) {
    checkArgument(!Strings.isNullOrEmpty(configFileName), "Config file path is required");

    File configFile = Paths.get(configFileName).toFile();
    Config config;
    try {
      config = YAML_READER.readValue(configFile);
    } catch (IOException e) {
      log.error("Couldn't load config from file {}", configFileName, e);
      throw new IllegalArgumentException("Couldn't load config file");
    }

    if (config == null) {
      throw new IllegalArgumentException("Config is empty");
    }

    validateConfig(config);

    return config;
  }

  public static Config fromCliArgs(CliArgs args) {
    Objects.requireNonNull(args);
    Config config = fromFileName(args.getConfPath());

    // GH assignees can be overwritten via cli
    if (args.getGithubAssignees() != null) {
      config.setGhIssuesAssignees(args.getGithubAssignees());
    }

    return config;
  }

  private static void validateConfig(Config config) {
    checkArgument(!Strings.isNullOrEmpty(config.getRegistryWsUrl()), "Registry ws url is required");
    checkArgument(!Strings.isNullOrEmpty(config.getGithubWsUrl()), "Github ws url is required");
    checkArgument(
        !Strings.isNullOrEmpty(config.getGithubUser())
            && !Strings.isNullOrEmpty(config.getGithubPassword()),
        "Github credentials are required");
    checkArgument(
        !Strings.isNullOrEmpty(config.getRegistryPortalUrl()), "Registry portal url is required");
  }
}
