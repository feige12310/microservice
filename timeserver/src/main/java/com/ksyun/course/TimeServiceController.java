package com.ksyun.course;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

@RestController
@EnableScheduling
public class TimeServiceController {

    @GetMapping("/api/getDateTime")
    public void getDateTime(@RequestParam String style, HttpServletResponse response) {

        regist();
        String dateTime;

        switch (style) {
            case "full":
//                dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                dateTime = dateFormat.format(new Date());
                break;
            case "date":
//                dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                SimpleDateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
                dateFormat2.setTimeZone(TimeZone.getTimeZone("GMT"));
                dateTime = dateFormat2.format(new Date());
                break;
            case "time":
//                dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                SimpleDateFormat dateFormat3 = new SimpleDateFormat("HH:mm:ss");
                dateFormat3.setTimeZone(TimeZone.getTimeZone("GMT"));
                dateTime = dateFormat3.format(new Date());
                break;
            case "unix":
                dateTime = String.valueOf(Instant.now().toEpochMilli());
                break;
            default:
                dateTime = "Unsupported style";
        }
        sendResponse(response, dateTime);
    }

    public void sendResponse(HttpServletResponse response,String dateTime) {
        String selfip=getServerIpAddress();
        int selfport=getServerPort();
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        String jsonResponse = "{\"result\":\""+dateTime+"\",\"serviceId\":\"" + selfip+String.valueOf(selfport) + "\"}";
        try {
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            response.getWriter().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 60000)
    public void sendHeartbeat(){
        String selfip=getServerIpAddress();
        int selfport=getServerPort();

        // 创建请求体
        HeartbeatRequest request = new HeartbeatRequest(selfip+String.valueOf(selfport),selfip,selfport);
        String url = "http://127.0.0.1:40001/api/heartbeat";
        sentMessage(request,url);
    }

    @PostConstruct
    public void regist(){
        String selfip=getServerIpAddress();
        int selfport=getServerPort();
        // 创建请求体
        TimeServerRequest request = new TimeServerRequest("time-service", selfip+String.valueOf(selfport),selfip,selfport);
        String url = "http://127.0.0.1:40001/api/register";
        sentMessage(request,url);
    }

    @PreDestroy
    public void stopHeartbeat(){
        String selfip=getServerIpAddress();
        int selfport=getServerPort();
        // 创建请求体
        TimeServerRequest request = new TimeServerRequest("time-service", selfip+String.valueOf(selfport),selfip,selfport);
        String url = "http://127.0.0.1:40001/api/unregister";
        sentMessage(request,url);
    }
    public <T> void sentMessage(T request,String url){
        // 创建RestTemplate对象
        RestTemplate restTemplate = new RestTemplate();
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<T> requestBody = new HttpEntity<>(request, headers);

        ResponseEntity<String> responseEntity =restTemplate.postForEntity(url, requestBody, String.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            String response = responseEntity.getBody();
            System.out.println("Response received: " + response);
        } else {
            System.out.println("Request failed with status code: " + responseEntity.getStatusCodeValue());
        }
    }
    public static String getServerIpAddress() {
        try {
            // 获取本机的 InetAddress 实例
            InetAddress localhost = InetAddress.getLocalHost();
            return localhost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Autowired
    private ServerProperties serverProperties;
    public int getServerPort() {
        return serverProperties.getPort();
    }
}