package uk.tanton.streaming.lambda.transcoder;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

public class StreamConsumer extends Thread {

    private InputStream is;
    private String type;
    private Optional<LambdaLogger> logger;

    public StreamConsumer(InputStream is, String type, Optional<LambdaLogger> logger) {
        this.is = is;
        this.type = type;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null)
                log(type + ">" + line);
        } catch (IOException e) {
            log("Error whilst consuming stream", e);
        }
    }

    private void log(final String log, final Exception e) {
        log(log);
        log(e.getMessage());
    }

    private void log(final String log) {
        if (logger.isPresent()) {
            logger.get().log(log);
        } else {
            System.out.println(log);    // NOSONAR
        }
    }
}
