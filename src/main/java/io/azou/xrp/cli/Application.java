package io.azou.xrp.cli;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.jline.PromptProvider;

@SpringBootApplication
@ComponentScan(basePackages = {"io.azou.xrp.cli.commands"})
public class Application {
  public static void main(String[] args) {
    try {
      System.out.println(""); // clear line on hot restart
      SpringApplication.run(Application.class, args);
    } catch (Exception e) {
    }
  }

  @Bean
  public PromptProvider promptProvider() {
    return () ->
        new AttributedString(
            "xrp-cli> ", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
  }
}
