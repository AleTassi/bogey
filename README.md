# Sapice

## Technology Stack

- Java SE
- Build: [Maven](https://maven.apache.org/)
  - Dependencies
    - [Logback](https://logback.qos.ch/): **1.2.3**
    - [openCV](https://opencv.org/): **3.4.2-0**
    - [Commons Imaging](https://commons.apache.org/proper/commons-imaging/): **1.0-SNAPSHOT**

## Configurazione Applicazione

### Per il profilo `local`:

- creare il file:  
  `/src/main/filters/filters-local.properties`  
  copiandolo da:  
  `/src/main/filters/sample-filters.properties`  
  avendo cura di creare le cartelle di logging definiti dalle properties *log.path* e *log.rolled.path*

- N.B. i path **non** devono avere lo slash `/` finale.

### Altri profili
...
