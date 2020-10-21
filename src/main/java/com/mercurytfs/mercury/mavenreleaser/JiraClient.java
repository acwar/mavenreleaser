package com.mercurytfs.mercury.mavenreleaser;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootApplication
public class JiraClient
{
	private static final Logger log;
    private static String apiURL;
    public static String userName;
    public static String password;
    public static String jiraURL;
    public static String hostName;
    public static int port;
    
    static {
    	log = LoggerFactory.getLogger((Class) JiraClient.class);
        JiraClient.jiraURL = "http://192.168.10.21:8080/rest/api/2/";
        JiraClient.hostName = "192.168.10.21";
        JiraClient.port = 8080;
        JiraClient.userName = "admin1";
        JiraClient.password = "mer01cury.";
    }
    

    
    public static Artefact getIssueKey(final String project, final String component, final String version) {
        String key = null;
        Artefact artefact = null;
        String description = null;
        log.info("Searching for the key of the issue: " + project + "-" + component + "-" + version);
        try {
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final String queryString = String.valueOf(JiraClient.jiraURL) + "search?jql=project=" + project + " AND issuetype = \"Artefacto Maven\" AND component=" + component + " AND fixVersion = " + version + " AND status in (\"Open\", \"In Progress\", \"Reopened\")";
            //log.debug(queryString);
            final ResponseEntity<Object> obj = (ResponseEntity<Object>)restTemplate.exchange(queryString, HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
            if (obj != null) {
                if (obj.getStatusCode().is2xxSuccessful()) {
                    final HashMap map = (HashMap)obj.getBody();
                    Map fields = null;
                    final ArrayList issues = (ArrayList) map.get("issues");
                    if (issues != null && issues.size() > 0) {
                        final Map issue = (Map) issues.get(0);
                        key = (String) issue.get("key");
                        fields = (Map) issue.get("fields");
                        description = (String) fields.get("description");
                        artefact = new Artefact();
                        artefact.setJiraIssue(key);
                        artefact.setDescription(description);
                    }
                    else {
                        log.warn("Issue Not Found");
                    }
                }
                else {
                    log.error("Error in request to jira: " + obj.getStatusCodeValue() + ":" + obj.getBody());
                }
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                log.error(e.toString());
            }
        }
        log.info("Retrieved the key of issue: " + project + "-" + component + "-" + version + " successfully");
        return artefact;
    }
    
    public static Artefact getIssueByKey(String key, final boolean unresolved) {
        Artefact artefact = null;
        String description = null;
        log.info("->getIssueByKey: " + key);
        try {
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            String queryString = String.valueOf(JiraClient.jiraURL) + "search?jql=key=" + key;
            if (unresolved) {
                queryString = String.valueOf(queryString) + " and  resolution = Unresolved";
            }
            log.debug(queryString);
            final ResponseEntity<Object> obj = (ResponseEntity<Object>)restTemplate.exchange(queryString, HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
            if (obj != null) {
                if (obj.getStatusCode().is2xxSuccessful()) {
                    final HashMap map = (HashMap)obj.getBody();
                    Map fields = null;
                    List<Map> issuelinks = null;
                    final ArrayList issues = (ArrayList) map.get("issues");
                    if (issues != null && issues.size() > 0) {
                        final Map issue = (Map) issues.get(0);
                        key = (String) issue.get("key");
                        fields = (Map) issue.get("fields");
                        description = (String) fields.get("description");
                        artefact = new Artefact();
                        artefact.setJiraIssue(key);
                        artefact.setDescription(description);
                        issuelinks = (List) fields.get("issuelinks");
                        if (issuelinks != null && issuelinks.size() > 0) {
                            for (final Map<String,Map> issueMap : issuelinks) {
                                final Artefact issueLinked = new Artefact();
                                if ((issueMap.get("outwardIssue") != null) && (issueMap.get("outwardIssue").get("key") != null)) {
                                    issueLinked.setJiraIssue((String)issueMap.get("outwardIssue").get("key"));
                                    artefact.getIssueslinked().add(issueLinked);
                                }
                                else if (issueMap.get("inwardIssue") != null && issueMap.get("inwardIssue").get("key") != null) {
                                    issueLinked.setJiraIssue((String)issueMap.get("inwardIssue").get("key"));
                                    artefact.getIssueslinked().add(issueLinked);
                                }
                                else {
                                    log.error("Cannot get issue key from " + issueMap.toString());
                                }
                            }
                        }
                    }
                    else {
                        log.warn("Issue Not Found");
                    }
                }
                else {
                    log.error("Error in request to jira: " + obj.getStatusCodeValue() + ":" + obj.getBody());
                }
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                log.error(e.toString());
            }
        }
        log.info("<-getIssueByKey: " + key);
        return artefact;
    }
    
    public static String closeIssue(final String key) {
        String result = key;
        log.info("Closing issue: " + key);
        try {
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final String queryString = String.valueOf(JiraClient.jiraURL) + "issue/" + key + "/transitions";
            //log.debug(queryString);
            final ResponseEntity<Object> obj2 = (ResponseEntity<Object>)restTemplate.exchange(queryString, HttpMethod.GET, (HttpEntity)null, (Class)Object.class, new Object[0]);
            final Map map = (HashMap)obj2.getBody();
            final ArrayList<Map> transitions = (ArrayList<Map>)map.get("transitions");
            String name = "";
            String actionId = null;
            for (final Map transition : transitions) {
                name = (String) transition.get("name");
                if (name.contains("Cerrar") || name.contains("Close")) {
                    actionId = (String) transition.get("id");
                }
            }
            if (actionId != null) {
                final String jsonTransition = "{\"transition\": {\"id\": \"" + actionId + "\"}}";
                HttpHeaders headers = new HttpHeaders();
                headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = (HttpEntity<String>)new HttpEntity((Object)jsonTransition, (MultiValueMap)headers);
                entity = (HttpEntity<String>)new HttpEntity((Object)jsonTransition, (MultiValueMap)headers);
                final ResponseEntity<String> response = (ResponseEntity<String>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "issue/" + key + "/transitions", HttpMethod.POST, (HttpEntity)entity, (Class)String.class, new Object[0]);
                if (response != null) {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Task closed successfully: " + key);
                    }
                }
                else {
                    result = null;
                    log.error("Error closing the task: " + response.getStatusCodeValue() + ":" + (String)response.getBody());
                }
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                log.error(e.toString());
            }
        }
        //log.info("<-closeIssue: " + key);
        return result;
    }
    
    public static String createIssue(final String project, final String summary, String descripcion, final String component, final String currentVersion, final String fixVersion) {
    	String logMessage = "Creating new Issue: \n";
    	if (project!=null){
    		logMessage += " project: "+ project + "\n";
    	} 
    	if (summary!=null){
    		logMessage += " summary: " + summary + "\n";
    	}
    	if (descripcion!=null){
    		logMessage += " descripcion: "+ descripcion + "\n";
    	} else {
    		descripcion = "" + "\n";
    	}
    	if (component!=null){
    		logMessage += " component: " + component + "\n";
    	}
    	if (currentVersion!=null){
    		logMessage += " currentVersion: " + currentVersion + "\n";
    	}
    	if (fixVersion!=null){
    		logMessage += " fixVersion: "+ fixVersion + "\n";
    	}
        log.info(logMessage);
        String key = null;
        
        
        try {
            final String fixDescripcion = fixCarrierReturn(descripcion);
            final String jsonCreate = "{\"fields\": {\"project\":{\"key\": \"" + project + "\"},\"summary\": \"" + summary + "\", \"fixVersions\" : [  {\"name\" : \"" + fixVersion + "\"}],\"versions\" : [  {\"name\" : \"" + currentVersion + "\"}],\"description\": \"" + fixDescripcion + "\", \"components\": [{\"name\":\"" + component + "\"}],\"issuetype\": {\"name\": \"Artefacto Maven\"}}}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            final HttpEntity<String> entity = (HttpEntity<String>)new HttpEntity((Object)jsonCreate, (MultiValueMap)headers);
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final ResponseEntity<Object> response = (ResponseEntity<Object>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "issue/", HttpMethod.POST, (HttpEntity)entity, (Class)Object.class, new Object[0]);
            if (response != null) {
                if (response.getStatusCode().is2xxSuccessful()) {
                    final Map map = (HashMap)response.getBody();
                    key = (String) map.get("key");
                    log.info("Task created successfully, the new task is: " + key);
                }
            }
            else {
                log.error("Error creating the new task: " + response.getStatusCodeValue() + ":" + response.getBody());
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                log.error(e.toString());
            }
        }
        //log.info("<- " + logMessage);
        return key;
    }
    
    public static String createVersion(final String project, final String version, String descripcion) {
    	String logMessage = "createVersion";
    	if (project!=null){
    		logMessage += " project: "+ project;
    	} 
    	
    	if (project!=null){
    		logMessage += " version: "+ version;
    	} 
    	
    	if (descripcion!=null){
    		logMessage += " descripcion: "+ descripcion;
    	} else {
    		descripcion = "";
    	}
    	    
        //log.info("-> " + logMessage);
        String key = null;
        
        
        try {
            final String fixDescripcion = fixCarrierReturn(descripcion);
            final String jsonCreate = "{\"description\":\""  + descripcion + "\",\"name\":\"" + version + "\",\"project\":\"" + project +  "\"}";            
            HttpHeaders headers = new HttpHeaders();
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);           
            final HttpEntity<String> entity = (HttpEntity<String>)new HttpEntity((Object)jsonCreate, (MultiValueMap)headers);
            //log.info(jsonCreate);
            final HttpHost host = new HttpHost(JiraClient.hostName, JiraClient.port, "http");
            final ClientHttpRequestFactory requestFactory = (ClientHttpRequestFactory)new HttpComponentsClientHttpRequestFactoryBasicAuth(host);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(JiraClient.userName, JiraClient.password));
            final ResponseEntity<Object> response = (ResponseEntity<Object>)restTemplate.exchange(String.valueOf(JiraClient.jiraURL) + "version/", HttpMethod.POST, (HttpEntity)entity, (Class)Object.class, new Object[0]);
            if (response != null) {
                if (response.getStatusCode().is2xxSuccessful()) {
                    final Map map = (HashMap)response.getBody();
                    key = (String) map.get("name");
                    //log.info("Version creada correctamente: " + key);
                }
            }
            else {
              //  log.error("Error al cerrar la tarea: " + response.getStatusCodeValue() + ":" + response.getBody());
            }
        }
        catch (Exception e) {
            if (e instanceof HttpClientErrorException) {
                log.error(((HttpClientErrorException)e).getResponseBodyAsString());
            }
            else {
                log.error(e.toString());
            }
        }
        //log.info("<- " + logMessage);
        return key;
    }
    
    
    public static String getUserName() {
        return JiraClient.userName;
    }
    
    public static void setUserName(final String userName) {
        JiraClient.userName = userName;
    }
    
    public static String getPassword() {
        return JiraClient.password;
    }
    
    public static void setPassword(final String password) {
        JiraClient.password = password;
    }
    
    private static String fixCarrierReturn(final String text) {
        String text2 = text.replaceAll("\r", " ** ");
        text2 = text2.replaceAll("\n", " ** ");
        return text2;
    }
}
