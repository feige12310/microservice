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
public class ClientService1Controller {

    @GetMapping("/api/getInfo")
    public void getInfo(HttpServletResponse response) {
        regist();
        String errorInfo=null;
        String resultInfo=null;
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:40100/api/getDateTime?style=full"; // 你要发送 GET 请求的 URL

        // 发送 GET 请求并获取响应
        TimeServerResponse timeresponse = restTemplate.getForObject(url, TimeServerResponse.class);

        // 输出响应
//        String gmtTime=timeresponse.getResult();
//        System.out.println("gmtTime:"+gmtTime);
        String beijingTime=timeTransfor(timeresponse.getResult());

        if(beijingTime!=null){
            System.out.println("beijingTime:"+beijingTime);
            resultInfo="Hello Kingsoft Cloud Star Camp - "+timeresponse.getServiceId()+" - "+beijingTime;
        }else {
            errorInfo="time-service not available";
        }
        sendResponse(response,errorInfo,resultInfo);
    }
    public String timeTransfor(String gmtTime) {

        // 创建一个 SimpleDateFormat 对象，指定输入时间格式为 GMT
        SimpleDateFormat gmtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        String beijingTime = null;
        try {
            // 解析 GMT 格式的时间字符串
            Date gmtDate = gmtFormat.parse(gmtTime);
            // 创建一个 SimpleDateFormat 对象，指定输出格式为北京时间
            SimpleDateFormat beijingFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            beijingFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            // 格式化为北京时间
            beijingTime = beijingFormat.format(gmtDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return beijingTime;
    }

    public void sendResponse(HttpServletResponse response,String errorInfo,String resultInfo) {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        String jsonResponse = "{\"error\": \""+ errorInfo+"\",\"result\": \"" + resultInfo + "\"}";
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
        TimeServerRequest request = new TimeServerRequest("client", selfip+String.valueOf(selfport),selfip,selfport);
        String url = "http://127.0.0.1:40001/api/register";
        sentMessage(request,url);
    }

    @PreDestroy
    public void stopHeartbeat(){
        String selfip=getServerIpAddress();
        int selfport=getServerPort();
        // 创建请求体
        TimeServerRequest request = new TimeServerRequest("client", selfip+String.valueOf(selfport),selfip,selfport);
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