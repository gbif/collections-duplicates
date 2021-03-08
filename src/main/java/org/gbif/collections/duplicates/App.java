package org.gbif.collections.duplicates;

import org.gbif.api.model.collections.duplicates.DuplicatesRequest;

import com.beust.jcommander.JCommander;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

  public static void main(String[] args) {
    // parse args
    CliArgs cliArgs = new CliArgs();
    JCommander.newBuilder().addObject(cliArgs).build().parse(args);

    Config config = Config.fromCliArgs(cliArgs);
    DuplicatesRequest request = requestFromArgs(cliArgs);
    log.debug("Request created: {}", request);

    if (request.isEmpty()) {
      throw new IllegalArgumentException(
          "Empty request - at least one matching parameter has to be specified (sameName, sameCode, etc.) ");
    }

    DuplicatesInspector duplicatesInspector = new DuplicatesInspector(config);

    if (cliArgs.getEntityType().contains(CliArgs.EntityType.ALL)
        || cliArgs.getEntityType().contains(CliArgs.EntityType.INSTITUTIONS)) {
      log.info("Inspecting institution duplicates");
      duplicatesInspector.inspectInstitutionDuplicates(request);
    }

    if (cliArgs.getEntityType().contains(CliArgs.EntityType.ALL)
        || cliArgs.getEntityType().contains(CliArgs.EntityType.COLLECTIONS)) {
      log.info("Inspecting collection duplicates");
      duplicatesInspector.inspectCollectionDuplicates(request);
    }
  }

  private static DuplicatesRequest requestFromArgs(CliArgs cliArgs) {
    DuplicatesRequest request = new DuplicatesRequest();

    if (cliArgs.getSameName() != null) {
      request.setSameName(cliArgs.getSameName());
    }

    if (cliArgs.getSameFuzzyName() != null) {
      request.setSameFuzzyName(cliArgs.getSameFuzzyName());
    }

    if (cliArgs.getSameCode() != null) {
      request.setSameCode(cliArgs.getSameCode());
    }

    if (cliArgs.getSameCountry() != null) {
      request.setSameCountry(cliArgs.getSameCountry());
    }

    if (cliArgs.getSameCity() != null) {
      request.setSameCity(cliArgs.getSameCity());
    }

    if (cliArgs.getSameInstitution() != null) {
      request.setSameInstitution(cliArgs.getSameInstitution());
    }

    request.setInCountries(cliArgs.getInCountries());
    request.setNotInCountries(cliArgs.getNotInCountries());
    request.setExcludeKeys(cliArgs.getExcludeKeys());
    request.setInInstitutions(cliArgs.getInInstitutions());
    request.setNotInInstitutions(cliArgs.getNotInInstitutions());

    return request;
  }
}
