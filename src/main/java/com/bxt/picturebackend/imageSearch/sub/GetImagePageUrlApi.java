package com.bxt.picturebackend.imageSearch.sub;

import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.bxt.picturebackend.exception.BusinessException;
import com.bxt.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.firewall.FirewalledRequest;


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.hutool.poi.excel.sax.AttributeName.r;


@Slf4j
public class GetImagePageUrlApi {
    public static List<String> getUrlList(String imageUrl){
        String imagePageUrl = getImagePageUrl(imageUrl);
        if (imagePageUrl == null || imagePageUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
        }
        String acsToken = "jmM4zyI8OUixvSuWh0sCy4xWbsttVMZb9qcRTmn6SuNWg0vCO7N0s6Lffec+IY5yuqHujHmCctF9BVCGYGH0H5SH/H3VPFUl4O4CP1jp8GoAzuslb8kkQQ4a21Tebge8yhviopaiK66K6hNKGPlWt78xyyJxTteFdXYLvoO6raqhz2yNv50vk4/41peIwba4lc0hzoxdHxo3OBerHP2rfHwLWdpjcI9xeu2nJlGPgKB42rYYVW50+AJ3tQEBEROlg/UNLNxY+6200B/s6Ryz+n7xUptHFHi4d8Vp8q7mJ26yms+44i8tyiFluaZAr66/+wW/KMzOhqhXCNgckoGPX1SSYwueWZtllIchRdsvCZQ8tFJymKDjCf3yI/Lw1oig9OKZCAEtiLTeKE9/CY+Crp8DHa8Tpvlk2/i825E3LuTF8EQfzjcGpVnR00Lb4/8A";
        HttpResponse httpResponse = HttpRequest.get(imagePageUrl)
                .timeout(10000)
                .header("Acs-Token", acsToken)
                .execute();
//        System.out.println("httpResponse = " + httpResponse);
        if (httpResponse.getStatus() != 200) {
            log.error("获取以图搜图页面地址失败，状态码：{}", httpResponse.getStatus());
            throw new RuntimeException("获取以图搜图页面地址失败，请稍后重试");
        }
        Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(httpResponse.body());
        String firstUrl;
        if (matcher.find()) {
            // 提取并替换 \/ 为 /
            firstUrl = matcher.group(1).replace("\\/", "/");
            System.out.println("firstUrl = " + firstUrl);
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
        }

        HttpResponse urlListPage = HttpRequest.get(firstUrl)
                .timeout(10000)
                .header("Acs-Token", acsToken)
                .execute();
//        System.out.println(urlListPage);

        pattern = Pattern.compile("\"thumbUrl\"\\s*:\\s*\"(.*?)\"");
        matcher = pattern.matcher(urlListPage.body());

        List<String> urlList = new java.util.ArrayList<>();
        while (matcher.find()) {
            String thumbUrl = matcher.group(1);
            // 转义 \u0026 -> &
            thumbUrl = thumbUrl.replaceAll("\\\\u0026", "&");
            urlList.add(thumbUrl);
        }
//        System.out.println("urlList = " + urlList);
        return urlList;






    }
    public static String getImagePageUrl(String imageUrl) {
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn","pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        long upTime = System.currentTimeMillis();
        String postUrl = "https://graph.baidu.com/upload?uptime="+ upTime;
        String acsToken = "jmM4zyI8OUixvSuWh0sCy4xWbsttVMZb9qcRTmn6SuNWg0vCO7N0s6Lffec+IY5yuqHujHmCctF9BVCGYGH0H5SH/H3VPFUl4O4CP1jp8GoAzuslb8kkQQ4a21Tebge8yhviopaiK66K6hNKGPlWt78xyyJxTteFdXYLvoO6raqhz2yNv50vk4/41peIwba4lc0hzoxdHxo3OBerHP2rfHwLWdpjcI9xeu2nJlGPgKB42rYYVW50+AJ3tQEBEROlg/UNLNxY+6200B/s6Ryz+n7xUptHFHi4d8Vp8q7mJ26yms+44i8tyiFluaZAr66/+wW/KMzOhqhXCNgckoGPX1SSYwueWZtllIchRdsvCZQ8tFJymKDjCf3yI/Lw1oig9OKZCAEtiLTeKE9/CY+Crp8DHa8Tpvlk2/i825E3LuTF8EQfzjcGpVnR00Lb4/8A";

        try {
            HttpResponse httpResponse=HttpRequest.post(postUrl)
                    .form(formData)
                    .timeout(10000)
                    .header("Acs-Token", acsToken)
                    .execute();
            if (httpResponse.getStatus() != 200) {
                log.error("获取以图搜图页面地址失败，状态码：{}", httpResponse.getStatus());
                throw new RuntimeException("获取以图搜图页面地址失败，请稍后重试");
            }
            String body = httpResponse.body();
            System.out.println("body = " + body);
            Map<String, Object> responseMap = JSONUtil.toBean(body, Map.class);
            System.out.println("responseMap = " + responseMap);
            if (responseMap == null ) {
                log.error("获取以图搜图页面地址失败，响应内容：{}", body);
                throw new RuntimeException("获取以图搜图页面地址失败，请稍后重试");
            }
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
            System.out.println("data = " + data);
            String rawUrl = (String) data.get("url");
            // 对 URL 进行解码
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            // 如果 URL 为空
            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            return searchResultUrl;
        }catch (Exception e) {
            log.error("获取以图搜图页面地址失败，错误信息：{}", e.getMessage());
            throw new RuntimeException("获取以图搜图页面地址失败，请稍后重试");
        }
    }
}
