package com.flinko.plugins;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

public class FlinkoRestApi {

    private static Logger logger = Logger.getLogger(FlinkoRestApi.class.getName());

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        System.out.println("hi");

//        String url = "http://10.10.10.250:8101/optimize/v1/public/user/signin";
//        Map<String, String> body = new HashMap<>();
//        body.put("emailId", "sheela.c@testyantra.com");
//            body.put("password", "Password@123");
//
//        Properties prop = new Properties();
//        try(InputStream stream = FlinkoRestApi.class.getClassLoader().getResourceAsStream("flinko.properties")){
//            prop.load(stream);
//            System.out.println(prop.get("name"));
//        }
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        String requestBody = objectMapper
//                .writeValueAsString(body);
//        System.out.println(sendPOST(url, requestBody));;
//
//        String directoryName = System.getProperty("user.dir");
//        System.out.println("Current Working Directory is = " +directoryName);
//
//        Path path = Paths.get("");
//        String directoryName2 = path.toAbsolutePath().toString();
//        System.out.println("Current Working Directory is = " +directoryName2);

    }


    public static String postRequest(String url, String body) throws IOException {

        System.out.println("Inside send post api....");
        String result = "";
        HttpPost post = new HttpPost(url);
        post.setHeader("content-type", "application/json");
        post.setEntity(new StringEntity(body));

        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)){

            result = EntityUtils.toString(response.getEntity());
        }
        System.out.println("result"+ result);
        return result;
    }

    public static String getRequest(String url, String token) throws IOException {
        String result = "";
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-type", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(get)){
            result = EntityUtils.toString(response.getEntity());
        }
        return result;
    }

    public static String getSuites(String url, String token, String projectId) throws IOException {
        String result = "";
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("projectId", projectId);
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-type", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(get)){
            result = EntityUtils.toString(response.getEntity());
        }
        return result;
    }

    public static String getProjects(String url, String token) throws IOException {
        String result = "";
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-type", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(get)){
            result = EntityUtils.toString(response.getEntity());
        }
        return result;
    }

    public static String executeSuite(String url, String token) throws IOException {
        String result = "";
        HttpPost get = new HttpPost(url);
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-type", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(get)){
            result = EntityUtils.toString(response.getEntity());
        }
        return result;
    }

    public static String getSuiteExecutionDetails(String url, String token) throws IOException {
        String result = "";
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + token);
        get.setHeader("Accept", "application/json");
        get.setHeader("Content-type", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(get)){
            result = EntityUtils.toString(response.getEntity());
        }
        return result;
    }

}
