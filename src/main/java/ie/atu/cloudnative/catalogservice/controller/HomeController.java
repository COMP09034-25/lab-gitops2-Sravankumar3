package ie.atu.cloudnative.catalogservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class HomeController {

    @GetMapping("/")
    public String getGreeting() {
        return "Welcome to the Cloud Native Book Catalog! Auto deploy ,, I am Working Fine ";
    }

    @GetMapping("/greeting/{name}")
    public String getPersonalGreeting(@PathVariable String name) {
        return "Hello " + name + ", welcome to the book catalog!";
    }

    @GetMapping("/health")
    public String health() throws UnknownHostException {
        String hostname = InetAddress.getLocalHost().getHostName();
        return "Application is healthy and running on: " + hostname +
               " (" + System.getProperty("os.name") + ")";
    }
}