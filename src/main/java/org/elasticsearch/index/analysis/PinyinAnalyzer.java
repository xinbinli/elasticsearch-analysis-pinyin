package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.analysis.PinyinConfig;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.dic.MonitorTask;

/**
 * Created by IntelliJ IDEA.
 * User: Medcl'
 * Date: 12-5-22
 * Time: 上午10:39
 */
public final class PinyinAnalyzer extends Analyzer {

    private PinyinConfig config;

    public PinyinAnalyzer(PinyinConfig config) {
        this.config=config;
        // MonitorTask.initial();   //启动有效，每次会启动5次线程
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        return new TokenStreamComponents(new PinyinTokenizer(config));
    }

}
