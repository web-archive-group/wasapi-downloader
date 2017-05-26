package edu.stanford.dlss.was;

import java.util.List;

import static org.junit.Assert.*;
import org.junit.*;

import static org.hamcrest.CoreMatchers.*;

public class TestWasapiCrawlSelector {

  WasapiFile file1 = new WasapiFile();
  WasapiFile file2 = new WasapiFile();
  WasapiFile file3 = new WasapiFile();
  WasapiFile[] candidateFiles = {file1, file2, file3};

  @Before
  public void setup() {
    file1.setCrawlId(111);
    file1.setCrawlStartDateStr("2015-01-01T00:00:00Z");
    file2.setCrawlId(222);
    file2.setCrawlStartDateStr("2016-12-31T23:59:59Z");
    file3.setCrawlId(333);
    file3.setCrawlStartDateStr("2017-01-01T00:00:00Z");
  }

  @Test
  public void constructor_loadsCrawlIdToFiles() {
    WasapiCrawlSelector selector = new WasapiCrawlSelector(candidateFiles);
    assertTrue("crawlIdsToFiles should be populated", selector.crawlIdToFiles.size() == 3);
  }

  @Test
  public void addCandidateFiles_worksWithMultipleFilesPerCrawl() {
    file2.setCrawlId(file1.getCrawlId());
    file2.setCrawlStartDateStr(file1.getCrawlStartDateStr());
    WasapiCrawlSelector selector = new WasapiCrawlSelector(candidateFiles);
    assertTrue("crawlIdsToFiles should be populated", selector.crawlIdToFiles.size() == 2);
  }

  @Test
  public void getSelectedCrawlIds_onlyReturnsIdsLargerThanArg() {
    WasapiCrawlSelector selector = new WasapiCrawlSelector(candidateFiles);
    List<Integer> selectedIds = selector.getSelectedCrawlIds(127);
    assertThat("selectedIds should contain 222 and 333 when last known crawl is 127", selectedIds, hasItems(222, 333));
    assertFalse("selectedIds should not contain 111 when last known crawl is 127", selectedIds.contains(111));
  }

  @Test
  public void getSelectedCrawlIds_doesNotReturnIdEqualToArg() {
    WasapiCrawlSelector selector = new WasapiCrawlSelector(candidateFiles);
    List<Integer> selectedIds = selector.getSelectedCrawlIds(222);
    assertThat("selectedIds should contain 333 when last known crawl is 222", selectedIds, hasItem(333));
    assertFalse("selectedIds should not contain 222 when last known crawl is 222", selectedIds.contains(222));
    assertFalse("selectedIds should not contain 111 when last known crawl is 222", selectedIds.contains(111));  // for good measure
  }

  @Test
  public void getFilesForCrawl() {
    file2.setCrawlId(file1.getCrawlId());
    file2.setCrawlStartDateStr(file1.getCrawlStartDateStr());
    WasapiCrawlSelector selector = new WasapiCrawlSelector(candidateFiles);
    List<WasapiFile> files = selector.getFilesForCrawl(file1.getCrawlId());
    assertThat("files should contain file1 and file2", files, hasItems(file1, file2));
    assertThat("files should not contain file3", files, not(hasItem(file3)));
    files = selector.getFilesForCrawl(file3.getCrawlId());
    assertThat("files should contain file3", files, hasItem(file3));
    assertThat("files should not contain file2", files, not(hasItems(file1, file2)));
  }

  @Test
  public void getFilesForCrawl_crawlIdDoesNotExist() {
    WasapiCrawlSelector selector = new WasapiCrawlSelector(candidateFiles);
    List<WasapiFile> files = selector.getFilesForCrawl(0);
    assertNull("list should be null for non-existent crawl", files);
  }
}
