package org.gbif.collections.duplicates.issues;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.collections.duplicates.Config;
import org.gbif.registry.ws.client.collections.InstitutionClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.common.base.Strings;
import lombok.SneakyThrows;

public class IssueFactory {

  private static final String NEW_LINE = "\n";
  private static final String TRIPLE_QUOTE_SEPARATOR = "```";
  private static final String QUOTE_SEPARATOR = "`";
  private static final String LIST_BULLET = "- ";
  private static final String CHECKBOX = "- [ ] ";

  // labels
  private static final String CONTAINS_INACTIVE_LABEL = "Contains inactive record";
  private static final String CONTAINS_IH_LABEL = "Contains IH record";
  private static final String CONTAINS_IDIGBIO_LABEL = "Contains IDigBio record";
  public static final String INSTITUTION_LABEL = "Institution";
  public static final String COLLECTION_LABEL = "Collection";
  public static final String MASTER_ISSUE_LABEL = "Master";

  private final Config config;
  private final InstitutionClient institutionClient;

  public IssueFactory(Config config, InstitutionClient institutionClient) {
    this.config = config;
    this.institutionClient = institutionClient;
  }

  public Issue createIssue(
      Set<Duplicate> duplicates, IssueType issueType, DuplicatesRequest request) {

    StringBuilder duplicatesBody = new StringBuilder();
    Set<String> labels = new HashSet<>();
    labels.add(issueType.label);

    // keep track of values used to find duplicates for the fields that can have different values
    // but are considered to be the same (fuzzy names, physical vs mailing city, etc.)
    Map<String, Integer> names = new HashMap<>();
    Map<String, Integer> cities = new HashMap<>();
    Map<String, Integer> countries = new HashMap<>();

    duplicates.forEach(
        duplicate -> {
          // link to the entity
          String entityLink =
              config.getRegistryPortalUrl() + "/" + issueType.urlPath + "/" + duplicate.getKey();
          duplicatesBody
              .append(createLink(entityLink, duplicate.getName() + " - " + duplicate.getCode()))
              .append(NEW_LINE);

          // duplicate info
          duplicatesBody.append(TRIPLE_QUOTE_SEPARATOR).append(NEW_LINE);
          duplicatesBody.append(duplicateToString(duplicate));
          duplicatesBody.append(TRIPLE_QUOTE_SEPARATOR).append(NEW_LINE);

          // add labels
          if (!duplicate.isActive()) {
            labels.add(CONTAINS_INACTIVE_LABEL);
          }
          if (duplicate.isIh()) {
            labels.add(CONTAINS_IH_LABEL);
          }
          if (duplicate.isIdigbio()) {
            labels.add(CONTAINS_IDIGBIO_LABEL);
          }

          // country labels
          if (duplicate.getPhysicalCountry() != null) {
            labels.add("Country " + duplicate.getPhysicalCountry().getIso2LetterCode());
          }
          if (duplicate.getMailingCountry() != null) {
            labels.add("Country " + duplicate.getMailingCountry().getIso2LetterCode());
          }

          // keep track of the values used to find the duplicates
          names.compute(duplicate.getName(), (k, v) -> v == null ? 1 : v + 1);

          if (!Strings.isNullOrEmpty(duplicate.getPhysicalCity())) {
            cities.compute(duplicate.getPhysicalCity(), (k, v) -> v == null ? 1 : v + 1);
          }
          if (!Strings.isNullOrEmpty(duplicate.getMailingCity())) {
            cities.compute(duplicate.getMailingCity(), (k, v) -> v == null ? 1 : v + 1);
          }
          if (duplicate.getPhysicalCountry() != null) {
            countries.compute(
                duplicate.getPhysicalCountry().getIso2LetterCode(),
                (k, v) -> v == null ? 1 : v + 1);
          }
          if (duplicate.getMailingCountry() != null) {
            countries.compute(
                duplicate.getMailingCountry().getIso2LetterCode(), (k, v) -> v == null ? 1 : v + 1);
          }
        });

    // get info about the params used in the request
    ParamsUsed paramsUsed =
        getParamsUsed(request, duplicates.iterator().next(), names, cities, countries);

    // add a label with the params used
    labels.add(paramsUsed.label);

    // create the tile based on the params used
    String title = buildTitle(paramsUsed);

    // duplicates page in the UI
    String duplicatesLink =
        config.getRegistryPortalUrl()
            + "/"
            + issueType.urlPath
            + "/search?"
            + paramsUsed.queryParams;

    // create body
    String body =
        createLink(duplicatesLink, "Duplicates")
            + " found for: "
            + NEW_LINE
            + paramsUsedToString(paramsUsed)
            + NEW_LINE
            + duplicatesBody.toString();

    return Issue.builder()
        .title(title)
        .body(body)
        .labels(labels)
        .assignees(config.getGhIssuesAssignees())
        .build();
  }

  public Issue createMasterIssue(
      List<CreatedIssueResponse> issues, DuplicatesRequest request, IssueType issueType) {
    StringBuilder body = new StringBuilder();

    body.append("List of duplicates issues created for execution on ")
        .append(QUOTE_SEPARATOR)
        .append(LocalDateTime.now())
        .append(QUOTE_SEPARATOR)
        .append(" with parameters: ");
    body.append(duplicatesRequestToString(request));

    body.append(issues.size() + " issues created: ");
    issues.forEach(
        issue ->
            body.append(NEW_LINE)
                .append(CHECKBOX)
                .append("#")
                .append(issue.getNumber())
                .append(" ")
                .append(issue.getTitle().replaceAll("\n", "")));

    String title = duplicatesRequestToStringInline(request);

    Set<String> labels = new HashSet<>();
    labels.add(MASTER_ISSUE_LABEL);
    labels.add(issueType.label);
    labels.add(LocalDateTime.now().toString());

    return Issue.builder()
        .title(title)
        .body(body.toString())
        .labels(labels)
        .assignees(config.getGhIssuesAssignees())
        .build();
  }

  private String buildTitle(ParamsUsed paramsUsed) {
    StringBuilder title = new StringBuilder();

    title.append(NEW_LINE);
    boolean first = true;
    for (String value : paramsUsed.paramsUsedAsString.values()) {
      if (!first) {
        title.append(" - ");
      }
      title.append(value);
      first = false;
    }
    title.append(NEW_LINE);

    return title.toString();
  }

  @SneakyThrows
  private ParamsUsed getParamsUsed(
      DuplicatesRequest request,
      Duplicate defaultDuplicate,
      Map<String, Integer> names,
      Map<String, Integer> cities,
      Map<String, Integer> countries) {

    ParamsUsed paramsUsed = new ParamsUsed();
    Map<ParamValue, String> paramsUsedMap = new EnumMap<>(ParamValue.class);
    paramsUsed.paramsUsedAsString = paramsUsedMap;

    StringBuilder label = new StringBuilder();
    Consumer<String> appendToLabel =
        value -> {
          if (label.length() != 0) {
            label.append("And");
          }

          label.append(value);
        };

    StringBuilder queryParams = new StringBuilder();
    BiConsumer<String, String> appendQueryParam =
        (field, value) -> {
          if (queryParams.length() != 0) {
            queryParams.append("&");
          }
          try {
            queryParams
                .append(field)
                .append("=")
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()));
          } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
          }
        };

    if (Boolean.TRUE.equals(request.getSameFuzzyName())) {
      String duplicateValue = getMostCommonValue(names);
      paramsUsedMap.put(ParamValue.SAME_FUZZY_NAME, duplicateValue);
      appendToLabel.accept("SameFuzzyName");
      appendQueryParam.accept("fuzzyName", duplicateValue);
    }
    if (Boolean.TRUE.equals(request.getSameName())) {
      String duplicateValue = getMostCommonValue(names);
      paramsUsedMap.put(ParamValue.SAME_NAME, duplicateValue);
      appendToLabel.accept("SameName");
      appendQueryParam.accept("name", duplicateValue);
    }
    if (Boolean.TRUE.equals(request.getSameCode())) {
      String duplicateValue = defaultDuplicate.getCode();
      paramsUsedMap.put(ParamValue.SAME_CODE, duplicateValue);
      appendToLabel.accept("SameCode");
      appendQueryParam.accept("code", duplicateValue);
    }
    if (Boolean.TRUE.equals(request.getSameCity())) {
      String duplicateValue = getMostCommonValue(cities);
      paramsUsedMap.put(ParamValue.SAME_CITY, duplicateValue);
      appendToLabel.accept("SameCity");
      appendQueryParam.accept("city", duplicateValue);
    }
    if (Boolean.TRUE.equals(request.getSameCountry())) {
      String duplicateValue = getMostCommonValue(countries);
      paramsUsedMap.put(ParamValue.SAME_COUNTRY, duplicateValue);
      appendToLabel.accept("SameCountry");
      appendQueryParam.accept("country", duplicateValue);
    }
    if (Boolean.TRUE.equals(request.getSameInstitution())) {
      Institution institution = institutionClient.get(defaultDuplicate.getInstitutionKey());
      paramsUsed.institution = institution;
      paramsUsedMap.put(ParamValue.SAME_INSTITUTION, institution.getName());
      appendToLabel.accept("SameInstitution");
      appendQueryParam.accept("institution", defaultDuplicate.getInstitutionKey().toString());
    }

    paramsUsed.label = label.toString();
    paramsUsed.queryParams = queryParams.toString();

    return paramsUsed;
  }

  private String getMostCommonValue(Map<String, Integer> map) {
    return map.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(map.keySet().iterator().next());
  }

  private String paramsUsedToString(ParamsUsed paramsUsed) {
    StringBuilder sb = new StringBuilder();
    paramsUsed.paramsUsedAsString.forEach(
        (k, v) -> {
          String value;
          if (k == ParamValue.SAME_INSTITUTION) {
            String institutionLink =
                config.getRegistryPortalUrl()
                    + "/institution/"
                    + paramsUsed.institution.getKey().toString();
            value =
                createLink(
                    institutionLink,
                    paramsUsed.institution.getName() + " - " + paramsUsed.institution.getCode());
          } else {
            value = QUOTE_SEPARATOR + v + QUOTE_SEPARATOR;
          }

          sb.append(LIST_BULLET).append(k.text).append(": ").append(value).append(NEW_LINE);
        });
    return sb.toString();
  }

  private String duplicateToString(Duplicate duplicate) {
    StringBuilder sb = new StringBuilder(NEW_LINE);

    if (duplicate.getKey() != null) {
      sb.append("Key: ").append(duplicate.getKey()).append(NEW_LINE);
    }
    if (duplicate.getCode() != null) {
      sb.append("Code: ").append(duplicate.getCode()).append(NEW_LINE);
    }
    if (duplicate.getName() != null) {
      sb.append("Name: ").append(duplicate.getName()).append(NEW_LINE);
    }
    if (duplicate.getPhysicalCountry() != null) {
      sb.append("Physical country: ").append(duplicate.getPhysicalCountry()).append(NEW_LINE);
    }
    if (duplicate.getPhysicalCity() != null) {
      sb.append("Physical city: ").append(duplicate.getPhysicalCity()).append(NEW_LINE);
    }
    if (duplicate.getMailingCountry() != null) {
      sb.append("Mailing country: ").append(duplicate.getMailingCountry()).append(NEW_LINE);
    }
    if (duplicate.getMailingCity() != null) {
      sb.append("Mailing city: ").append(duplicate.getMailingCity()).append(NEW_LINE);
    }

    sb.append("Active: ").append(duplicate.isActive()).append(NEW_LINE);
    sb.append("Is IH: ").append(duplicate.isIh()).append(NEW_LINE);
    sb.append("Is IDigBio: ").append(duplicate.isIdigbio()).append(NEW_LINE);

    return sb.toString();
  }

  private String duplicatesRequestToString(DuplicatesRequest request) {
    StringBuilder res = new StringBuilder();

    res.append(NEW_LINE);
    if (Boolean.TRUE.equals(request.getSameFuzzyName())) {
      res.append(LIST_BULLET).append("Same Fuzzy Name").append(NEW_LINE);
    }
    if (Boolean.TRUE.equals(request.getSameName())) {
      res.append(LIST_BULLET).append("Same Name").append(NEW_LINE);
    }
    if (Boolean.TRUE.equals(request.getSameCode())) {
      res.append(LIST_BULLET).append("Same Code").append(NEW_LINE);
    }
    if (Boolean.TRUE.equals(request.getSameCity())) {
      res.append(LIST_BULLET).append("Same City").append(NEW_LINE);
    }
    if (Boolean.TRUE.equals(request.getSameCountry())) {
      res.append(LIST_BULLET).append("Same Country").append(NEW_LINE);
    }
    if (Boolean.TRUE.equals(request.getSameInstitution())) {
      res.append(LIST_BULLET).append("Same Institution").append(NEW_LINE);
    }
    if (request.getInCountries() != null && !request.getInCountries().isEmpty()) {
      res.append(LIST_BULLET)
          .append("In Countries: ")
          .append(request.getInCountries())
          .append(NEW_LINE);
    }
    if (request.getNotInCountries() != null && !request.getNotInCountries().isEmpty()) {
      res.append(LIST_BULLET)
          .append("Not in Countries: ")
          .append(request.getNotInCountries())
          .append(NEW_LINE);
    }
    if (request.getExcludeKeys() != null && !request.getExcludeKeys().isEmpty()) {
      res.append(LIST_BULLET)
          .append("Exclude Keys: ")
          .append(request.getExcludeKeys())
          .append(NEW_LINE);
    }
    if (request.getInInstitutions() != null && !request.getInInstitutions().isEmpty()) {
      res.append(LIST_BULLET)
          .append("In Institutions: ")
          .append(request.getInInstitutions())
          .append(NEW_LINE);
    }
    if (request.getNotInInstitutions() != null && !request.getNotInInstitutions().isEmpty()) {
      res.append(LIST_BULLET)
          .append("Not in Institutions: ")
          .append(request.getNotInInstitutions())
          .append(NEW_LINE);
    }

    res.append(NEW_LINE);

    return res.toString();
  }

  private String duplicatesRequestToStringInline(DuplicatesRequest request) {
    StringBuilder res = new StringBuilder();

    Consumer<String> appendToLabel =
        value -> {
          if (res.length() != 0) {
            res.append(" and ");
          }
          res.append(value);
        };

    if (Boolean.TRUE.equals(request.getSameFuzzyName())) {
      appendToLabel.accept("Same Fuzzy Name");
    }
    if (Boolean.TRUE.equals(request.getSameName())) {
      appendToLabel.accept("Same Name");
    }
    if (Boolean.TRUE.equals(request.getSameCode())) {
      appendToLabel.accept("Same Code");
    }
    if (Boolean.TRUE.equals(request.getSameCity())) {
      appendToLabel.accept("Same City");
    }
    if (Boolean.TRUE.equals(request.getSameCountry())) {
      appendToLabel.accept("Same Country");
    }
    if (Boolean.TRUE.equals(request.getSameInstitution())) {
      appendToLabel.accept("Same Institution");
    }

    return res.toString();
  }

  private String createLink(String link, String name) {
    return "[" + name + "](" + link + ")";
  }

  public enum IssueType {
    INSTITUTION("institution", INSTITUTION_LABEL),
    COLLECTION("collection", COLLECTION_LABEL);

    private final String urlPath;
    private final String label;

    IssueType(String urlPath, String label) {
      this.urlPath = urlPath;
      this.label = label;
    }
  }

  public enum ParamValue {
    SAME_CODE("Same code"),
    SAME_FUZZY_NAME("Same fuzzy name"),
    SAME_NAME("Same name"),
    SAME_CITY("Same city"),
    SAME_COUNTRY("Same country"),
    SAME_INSTITUTION("Same institution");

    private final String text;

    ParamValue(String text) {
      this.text = text;
    }
  }

  private static class ParamsUsed {
    Map<ParamValue, String> paramsUsedAsString;
    private String label;
    private String queryParams;
    // only for collections
    Institution institution;
  }
}
