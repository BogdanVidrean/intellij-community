package com.intellij.localvcs.integration;

import com.intellij.localvcs.core.ContentHolder;
import com.intellij.localvcs.core.ILocalVcs;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;

// todo synchronization
public abstract class ServiceState {
  protected ServiceStateHolder myHolder;
  protected ILocalVcs myVcs;
  protected IdeaGateway myGateway;

  public ServiceState(ServiceStateHolder h, ILocalVcs vcs, IdeaGateway gw) {
    myHolder = h;
    myGateway = gw;
    myVcs = vcs;
  }

  public void startRefreshing() {
    illegalState();
  }

  public void finishRefreshing() {
    illegalState();
  }

  public void startCommand(String name) {
    illegalState();
  }

  public void finishCommand() {
    illegalState();
  }

  public void startAction(String name) {
    illegalState();
  }

  public void finishAction() {
    illegalState();
  }

  protected void goToState(ServiceState s) {
    beforeExitingFromState();
    myHolder.setState(s);
  }

  public void create(VirtualFile f) {
    createRecursively(f);
  }

  private void createRecursively(VirtualFile f) {
    if (f.isDirectory()) {
      myVcs.createDirectory(f.getPath());
      for (VirtualFile child : f.getChildren()) createRecursively(child);
    }
    else {
      myVcs.createFile(f.getPath(), contentHolderOf(f), f.getTimeStamp());
    }
  }

  public void changeFileContent(VirtualFile f) {
    myVcs.changeFileContent(f.getPath(), contentHolderOf(f), f.getTimeStamp());
  }

  private ContentHolder contentHolderOf(final VirtualFile f) {
    return new ContentHolder() {
      public byte[] getBytes() {
        try {
          return myGateway.getPhysicalContent(f);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      public long getLength() {
        try {
          return myGateway.getPhysicalLength(f);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  public void rename(VirtualFile f, String newName) {
    myVcs.rename(f.getPath(), newName);
  }

  public void move(VirtualFile file, VirtualFile newParent) {
    myVcs.move(file.getPath(), newParent.getPath());
  }

  public void delete(VirtualFile f) {
    myVcs.delete(f.getPath());
  }

  protected void afterEnteringState() {
  }

  protected void beforeExitingFromState() {
  }

  private void illegalState() {
    // todo move to logging proxy...
    throw new IllegalStateException(getClass().getSimpleName());
  }
}