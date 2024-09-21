package org.example;

import net.bytebuddy.agent.ByteBuddyAgent;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class MainAgent {
    public static void main(String[] args) {
        String pid = "52478";
        ByteBuddyAgent.attach(new File("/Users/stiglau/.m2/repository/net/bytebuddy/byte-buddy/1.15.1/byte-buddy-1.15.1.jar"), pid);
        Instrumentation instruments = ByteBuddyAgent.getInstrumentation();
        //instruments.addTransformer();

    }
}
