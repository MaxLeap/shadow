package com.maxleap.shadow.impl.plugins.input.dir;

import io.vertx.core.file.AsyncFile;

/**
 * Created by stream.
 */
class TargetFile implements Comparable<TargetFile> {
  private String filePath;
  private String inode;
  private long totalSize;
  private long lastModifiedTime;
  private long currentPos;
  private AsyncFile asyncFile;
  private String inputDir;

  TargetFile(String inputDir, String filePath, long totalSize, long lastModifiedTime, long currentPos) {
    this.inputDir = inputDir;
    this.filePath = filePath;
    this.totalSize = totalSize;
    this.lastModifiedTime = lastModifiedTime;
    this.currentPos = currentPos;
  }

  String getInputDir() {
    return inputDir;
  }

  String getFilePath() {
    return filePath;
  }

  long getTotalSize() {
    //totalSize,会变,比如执行了rotate,所以要想办法读取最新的totalSize
    return totalSize;
  }

  public String getInode() {
    return inode;
  }

  public void setInode(String inode) {
    this.inode = inode;
  }

  void setTotalSize(long totalSize) {
    this.totalSize = totalSize;
  }

  private long getLastModifiedTime() {
    return lastModifiedTime;
  }

  void setLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  long getCurrentPos() {
    return currentPos;
  }

  void setCurrentPos(long currentPos) {
    this.currentPos = currentPos;
  }

  AsyncFile getAsyncFile() {
    return asyncFile;
  }

  void setAsyncFile(AsyncFile asyncFile) {
    this.asyncFile = asyncFile;
  }

  @Override
  public int compareTo(TargetFile targetFile) {
    return this.getLastModifiedTime() > targetFile.getLastModifiedTime() ? 1 : -1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TargetFile)) return false;

    TargetFile that = (TargetFile) o;

    return filePath.equals(that.filePath);

  }

  @Override
  public int hashCode() {
    return filePath.hashCode();
  }



  @Override
  public String toString() {
    return "LogFile{" +
      "filePath='" + filePath + '\'' +
      ", totalSize=" + totalSize +
      ", lastModifiedTime=" + lastModifiedTime +
      ", currentPos=" + currentPos +
      '}';
  }
}