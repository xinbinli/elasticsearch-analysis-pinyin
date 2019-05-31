package org.elasticsearch.index.dic;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.SpecialPermission;
import org.nlpcn.commons.lang.pinyin.Pinyin;
import org.nlpcn.commons.lang.tire.domain.SmartForest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class Monitor implements Runnable {

    private static final org.apache.logging.log4j.Logger logger = ESPluginLoggerFactory.getLogger(Monitor.class.getName());

    private static CloseableHttpClient httpclient = HttpClients.createDefault();

    public static final String EMPTY = "";
    public static final String SHARP = "#";
    public static final String EQUAL = "=";
    public static final String COMMA = ",";
    public static final String SPACE = " ";

    private SmartForest<String[]> polyphoneDict = null;

    private int maxLen = 2;

    /*
     * 上次更改时间
     */
    private String last_modified;
    /*
     * 资源属性
     */
    private String eTags;

    /*
     * 请求地址
     */
    private String location;

    public Monitor(String location) {
        this.location = location;
        this.last_modified = null;
        this.eTags = null;
    }

    public void run() {
        logger.info(">>>>>>监控文件请求线程启动！");
        SpecialPermission.check();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            this.runUnprivileged();
            return null;
        });
    }

    /**
     * 监控流程：
     * ①向词库服务器发送Head请求
     * ②从响应中获取Last-Modify、ETags字段值，判断是否变化
     * ③如果未变化，休眠1min，返回第①步
     * ④如果有变化，重新加载词典
     * ⑤休眠1min，返回第①步
     */

    public void runUnprivileged() {

        //超时设置
        RequestConfig rc = RequestConfig.custom().setConnectionRequestTimeout(10 * 1000)
                .setConnectTimeout(10 * 1000).setSocketTimeout(15 * 1000).build();

        HttpHead head = new HttpHead(location);
        logger.info(">>>>>> 拼装head {}", head);
        head.setConfig(rc);

        //设置请求头
        if (last_modified != null) {
            head.setHeader("If-Modified-Since", last_modified);
        }
        if (eTags != null) {
            head.setHeader("If-None-Match", eTags);
        }

        CloseableHttpResponse response = null;
        try {

            response = httpclient.execute(head);
            logger.info(">>>>>> 监控文件返回，response = " + response.toString());

            //返回200 才做操作
            if (response.getStatusLine().getStatusCode() == 200) {

                logger.info("last_modified is {}", response.getLastHeader("Last-Modified").getValue());
                logger.info("eTags is {}", response.getLastHeader("ETag").getValue());

                if (((response.getLastHeader("Last-Modified") != null) && !response.getLastHeader("Last-Modified").getValue().equalsIgnoreCase(last_modified))
                        || ((response.getLastHeader("ETag") != null) && !response.getLastHeader("ETag").getValue().equalsIgnoreCase(eTags))) {


                    // 远程词库有更新,需要重新加载词典，并修改last_modified,eTags
                    loadPolyphoneMapping();

                    last_modified = response.getLastHeader("Last-Modified") == null ? null : response.getLastHeader("Last-Modified").getValue();
                    eTags = response.getLastHeader("ETag") == null ? null : response.getLastHeader("ETag").getValue();
                }
            } else if (response.getStatusLine().getStatusCode() == 304) {
                //没有修改，不做操作
                //noop
            } else {
                logger.info("info-remote_ext_dict {} return bad code {}", location, response.getStatusLine().getStatusCode());
            }

        } catch (Exception e) {
            logger.error("error-remote_ext_dict {} error!", e, location);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 更新多音字文件
     */
    public void loadPolyphoneMapping() {

        try {
            logger.info("重新加载词典...");
            URL url = new URL(location);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(new BufferedInputStream(url.openStream()), StandardCharsets.UTF_8));

            polyphoneDict = new SmartForest<>();


            String line;
            while (null != (line = in.readLine())) {
                // line = line.trim();
                if ((line.length() == 0) || line.startsWith(SHARP)) {
                    continue;
                }
                logger.info(">>>>>>line is {}", line);
                String[] pair = line.split(EQUAL);

                if (pair.length < 2) {
                    continue;
                }
                maxLen = maxLen < pair[0].length() ? pair[0].length() : maxLen;

                // polyphoneDict.add(pair[0], pair[1].split(SPACE));

                Pinyin.insertPinyin(pair[0], pair[1].split(SPACE));

            }

            in.close();
            logger.info("重新加载词典完毕...");

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}

