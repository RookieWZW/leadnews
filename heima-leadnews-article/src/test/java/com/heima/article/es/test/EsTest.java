package com.heima.article.es.test;

import com.alibaba.fastjson.JSON;
import com.heima.article.service.ApArticleSearchService;
import com.heima.article.util.HttpClientUtils;
import com.heima.common.common.pojo.EsIndexEntity;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.UserSearchDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.constants.ESIndexConstants;
import com.heima.model.mappers.app.ApArticleContentMapper;
import com.heima.model.mappers.app.ApArticleMapper;
import com.heima.utils.common.ZipUtils;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.Index;



import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class EsTest {

    @Autowired
    private JestClient jestClient;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Test
    public void testSave() throws IOException {

        ArticleHomeDto dto = new ArticleHomeDto();
        dto.setSize(50);
        dto.setTag("__all__");
        List<ApArticle> apArticles = apArticleMapper.loadArticleListByLocation(dto, null);
        for (ApArticle apArticle : apArticles) {
            ApArticleContent apArticleContent = apArticleContentMapper.selectByArticleId(apArticle.getId());

            EsIndexEntity esIndexEntity = new EsIndexEntity();
            esIndexEntity.setChannelId(new Long(apArticle.getChannelId()));
            esIndexEntity.setId(apArticle.getId().longValue());
            esIndexEntity.setContent(ZipUtils.gunzip(apArticleContent.getContent()));
            esIndexEntity.setPublishTime(apArticle.getPublishTime());
            esIndexEntity.setStatus(new Long(1));
            esIndexEntity.setTag("article");
            esIndexEntity.setTitle(apArticle.getTitle());
            Index.Builder builder = new Index.Builder(esIndexEntity);
            builder.id(apArticle.getId().toString());
            builder.refresh(true);
            Index index = builder.index(ESIndexConstants.ARTICLE_INDEX).type(ESIndexConstants.DEFAULT_DOC).build();
            JestResult result = jestClient.execute(index);
            if (result != null && !result.isSucceeded()) {
                throw new RuntimeException(result.getErrorMessage() + "插入更新索引失败!");
            }
        }


    }

    @Autowired
    private  ApArticleSearchService apArticleSearchService;

    @Test
    public void testSearchAssociateByEs()  {
        UserSearchDto dto = new UserSearchDto();
        dto.setSearchWords("java");
        dto.setPageSize(2);
//      String jsonData = null;
//      String url = "http://suggestion.baidu.com/su?wd=学&p=3&cb=window.bdsug.sug#";
//      jsonData = HttpClientUtils.get(url, null,"UTF-8");
//        String[] jsonDataList = jsonData.split("s:");
//      jsonData =  jsonDataList[1].substring(0,jsonDataList[1].length()-3);
//        List maps = (List) JSON.parse(jsonData);
//        System.out.println("-------------"+jsonData);
        apArticleSearchService.searchAssociate(dto);
    }
}
