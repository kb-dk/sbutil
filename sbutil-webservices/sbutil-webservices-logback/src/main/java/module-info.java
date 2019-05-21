module sbutil.webservices.logback {
    
    requires servlet.api;
    requires slf4j.api;
    requires logback.classic;
    requires logback.core;
    requires java.xml;
    exports dk.statsbiblioteket.sbutil;
}