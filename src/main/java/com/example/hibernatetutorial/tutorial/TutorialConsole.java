package com.example.hibernatetutorial.tutorial;

import org.springframework.stereotype.Component;

@Component
public class TutorialConsole {

    public void title(String title) {
        System.out.println();
        System.out.println("================================================================================");
        System.out.println(title);
        System.out.println("================================================================================");
    }

    public void step(String message) {
        System.out.println(" - " + message);
    }

    public void value(String label, Object value) {
        System.out.printf("   %-48s : %s%n", label, value);
    }
}
