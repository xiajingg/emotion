package com.emotion.api;

import cn.hutool.http.HttpUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class HttpBenchmark {



    private static final String URL_STRING = "http://localhost:8090/user/api/v1/test";

    // Feign client interface

    public static void main(String[] args) throws IOException {
        // Initialize clients for different HTTP libraries
        CloseableHttpClient httpClient = HttpClients.createDefault(); // Apache HttpClient
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build(); // OkHttp
        RestTemplate restTemplate = new RestTemplate(); // Spring RestTemplate

        // Perform benchmarking for each HTTP client and print out the time taken
        System.out.println("Starting performance benchmarks...");
        benchmark(() -> {
            try {
                performApacheHttpClient(httpClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "Apache HttpClient");
        benchmark(() -> {
            try {
                performOkHttp(okHttpClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "OkHttp");
        benchmark(() -> performRestTemplate(restTemplate), "RestTemplate");
        benchmark(() -> {
            try {
                performHttpURLConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "HttpURLConnection");
        benchmark(HttpBenchmark::performHutoolHttpUtil, "Hutool HttpUtil");
        // Close resources to prevent resource leaks
        httpClient.close();

        System.out.println("Performance benchmarks completed.");
    }

    /**
     * Executes a given task and prints the time it took to execute.
     */
    private static void benchmark(Runnable task, String name) {
        long start = System.nanoTime(); // Record the start time in nanoseconds
        task.run(); // Execute the task
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start); // Calculate the elapsed time in milliseconds
        System.out.println(name + ": " + duration + " ms"); // Print the name of the client and the time it took
    }

    /**
     * Performs an HTTP GET request using HttpURLConnection.
     */
    private static void performHttpURLConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(URL_STRING).openConnection();
        connection.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            while (reader.readLine() != null) {
                // Consume response content to ensure it's fully read
            }
        }
    }

    /**
     * Performs an HTTP GET request using Apache HttpClient.
     */
    private static void performApacheHttpClient(CloseableHttpClient httpClient) throws IOException {
        HttpGet request = new HttpGet(URL_STRING);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                while (reader.readLine() != null) {
                    // Consume response content to ensure it's fully read
                }
            }
        }
    }

    /**
     * Performs an HTTP GET request using OkHttp.
     */
    private static void performOkHttp(OkHttpClient okHttpClient) throws IOException {
        Request request = new Request.Builder().url(URL_STRING).build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            try (BufferedReader reader = new BufferedReader(response.body().charStream())) {
                while (reader.readLine() != null) {
                    // Consume response content to ensure it's fully read
                }
            }
        }
    }


    /**
     * Performs an HTTP GET request using Spring RestTemplate.
     */
    private static void performRestTemplate(RestTemplate restTemplate) {
        restTemplate.getForObject(URL_STRING, String.class); // RestTemplate handles the HTTP call internally
    }

    /**
     * Performs an HTTP GET request using Hutool HttpUtil.
     */
    private static void performHutoolHttpUtil() {
        HttpUtil.get(URL_STRING); // Hutool HttpUtil handles the HTTP call internally
    }
}
