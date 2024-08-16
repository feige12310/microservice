package com.ksyun.course;

// ServiceRegistryController.java

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ServiceRegistryController {

    private List<Register> registers = new ArrayList<>();
    //    private List<Service> services = new ArrayList<>();
    private static final int HEARTBEAT_INTERVAL = 60; // 心跳间隔时间，单位：秒


    @PostMapping("/api/register")
    public String registerService(@RequestBody Service service) {
        System.out.println("====="+service.getIpAddress()+" :"+service.getPort()+" use registerService!=====");
//        if (register.stream().anyMatch(s -> s.getServiceName().equals(service.getServiceName())
//                && s.getIpAddress().equals(service.getIpAddress())
//                && s.getPort() == service.getPort())) {
//            return "Service already registered.";
//        }
        if (registers.stream().anyMatch(s -> s.getServiceId().equals(service.getServiceId()))) {
            registers.stream().filter(s -> s.getServiceId().equals(service.getServiceId())
                            && s.getIpAddress().equals(service.getIpAddress())
                            && s.getPort() == service.getPort())
                    .forEach(r -> r.setCreateTime(System.currentTimeMillis()));
            return "Service already registered.";
        }
        Register register = new Register(service.getServiceName(), service.getServiceId(),
                service.getIpAddress(), service.getPort(), System.currentTimeMillis());

        registers.add(register);
        for(Register r : registers) {
            System.out.println(r.toString());
        }
        return "Service registered successfully.";
    }

    @PostMapping("/api/unregister")
    public String unregisterService(@RequestBody Service service) {
        System.out.println("====="+service.getIpAddress()+" :"+service.getPort()+" use unregisterService!=====");

        if (registers.removeIf(s -> s.getServiceName().equals(service.getServiceName())
                && s.getIpAddress().equals(service.getIpAddress())
                && s.getPort() == service.getPort()
                && s.getServiceId().equals(service.getServiceId()))) {
            for(Register r : registers) {
                System.out.println(r.toString());
            }
            return "Service unregistered successfully.";
        }
        for(Register r : registers) {
            System.out.println(r.toString());
        }
        System.out.println("Service not found.");
        return "Service not found.";
    }

//    @PostMapping("/api/heartbeat")
//    public String sendHeartbeat(@RequestBody Service service) {
//        // Update heartbeat for the service
//        return "Heartbeat received.";
//    }


    // 接收心跳请求
    @PostMapping("/api/heartbeat")
    public String receiveHeartbeat(@RequestBody HeartbeatRequest request) {
        System.out.println("====="+request.getIpAddress()+" :"+request.getPort()+" use receiveHeartbeat!=====");

        registers.stream().filter(s -> s.getServiceId().equals(request.getServiceId())
                        && s.getIpAddress().equals(request.getIpAddress())
                        && s.getPort() == request.getPort())
                .forEach(r -> r.setCreateTime(System.currentTimeMillis()));
        for(Register r : registers) {
            System.out.println(r.toString());
        }
        return "Heartbeat received.";
    }

    // 定时检查服务节点的心跳时间
    @Scheduled(fixedRate = 10000) // 每10秒执行一次
    public void checkHeartbeat() {
        long currentTime = System.currentTimeMillis();
        registers.removeIf(s -> currentTime - s.getCreateTime() > HEARTBEAT_INTERVAL * 1000);
    }


    @GetMapping("/api/discovery")
    public List<Service> discoverService(@RequestParam(required = false) String name) {
        System.out.println("====="+"use discoverService! name:"+name+"=====");
        List<Service> services = new ArrayList<>();
        for (Register r : registers) {
            services.add(new Service(r.getServiceName(), r.getServiceId(), r.getIpAddress(), r.getPort()));
        }

        if (name == null || name.isEmpty()) {
            return services;
        }

        List<Service> result = new ArrayList<>();
        List<Service> availableServices = services.stream()
                .filter(s -> s.getServiceName().equals(name))
                .collect(Collectors.toList());

        if (!availableServices.isEmpty()) {
            // 使用负载均衡算法选择一个服务节点
            Service selectedService = loadBalance(availableServices);
            result.add(selectedService);
            // 从服务列表中移除已选择的服务节点，以确保下一次调用返回不同的节点
//            services.remove(selectedService);
            return result;
        }
        return null; // 如果没有找到对应服务，返回null或者合适的默认值

//        if (name != null) {
//            return services.stream()
//                    .filter(s -> s.getServiceName().equals(name))
//                    .findFirst()
//                    .map(List::of)
//                    .orElse(new ArrayList<>());
//        }
//        return services;
    }


    private int currentIndex = 0;

    private Service loadBalance(List<Service> services) {
        Service selectedService = services.get(currentIndex);
        currentIndex = (currentIndex + 1) % services.size(); // 更新索引以实现轮询
        return selectedService;
    }
}