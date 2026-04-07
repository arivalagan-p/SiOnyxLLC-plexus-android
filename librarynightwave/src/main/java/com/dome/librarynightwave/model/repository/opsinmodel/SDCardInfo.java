package com.dome.librarynightwave.model.repository.opsinmodel;

public class SDCardInfo {
    private boolean isPresent;
    private boolean isMounted;
    private boolean isFormatting;
    private boolean isBusy;
    private int totalSize;
    private int availableSize;
    private int usedSize;
    private int fileCount;
    private int serialNumber;
    private String label;

    public boolean isPresent() {
        return isPresent;
    }

    public void setPresent(boolean present) {
        isPresent = present;
    }

    public boolean isMounted() {
        return isMounted;
    }

    public void setMounted(boolean mounted) {
        isMounted = mounted;
    }

    public boolean isFormatting() {
        return isFormatting;
    }

    public void setFormatting(boolean formatting) {
        isFormatting = formatting;
    }

    public boolean isBusy() {
        return isBusy;
    }

    public void setBusy(boolean busy) {
        isBusy = busy;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getAvailableSize() {
        return availableSize;
    }

    public void setAvailableSize(int availableSize) {
        this.availableSize = availableSize;
    }

    public int getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(int usedSize) {
        this.usedSize = usedSize;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }


}
