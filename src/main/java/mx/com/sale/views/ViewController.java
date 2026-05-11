package mx.com.sale.views;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {
    @GetMapping("/login")
    public String showLoginPage() {
        // Spring buscará el archivo en /WEB-INF/login.html
        // dependiendo de cómo configures el ViewResolver
        return "login";
    }

    @GetMapping("/index")
    public String showIndexPage() {
        // Spring buscará el archivo en /WEB-INF/index.html
        // dependiendo de cómo configures el ViewResolver
        return "index";
    }

    @GetMapping("/views/{page}")
    public String layout(@PathVariable("page") String page) {
        return "views/" + page;
    }
}
