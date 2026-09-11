package com.hrcontract.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class IamClient {
    private final IamProperties properties; private final ObjectMapper mapper;
    public IamClient(IamProperties properties, ObjectMapper mapper) { this.properties = properties; this.mapper = mapper; }
    public JsonNode passwordLogin(String loginName, String password, String deviceInfo) { return postJson("/api/v1/auth/login/password", Map.of("loginName", loginName, "password", password, "deviceInfo", deviceInfo), null); }
    public JsonNode exchangeCode(String code, String verifier) { Map<String, String> form = new LinkedHashMap<>(); form.put("grant_type", "authorization_code"); form.put("client_id", properties.clientId()); if (!properties.clientSecret().isBlank()) form.put("client_secret", properties.clientSecret()); form.put("redirect_uri", properties.redirectUri()); form.put("code", code); if (verifier != null) form.put("code_verifier", verifier); return postForm(properties.tokenPath(), form); }
    public String authorizationUrl(String state, String challenge) { String query = "response_type=code&client_id=" + enc(properties.clientId()) + "&redirect_uri=" + enc(properties.redirectUri()) + "&scope=" + enc("openid profile") + "&state=" + enc(state); if (challenge != null) query += "&code_challenge=" + enc(challenge) + "&code_challenge_method=S256"; return trim(properties.baseUrl()) + properties.authorizePath() + "?" + query; }
    public JsonNode userInfo(String accessToken) { return getJson("/api/v1/auth/session/current", accessToken); }
    public JsonNode proxy(String path, String method, String accessToken, String body) { try { HttpURLConnection c = connection(properties.gatewayBaseUrl(), path); c.setRequestMethod(method); c.setRequestProperty("Authorization", "Bearer " + accessToken); c.setRequestProperty("X-App-Code", properties.appCode()); if (body != null) { c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json"); c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8)); } int status=c.getResponseCode(); String value=read(status >= 400 ? c.getErrorStream() : c.getInputStream()); if(status<200||status>=300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IAM gateway request failed: " + status); return value.isBlank()?mapper.createObjectNode():mapper.readTree(value); } catch (ResponseStatusException e) { throw e; } catch(Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IAM gateway request failed: " + e.getMessage(), e); } }
    private JsonNode postJson(String path, Map<String, ?> body, String token) { return request(path, "POST", "application/json", write(body), token); }
    private JsonNode postForm(String path, Map<String, String> body) { StringBuilder s=new StringBuilder(); body.forEach((k,v)->{if(s.length()>0)s.append('&');s.append(enc(k)).append('=').append(enc(v));}); return request(path,"POST","application/x-www-form-urlencoded",s.toString(),null); }
    private JsonNode getJson(String path, String token) { return request(path,"GET",null,null,token); }
    private JsonNode request(String path,String method,String contentType,String body,String token) { try { HttpURLConnection c=connection(path); c.setRequestMethod(method); if(token!=null)c.setRequestProperty("Authorization","Bearer "+token); if(contentType!=null)c.setRequestProperty("Content-Type",contentType); if(body!=null){c.setDoOutput(true);c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));} int status=c.getResponseCode(); String value=read(status>=400?c.getErrorStream():c.getInputStream()); if(status<200||status>=300) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"IAM request failed: "+status); return mapper.readTree(value); } catch(ResponseStatusException e){throw e;} catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"IAM request failed: "+e.getMessage(),e);} }
    private HttpURLConnection connection(String path) throws Exception { return connection(properties.baseUrl(), path); }
    private HttpURLConnection connection(String baseUrl, String path) throws Exception { return (HttpURLConnection) URI.create(trim(baseUrl)+path).toURL().openConnection(); }
    private String write(Object value){try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException(e);}}
    private static String read(java.io.InputStream in)throws Exception{return in==null?"":new String(in.readAllBytes(),StandardCharsets.UTF_8);}
    private static String enc(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static String trim(String value){return value==null?"":value.replaceAll("/+$","");}
}
