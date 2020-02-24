package com.mercurytfs.mercury.mavenreleaser.enums;

public enum ProjectsEnum {

    MERCURY_CORE ("com.mercurytfs.mercury.core","MERCURY"),
    MERCURY_CONFIG ("com.mercurytfs.mercury.config","MERCURY"),
    MERCURY_PRODUCTS ("com.mercurytfs.mercury.products","MERCURY"),
    MERCURY_WEB ("com.mercurytfs.mercury.web","MERCURY"),
    MERCURY_MODULES ("com.mercurytfs.mercury.modules","MERCURY"),
    MERCURY_INIT ("com.mercurytfs.mercury.init","MERCURY"),
    MERCURY_SCRIPTS ("com.mercurytfs.mercury.scripts","MERCURY"),
    MERCURY_INTEGRATION ("com.mercurytfs.mercury.integration","MERCURY"),
    MERCURY_CLOUD ("com.mercurytfs.mercury.cloud","MERCURY"),

    SANTANDER_COMEX ("com.santander.comex","SANESPBACK"),
    SPAIN_SANTANDERCOMEX ("com.mercurytfs.mercury.customers.bancosantander.spain.santandercomex","SANESPBACK"),

    CUSTOMERS_LIBERBANK ("com.mercurytfs.mercury.customers.liberbank","LIBERBANK"),

    MERCURY_PRUEBA ("com.mercurytfs.mercury.prueba","PRUEB"),

    SANTANDER_MEXICO ("com.mercurytfs.mercury.customers.santander.mexico","SANMEXICO"),

    SANTANDER_SPAIN ("com.mercurytfs.mercury.customers.santander.spain","SANESP"),

    CHILE_PRODUCTS ("com.mercurytfs.mercury.customers.santander.chile.products","SANCHILE"),
    CHILE_COMMON ("com.mercurytfs.mercury.customers.santander.chile.common","SANCHILE"),
    CHILE_MODULES ("com.mercurytfs.mercury.customers.santander.chile.modules","SANCHILE"),
    CHILE_WEB ("com.mercurytfs.mercury.customers.santander.chile.web","SANCHILE"),
    CHILE_CONFIG ("com.mercurytfs.mercury.customers.santander.chile.config","SANCHILE"),

    CHILE_TARIFARIO ("com.mercurytfs.mercury.customers.santander.chile.tarifario","TARIFARIO"),

    CUSTOMERS_BANORTE ("com.mercurytfs.mercury.customers.banorte","BANORTE"),

    SANTANDER_GERMANY ("com.mercurytfs.mercury.customers.santander.germany","SANGER"),

    CHILE_BACK ("com.mercurytfs.mercury.customers.santander.chile.back","SANCHILEBK"),

    SPAIN_CLOUD ("com.mercurytfs.mercury.customers.bancosantander.spain.cloud","SANESPBCK2"),

    SPAIN_WETRADE ("com.mercurytfs.mercury.customers.bancosantander.spain.wetrade","WETRADE"),

    MERCURYTFS_CLOUD ("com.mercury.mercurytfs.cloud","SANGTS");

    private String groupIdPrefix;
    private String projectName;

    ProjectsEnum(String groupIdPrefix, String projectName){
        this.groupIdPrefix = groupIdPrefix;
        this.projectName = projectName;
    }

    public static String getProjectNameFromGroupId(String groupId){
        for (ProjectsEnum aProject:ProjectsEnum.values())
            if (groupId.startsWith(aProject.groupIdPrefix))
                return aProject.projectName;
        return "";
    }
}
