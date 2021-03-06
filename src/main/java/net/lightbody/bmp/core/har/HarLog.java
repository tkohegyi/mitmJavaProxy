package net.lightbody.bmp.core.har;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HarLog {
    private String version = "1.2";
    private HarNameVersion creator;
    private HarNameVersion browser;
    private List<HarPage> pages = new CopyOnWriteArrayList<HarPage>();
    private List<HarEntry> entries = new CopyOnWriteArrayList<HarEntry>();

    public HarLog(HarNameVersion creator) {
        this.creator = creator;
    }

    public void addPage(HarPage page) {
        if (pages == null) {
            pages = new CopyOnWriteArrayList<>();
        }

        pages.add(page);
    }

    public void addEntry(HarEntry entry) {
        if (entries == null) {
            entries = new CopyOnWriteArrayList<>();
        }

        entries.add(entry);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public HarNameVersion getBrowser() {
        return browser;
    }

    public void setBrowser(HarNameVersion browser) {
        this.browser = browser;
    }

    public List<HarPage> getPages() {
        return pages;
    }

    public void setPages(List<HarPage> pages) {
        this.pages = pages;
    }

    public List<HarEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<HarEntry> entries) {
        this.entries = entries;
    }
}
