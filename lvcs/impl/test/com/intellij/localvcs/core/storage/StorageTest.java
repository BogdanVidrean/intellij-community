package com.intellij.localvcs.core.storage;

import com.intellij.localvcs.core.ContentHolder;
import com.intellij.localvcs.core.LocalVcs;
import com.intellij.localvcs.core.TempDirTestCase;
import com.intellij.localvcs.core.changes.ChangeSet;
import com.intellij.localvcs.core.changes.CreateDirectoryChange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class StorageTest extends TempDirTestCase {
  Storage s;
  LocalVcs.Memento m = new LocalVcs.Memento();

  @Before
  public void setUp() {
    initStorage();
  }

  @After
  public void tearDown() {
    if (s != null) s.close();
  }

  @Test
  public void testCleanStorage() {
    m = s.load();

    assertTrue(m.myRoot.getChildren().isEmpty());
    assertEquals(0, m.myEntryCounter);
    assertTrue(m.myChangeList.getChanges().isEmpty());
  }

  @Test
  public void testSaving() {
    ChangeSet cs = cs(new CreateDirectoryChange(1, "dir"));
    cs.applyTo(m.myRoot);
    m.myChangeList.addChange(cs);
    m.myEntryCounter = 11;

    s.store(m);

    initStorage();
    m = s.load();

    assertEquals(1, m.myRoot.getChildren().size());
    assertEquals(11, m.myEntryCounter);
    assertEquals(1, m.myChangeList.getChanges().size());
  }

  @Test
  public void testCreatingAbsentDirs() {
    File dir = new File(tempDir, "dir1/dir2/dir3");
    assertFalse(dir.exists());

    m.myEntryCounter = 111;

    initStorage(dir);
    s.store(m);

    assertTrue(dir.exists());
  }

  @Test
  public void testCleaningStorageOnVersionChange() {
    initStorage(123);

    m.myEntryCounter = 111;
    s.store(m);

    initStorage(666);

    m = s.load();
    assertEquals(0, m.myEntryCounter);
  }

  @Test
  public void testDoesNotCleanStorageWithProperVersion() {
    initStorage(123);

    m.myEntryCounter = 111;
    s.store(m);

    initStorage(123);

    m = s.load();
    assertEquals(111, m.myEntryCounter);
  }

  @Test
  public void testCreatingContent() {
    Content c = s.storeContent(ch("abc"));
    assertEquals("abc".getBytes(), c.getBytes());
  }

  @Test
  public void testCreatingLongContent() {
    Content c = s.storeContent(bigContentHolder());
    assertEquals(UnavailableContent.class, c.getClass());
  }

  @Test
  public void testDoesNotAcquireContentOfItIsTooLong() {
    final boolean[] isCalled = new boolean[]{false};
    ContentHolder h = new ContentHolder() {
      public byte[] getBytes() {
        isCalled[0] = true;
        return null;
      }

      public long getLength() {
        return bigContentHolder().getLength();
      }
    };
    s.storeContent(h);
    assertFalse(isCalled[0]);
  }

  @Test
  public void testPurgingContents() {
    Content c1 = s.storeContent(ch("1"));
    Content c2 = s.storeContent(ch("2"));
    Content c3 = s.storeContent(ch("3"));
    s.purgeContents(Arrays.asList(c1, c3));

    assertTrue(s.isContentPurged(c1));
    assertFalse(s.isContentPurged(c2));
    assertTrue(s.isContentPurged(c3));
  }

  @Test
  public void testRecreationOfStorageOnLoadingError() {
    Content c = s.storeContent(ch("abc"));
    m.myEntryCounter = 10;
    s.store(m);
    s.close();

    corruptFile("storage");

    initStorage();
    m = s.load();
    assertEquals(0, m.myEntryCounter);

    assertEquals(c.getId(), s.storeContent(ch("abc")).getId());
  }

  @Test
  public void testRecreationOfStorageOnContentLoadingError() {
    Content c = s.storeContent(ch("abc"));
    m.myEntryCounter = 10;
    s.store(m);
    s.close();

    corruptFile("contents");
    initStorage();
    try {
      s.loadContentData(c.getId());
      fail();
    }
    catch (IOException e) {
    }

    initStorage();
    m = s.load();

    assertEquals(0, m.myEntryCounter);
  }

  @Test
  public void testThrowingExceptionForGoodContentWhenContentStorageIsBroken() {
    Content c = s.storeContent(ch("abc"));
    try {
      s.loadContentData(123);
    }
    catch (IOException e) {
    }

    try {
      s.loadContentData(c.getId());
      fail();
    }
    catch (IOException e) {
    }
  }

  @Test
  public void testReturningBrokenContentWhenContentStorageBreaksOnSave() {
    s.storeContent(ch("abc"));
    s.close();

    corruptFile("contents");
    initStorage();

    Content c = s.storeContent(ch("def"));
    assertEquals(UnavailableContent.class, c.getClass());
  }

  @Test
  public void testReturningBrokenContentWhenContentStorageIsBroken() {
    try {
      s.loadContentData(123);
    }
    catch (IOException e) {
    }

    Content c = s.storeContent(ch("abc"));
    assertEquals(UnavailableContent.class, c.getClass());
  }

  private void corruptFile(String name) {
    try {
      File f = new File(tempDir, name);
      assertTrue(f.exists());

      FileWriter w = new FileWriter(f);
      w.write("bla-bla-bla");
      w.close();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void initStorage() {
    initStorage(tempDir);
  }

  private void initStorage(File dir) {
    if (s != null) s.close();
    s = new Storage(dir);
  }

  private void initStorage(final int version) {
    if (s != null) s.close();
    s = new Storage(tempDir) {
      @Override
      protected int getVersion() {
        return version;
      }
    };
  }
}
