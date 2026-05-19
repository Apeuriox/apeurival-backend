package me.aloic.apeurival.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.upload.path")
public class UploadPathConfig {

    private String windows;
    private String linux;

    public String resolve() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return linux;
        }
        return windows;
    }

    public String getWindows() { return windows; }
    public void setWindows(String windows) { this.windows = windows; }
    public String getLinux() { return linux; }
    public void setLinux(String linux) { this.linux = linux; }
}
