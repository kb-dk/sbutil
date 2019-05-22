module sbutil.webservices.common {
    
    requires slf4j.api;
    requires servlet.api;
    
    requires java.activation;
    
    requires java.xml.bind;
    requires java.xml.ws;
    requires java.annotation;
    
    exports dk.statsbiblioteket.sbutil.webservices.authentication;
    exports dk.statsbiblioteket.sbutil.webservices.configuration;
}