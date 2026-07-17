package com.queuectl.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuectl.cli.config.AppConfig;
import com.queuectl.entity.Job;
import com.queuectl.service.JobService;
import com.queuectl.utils.ConsolePrinter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Callable;

/**
 * CLI command to enqueue a new job into the queue.
 *
 * Usage: queuectl enqueue '{"id":"job1","command":"echo hello"}'
 *        queuectl enqueue --file job.json
 *        echo '{"id":"job1","command":"echo hello"}' | queuectl enqueue --stdin
 */
@Command(
    name = "enqueue",
    description = "Enqueue a new job into the queue",
    mixinStandardHelpOptions = true
)
public class EnqueueCommand implements Callable<Integer> {

    @Parameters(index = "0", arity = "0..1", description = "Job JSON with id, command, and optional fields")
    private String jobJson;

    @Option(names = {"-f", "--file"}, description = "Read job JSON from a file")
    private File jsonFile;

    @Option(names = {"-i", "--stdin"}, description = "Read job JSON from stdin")
    private boolean readStdin;

    @Override
    public Integer call() {
        try {
            String json = resolveJson();
            Job job = parseJob(json);
            JobService jobService = new JobService(AppConfig.getDatabaseManager());
            jobService.enqueue(job);
            ConsolePrinter.printSuccess("Job enqueued successfully: " + job.getId());
            return 0;
        } catch (Exception e) {
            ConsolePrinter.printError("Failed to enqueue job: " + e.getMessage());
            return 1;
        }
    }

    private String resolveJson() throws Exception {
        if (jsonFile != null && jsonFile.exists()) {
            return Files.readString(jsonFile.toPath()).trim();
        }
        if (readStdin) {
            return new String(System.in.readAllBytes()).trim();
        }
        if (jobJson != null && !jobJson.isBlank()) {
            return jobJson;
        }
        throw new IllegalArgumentException("Provide job JSON as argument, --file, or --stdin");
    }

    private Job parseJob(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var node = mapper.readTree(json);

            Job job = new Job();

            if (node.has("id")) {
                job.setId(node.get("id").asText());
            } else {
                throw new IllegalArgumentException("Job JSON must contain 'id' field");
            }

            if (node.has("command")) {
                job.setCommand(node.get("command").asText());
            } else {
                throw new IllegalArgumentException("Job JSON must contain 'command' field");
            }

            if (node.has("priority")) {
                job.setPriority(node.get("priority").asInt());
            }

            if (node.has("maxRetries")) {
                job.setMaxRetries(node.get("maxRetries").asInt());
            }

            if (node.has("runAt")) {
                job.setRunAt(java.time.LocalDateTime.parse(node.get("runAt").asText()));
            }

            return job;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage());
        }
    }
}
