package xyz.mrehber.service.ai;

import java.io.IOException;

public interface AIProvider {
    String getName();
    String analyzeMessages(String prompt) throws IOException;
}
