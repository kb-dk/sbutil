module sbutil.webservices.log4j {
    
    requires servlet.api;
    requires log4j;
    requires jdk.xml.dom;
    exports dk.statsbiblioteket.sbutil.webservices.logging;
}