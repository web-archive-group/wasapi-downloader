package edu.stanford.dlss.was;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.UrlValidator;

@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity", "checkstyle:LineLength"})
public class WasapiDownloaderSettings {
  // to add a new setting:
  // * add a String constant for the setting/arg name
  // * add a corresponding Option entry in optList
  // * add an accessor method (preferably with a name corresponding to the setting name)
  // * add the appropriate validation in getSettingsErrorMessages()
  // * add tests to make sure it: shows up in usage info and settings dump; is checked for validity; has a value in example settings, if applicable
  public static final String HELP_PARAM_NAME = "help";
  public static final String ACCCOUNT_ID_PARAM_NAME = "accountId";
  public static final String AUTH_URL_PARAM_NAME = "authurl";
  public static final String BASE_URL_PARAM_NAME = "baseurl";
  public static final String CHECKSUM_ALGORITHM_PARAM_NAME = "checksumAlgorithm";
  public static final String COLLECTION_ID_PARAM_NAME = "collectionId";
  public static final String CRAWL_ID_PARAM_NAME = "crawlId";
  public static final String CRAWL_ID_LOWER_BOUND_PARAM_NAME = "crawlIdLowerBound";
  public static final String CRAWL_START_AFTER_PARAM_NAME = "crawlStartAfter";
  public static final String CRAWL_START_BEFORE_PARAM_NAME = "crawlStartBefore";
  public static final String FILENAME_PARAM_NAME = "filename";
  public static final String OUTPUT_BASE_DIR_PARAM_NAME = "outputBaseDir";
  public static final String PASSWORD_PARAM_NAME = "password";
  public static final String RETRIES_PARAM_NAME = "retries";
  public static final String USERNAME_PARAM_NAME = "username";

  protected PrintStream errStream = System.err;
  protected Properties settings;

  private HelpFormatter helpFormatter;
  private static Options wdsOpts;
  private String helpAndSettingsMessage;

  private static Option[] optList = {
    Option.builder("h").longOpt(HELP_PARAM_NAME).desc("print this message describing arguments and current configuration)").build(),
    buildArgOption(ACCCOUNT_ID_PARAM_NAME, "limit files to this account (e.g. when multiple accounts per username)"),
    buildArgOption(AUTH_URL_PARAM_NAME, "WASAPI server URL for login credentials"),
    buildArgOption(BASE_URL_PARAM_NAME, "base URL of WASAPI server (expects ending slash)"),
    buildArgOption(CHECKSUM_ALGORITHM_PARAM_NAME, "checksum algorithm to use (md5 or sha1"),
    buildArgOption(COLLECTION_ID_PARAM_NAME, "limit files to this collection"),
    buildArgOption(CRAWL_ID_PARAM_NAME, "limit files to this crawl id"),
    buildArgOption(CRAWL_ID_LOWER_BOUND_PARAM_NAME, "\"last crawl downloaded\": limit files to crawls with a higher crawl ID (not inclusive)"),
    buildArgOption(CRAWL_START_AFTER_PARAM_NAME, "limit files to crawls started after this date"),
    buildArgOption(CRAWL_START_BEFORE_PARAM_NAME, "limit files to crawls started before this date"),
    buildArgOption(FILENAME_PARAM_NAME, "name of single file to download"),
    buildArgOption(OUTPUT_BASE_DIR_PARAM_NAME, "destination directory for downloaded files (expects ending slash)"),
    buildArgOption(PASSWORD_PARAM_NAME, "password for WASAPI server login"),
    buildArgOption(RETRIES_PARAM_NAME, "how many times to retry a download for each file (retries + 1 = total tries)"),
    buildArgOption(USERNAME_PARAM_NAME, "username for WASAPI server login")
  };

  static {
    wdsOpts = new Options();
    for (Option opt : optList) {
      wdsOpts.addOption(opt);
    }
  }

  public WasapiDownloaderSettings(String settingsFileLocation, String[] args) throws SettingsLoadException {
    try {
      loadPropertiesFile(settingsFileLocation);
      parseArgsIntoSettings(args);
      validateSettings();
    } catch (IOException e) {
      throw new SettingsLoadException("Error reading properties file: " + e.getMessage(), e);
    } catch (ParseException e) {
      throw new SettingsLoadException("Error parsing command line arguments: " + e.getMessage(), e);
    }
  }

  protected WasapiDownloaderSettings() { } //for testing


  public void setErrStream(PrintStream errStream) {
    this.errStream = errStream;
  }

  public boolean shouldDisplayHelp() {
    return settings.getProperty(HELP_PARAM_NAME) != null;
  }

  public String accountId() {
    return settings.getProperty(ACCCOUNT_ID_PARAM_NAME);
  }

  public String authUrlString() {
    return settings.getProperty(AUTH_URL_PARAM_NAME);
  }

  public String baseUrlString() {
    return settings.getProperty(BASE_URL_PARAM_NAME);
  }

  public String checksumAlgorithm() {
    return settings.getProperty(CHECKSUM_ALGORITHM_PARAM_NAME);
  }

  public String collectionId() {
    return settings.getProperty(COLLECTION_ID_PARAM_NAME);
  }

  public String crawlId() {
    return settings.getProperty(CRAWL_ID_PARAM_NAME);
  }

  public String crawlIdLowerBound() {
    return settings.getProperty(CRAWL_ID_LOWER_BOUND_PARAM_NAME);
  }

  // e.g. 2014-01-01, see https://github.com/WASAPI-Community/data-transfer-apis/tree/master/ait-reference-specification#paths--examples
  public String crawlStartAfter() {
    return settings.getProperty(CRAWL_START_AFTER_PARAM_NAME);
  }

  // e.g. 2014-01-01, see https://github.com/WASAPI-Community/data-transfer-apis/tree/master/ait-reference-specification#paths--examples
  public String crawlStartBefore() {
    return settings.getProperty(CRAWL_START_BEFORE_PARAM_NAME);
  }

  public String filename() {
    return settings.getProperty(FILENAME_PARAM_NAME);
  }

  public String outputBaseDir() {
    return settings.getProperty(OUTPUT_BASE_DIR_PARAM_NAME);
  }

  public String password() {
    return settings.getProperty(PASSWORD_PARAM_NAME);
  }

  public String retries() {
    return settings.getProperty(RETRIES_PARAM_NAME);
  }

  public String username() {
    return settings.getProperty(USERNAME_PARAM_NAME);
  }


  public String getHelpAndSettingsMessage() {
    if (helpAndSettingsMessage == null)
      helpAndSettingsMessage = new StringBuilder(getCliHelpMessageCharSeq()).append(getSettingsSummaryCharSeq()).toString();

    return helpAndSettingsMessage;
  }

  @Override
  public String toString() {
    return getHelpAndSettingsMessage();
  }


  protected void validateSettings() throws SettingsLoadException {
    List<String> errMessages = getSettingsErrorMessages();
    if (errMessages.size() > 0) {
      StringBuilder buf = new StringBuilder("Invalid settings state:\n");
      for (String errMsg : errMessages) {
        buf.append("  ");
        buf.append(errMsg);
        buf.append(".\n");
      }
      throw new SettingsLoadException(buf.toString());
    }
  }

  @SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity", "checkstyle:MethodLength", "checkstyle:MultipleStringLiterals"})
  protected List<String> getSettingsErrorMessages() {
    String[] schemes = {"https"};
    UrlValidator urlValidator = new UrlValidator(schemes);
    IntegerValidator intValidator = new IntegerValidator();
    List<String> errMessages = new LinkedList<String>();

    // required
    if (isNullOrEmpty(baseUrlString()) || !urlValidator.isValid(baseUrlString()))
      errMessages.add(BASE_URL_PARAM_NAME + " is required, and must be a valid URL");
    if (isNullOrEmpty(authUrlString()) || !urlValidator.isValid(authUrlString()))
      errMessages.add(AUTH_URL_PARAM_NAME + " is required, and must be a valid URL");
    if (isNullOrEmpty(username()))
      errMessages.add(USERNAME_PARAM_NAME + " is required");
    if (isNullOrEmpty(password()))
      errMessages.add(PASSWORD_PARAM_NAME + " is required");
    if (isNullOrEmpty(outputBaseDir()) || !isDirWritable(outputBaseDir()))
      errMessages.add(OUTPUT_BASE_DIR_PARAM_NAME + " is required (and must be an extant, writable directory)");
    if (isNullOrEmpty(checksumAlgorithm()) || !("md5".equals(checksumAlgorithm())) || "sha1".equals(checksumAlgorithm()))
      errMessages.add(CHECKSUM_ALGORITHM_PARAM_NAME + " is required and must be md5 or sha1");
    if (isNullOrEmpty(retries()) || !intValidator.isValid(retries()) || !intValidator.minValue(Integer.valueOf(retries()), 0))
      errMessages.add(RETRIES_PARAM_NAME + " is required and must be an integer >= 0");

    // optional, validate if specified
    if (!isNullOrEmpty(accountId()) && !intValidator.isValid(accountId()))
      errMessages.add(ACCCOUNT_ID_PARAM_NAME + " must be an integer (if specified)");
    if (!isNullOrEmpty(collectionId()) && !intValidator.isValid(collectionId()))
      errMessages.add(COLLECTION_ID_PARAM_NAME + " must be an integer (if specified)");
    if (!isNullOrEmpty(crawlId()) && !intValidator.isValid(crawlId()))
      errMessages.add(CRAWL_ID_PARAM_NAME + " must be an integer (if specified)");
    if (!isNullOrEmpty(crawlStartBefore()) && !normalizeIso8601Setting(CRAWL_START_BEFORE_PARAM_NAME))
      errMessages.add(CRAWL_START_BEFORE_PARAM_NAME + " must be a valid ISO 8601 date string (if specified)");
    if (!isNullOrEmpty(crawlStartAfter()) && !normalizeIso8601Setting(CRAWL_START_AFTER_PARAM_NAME))
      errMessages.add(CRAWL_START_AFTER_PARAM_NAME + " must be a valid ISO 8601 date string (if specified)");
    if (!isNullOrEmpty(crawlIdLowerBound()) && !intValidator.isValid(crawlIdLowerBound()))
      errMessages.add(CRAWL_ID_LOWER_BOUND_PARAM_NAME + " must be an integer (if specified)");

    return errMessages;
  }

  protected static boolean isNullOrEmpty(String str) {
    return str == null || str.isEmpty();
  }

  protected static boolean isDirWritable(String dirPath) {
    File outputBaseDirFile = new File(dirPath);
    return outputBaseDirFile.exists() && outputBaseDirFile.isDirectory() && outputBaseDirFile.canWrite();
  }

  /**
   * Attempts to normalize an ISO 8601 date string setting to the format accepted by the WASAPI endpoint.  Prints a warning
   * if normalization results in a change to the setting value.
   *
   * @param settingName  the name of the setting field that contains the date string.  you should use a _PARAM_NAME constant.
   * @return boolean  indicating whether the setting string was successfully parsed and normalized (true if successful, false otherwise)
   */
  protected boolean normalizeIso8601Setting(String settingName) {
    try {
      String rawDateStr = settings.getProperty(settingName);
      String normalizedDateStr = normalizeIso8601StringForEndpoint(rawDateStr);
      if (!rawDateStr.equals(normalizedDateStr)) {
        settings.setProperty(settingName, normalizedDateStr);
        errStream.println("Normalized " + settingName + " to " + normalizedDateStr + " from " + rawDateStr + " (possible loss in time precision)");
      }
    } catch(IllegalArgumentException e) {
      return false;
    }
    return true;
  }


  /**
   * Takes an ISO 8601 date string, and normalizes it to a format acceptable by the WASAPI endpoint.
   * As of this writing, the WASAPI endpoint only accepts dates of the format yyyy-MM-dd.
   * Accepting any ISO 8601 date and normalizing it makes parsing (and testing of parsing) easier.
   *
   * @param settingName  the name of the setting field that contains the date string.  you should use one of the _PARAM_NAME constants.
   * @return a boolean indicating whether the setting string was successfully parsed and normalized (true if successful, false otherwise)
   */
  private static String normalizeIso8601StringForEndpoint(String rawDateStr) throws IllegalArgumentException {
    // neither the java.util. Date and Calendar classes, nor the apache commons
    // validator classes provide an easy way to parse or validate ISO 8601 date strings.
    // but javax.xml.bind.DatatypeConverter does it, since xsd:dateTime is ISO 8601.
    // https://www.w3.org/TR/xmlschema11-2/#dateTime
    // https://docs.oracle.com/javase/7/docs/api/javax/xml/bind/DatatypeConverter.html
    Calendar cal = DatatypeConverter.parseDateTime(rawDateStr);
    SimpleDateFormat wasapiFormat = new SimpleDateFormat("yyyy-MM-dd");
    return wasapiFormat.format(cal.getTime());
  }

  private CharSequence getCliHelpMessageCharSeq() {
    helpFormatter = new HelpFormatter();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    int width = HelpFormatter.DEFAULT_WIDTH;
    int leftPad = HelpFormatter.DEFAULT_LEFT_PAD;
    int descPad = HelpFormatter.DEFAULT_DESC_PAD;
    String helpMsgHeader = "=====\nallowed arguments\n(may also also be specified in config/settings.properties, sans leading hyphens)\n---";
    helpFormatter.printHelp(pw, width, "bin/wasapi-downloader", helpMsgHeader, wdsOpts, leftPad, descPad, "=====", true);
    pw.flush();
    sw.flush();
    return sw.getBuffer();
  }

  private CharSequence getSettingsSummaryCharSeq() {
    StringBuilder buf = new StringBuilder();
    buf.append("=====\n");
    buf.append("current settings (arg values override settings.properties values)\n");
    buf.append("---\n");
    for (String settingName : settings.stringPropertyNames()) {
      if (!settingName.equals(PASSWORD_PARAM_NAME)) {
        buf.append(settingName);
        buf.append(" : ");
        buf.append(settings.getProperty(settingName));
      } else {
        buf.append(settingName);
        buf.append(" : [password hidden]");
      }
      buf.append("\n");
    }
    buf.append("\n=====\n");

    return buf;
  }

  private static Option buildArgOption(String optionName, String description) {
    return Option.builder().hasArg().longOpt(optionName).desc(description).build();
  }

  private void addParsedArgsToSettings(CommandLine parsedArgs) {
    for (Option opt : parsedArgs.getOptions()) {
      String optName = opt.getLongOpt();
      String optValue = parsedArgs.getOptionValue(optName);
      if (optValue != null)
        settings.setProperty(optName, optValue);
    }

    if (parsedArgs.hasOption(HELP_PARAM_NAME))
      settings.setProperty(HELP_PARAM_NAME, "true");
  }

  private void parseArgsIntoSettings(String[] args) throws ParseException {
    CommandLineParser cliParser = new DefaultParser();
    CommandLine parsedArgs = cliParser.parse(wdsOpts, args);
    addParsedArgsToSettings(parsedArgs);
  }

  private void loadPropertiesFile(String settingsFileLocation) throws IOException {
    if (settings == null) {
      InputStream input = null;
      try {
        input = new FileInputStream(settingsFileLocation);
        settings = new Properties();
        settings.load(input);
      } finally {
        if (input != null)
          input.close();
      }
    } else {
      // should never get here, since this method is private, and only gets called once from the constructor
      errStream.println("Properties already loaded from " + settingsFileLocation);
    }
  }
}
